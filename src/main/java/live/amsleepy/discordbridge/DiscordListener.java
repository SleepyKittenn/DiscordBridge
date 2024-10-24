package live.amsleepy.discordbridge;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter {
    private final JavaPlugin plugin;

    public DiscordListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw();
        String commandPrefix = DiscordBridge.getCommandPrefix();
        if (content.startsWith(commandPrefix)) {
            handleCommand(event, content.substring(commandPrefix.length()).trim());
        }
    }

    private void handleCommand(MessageReceivedEvent event, String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();  // Ensure case insensitivity
        String args = parts.length > 1 ? parts[1] : "";

        System.out.println("Received command: " + cmd + " with args: " + args);

        if ("help".equals(cmd)) {
            sendHelpMessage(event);
            return;
        }

        CommandConfiguration.CommandInfo commandInfo = CommandConfiguration.getCommand(cmd);
        if (commandInfo == null) {
            event.getChannel().sendMessage("Unknown command! Type `" + DiscordBridge.getCommandPrefix() + "help` to see available commands.").queue();
            System.err.println("Unknown command received: " + cmd);
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.getChannel().sendMessage("Member information is unavailable!").queue();
            return;
        }

        boolean hasPermission = CommandConfiguration.userHasRole(member, commandInfo);
        plugin.getLogger().info("Command: " + cmd + " | Member: " + member.getEffectiveName() + " | Roles: " + member.getRoles().stream().map(role -> role.getId()).collect(Collectors.toList()) + " | Has permission: " + hasPermission);

        if (!hasPermission) {
            event.getChannel().sendMessage("You do not have permission to use this command!").queue();
            return;
        }

        // Validate command format
        String format = commandInfo.getFormat();
        int expectedArgsCount = format.split("%").length - 1; // Count the number of format specifiers
        String[] providedArgs = args.split(" ");

        if (providedArgs.length != expectedArgsCount) {
            String correctFormat = "/" + cmd + " " + commandInfo.getFields().stream()
                    .map(field -> "<" + field.getName() + ">")
                    .collect(Collectors.joining(" "));
            event.getChannel().sendMessage("Invalid command format! Correct format: `" + correctFormat + "`").queue();
            return;
        }

        String formattedCommand = String.format(format, (Object[]) providedArgs);
        executeCommandIngame(formattedCommand);

        // Send a confirmation message to the channel and ping the specified role
        event.getChannel().sendMessage("Executed command: " + formattedCommand).queue();

        // Ping the role if specified
        String pingRole = commandInfo.getPingRoleOnExecute();
        Map<String, List<String>> roleGroups = CommandConfiguration.getRoleGroups();
        if (pingRole != null && !pingRole.isEmpty()) {
            if (roleGroups.containsKey(pingRole.toLowerCase())) {
                for (String roleId : roleGroups.get(pingRole.toLowerCase())) {
                    event.getChannel().sendMessage("<@&" + roleId + ">").queue();
                }
            } else {
                event.getChannel().sendMessage("<@&" + pingRole + ">").queue();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName().toLowerCase(); // Ensure case insensitivity
        List<String> args = event.getOptions().stream()
                .map(option -> option.getAsString())
                .collect(Collectors.toList());

        System.out.println("Received slash command: " + cmd + " with args: " + args);

        if ("help".equals(cmd)) {
            sendHelpMessage(event);
            return;
        }

        CommandConfiguration.CommandInfo commandInfo = CommandConfiguration.getCommand(cmd);
        if (commandInfo == null) {
            event.reply("Unknown command! Type `" + DiscordBridge.getCommandPrefix() + "help` to see available commands.").queue();
            System.err.println("Unknown slash command received: " + cmd);
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.reply("Member information is unavailable!").queue();
            return;
        }

        boolean hasPermission = CommandConfiguration.userHasRole(member, commandInfo);
        plugin.getLogger().info("Command: " + cmd + " | Member: " + member.getEffectiveName() + " | Roles: " + member.getRoles().stream().map(role -> role.getId()).collect(Collectors.toList()) + " | Has permission: " + hasPermission);

        if (!hasPermission) {
            event.reply("You do not have permission to use this command!").queue();
            return;
        }

        String format = commandInfo.getFormat();
        if (args.size() != format.split("%").length - 1) {
            String correctFormat = "/" + cmd + " " + commandInfo.getFields().stream()
                    .map(field -> "<" + field.getName() + ">")
                    .collect(Collectors.joining(" "));
            event.reply("Invalid command format! Correct format: `" + correctFormat + "`").queue();
            return;
        }

        String formattedCommand = String.format(format, args.toArray());
        executeCommandIngame(formattedCommand);

        // Send a confirmation message to the channel and ping the specified role
        event.reply("Executed command: " + formattedCommand).queue();

        // Ping the role if specified
        String pingRole = commandInfo.getPingRoleOnExecute();
        Map<String, List<String>> roleGroups = CommandConfiguration.getRoleGroups();
        if (pingRole != null && !pingRole.isEmpty()) {
            if (roleGroups.containsKey(pingRole.toLowerCase())) {
                for (String roleId : roleGroups.get(pingRole.toLowerCase())) {
                    event.getChannel().sendMessage("<@&" + roleId + ">").queue();
                }
            } else {
                event.getChannel().sendMessage("<@&" + pingRole + ">").queue();
            }
        }
    }

    private void sendHelpMessage(MessageReceivedEvent event) {
        List<String> commands = CommandConfiguration.getSlashCommands().stream()
                .map(commandInfo -> "/" + commandInfo.getName() + " " + commandInfo.getFields().stream()
                        .map(field -> "<" + field.getName() + ">")
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.toList());
        String helpMessage = "Available commands:\n" + String.join("\n", commands);
        event.getChannel().sendMessage(helpMessage).queue();
    }

    private void sendHelpMessage(SlashCommandInteractionEvent event) {
        List<String> commands = CommandConfiguration.getSlashCommands().stream()
                .map(commandInfo -> "/" + commandInfo.getName() + " " + commandInfo.getFields().stream()
                        .map(field -> "<" + field.getName() + ">")
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.toList());
        String helpMessage = "Available commands:\n" + String.join("\n", commands);
        event.reply(helpMessage).queue();
    }

    private void executeCommandIngame(String command) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().dispatchCommand(console, command));
    }
}
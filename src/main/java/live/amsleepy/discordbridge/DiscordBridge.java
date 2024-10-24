package live.amsleepy.discordbridge;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public final class DiscordBridge extends JavaPlugin {
    private JDA jda;
    private static String commandPrefix;
    private final String prefix = ChatColor.DARK_PURPLE + "[DiscordBridge] " + ChatColor.WHITE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        String token = getConfig().getString("discord.token");
        if (token == null || token.isEmpty()) {
            getLogger().severe(prefix + "Discord bot token is not specified in the config.yml");
            getPluginLoader().disablePlugin(this);
            return;
        }

        commandPrefix = getConfig().getString("commandPrefix", "!");

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordListener(this))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();

            getLogger().info(prefix + "Discord bot is successfully initialized.");
            registerSlashCommands();

        } catch (InvalidTokenException e) {
            getLogger().severe(prefix + "Invalid Discord bot token: " + e.getMessage());
            getPluginLoader().disablePlugin(this);
        } catch (Exception e) {
            getLogger().severe(prefix + "Failed to initialize Discord bot: " + e.getMessage());
            getPluginLoader().disablePlugin(this);
        }

        // Register the reload command and tab completer
        getCommand("discordbridge").setExecutor(new ReloadCommand(this));
        getCommand("discordbridge").setTabCompleter(new CommandTabCompleter());

        // Register alias /db
        getCommand("db").setExecutor(new ReloadCommand(this));
        getCommand("db").setTabCompleter(new CommandTabCompleter());

        getLogger().info(prefix + "DiscordBridge v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
        getLogger().info(prefix + "DiscordBridge disabled!");
    }

    private void loadConfig() {
        CommandConfiguration.loadConfig(getConfig());
        System.out.println("Configuration loaded");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadConfig();
        registerSlashCommands(); // Register commands after reloading the config
        getLogger().info(prefix + "DiscordBridge plugin configuration reloaded.");
    }

    public static String getCommandPrefix() {
        return commandPrefix;
    }

    public String getPrefix() {
        return prefix;
    }

    private void registerSlashCommands() {
        List<CommandData> commands = CommandConfiguration.getSlashCommands().stream()
                .map(CommandConfiguration::toCommandData)
                .collect(Collectors.toList());

        jda.updateCommands().addCommands(commands).queue(success -> {
            getLogger().info(prefix + "Slash commands registered successfully");
        }, failure -> {
            getLogger().severe(prefix + "Failed to register slash commands: " + failure.getMessage());
        });
    }
}
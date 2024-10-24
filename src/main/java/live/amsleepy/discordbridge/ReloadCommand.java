package live.amsleepy.discordbridge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final String prefix;

    public ReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        if (plugin instanceof DiscordBridge) {
            this.prefix = ((DiscordBridge) plugin).getPrefix();
        } else {
            this.prefix = "[ReloadCommand] ";  // Default prefix if not instance of DiscordBridge
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (plugin instanceof DiscordBridge) {
                ((DiscordBridge) plugin).reloadPluginConfig();
                sender.sendMessage(prefix + "DiscordBridge plugin configuration reloaded.");
            } else {
                sender.sendMessage(prefix + "Failed to reload the configuration.");
            }
            return true;
        }
        return false;
    }
}
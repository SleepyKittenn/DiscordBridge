package live.amsleepy.discordbridge;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections; // Add this import
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandConfiguration {
    private static final Map<String, CommandInfo> commands = new HashMap<>();
    private static final Map<String, List<String>> roleGroups = new HashMap<>();

    public static void loadConfig(FileConfiguration config) {
        // Load role groups
        ConfigurationSection roleGroupSection = config.getConfigurationSection("roleGroups");
        if (roleGroupSection != null) {
            roleGroupSection.getKeys(false).forEach(key -> {
                List<String> roleIds = roleGroupSection.getStringList(key);
                roleGroups.put(key.toLowerCase(), roleIds);
            });
        }

        System.out.println("Loaded roleGroups: " + roleGroups);

        // Load commands
        ConfigurationSection commandSection = config.getConfigurationSection("commands");
        if (commandSection == null) {
            System.err.println("No commands section found in config!");
            return;
        }

        commandSection.getKeys(false).forEach(key -> {
            ConfigurationSection cmdSection = commandSection.getConfigurationSection(key);
            if (cmdSection == null) return;

            String name = key.toLowerCase();  // Ensure lowercase key for case-insensitivity
            String description = cmdSection.getString("description");
            List<String> roles = cmdSection.getStringList("roles");
            String format = cmdSection.getString("format");
            String pingRoleOnExecute = cmdSection.getString("pingroleonexecute");

            List<CommandField> fields = cmdSection.getMapList("fields").stream()
                    .map(field -> new CommandField(
                            (String) field.get("name"),
                            (String) field.get("type"),
                            (String) field.get("description"))
                    )
                    .collect(Collectors.toList());

            commands.put(name, new CommandInfo(name, description, roles, format, fields, pingRoleOnExecute));

            // Add logging to review the loaded command
            System.out.println("Loaded command: " + name + " with roles: " + roles);
        });

        System.out.println("Total commands loaded: " + commands.size());
    }

    public static CommandInfo getCommand(String command) {
        CommandInfo cmdInfo = commands.get(command.toLowerCase()); // Ensure case insensitivity
        if (cmdInfo == null) {
            System.err.println("Command not found: " + command);
        }
        return cmdInfo;
    }

    public static List<CommandInfo> getSlashCommands() {
        return commands.values().stream()
                .collect(Collectors.toList());
    }

    public static CommandData toCommandData(CommandInfo command) {
        return Commands.slash(command.getName(), command.getDescription())
                .addOptions(command.getFields().stream().map(CommandField::toOptionData).collect(Collectors.toList()));
    }

    // Enhanced role checking with detailed logging and role group resolution
    public static boolean userHasRole(Member member, CommandInfo command) {
        List<String> memberRoleIDs = member.getRoles().stream().map(role -> role.getId()).collect(Collectors.toList());
        System.out.println("Member roles: " + memberRoleIDs);
        System.out.println("Required roles for command " + command.getName() + ": " + command.getRoles());

        // Resolve group names to role IDs
        HashSet<String> requiredRoleIDs = new HashSet<>();
        for (String roleNameOrID : command.getRoles()) {
            if (roleGroups.containsKey(roleNameOrID.toLowerCase())) {
                requiredRoleIDs.addAll(roleGroups.get(roleNameOrID.toLowerCase()));
            } else {
                requiredRoleIDs.add(roleNameOrID);
            }
        }

        System.out.println("Translated required roles for command " + command.getName() + ": " + requiredRoleIDs);

        boolean hasRole = member.getRoles().stream().anyMatch(role -> requiredRoleIDs.contains(role.getId()));
        System.out.println("User has required role: " + hasRole);

        return hasRole;
    }

    // Public method to get roleGroups
    public static Map<String, List<String>> getRoleGroups() {
        return Collections.unmodifiableMap(roleGroups);
    }

    public static class CommandInfo {
        private final String name;
        private final String description;
        private final List<String> roles;
        private final String format;
        private final List<CommandField> fields;
        private final String pingRoleOnExecute;

        public CommandInfo(String name, String description, List<String> roles, String format, List<CommandField> fields, String pingRoleOnExecute) {
            this.name = name;
            this.description = description;
            this.roles = roles;
            this.format = format;
            this.fields = fields;
            this.pingRoleOnExecute = pingRoleOnExecute;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getRoles() { return roles; }
        public String getFormat() { return format; }
        public List<CommandField> getFields() { return fields; }
        public String getPingRoleOnExecute() { return pingRoleOnExecute; }
    }

    public static class CommandField {
        private final String name;
        private final String type;
        private final String description;

        public CommandField(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getDescription() { return description; }

        public net.dv8tion.jda.api.interactions.commands.build.OptionData toOptionData() {
            return new net.dv8tion.jda.api.interactions.commands.build.OptionData(
                    net.dv8tion.jda.api.interactions.commands.OptionType.valueOf(type.toUpperCase()), name, description, true
            );
        }
    }
}
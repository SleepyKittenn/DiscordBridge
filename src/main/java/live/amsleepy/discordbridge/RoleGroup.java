package live.amsleepy.discordbridge;

import java.util.List;

public class RoleGroup {
    private final String name;
    private final List<String> roleIds;

    public RoleGroup(String name, List<String> roleIds) {
        this.name = name;
        this.roleIds = roleIds;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }
}
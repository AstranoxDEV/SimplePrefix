package de.astranox.simpleprefix.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.entity.Player;

public class LuckPermsWrapper {

    private final LuckPerms luckPerms;

    public LuckPermsWrapper() {
        this.luckPerms = LuckPermsProvider.get();
    }

    public String getPrimaryGroup(Player player) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if (user == null) {
            return "default";
        }
        return user.getPrimaryGroup();
    }

    public boolean createGroup(String groupName) {
        if (groupExists(groupName)) {
            return false;
        }

        try {
            luckPerms.getGroupManager().createAndLoadGroup(groupName).join();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setGroupPrefix(String groupName, String prefix, int priority) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group == null) {
            return false;
        }

        group.data().clear(node -> node instanceof PrefixNode);

        if (prefix != null && !prefix.isEmpty()) {
            group.data().add(PrefixNode.builder(prefix, priority).build());
        }

        luckPerms.getGroupManager().saveGroup(group);
        return true;
    }

    public boolean setGroupSuffix(String groupName, String suffix, int priority) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group == null) {
            return false;
        }

        group.data().clear(node -> node instanceof SuffixNode);

        if (suffix != null && !suffix.isEmpty()) {
            group.data().add(SuffixNode.builder(suffix, priority).build());
        }

        luckPerms.getGroupManager().saveGroup(group);
        return true;
    }

    public boolean deleteGroup(String groupName) {
        if (!groupExists(groupName)) {
            return false;
        }

        if (groupName.equalsIgnoreCase("default")) {
            return false;
        }

        try {
            luckPerms.getGroupManager().deleteGroup(luckPerms.getGroupManager().getGroup(groupName)).join();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean groupExists(String groupName) {
        return luckPerms.getGroupManager().getGroup(groupName) != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}

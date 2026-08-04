package fastui.yure.config;

public record GroupItem(String modId, String groupId, String configName, boolean expanded) {
    public boolean isSameTarget(String modId, String groupId, String configName) {
        return this.modId.equals(modId) && this.groupId.equals(groupId) && this.configName.equals(configName);
    }

    public GroupItem withExpanded(boolean expanded) {
        return new GroupItem(this.modId, this.groupId, this.configName, expanded);
    }
}

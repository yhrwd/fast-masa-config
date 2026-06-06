package fastui.yure.client.index;

import fi.dy.masa.malilib.config.IConfigBase;

public record ConfigIndexEntry(
        String modId,
        String modName,
        String groupId,
        String groupName,
        String configName,
        String displayName,
        IConfigBase config
) {
    public String manualId() {
        return this.modId + "/" + this.groupId + "/" + this.configName;
    }
}

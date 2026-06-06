package fastui.yure.config;

import fi.dy.masa.malilib.config.ConfigManager;

public final class MaLiLibConfigCommitter implements ConfigCommitter {
    @Override
    public void commit(String modId) {
        // modId 是被修改配置所属的目标模组，MaLiLib 会据此保存并刷新对应模组的配置状态。
        ConfigManager.getInstance().onConfigsChanged(modId);
    }
}

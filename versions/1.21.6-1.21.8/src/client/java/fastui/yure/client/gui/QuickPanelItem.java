package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;

/**
 * 快捷面板中的可操作项。
 * 快捷方式页来自用户收藏，已启用页来自实时扫描到的布尔配置，两者用同一套绘制和点击逻辑。
 */
public record QuickPanelItem(ShortcutEntry shortcut, ConfigIndexEntry configEntry) {
    public static QuickPanelItem fromShortcut(ResolvedShortcut shortcut) {
        return new QuickPanelItem(shortcut.shortcut(), shortcut.configEntry());
    }

    public static QuickPanelItem fromEnabledConfig(ConfigIndexEntry entry) {
        return new QuickPanelItem(new ShortcutEntry(entry.modId(), entry.groupId(), entry.configName(), "", ShortcutControlType.TOGGLE, 1.0, null, null), entry);
    }
}

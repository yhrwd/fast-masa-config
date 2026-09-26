package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.client.shortcut.ResolvedShortcut;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;

/**
 * 快捷面板中的可操作项。
 * 快捷方式页和已启用页共用同一绘制与交互模型。
 */
public record QuickPanelItem(ShortcutEntry shortcut, ConfigIndexEntry configEntry) {
    public static QuickPanelItem fromShortcut(ResolvedShortcut shortcut) {
        return new QuickPanelItem(shortcut.shortcut(), shortcut.configEntry());
    }

    public static QuickPanelItem fromEnabledConfig(ConfigIndexEntry entry) {
        return new QuickPanelItem(new ShortcutEntry(entry.modId(), entry.groupId(), entry.configName(), "",
                ShortcutControlType.TOGGLE, 1.0, null, null), entry);
    }
}

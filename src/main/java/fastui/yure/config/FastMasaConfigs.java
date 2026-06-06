package fastui.yure.config;

import com.google.common.collect.ImmutableList;
import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

import java.util.List;

public final class FastMasaConfigs {
    private FastMasaConfigs() {
    }

    public static final class Generic {
        private static final String GENERIC_KEY = FastMasaConfig.MOD_ID + ".config.generic";
        private static final KeybindSettings QUICK_CONFIG_KEY_SETTINGS = KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, true, false, true);

        public static final ConfigHotkey OPEN_QUICK_CONFIG = new ConfigHotkey("openQuickConfig", "LEFT_ALT,C", QUICK_CONFIG_KEY_SETTINGS,
                "按住时显示 Fast Masa Config 快捷配置面板。", "Open Quick Config").apply(GENERIC_KEY);
        public static final ConfigInteger PANEL_WIDTH = new ConfigInteger("panelWidth", 260, 220, 420,
                "快捷配置面板宽度。", "Panel Width").apply(GENERIC_KEY);
        public static final ConfigInteger PANEL_MAX_HEIGHT = new ConfigInteger("panelMaxHeight", 240, 120, 520,
                "快捷配置面板最大高度。超过该高度后列表滚动显示。", "Panel Max Height").apply(GENERIC_KEY);
        public static final ConfigDouble PANEL_SCALE = new ConfigDouble("panelScale", 1.0, 0.5, 2.5,
                "快捷配置面板缩放。", "Panel Scale").apply(GENERIC_KEY);
        public static final ConfigDouble PANEL_OPACITY = new ConfigDouble("panelOpacity", 0.72, 0.2, 1.0,
                "快捷配置面板背景透明度。", "Panel Opacity").apply(GENERIC_KEY);
        public static final ConfigBoolean SHOW_SCAN_SUMMARY = new ConfigBoolean("showScanSummary", true,
                "是否在快捷面板中显示配置扫描摘要。", "Show Scan Summary").apply(GENERIC_KEY);
        public static final ConfigBoolean RELEASE_TO_CLOSE = new ConfigBoolean("releaseToClose", true,
                "松开快捷配置热键时自动关闭面板。", "Release To Close").apply(GENERIC_KEY);
        public static final ConfigBoolean CLOSE_ON_INVENTORY_KEY = new ConfigBoolean("closeOnInventoryKey", true,
                "按背包键或 ESC 时关闭快捷配置面板。", "Close On Inventory Key").apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                OPEN_QUICK_CONFIG,
                PANEL_WIDTH,
                PANEL_MAX_HEIGHT,
                PANEL_SCALE,
                PANEL_OPACITY,
                SHOW_SCAN_SUMMARY,
                RELEASE_TO_CLOSE,
                CLOSE_ON_INVENTORY_KEY
        );

        public static final List<IHotkey> HOTKEY_LIST = ImmutableList.of(OPEN_QUICK_CONFIG);

        private Generic() {
        }
    }
}

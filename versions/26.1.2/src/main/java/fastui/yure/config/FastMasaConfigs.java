package fastui.yure.config;

import com.google.common.collect.ImmutableList;
import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
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
                private static final KeybindSettings QUICK_CONFIG_KEY_SETTINGS = KeybindSettings
                                .create(KeybindSettings.Context.INGAME, KeyAction.PRESS, true, true, false, true);

                public static final ConfigHotkey OPEN_QUICK_CONFIG = new ConfigHotkey("openQuickConfig", "RIGHT_SHIFT",
                                QUICK_CONFIG_KEY_SETTINGS,
                                "按住时显示 Fast Masa Config 快捷配置面板。", "Open Quick Config").apply(GENERIC_KEY);
                public static final ConfigBoolean RELEASE_TO_CLOSE = new ConfigBoolean("releaseToClose", true,
                                "松开快捷配置热键时自动关闭面板。", "Release To Close").apply(GENERIC_KEY);
                public static final ConfigBoolean CLOSE_ON_INVENTORY_KEY = new ConfigBoolean("closeOnInventoryKey",
                                true,
                                "按背包键或 ESC 时关闭快捷配置面板。", "Close On Inventory Key").apply(GENERIC_KEY);
                public static final ConfigInteger FLOATING_BACKGROUND_ALPHA = new ConfigInteger(
                                "floatingBackgroundAlpha", 216, 0, 255, true,
                                "悬浮菜单背景透明度。", "Floating Menu Background Opacity").apply(GENERIC_KEY);
                public static final ConfigBoolean BLOCK_BREAK_INDICATOR = new ConfigBoolean("blockBreakIndicator", true,
                                "用动态方块描边替换原版挖掘裂纹动画。", "Replace Block Break Overlay").apply(GENERIC_KEY);
                public static final ConfigBoolean BLOCK_BREAK_LINES = new ConfigBoolean("blockBreakLines", true,
                                "显示挖掘动画的方块描边。", "Render Break Outline").apply(GENERIC_KEY);
                public static final ConfigBoolean BLOCK_BREAK_SIDES = new ConfigBoolean("blockBreakSides", true,
                                "显示挖掘动画的半透明填充面。", "Render Break Sides").apply(GENERIC_KEY);
                public static final ConfigBoolean BLOCK_BREAK_REMOTE = new ConfigBoolean("blockBreakRemote", true,
                                "显示多人游戏中服务器同步的其它玩家挖掘进度。", "Render Remote Break Progress").apply(GENERIC_KEY);
                public static final ConfigInteger BLOCK_BREAK_LINE_WIDTH = new ConfigInteger("blockBreakLineWidth", 3, 1,
                                8, true, "挖掘动画描边宽度。", "Break Outline Width").apply(GENERIC_KEY);
                public static final ConfigColor BLOCK_BREAK_START_LINE = new ConfigColor("blockBreakStartLine", "#9619FC19",
                                "挖掘开始时的描边颜色。", "Start Outline Color").apply(GENERIC_KEY);
                public static final ConfigColor BLOCK_BREAK_END_LINE = new ConfigColor("blockBreakEndLine", "#96FF1919",
                                "挖掘完成时的描边颜色。", "End Outline Color").apply(GENERIC_KEY);
                public static final ConfigColor BLOCK_BREAK_START_SIDE = new ConfigColor("blockBreakStartSide", "#9619FC19",
                                "挖掘开始时的填充颜色。", "Start Fill Color").apply(GENERIC_KEY);
                public static final ConfigColor BLOCK_BREAK_END_SIDE = new ConfigColor("blockBreakEndSide", "#96FF1919",
                                "挖掘完成时的填充颜色。", "End Fill Color").apply(GENERIC_KEY);

                @SuppressWarnings("nullness")
                private static final @org.jetbrains.annotations.NotNull IConfigBase[] OPTION_ARRAY = createOptionArray();

                @SuppressWarnings("nullness")
                private static @org.jetbrains.annotations.NotNull IConfigBase[] createOptionArray() {
                        return new @org.jetbrains.annotations.NotNull IConfigBase[] {
                                        OPEN_QUICK_CONFIG,
                                        RELEASE_TO_CLOSE,
                                        CLOSE_ON_INVENTORY_KEY,
                                        FLOATING_BACKGROUND_ALPHA,
                                        BLOCK_BREAK_INDICATOR,
                                        BLOCK_BREAK_LINES,
                                        BLOCK_BREAK_SIDES,
                                        BLOCK_BREAK_REMOTE,
                                        BLOCK_BREAK_LINE_WIDTH,
                                        BLOCK_BREAK_START_LINE,
                                        BLOCK_BREAK_END_LINE,
                                        BLOCK_BREAK_START_SIDE,
                                        BLOCK_BREAK_END_SIDE
                        };
                }

                public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.copyOf(OPTION_ARRAY);

                public static final List<IHotkey> HOTKEY_LIST = List.of(OPEN_QUICK_CONFIG);

                private Generic() {
                }
        }
}

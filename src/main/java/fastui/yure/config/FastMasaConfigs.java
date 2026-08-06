package fastui.yure.config;

import com.google.common.collect.ImmutableList;
import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
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

                public static final ConfigHotkey OPEN_QUICK_CONFIG = new ConfigHotkey("openQuickConfig", "LEFT_ALT,C",
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

                @SuppressWarnings("nullness")
                private static final @org.jetbrains.annotations.NotNull IConfigBase[] OPTION_ARRAY = createOptionArray();

                @SuppressWarnings("nullness")
                private static @org.jetbrains.annotations.NotNull IConfigBase[] createOptionArray() {
                        return new @org.jetbrains.annotations.NotNull IConfigBase[] {
                                        OPEN_QUICK_CONFIG,
                                        RELEASE_TO_CLOSE,
                                        CLOSE_ON_INVENTORY_KEY,
                                        FLOATING_BACKGROUND_ALPHA
                        };
                }

                public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.copyOf(OPTION_ARRAY);

                public static final List<IHotkey> HOTKEY_LIST = List.of(OPEN_QUICK_CONFIG);

                private Generic() {
                }
        }
}

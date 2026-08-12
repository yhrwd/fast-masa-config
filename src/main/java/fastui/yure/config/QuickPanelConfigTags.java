package fastui.yure.config;

import java.util.Set;

/** Pure allowlist for own settings that may appear in shortcut groups. */
public final class QuickPanelConfigTags {
    private static final Set<String> NAMES = Set.of(
            "entityRenderFilter",
            "entityRenderWhitelist",
            "blockBreakIndicator",
            "blockBreakLines",
            "blockBreakSides",
            "blockBreakRemote");

    private QuickPanelConfigTags() {
    }

    public static boolean contains(String configName) {
        return NAMES.contains(configName);
    }
}

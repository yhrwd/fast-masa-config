package fastui.yure.client.scan;

import fi.dy.masa.malilib.config.gui.ButtonPressDirtyListenerSimple;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IConfigInfoProvider;
import fastui.yure.client.scan.methodbacked.data.DataManager;
import fastui.yure.client.scan.methodbacked.gui.MethodBackedScreen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigGuiGroupScannerTest {
    @Test
    void scansDirectStaticTabSelector() {
        DirectTabScreen.tab = DirectTab.GENERIC;
        TestConfigGui configGui = new TestConfigGui(() -> switch (DirectTabScreen.tab) {
            case GENERIC -> wrappers(new ConfigBoolean("genericOption", true));
            case HOTKEYS -> wrappers(new ConfigBoolean("hotkeyOption", true));
        });

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(new DirectTabScreen(),
                configGui);

        assertEquals(DirectTab.GENERIC, DirectTabScreen.tab);
        assertEquals(List.of("GENERIC", "HOTKEYS"), groups.stream().map(group -> group.id()).toList());
        assertEquals(List.of("genericOption"), configNames(groups.get(0)));
        assertEquals(List.of("hotkeyOption"), configNames(groups.get(1)));
    }

    @Test
    void scansNestedStaticSettingsCategoryAndSkipsAggregateAllGroup() {
        NestedSettingsScreen.SETTING.category = Category.FEATURES;
        NestedSettingsScreen.SETTING.sortingStrategy = SortingStrategy.ALPHABET;
        TestConfigGui configGui = new TestConfigGui(() -> switch (NestedSettingsScreen.SETTING.category) {
            case ALL -> wrappers(
                    new ConfigBoolean("featureOption", true),
                    new ConfigBoolean("mcTweakOption", true),
                    new ConfigBoolean("settingOption", true));
            case FEATURES -> wrappers(new ConfigBoolean("featureOption", true));
            case MC_TWEAKS -> wrappers(new ConfigBoolean("mcTweakOption", true));
            case SETTING -> wrappers(new ConfigBoolean("settingOption", true));
        });

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(new NestedSettingsScreen(),
                configGui);

        assertEquals(Category.FEATURES, NestedSettingsScreen.SETTING.category);
        assertEquals(SortingStrategy.ALPHABET, NestedSettingsScreen.SETTING.sortingStrategy);
        assertEquals(List.of("FEATURES", "MC_TWEAKS", "SETTING"), groups.stream().map(group -> group.id()).toList());
        assertEquals(List.of("featureOption"), configNames(groups.get(0)));
        assertEquals(List.of("mcTweakOption"), configNames(groups.get(1)));
        assertEquals(List.of("settingOption"), configNames(groups.get(2)));
    }

    @Test
    void fallsBackToDefaultWhenEnumSelectorDoesNotChangeVisibleConfigs() {
        SortingOnlyScreen.tab = SortingStrategy.ALPHABET;
        TestConfigGui configGui = new TestConfigGui(() -> wrappers(new ConfigBoolean("sameOption", true)));

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(new SortingOnlyScreen(),
                configGui);

        assertEquals(SortingStrategy.ALPHABET, SortingOnlyScreen.tab);
        assertEquals(List.of("default"), groups.stream().map(group -> group.id()).toList());
        assertEquals(List.of("sameOption"), configNames(groups.get(0)));
    }

    @Test
    void scansIntegerSelectedIndexBackedListGroups() {
        IndexedListScreen screen = new IndexedListScreen();
        screen.selectedIndex = 1;
        TestConfigGui configGui = new TestConfigGui(() -> switch (screen.selectedIndex) {
            case 0 -> wrappers(new ConfigBoolean("genericOption", true));
            case 1 -> wrappers(new ConfigBoolean("toolsOption", true));
            case 2 -> wrappers(new ConfigBoolean("scriptOption", true));
            default -> List.of();
        });

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(screen, configGui);

        assertEquals(1, screen.selectedIndex);
        assertEquals(List.of("generic", "tools", "script"), groups.stream().map(group -> group.id()).toList());
        assertEquals(List.of("Generic", "Tools", "Scripts"),
                groups.stream().map(group -> group.displayName()).toList());
        assertEquals(List.of("scriptOption"), configNames(groups.get(2)));
    }

    @Test
    void scansStaticMethodBackedEnumGroups() {
        DataManager.setConfigGuiTab(MethodBackedScreen.ConfigGuiTab.GENERIC);
        TestConfigGui configGui = new TestConfigGui(() -> switch (DataManager.getConfigGuiTab()) {
            case GENERIC -> wrappers(new ConfigBoolean("genericOption", true));
            case VISUALS -> wrappers(new ConfigBoolean("visualOption", true));
        });

        List<ConfigGuiGroupScanner.Group> groups = ConfigGuiGroupScanner.collectGroups(new MethodBackedScreen(),
                configGui);

        assertEquals(MethodBackedScreen.ConfigGuiTab.GENERIC, DataManager.getConfigGuiTab());
        assertEquals(List.of("GENERIC", "VISUALS"), groups.stream().map(group -> group.id()).toList());
        assertEquals(List.of("Generic", "Visuals"), groups.stream().map(group -> group.displayName()).toList());
        assertEquals(List.of("visualOption"), configNames(groups.get(1)));
    }

    private static List<String> configNames(ConfigGuiGroupScanner.Group group) {
        return group.configs().stream()
                .map(wrapper -> wrapper.getConfig())
                .map(config -> config == null ? "" : config.getName())
                .toList();
    }

    private static List<GuiConfigsBase.ConfigOptionWrapper> wrappers(ConfigBoolean... configs) {
        return GuiConfigsBase.ConfigOptionWrapper.createFor(List.of(configs));
    }

    private static final class TestConfigGui implements IConfigGui {
        private final Supplier<List<GuiConfigsBase.ConfigOptionWrapper>> configs;

        private TestConfigGui(Supplier<List<GuiConfigsBase.ConfigOptionWrapper>> configs) {
            this.configs = configs;
        }

        @Override
        public String getModId() {
            return "test";
        }

        @Override
        public void clearOptions() {
        }

        @Override
        public List<GuiConfigsBase.ConfigOptionWrapper> getConfigs() {
            return this.configs.get();
        }

        @Override
        public ButtonPressDirtyListenerSimple getButtonPressListener() {
            return null;
        }

        @Override
        public IConfigInfoProvider getHoverInfoProvider() {
            return null;
        }
    }

    private static final class DirectTabScreen {
        private static DirectTab tab = DirectTab.GENERIC;
    }

    private enum DirectTab {
        GENERIC,
        HOTKEYS;

        @SuppressWarnings("unused")
        public String getDisplayName() {
            return this.name().toLowerCase();
        }
    }

    private static final class NestedSettingsScreen {
        private static final Setting SETTING = new Setting();
    }

    private static final class Setting {
        private Category category = Category.FEATURES;
        private SortingStrategy sortingStrategy = SortingStrategy.ALPHABET;
    }

    private enum Category {
        ALL,
        FEATURES,
        MC_TWEAKS,
        SETTING;

        @SuppressWarnings("unused")
        public String getDisplayName() {
            return this.name().toLowerCase();
        }
    }

    private static final class SortingOnlyScreen {
        private static SortingStrategy tab = SortingStrategy.ALPHABET;
    }

    private enum SortingStrategy {
        ALPHABET,
        MOST_COMMONLY_USED
    }

    private static final class IndexedListScreen {
        @SuppressWarnings("unused")
        private final List<TestListGroup> lists = List.of(
                new TestListGroup("generic", "Generic"),
                new TestListGroup("tools", "Tools"),
                new TestListGroup("script", "Scripts"));
        private int selectedIndex = 0;
    }

    private record TestListGroup(String name, String title) {
        @SuppressWarnings("unused")
        public String getName() {
            return this.name;
        }

        @SuppressWarnings("unused")
        public String getTitleDisplayName() {
            return this.title;
        }
    }
}

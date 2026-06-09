package fastui.yure.client.scan.methodbacked.data;

import fastui.yure.client.scan.methodbacked.gui.MethodBackedScreen;

public final class DataManager {
    private static MethodBackedScreen.ConfigGuiTab configGuiTab = MethodBackedScreen.ConfigGuiTab.GENERIC;

    private DataManager() {
    }

    public static MethodBackedScreen.ConfigGuiTab getConfigGuiTab() {
        return configGuiTab;
    }

    public static void setConfigGuiTab(MethodBackedScreen.ConfigGuiTab tab) {
        configGuiTab = tab;
    }
}

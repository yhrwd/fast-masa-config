package fastui.yure.client.scan.methodbacked.gui;

public final class MethodBackedScreen {
    public enum ConfigGuiTab {
        GENERIC("Generic"),
        VISUALS("Visuals");

        private final String displayName;

        ConfigGuiTab(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }
}

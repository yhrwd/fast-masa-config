package fastui.yure.client.gui;

import fastui.yure.FastMasaConfig;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ShortcutConfigStore;
import fastui.yure.config.ShortcutControlType;
import fastui.yure.config.ShortcutEntry;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.options.ConfigBoolean;

final class ShortcutToggleConfig extends ConfigBoolean {
    private final ConfigIndexEntry entry;
    private final Runnable changeListener;

    ShortcutToggleConfig(ConfigIndexEntry entry, Runnable changeListener) {
        super(entry.manualId(), ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName()), entry.manualId(), entry.displayName(), entry.displayName());
        this.entry = entry;
        this.changeListener = changeListener;
        this.setComment(String.format("%s / %s\n%s", entry.modName(), entry.groupName(), entry.manualId()));
    }

    @Override
    public boolean getBooleanValue() {
        return ShortcutConfigStore.containsTarget(this.entry.modId(), this.entry.groupId(), this.entry.configName());
    }

    @Override
    public boolean isModified() {
        return this.getBooleanValue();
    }

    @Override
    public void resetToDefault() {
        this.setBooleanValue(false);
    }

    @Override
    public void setBooleanValue(boolean value) {
        boolean oldValue = this.getBooleanValue();

        if (value) {
            addShortcut(this.entry);
        } else {
            ShortcutConfigStore.removeTarget(this.entry.modId(), this.entry.groupId(), this.entry.configName());
        }

        if (oldValue != this.getBooleanValue()) {
            ConfigManager.getInstance().onConfigsChanged(FastMasaConfig.MOD_ID);
            this.changeListener.run();
        }
    }

    @Override
    public String getStringValue() {
        return Boolean.toString(this.getBooleanValue());
    }

    @Override
    public String getDefaultStringValue() {
        return "false";
    }

    @Override
    public void setValueFromString(String value) {
        this.setBooleanValue(Boolean.parseBoolean(value));
    }

    private static void addShortcut(ConfigIndexEntry entry) {
        if (ShortcutConfigStore.containsTarget(entry.modId(), entry.groupId(), entry.configName())) {
            return;
        }

        ShortcutConfigStore.add(new ShortcutEntry(
                entry.modId(),
                entry.groupId(),
                entry.configName(),
                "",
                entry.config().getType() == ConfigType.BOOLEAN ? ShortcutControlType.TOGGLE : ShortcutControlType.SLIDER,
                defaultStep(entry.config().getType()),
                null,
                null
        ));
    }

    private static double defaultStep(ConfigType type) {
        return type == ConfigType.INTEGER ? 1.0 : 0.05;
    }
}

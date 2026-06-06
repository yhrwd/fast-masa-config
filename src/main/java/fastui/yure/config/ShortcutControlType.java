package fastui.yure.config;

public enum ShortcutControlType {
    TOGGLE("toggle"),
    SLIDER("slider");

    private final String id;

    ShortcutControlType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static ShortcutControlType fromId(String id) {
        for (ShortcutControlType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }

        return TOGGLE;
    }
}

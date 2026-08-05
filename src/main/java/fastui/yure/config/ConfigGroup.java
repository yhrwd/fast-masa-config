package fastui.yure.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigGroup {
    private final String id;
    private String name;
    private boolean system;
    private boolean hidden;
    private boolean collapsed;
    private int x;
    private int y;
    private final List<GroupItem> items = new ArrayList<>();

    ConfigGroup(String id, String name, boolean system) {
        this.id = id;
        this.name = name;
        this.system = system;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public boolean system() {
        return this.system;
    }

    public boolean hidden() {
        return this.hidden;
    }

    public boolean collapsed() {
        return this.collapsed;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public List<GroupItem> items() {
        return Collections.unmodifiableList(this.items);
    }

    void rename(String name) {
        this.name = name;
    }

    void setSystem(boolean system) {
        this.system = system;
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    void setWindowState(boolean collapsed, int x, int y) {
        this.collapsed = collapsed;
        this.x = x;
        this.y = y;
    }

    boolean addItem(GroupItem item) {
        if (this.items.stream().anyMatch(existing -> existing.isSameTarget(item.modId(), item.groupId(), item.configName()))) {
            return false;
        }

        this.items.add(item);
        return true;
    }

    boolean removeItem(int index) {
        if (index < 0 || index >= this.items.size()) {
            return false;
        }

        this.items.remove(index);
        return true;
    }

    boolean moveItem(int index, int offset) {
        int target = index + offset;

        if (index < 0 || index >= this.items.size() || target < 0 || target >= this.items.size()) {
            return false;
        }

        Collections.swap(this.items, index, target);
        return true;
    }

    boolean setItemExpanded(int index, boolean expanded) {
        if (index < 0 || index >= this.items.size()) {
            return false;
        }

        this.items.set(index, this.items.get(index).withExpanded(expanded));
        return true;
    }
}

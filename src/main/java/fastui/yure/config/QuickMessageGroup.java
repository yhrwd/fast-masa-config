package fastui.yure.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 快捷消息的独立悬浮窗口分组。 */
public final class QuickMessageGroup {
    private final String id;
    private String name;
    private boolean hidden;
    private boolean collapsed;
    private int x;
    private int y;
    private final List<QuickMessage> messages = new ArrayList<>();

    QuickMessageGroup(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
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

    public List<QuickMessage> messages() {
        return Collections.unmodifiableList(this.messages);
    }

    void rename(String name) {
        this.name = name;
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    void setWindowState(boolean collapsed, int x, int y) {
        this.collapsed = collapsed;
        this.x = x;
        this.y = y;
    }

    boolean addMessage(QuickMessage message) {
        if (message == null || this.messages.stream().anyMatch(existing -> existing.id().equals(message.id()))) {
            return false;
        }
        this.messages.add(message);
        return true;
    }

    boolean updateMessage(String messageId, QuickMessage message) {
        if (message == null || messageId == null || !messageId.equals(message.id())) {
            return false;
        }
        for (int index = 0; index < this.messages.size(); index++) {
            if (this.messages.get(index).id().equals(messageId)) {
                this.messages.set(index, message);
                return true;
            }
        }
        return false;
    }

    boolean removeMessage(int index) {
        if (index < 0 || index >= this.messages.size()) {
            return false;
        }
        this.messages.remove(index);
        return true;
    }

    boolean moveMessage(int index, int offset) {
        int target = index + offset;
        if (index < 0 || index >= this.messages.size() || target < 0 || target >= this.messages.size()) {
            return false;
        }
        Collections.swap(this.messages, index, target);
        return true;
    }
}

package fastui.yure.config;

import java.util.UUID;

/** 一条可在快捷面板中直接发送的聊天消息或客户端指令。 */
public record QuickMessage(String id, String label, String content) {
    public QuickMessage {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        if (content.startsWith("/") && content.substring(1).isBlank()) {
            throw new IllegalArgumentException("指令内容不能为空");
        }
        label = label == null ? "" : label.trim();
    }

    public QuickMessage(String label, String content) {
        this(UUID.randomUUID().toString(), label, content);
    }

    public String displayName() {
        return this.label.isBlank() ? this.content : this.label;
    }

    public boolean isCommand() {
        return this.content.startsWith("/");
    }
}

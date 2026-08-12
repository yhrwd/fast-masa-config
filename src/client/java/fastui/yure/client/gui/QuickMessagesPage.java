package fastui.yure.client.gui;

import fastui.yure.config.QuickMessage;
import fastui.yure.config.QuickMessageGroup;

import java.util.List;
import java.util.Locale;

/** 快捷消息页的筛选逻辑，保证全屏页和后续其它入口使用同一规则。 */
final class QuickMessagesPage {
    private QuickMessagesPage() {
    }

    static List<QuickMessage> filter(QuickMessageGroup group, String filter) {
        if (group == null) {
            return List.of();
        }
        String normalized = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return group.messages().stream()
                .filter(message -> normalized.isBlank()
                        || (message.displayName() + " " + message.content()).toLowerCase(Locale.ROOT)
                                .contains(normalized))
                .toList();
    }
}

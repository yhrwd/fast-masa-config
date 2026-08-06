package fastui.yure.client.input;

import java.util.HashSet;
import java.util.Set;

/** 防止按住打开热键时，关闭快捷面板后立即被同一次按键重新打开。 */
public final class HotkeyReleaseGate {
    private final Set<Integer> armedKeys = new HashSet<>();

    public void arm(Set<Integer> heldKeys) {
        this.armedKeys.clear();
        this.armedKeys.addAll(heldKeys);
    }

    public boolean isBlocked(Set<Integer> currentlyHeldKeys) {
        if (this.armedKeys.isEmpty()) {
            return false;
        }
        if (currentlyHeldKeys.isEmpty()) {
            this.armedKeys.clear();
            return false;
        }
        return true;
    }

    public void refresh(Set<Integer> currentlyHeldKeys) {
        if (currentlyHeldKeys.isEmpty()) {
            this.armedKeys.clear();
        }
    }
}

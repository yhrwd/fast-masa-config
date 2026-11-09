package fastui.yure.client.input;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/** Mouse buttons need independent event state; MaLiLib's last event is not a held-button snapshot. */
public final class HeldInputState {
    private final Set<Integer> heldMouseKeys = new HashSet<>();
    private boolean focused = true;

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            this.heldMouseKeys.clear();
        }
    }

    /** The adapters pass the native button number; MaLiLib encodes it as button - 100. */
    public void onMouseButton(int button, boolean pressed) {
        if (!this.focused || button < 0 || button >= 100) {
            return;
        }
        int keyCode = button - 100;
        if (pressed) {
            this.heldMouseKeys.add(keyCode);
        } else {
            this.heldMouseKeys.remove(keyCode);
        }
    }

    public boolean isKeyHeld(int keyCode, IntPredicate keyboardState) {
        return this.focused && (keyCode < 0 ? this.heldMouseKeys.contains(keyCode) : keyboardState.test(keyCode));
    }

    public boolean isChordHeld(Collection<Integer> keys, IntPredicate keyboardState) {
        return !keys.isEmpty() && keys.stream().allMatch(key -> isKeyHeld(key, keyboardState));
    }

    public boolean isChordPressedAgain(Collection<Integer> keys, int pressedKeyCode, IntPredicate keyboardState) {
        return this.focused && keys.contains(pressedKeyCode)
                && keys.stream().allMatch(key -> key == pressedKeyCode || isKeyHeld(key, keyboardState));
    }

    public Set<Integer> heldKeys(Collection<Integer> keys, IntPredicate keyboardState) {
        Set<Integer> heldKeys = new HashSet<>();
        for (int keyCode : keys) {
            if (isKeyHeld(keyCode, keyboardState)) {
                heldKeys.add(keyCode);
            }
        }
        return Set.copyOf(heldKeys);
    }
}

package fastui.yure.config;

import java.util.Set;

public final class MovementKeyPassthrough {
    private final Set<Integer> keyCodes;

    public MovementKeyPassthrough(Set<Integer> keyCodes) {
        this.keyCodes = Set.copyOf(keyCodes);
    }

    public boolean shouldPassThrough(int keyCode) {
        return this.keyCodes.contains(keyCode);
    }
}

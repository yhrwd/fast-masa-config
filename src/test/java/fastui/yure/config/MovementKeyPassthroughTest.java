package fastui.yure.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementKeyPassthroughTest {
    @Test
    void allowsConfiguredMovementKeyCodes() {
        MovementKeyPassthrough passthrough = new MovementKeyPassthrough(Set.of(17, 30, 31, 32, 57, 42, 54, 345));

        assertTrue(passthrough.shouldPassThrough(17));
        assertTrue(passthrough.shouldPassThrough(30));
        assertTrue(passthrough.shouldPassThrough(57));
        assertTrue(passthrough.shouldPassThrough(54));
        assertTrue(passthrough.shouldPassThrough(345));
    }

    @Test
    void rejectsNonMovementKeyCodes() {
        MovementKeyPassthrough passthrough = new MovementKeyPassthrough(Set.of(17, 30, 31, 32, 57, 42));

        assertFalse(passthrough.shouldPassThrough(1));
        assertFalse(passthrough.shouldPassThrough(46));
    }
}

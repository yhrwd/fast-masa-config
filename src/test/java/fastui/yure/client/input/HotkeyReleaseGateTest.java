package fastui.yure.client.input;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotkeyReleaseGateTest {
    @Test
    void blocksReopenUntilEveryKeyFromTheOpeningChordIsReleased() {
        HotkeyReleaseGate gate = new HotkeyReleaseGate();
        gate.arm(Set.of(342, 46));

        assertTrue(gate.isBlocked(Set.of(342, 46)));
        assertTrue(gate.isBlocked(Set.of(342)));
        assertFalse(gate.isBlocked(Set.of()));
        assertFalse(gate.isBlocked(Set.of(342, 46)));
    }

    @Test
    void doesNotBlockAnOpeningChordWhenItWasNotArmed() {
        HotkeyReleaseGate gate = new HotkeyReleaseGate();

        assertFalse(gate.isBlocked(Set.of(342, 46)));
    }

    @Test
    void clearsAfterTheOpeningChordIsObservedAsReleased() {
        HotkeyReleaseGate gate = new HotkeyReleaseGate();
        gate.arm(Set.of(342));

        gate.refresh(Set.of(342));
        assertTrue(gate.isBlocked(Set.of(342)));

        gate.refresh(Set.of());
        assertFalse(gate.isBlocked(Set.of(342)));
    }
}

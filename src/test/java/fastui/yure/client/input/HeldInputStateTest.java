package fastui.yure.client.input;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldInputStateTest {
    private static final IntPredicate NO_KEYS = key -> false;

    @Test
    void unrelatedMouseAndKeyboardEventsDoNotReleaseHeldOpeningButton() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(4, true);
        state.onMouseButton(0, true);
        state.onMouseButton(0, false);

        // Keyboard polling must never be used for negative mouse codes, regardless of its last-event snapshot.
        IntPredicate keyboardState = key -> {
            assertTrue(key >= 0);
            return key == 340;
        };
        assertTrue(state.isChordHeld(List.of(-96), keyboardState));
        assertTrue(state.isChordHeld(List.of(340, -96), keyboardState));
        assertEquals(Set.of(340, -96), state.heldKeys(List.of(340, -96, -100), keyboardState));

        state.onMouseButton(4, false);
        assertFalse(state.isChordHeld(List.of(-96), keyboardState));
    }

    @Test
    void everyMouseButtonInAChordHasIndependentPressAndReleaseState() {
        HeldInputState state = new HeldInputState();
        List<Integer> chord = List.of(-99, -95);
        state.onMouseButton(1, true);
        assertFalse(state.isChordHeld(chord, NO_KEYS));
        state.onMouseButton(5, true);
        assertTrue(state.isChordHeld(chord, NO_KEYS));

        state.onMouseButton(1, false);
        assertFalse(state.isChordHeld(chord, NO_KEYS));
        assertEquals(Set.of(-95), state.heldKeys(chord, NO_KEYS));
    }

    @Test
    void rawReleaseEndsAHoldWithoutAHotkeyReleaseCallback() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(2, true);
        assertTrue(state.isChordHeld(List.of(-98), NO_KEYS));
        state.onMouseButton(2, false);
        assertFalse(state.isChordHeld(List.of(-98), NO_KEYS));
    }

    @Test
    void focusLossClearsMouseStateAndIgnoresInputUntilFocusReturns() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(3, true);
        state.setFocused(false);
        state.onMouseButton(4, true);
        assertEquals(Set.of(), state.heldKeys(List.of(-97, -96, 340), key -> true));

        // A release outside the window may never arrive. Regaining focus must not restore that hold.
        state.setFocused(true);
        assertFalse(state.isChordHeld(List.of(-97), NO_KEYS));
        assertFalse(state.isChordHeld(List.of(-96), NO_KEYS));
        state.onMouseButton(3, true);
        assertTrue(state.isChordHeld(List.of(-97), NO_KEYS));
    }

    @Test
    void keyboardModifiersArePolledAsPhysicalState() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(4, true);
        List<Integer> chord = List.of(340, -96);
        assertTrue(state.isChordHeld(chord, key -> key == 340));
        assertFalse(state.isChordHeld(chord, NO_KEYS));
        assertEquals(Set.of(-96), state.heldKeys(chord, NO_KEYS));
    }

    @Test
    void pressedAgainChecksTheOtherKeysAndRejectsUnrelatedKeys() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(4, true);
        List<Integer> chord = List.of(340, -96);
        assertTrue(state.isChordPressedAgain(chord, 340, NO_KEYS));
        assertFalse(state.isChordPressedAgain(chord, 65, key -> true));
        state.onMouseButton(4, false);
        assertFalse(state.isChordPressedAgain(chord, 340, NO_KEYS));
        state.setFocused(false);
        assertFalse(state.isChordPressedAgain(List.of(340), 340, key -> true));
    }

    @Test
    void emptyBindingsNeverCountAsHeldOrPressedAgain() {
        HeldInputState state = new HeldInputState();
        assertFalse(state.isChordHeld(List.of(), key -> true));
        assertFalse(state.isChordPressedAgain(List.of(), 340, key -> true));
    }

    @Test
    void repeatedAndInvalidMouseEventsCannotCreatePhantomHolds() {
        HeldInputState state = new HeldInputState();
        state.onMouseButton(-1, true);
        state.onMouseButton(100, true);
        state.onMouseButton(4, false);
        assertEquals(Set.of(), state.heldKeys(List.of(-101, -96, 0), NO_KEYS));

        state.onMouseButton(4, true);
        state.onMouseButton(4, true);
        state.onMouseButton(4, false);
        state.onMouseButton(4, false);
        assertFalse(state.isChordHeld(List.of(-96), NO_KEYS));
    }

    @Test
    void rawReleaseRearmsTheGateBeforeAnotherPressInTheSameTick() {
        HeldInputState state = new HeldInputState();
        HotkeyReleaseGate gate = new HotkeyReleaseGate();
        Set<Integer> chord = Set.of(-96);

        // The activating callback runs before its raw press listener.
        assertFalse(gate.isBlocked(chord));
        gate.arm(chord);
        state.onMouseButton(4, true);
        gate.refresh(state.heldKeys(chord, NO_KEYS));
        assertTrue(gate.isBlocked(chord));

        state.onMouseButton(0, true);
        state.onMouseButton(0, false);
        gate.refresh(state.heldKeys(chord, NO_KEYS));
        assertTrue(gate.isBlocked(chord));

        state.onMouseButton(4, false);
        gate.refresh(state.heldKeys(chord, NO_KEYS));
        assertFalse(gate.isBlocked(chord));
    }

    @Test
    void losingFocusClearsTheGateWithoutAReleaseEvent() {
        HeldInputState state = new HeldInputState();
        HotkeyReleaseGate gate = new HotkeyReleaseGate();
        Set<Integer> chord = Set.of(340, -96);
        gate.arm(chord);
        state.onMouseButton(4, true);
        state.setFocused(false);
        gate.refresh(state.heldKeys(chord, key -> true));
        state.setFocused(true);
        assertFalse(gate.isBlocked(chord));
    }
}

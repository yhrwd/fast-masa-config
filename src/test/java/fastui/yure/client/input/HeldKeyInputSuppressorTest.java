package fastui.yure.client.input;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldKeyInputSuppressorTest {
    @Test
    void suppressesHeldKeyAndCharactersUntilReleased() {
        HeldKeyInputSuppressor suppressor = new HeldKeyInputSuppressor(Set.of(67));

        assertTrue(suppressor.shouldSuppressKey(67));
        assertTrue(suppressor.shouldSuppressChar());

        suppressor.release(67);

        assertFalse(suppressor.shouldSuppressKey(67));
        assertFalse(suppressor.shouldSuppressChar());
    }

    @Test
    void ignoresKeysThatWereNotHeldWhenOpening() {
        HeldKeyInputSuppressor suppressor = new HeldKeyInputSuppressor(Set.of(67));

        assertFalse(suppressor.shouldSuppressKey(68));
    }
}

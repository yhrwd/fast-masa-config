package fastui.yure.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullConfigListLayoutTest {
    @Test
    void rejectsTheUndrawnBottomRemainderAndRowGaps() {
        int listTop = 60;
        int height = 151;
        assertEquals(2, FullConfigListLayout.visibleRows(height, listTop));
        assertEquals(0, FullConfigListLayout.rowIndexAt(20, 60, 300, height, listTop, 0, 10));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(20, 90, 300, height, listTop, 0, 10));
        assertEquals(1, FullConfigListLayout.rowIndexAt(20, 93, 300, height, listTop, 0, 10));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(20, 126, 300, height, listTop, 0, 10));
    }

    @Test
    void appliesScrollOffsetButNeverReturnsBeyondTheCollection() {
        assertEquals(4, FullConfigListLayout.rowIndexAt(20, 60, 300, 151, 60, 4, 5));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(20, 93, 300, 151, 60, 4, 5));
        assertEquals(-1, FullConfigListLayout.rowIndexAt(11, 60, 300, 151, 60, 0, 5));
    }
}

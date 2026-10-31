package fastui.yure.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import fastui.yure.client.compat.TargetCompat;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResizeMouseInputTest {
    @Test
    void acceptsTheActualSdlLeftButtonEventAndRejectsOtherButtons() {
        MouseButtonEvent click = new MouseButtonEvent(100, 80,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        assertTrue(TargetCompat.isPrimaryMouseButton(click.input()));
        assertFalse(TargetCompat.isPrimaryMouseButton(0));
        assertFalse(TargetCompat.isPrimaryMouseButton(InputConstants.MOUSE_BUTTON_RIGHT));
        assertFalse(TargetCompat.isPrimaryMouseButton(InputConstants.MOUSE_BUTTON_MIDDLE));
    }
}

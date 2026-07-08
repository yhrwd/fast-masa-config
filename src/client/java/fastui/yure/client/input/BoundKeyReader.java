package fastui.yure.client.input;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public final class BoundKeyReader {
    private BoundKeyReader() {
    }

    public static int getBoundKeyCode(KeyMapping KeyMapping) {
        InputConstants.Key key = InputConstants.getKey(KeyMapping.saveString());
        return key.getType() == InputConstants.Type.MOUSE ? key.getValue() - 100 : key.getValue();
    }
}

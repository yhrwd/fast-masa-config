package fastui.yure.client.input;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class BoundKeyReader {
    private BoundKeyReader() {
    }

    public static int getBoundKeyCode(KeyBinding keyBinding) {
        InputUtil.Key key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        return key.getCategory() == InputUtil.Type.MOUSE ? key.getCode() - 100 : key.getCode();
    }
}

package fastui.yure.client.input;

import java.util.HashSet;
import java.util.Set;

/**
 * 记录“打开全屏 UI 时仍被按住”的热键，并临时吞掉它们产生的输入事件。
 * 典型场景是用户按住 C 打开快捷面板，再点设置进入全屏配置页；如果不拦截，C 会直接进入搜索框。
 */
public final class HeldKeyInputSuppressor {
    private final Set<Integer> heldKeys;

    /**
     * 拷贝一份进入 UI 瞬间仍按下的按键集合。
     * 后续 release() 会修改内部集合，所以不能直接持有调用方传入的 Set。
     */
    public HeldKeyInputSuppressor(Set<Integer> heldKeys) {
        // 只拦截打开 UI 时已经按住的键；松开后立即恢复正常搜索输入。
        this.heldKeys = new HashSet<>(heldKeys);
    }

    /**
     * 判断 keyPressed 事件是否需要吞掉。
     * 只拦截打开时已经按住的键，新按下的正常搜索键不会被影响。
     */
    public boolean shouldSuppressKey(int keyCode) {
        return this.heldKeys.contains(keyCode);
    }

    /**
     * 判断 charTyped 事件是否需要吞掉。
     * GLFW 会在 keyPressed 后继续发字符事件，所以只要还有打开热键未松开，就先不让字符进入搜索框。
     */
    public boolean shouldSuppressChar() {
        return this.heldKeys.isEmpty() == false;
    }

    /**
     * 标记某个打开热键已经松开。
     * 当集合清空后，keyPressed 和 charTyped 都恢复正常流向 MaLiLib 搜索框。
     */
    public void release(int keyCode) {
        this.heldKeys.remove(keyCode);
    }
}

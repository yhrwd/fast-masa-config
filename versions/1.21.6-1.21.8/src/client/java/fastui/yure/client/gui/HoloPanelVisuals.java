package fastui.yure.client.gui;

import net.minecraft.client.gui.DrawContext;
import fi.dy.masa.malilib.render.RenderUtils;

/**
 * 快捷面板视觉计算工具。
 * 只处理缓动、透明度和颜色混合，不直接绘制任何内容，便于渲染代码保持清爽。
 */
public final class HoloPanelVisuals {
    private HoloPanelVisuals() {
    }

    /**
     * 替换 ARGB 颜色中的 alpha 通道。
     * RGB 保持不变，alpha 会被限制在 8 bit 内，方便复用同一主题色绘制不同透明层级。
     */
    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        RenderUtils.drawRect(x, y, width, 1, color);
        if (height > 1) {
            RenderUtils.drawRect(x, y + height - 1, width, 1, color);
        }
        RenderUtils.drawRect(x, y, 1, height, color);
        if (width > 1) {
            RenderUtils.drawRect(x + width - 1, y, 1, height, color);
        }
    }

}

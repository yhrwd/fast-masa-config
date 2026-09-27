package fastui.yure.client.gui;

import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/** Shared floating-panel visual calculations with the 1.21.1 draw-border bridge. */
public final class HoloPanelVisuals {
    private HoloPanelVisuals() {
    }

    public static double easeOutQuart(double value) {
        double clamped = clamp01(value);
        return 1.0 - Math.pow(1.0 - clamped, 4.0);
    }

    public static double openProgress(long elapsedMillis, long durationMillis) {
        if (durationMillis <= 0L) {
            return 1.0;
        }
        return easeOutQuart(elapsedMillis / (double) durationMillis);
    }

    public static double approach(double current, double target, double speed) {
        double clampedSpeed = Math.max(0.0, Math.min(1.0, speed));
        return current + (target - current) * clampedSpeed;
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static int mixRgb(int startColor, int endColor, double ratio) {
        double clamped = clamp01(ratio);
        int red = mixChannel((startColor >> 16) & 0xFF, (endColor >> 16) & 0xFF, clamped);
        int green = mixChannel((startColor >> 8) & 0xFF, (endColor >> 8) & 0xFF, clamped);
        int blue = mixChannel(startColor & 0xFF, endColor & 0xFF, clamped);
        return 0xFF000000 | red << 16 | green << 8 | blue;
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

    private static int mixChannel(int start, int end, double ratio) {
        return (int) Math.round(start + (end - start) * ratio);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

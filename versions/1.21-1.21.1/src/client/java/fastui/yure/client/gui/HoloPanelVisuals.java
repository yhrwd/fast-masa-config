package fastui.yure.client.gui;

/**
 * 快捷面板视觉计算工具。
 * 只处理缓动、透明度和颜色混合，不直接绘制任何内容，便于渲染代码保持清爽。
 */
public final class HoloPanelVisuals {
    private HoloPanelVisuals() {
    }

    /**
     * 四次方 ease-out 缓动。
     * 输入会被限制到 0..1，返回值也保持在 0..1；用于让面板出现时前段更快、尾段更柔和。
     */
    public static double easeOutQuart(double value) {
        double clamped = clamp01(value);
        return 1.0 - Math.pow(1.0 - clamped, 4.0);
    }

    /**
     * 根据已过去的毫秒数计算打开动画进度。
     * durationMillis 小于等于 0 时直接返回完成状态，避免除零，也便于后续关闭动画复用。
     */
    public static double openProgress(long elapsedMillis, long durationMillis) {
        if (durationMillis <= 0L) {
            return 1.0;
        }

        return easeOutQuart(elapsedMillis / (double) durationMillis);
    }

    /**
     * 将当前值按固定速度向目标值逼近。
     * 用于 hover 和 toggle 动画，每帧只推进一小段，避免状态突然跳变。
     */
    public static double approach(double current, double target, double speed) {
        double clampedSpeed = Math.max(0.0, Math.min(1.0, speed));
        return current + (target - current) * clampedSpeed;
    }

    /**
     * 替换 ARGB 颜色中的 alpha 通道。
     * RGB 保持不变，alpha 会被限制在 8 bit 内，方便复用同一主题色绘制不同透明层级。
     */
    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    /**
     * 在两个颜色之间按 ratio 混合 RGB 通道。
     * alpha 固定为不透明，调用方需要半透明时再用 withAlpha 包一层。
     */
    public static int mixRgb(int startColor, int endColor, double ratio) {
        double clamped = clamp01(ratio);
        int red = mixChannel((startColor >> 16) & 0xFF, (endColor >> 16) & 0xFF, clamped);
        int green = mixChannel((startColor >> 8) & 0xFF, (endColor >> 8) & 0xFF, clamped);
        int blue = mixChannel(startColor & 0xFF, endColor & 0xFF, clamped);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /**
     * 单个颜色通道的线性插值。
     * 使用 round 避免长时间动画中颜色偏暗或偏亮。
     */
    private static int mixChannel(int start, int end, double ratio) {
        return (int) Math.round(start + (end - start) * ratio);
    }

    /**
     * 将比例值限制在 0..1。
     * 所有缓动和混色入口都先收敛输入，避免外部传入负值或超过 1 时产生异常视觉结果。
     */
    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

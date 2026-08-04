package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.config.ConfigGroupStore;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 只维护可见分组的浮动窗口和它们的绘制层级。 */
public final class QuickConfigPanel {
    private final Font font;
    private final Map<String, FloatingGroupPanel> floatingPanels = new HashMap<>();
    private final List<String> floatingOrder = new ArrayList<>();
    private int recoveryX;
    private int recoveryY;
    private boolean recoveryVisible;

    public QuickConfigPanel(Minecraft client) {
        this.font = client.font;
    }

    public void render(GuiContext context, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        var configIndex = ConfigIndexService.scanSupportedConfigs();
        for (var group : ConfigGroupStore.getGroups()) {
            if (group.hidden()) {
                continue;
            }
            this.floatingPanels.computeIfAbsent(group.id(), id -> new FloatingGroupPanel(this.font, id));
            if (!this.floatingOrder.contains(group.id())) {
                this.floatingOrder.add(group.id());
            }
        }
        this.floatingPanels.keySet().removeIf(id -> ConfigGroupStore.get(id).map(group -> group.hidden()).orElse(true));
        this.floatingOrder.removeIf(id -> !this.floatingPanels.containsKey(id));
        this.recoveryVisible = this.floatingOrder.isEmpty() && !ConfigGroupStore.getGroups().isEmpty();
        for (String groupId : this.floatingOrder) {
            FloatingGroupPanel panel = this.floatingPanels.get(groupId);
            if (panel != null) {
                panel.render(context, screenWidth, screenHeight, mouseX, mouseY, configIndex);
            }
        }
        if (this.recoveryVisible) {
            int width = Math.min(180, Math.max(120, screenWidth - 40));
            this.recoveryX = Math.max(0, (screenWidth - width) / 2);
            this.recoveryY = Math.max(0, Math.min(screenHeight - 24, 20));
            RenderUtils.drawRect(context, this.recoveryX, this.recoveryY, width, 24, 0xE81A1A1D);
            context.drawString(this.font, StringUtils.translate("fast-masa-config.gui.recovery.open"),
                    this.recoveryX + 8, this.recoveryY + 8, 0xFFFFEAF2, false);
        }
    }

    public List<FloatingGroupPanel> floatingPanels() {
        return this.floatingOrder.stream().map(this.floatingPanels::get).filter(panel -> panel != null).toList();
    }

    public boolean moveFloatingGroup(String groupId, int x, int y, int screenWidth, int screenHeight) {
        FloatingGroupPanel panel = this.floatingPanels.get(groupId);
        return panel != null && panel.moveTo(x, y, screenWidth, screenHeight);
    }

    public void raiseFloatingGroup(String groupId) {
        if (this.floatingOrder.remove(groupId)) {
            this.floatingOrder.add(groupId);
        }
    }

    public boolean isRecoveryHit(int mouseX, int mouseY) {
        return this.recoveryVisible && GuiHitTest.isInside(mouseX, mouseY, this.recoveryX, this.recoveryY,
                Math.min(180, Math.max(120, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 40)), 24);
    }
}

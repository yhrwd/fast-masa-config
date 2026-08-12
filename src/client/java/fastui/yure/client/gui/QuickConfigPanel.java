package fastui.yure.client.gui;

import fastui.yure.client.index.ConfigIndexService;
import fastui.yure.client.index.ConfigIndexEntry;
import fastui.yure.config.ConfigGroup;
import fastui.yure.config.ConfigGroupStore;
import fastui.yure.config.QuickMessageGroup;
import fastui.yure.config.QuickMessageStore;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** 只维护可见分组的浮动窗口和它们的绘制层级。 */
public final class QuickConfigPanel {
    private final Font font;
    private final Map<String, FloatingGroupPanel> floatingPanels = new HashMap<>();
    private final Map<String, FloatingMessagePanel> floatingMessagePanels = new HashMap<>();
    private final List<WindowEntry> floatingOrder = new ArrayList<>();
    private List<FloatingGroupPanel> visiblePanels = List.of();
    private List<FloatingMessagePanel> visibleMessagePanels = List.of();
    private List<FloatingWindow> visibleWindows = List.of();
    private Map<ConfigIndexService.Target, ConfigIndexEntry> configIndexByTarget = Map.of();
    private long indexedConfigGeneration = -1;
    private long synchronizedGroupRevision = -1;
    private long synchronizedMessageGroupRevision = -1;

    public QuickConfigPanel(Minecraft client) {
        this.font = client.font;
    }

    public void render(GuiContext context, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        this.refreshConfigIndex();
        this.syncFloatingPanels();
        this.syncFloatingMessagePanels();
        for (FloatingWindow window : this.visibleWindows) {
            if (window instanceof MessageWindow messageWindow) {
                messageWindow.panel().render(context, screenWidth, screenHeight, mouseX, mouseY);
            } else if (window instanceof ConfigWindow configWindow) {
                configWindow.panel().render(context, screenWidth, screenHeight, mouseX, mouseY,
                        this.configIndexByTarget);
            }
        }
    }

    public List<FloatingGroupPanel> floatingPanels() {
        this.syncFloatingPanels();
        return this.visiblePanels;
    }

    public List<FloatingMessagePanel> floatingMessagePanels() {
        this.syncFloatingMessagePanels();
        return this.visibleMessagePanels;
    }

    public boolean moveFloatingGroup(String groupId, int x, int y, int screenWidth, int screenHeight) {
        FloatingGroupPanel panel = this.floatingPanels.get(groupId);
        return panel != null && panel.moveTo(x, y, screenWidth, screenHeight);
    }

    public void raiseFloatingGroup(String groupId) {
        if (this.floatingOrder.removeIf(entry -> !entry.message() && entry.groupId().equals(groupId))) {
            this.floatingOrder.add(new WindowEntry(groupId, false));
            this.refreshVisibleCollections();
        }
    }

    public boolean moveFloatingMessageGroup(String groupId, int x, int y, int screenWidth, int screenHeight) {
        FloatingMessagePanel panel = this.floatingMessagePanels.get(groupId);
        return panel != null && panel.moveTo(x, y, screenWidth, screenHeight);
    }

    public void raiseFloatingMessageGroup(String groupId) {
        if (this.floatingOrder.removeIf(entry -> entry.message() && entry.groupId().equals(groupId))) {
            this.floatingOrder.add(new WindowEntry(groupId, true));
            this.refreshVisibleCollections();
        }
    }

    private void refreshConfigIndex() {
        if (this.indexedConfigGeneration == ConfigIndexService.generation()) {
            return;
        }
        ConfigIndexService.scanSupportedConfigs();
        this.configIndexByTarget = ConfigIndexService.indexByTarget();
        this.indexedConfigGeneration = ConfigIndexService.generation();
    }

    private void syncFloatingPanels() {
        if (this.synchronizedGroupRevision == ConfigGroupStore.revision()) {
            return;
        }
        this.syncPanels(ConfigGroupStore.getGroups(), ConfigGroup::id, ConfigGroup::hidden, this.floatingPanels,
                id -> new FloatingGroupPanel(this.font, id), false);
        this.synchronizedGroupRevision = ConfigGroupStore.revision();
    }

    private void syncFloatingMessagePanels() {
        if (this.synchronizedMessageGroupRevision == QuickMessageStore.revision()) {
            return;
        }
        this.syncPanels(QuickMessageStore.getGroups(), QuickMessageGroup::id, QuickMessageGroup::hidden,
                this.floatingMessagePanels,
                id -> new FloatingMessagePanel(this.font, id), true);
        this.synchronizedMessageGroupRevision = QuickMessageStore.revision();
    }

    private <G, P> void syncPanels(Collection<G> groups, Function<G, String> idFunction,
            Predicate<G> hiddenPredicate, Map<String, P> panels,
            Function<String, P> panelFactory, boolean message) {
        Set<String> visibleGroupIds = new HashSet<>();
        for (G group : groups) {
            if (hiddenPredicate.test(group)) {
                continue;
            }
            String groupId = idFunction.apply(group);
            visibleGroupIds.add(groupId);
            panels.computeIfAbsent(groupId, panelFactory);
            WindowEntry entry = new WindowEntry(groupId, message);
            if (!this.floatingOrder.contains(entry)) {
                this.floatingOrder.add(entry);
            }
        }
        panels.keySet().removeIf(id -> !visibleGroupIds.contains(id));
        this.floatingOrder.removeIf(entry -> entry.message() == message && !panels.containsKey(entry.groupId()));
        this.refreshVisibleCollections();
    }

    public List<FloatingWindow> floatingWindows() {
        this.syncFloatingPanels();
        this.syncFloatingMessagePanels();
        return this.visibleWindows;
    }

    private void refreshVisibleCollections() {
        List<FloatingGroupPanel> panels = new ArrayList<>(this.floatingOrder.size());
        List<FloatingMessagePanel> messagePanels = new ArrayList<>(this.floatingOrder.size());
        List<FloatingWindow> windows = new ArrayList<>(this.floatingOrder.size());
        for (WindowEntry entry : this.floatingOrder) {
            if (entry.message()) {
                FloatingMessagePanel panel = this.floatingMessagePanels.get(entry.groupId());
                if (panel != null) {
                    messagePanels.add(panel);
                    windows.add(new MessageWindow(panel));
                }
            } else {
                FloatingGroupPanel panel = this.floatingPanels.get(entry.groupId());
                if (panel != null) {
                    panels.add(panel);
                    windows.add(new ConfigWindow(panel));
                }
            }
        }
        this.visiblePanels = List.copyOf(panels);
        this.visibleMessagePanels = List.copyOf(messagePanels);
        this.visibleWindows = List.copyOf(windows);
    }

    public sealed interface FloatingWindow permits ConfigWindow, MessageWindow {
    }

    public record ConfigWindow(FloatingGroupPanel panel) implements FloatingWindow {
    }

    public record MessageWindow(FloatingMessagePanel panel) implements FloatingWindow {
    }

    private record WindowEntry(String groupId, boolean message) {
    }

}

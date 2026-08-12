package fastui.yure.client.gui;

import fastui.yure.config.EntityRenderFilter;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Searchable entity registry selector used by the rendering filter's secondary menu. */
final class EntityRenderSelectionScreen extends GuiBase {
    private static final int MARGIN = 12;
    private static final int SEARCH_Y = 34;
    private static final int LIST_Y = 60;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int BUTTON_HEIGHT = 20;

    private final IConfigStringList entityIds;
    private final Runnable onChanged;
    private final List<Entry> allEntries;
    private List<Entry> visibleEntries;
    private String filter = "";
    private int scrollOffset;
    private GuiTextFieldGeneric searchField;

    EntityRenderSelectionScreen(Screen parent, IConfigStringList entityIds, Runnable onChanged) {
        this.entityIds = entityIds;
        this.onChanged = onChanged;
        this.allEntries = BuiltInRegistries.ENTITY_TYPE.stream()
                .map(type -> new Entry(BuiltInRegistries.ENTITY_TYPE.getKey(type), type.getDescription().getString()))
                .sorted(Comparator.comparing(Entry::displayName).thenComparing(entry -> entry.id().toString()))
                .toList();
        this.visibleEntries = this.allEntries;
        this.setParent(parent);
        this.setTitle(StringUtils.translate("fast-masa-config.gui.tools.entities_title"));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.visibleEntries = this.filterEntries();
        this.scrollOffset = clamp(this.scrollOffset, 0, Math.max(0, this.visibleEntries.size() - this.visibleRows()));
        this.searchField = new GuiTextFieldGeneric(MARGIN, SEARCH_Y, this.width - MARGIN * 2, 18, this.font);
        this.searchField.setValue(this.filter);
        this.searchField.setMaxLength(128);
        this.addTextField(this.searchField, field -> {
            this.filter = field.getValue();
            this.scrollOffset = 0;
            this.initGui();
            return true;
        });
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor drawContext, int mouseX, int mouseY,
            float partialTicks) {
        GuiContext context = GuiContext.fromGuiGraphics(drawContext);
        super.drawScreenBackground(context, mouseX, mouseY);
        RenderUtils.drawRect(context, 0, 0, this.width, this.height, FullConfigPalette.SCREEN_BACKGROUND);
        RenderUtils.drawRect(context, 0, 0, this.width, 26, FullConfigPalette.SCREEN_HEADER);
        RenderUtils.drawRect(context, 0, 25, this.width, 1, FullConfigPalette.BORDER);
        this.drawString(context, StringUtils.translate("fast-masa-config.gui.tools.entities_title"), MARGIN, 9,
                FullConfigPalette.TEXT);
        this.drawString(context, this.visibleEntries.size() + " / " + this.allEntries.size(),
                this.width - MARGIN - this.getStringWidth(this.visibleEntries.size() + " / " + this.allEntries.size()),
                40, FullConfigPalette.MUTED);
        if (this.searchField.getValue().isBlank() && !this.searchField.isFocused()) {
            this.drawString(context, StringUtils.translate("fast-masa-config.gui.tools.entities_search"), MARGIN + 4,
                    SEARCH_Y + 5, FullConfigPalette.MUTED);
        }
        this.drawRows(context, mouseX, mouseY);
        this.drawButtons(context, mouseX, mouseY, partialTicks);
        this.drawTextFields(context, mouseX, mouseY);
        this.drawHoveredWidget(context, mouseX, mouseY);
        this.drawButtonHoverTexts(context, mouseX, mouseY, partialTicks);
        this.drawGuiMessages(context);
    }

    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubleClick) {
        if (super.onMouseClicked(click, doubleClick)) {
            return true;
        }
        int index = this.rowIndexAt((int) click.x(), (int) click.y());
        if (index < 0) {
            return false;
        }
        Entry entry = this.visibleEntries.get(index);
        List<String> ids = new ArrayList<>(EntityRenderFilter.normalizedIds(this.entityIds.getStrings()));
        String id = entry.id().toString();
        if (ids.contains(id)) {
            ids.remove(id);
        } else {
            ids.add(id);
            ids.sort(String::compareTo);
        }
        this.entityIds.setStrings(ids);
        this.onChanged.run();
        this.initGui();
        return true;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (mouseX < MARGIN || mouseX >= this.width - MARGIN || mouseY < LIST_Y || mouseY >= this.height - 18) {
            return false;
        }
        int previous = this.scrollOffset;
        this.scrollOffset = clamp(this.scrollOffset + (verticalAmount < 0 ? 1 : -1), 0,
                Math.max(0, this.visibleEntries.size() - this.visibleRows()));
        return previous != this.scrollOffset;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRows(GuiContext context, int mouseX, int mouseY) {
        Set<String> selected = EntityRenderFilter.normalizedIds(this.entityIds.getStrings());
        int end = Math.min(this.visibleEntries.size(), this.scrollOffset + this.visibleRows());
        for (int index = this.scrollOffset; index < end; index++) {
            Entry entry = this.visibleEntries.get(index);
            int y = LIST_Y + (index - this.scrollOffset) * (ROW_HEIGHT + ROW_GAP);
            boolean active = selected.contains(entry.id().toString());
            boolean hovered = GuiHitTest.isInside(mouseX, mouseY, MARGIN, y, this.width - MARGIN * 2, ROW_HEIGHT);
            RenderUtils.drawRect(context, MARGIN, y, this.width - MARGIN * 2, ROW_HEIGHT,
                    active ? FullConfigPalette.MODULE_BACKGROUND
                            : (hovered ? FullConfigPalette.ROW_HOVER : FullConfigPalette.ROW));
            RenderUtils.drawRect(context, MARGIN, y, 3, ROW_HEIGHT,
                    active ? FullConfigPalette.ACCENT : FullConfigPalette.BORDER);
            this.drawString(context, fit(entry.displayName(), this.width - MARGIN * 2 - 94), MARGIN + 8, y + 6,
                    FullConfigPalette.TEXT);
            this.drawString(context, entry.id().toString(), MARGIN + 8, y + 18, FullConfigPalette.MUTED);
            String toggle = active ? StringUtils.translate("fast-masa-config.gui.tools.entities.remove")
                    : StringUtils.translate("fast-masa-config.gui.tools.entities.add");
            int buttonWidth = 58;
            int buttonX = this.width - MARGIN - buttonWidth - 6;
            RenderUtils.drawRect(context, buttonX, y + 5, buttonWidth, BUTTON_HEIGHT,
                    active ? FullConfigPalette.ACTION_REMOVE : FullConfigPalette.ACTION_ADD);
            RenderUtils.drawRect(context, buttonX, y + 5, buttonWidth, 1, FullConfigPalette.BORDER);
            RenderUtils.drawRect(context, buttonX, y + BUTTON_HEIGHT + 4, buttonWidth, 1, FullConfigPalette.BORDER);
            RenderUtils.drawRect(context, buttonX, y + 5, 1, BUTTON_HEIGHT, FullConfigPalette.BORDER);
            RenderUtils.drawRect(context, buttonX + buttonWidth - 1, y + 5, 1, BUTTON_HEIGHT,
                    FullConfigPalette.BORDER);
            this.drawString(context, toggle, buttonX + (buttonWidth - this.getStringWidth(toggle)) / 2, y + 11,
                    0xFFFFFFFF);
        }
    }

    private List<Entry> filterEntries() {
        String query = this.filter.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return this.allEntries;
        }
        return this.allEntries.stream().filter(entry -> entry.displayName().toLowerCase(Locale.ROOT).contains(query)
                || entry.id().toString().contains(query)).toList();
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 18 - LIST_Y) / (ROW_HEIGHT + ROW_GAP));
    }

    private int rowIndexAt(int mouseX, int mouseY) {
        if (!GuiHitTest.isInside(mouseX, mouseY, MARGIN, LIST_Y, this.width - MARGIN * 2,
                this.height - 18 - LIST_Y)) {
            return -1;
        }
        int visibleIndex = (mouseY - LIST_Y) / (ROW_HEIGHT + ROW_GAP);
        int rowY = LIST_Y + visibleIndex * (ROW_HEIGHT + ROW_GAP);
        int index = this.scrollOffset + visibleIndex;
        return mouseY >= rowY + ROW_HEIGHT || index >= this.visibleEntries.size() ? -1 : index;
    }

    private String fit(String text, int maxWidth) {
        return FloatingGroupPanel.fitText(text, maxWidth, this::getStringWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Entry(Identifier id, String displayName) {
    }
}

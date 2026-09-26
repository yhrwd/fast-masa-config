package fastui.yure.client.gui;

import java.util.List;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;

/**
 * 筛选下拉：替代全配置页的模组/分组循环按钮，条目多时可直接跳选。
 * malilib 的 WidgetDropDownList 点击区域外不会自动收起，展开区也可能盖住下方按钮，
 * 由 FastMasaConfigGui 在鼠标事件链里做收起与拦截兜底。
 */
final class FilterDropdownList extends WidgetDropDownList<FilterDropdownList.Option> {
    private Consumer<Option> changedHandler;

    FilterDropdownList(int x, int y, int width, int height, int maxHeight, int maxVisibleEntries,
            List<Option> entries) {
        super(x, y, width, height, maxHeight, maxVisibleEntries, entries);
    }

    boolean isOpenDropdown() {
        return this.isOpen;
    }

    void setChangedHandler(Consumer<Option> changedHandler) {
        this.changedHandler = changedHandler;
    }

    void closeDropdown() {
        if (this.isOpen) {
            this.isOpen = false;
            this.searchBar.getTextField().setText("");
            this.updateFilteredEntries();
        }
    }

    @Override
    protected void setSelectedEntry(int index) {
        super.setSelectedEntry(index);

        if (this.selectedEntry != null && this.changedHandler != null) {
            this.changedHandler.accept(this.selectedEntry);
        }
    }

    @Override
    protected String getDisplayString(Option entry) {
        return entry.label();
    }

    record Option(String id, String label) {
        @Override
        public String toString() {
            return this.label;
        }
    }
}

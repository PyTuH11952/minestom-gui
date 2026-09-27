package com.minebreak.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * A paginated GUI that distributes items across multiple pages.
 *
 * <p>Displays items in a 4-row content area with a navigation bar on the last row containing
 * Previous, Page Indicator, and Next buttons. Supports localised navigation labels when constructed
 * with a {@link Player}.
 */
public class PagedGui extends Gui {

    private static final int CONTENT_ROWS = 4;
    private static final int CONTENT_SIZE = CONTENT_ROWS * 9;

    private final List<GuiItem> allItems = new ArrayList<>();
    private final List<List<GuiItem>> pages = new ArrayList<>();
    private int currentPage = 0;
    private final Player player;

    private GuiItem previousPageItem;
    private GuiItem nextPageItem;
    private GuiItem pageIndicatorItem;
    private GuiItem backgroundItem;
    private GuiItem borderItem;

    /**
     * Constructs a new paged GUI.
     *
     * @param player the player for locale-aware navigation labels, or null
     * @param title the GUI title
     */
    private PagedGui(Player player, Component title) {
        super(CONTENT_ROWS + 1, title);
        this.player = player;

        // NOTE(fork): localization dependency stripped to keep this library
        // Minestom-only (localization pulls Adventure 5.x). Navigation labels
        // are plain text; callers can pre-localize via GuiItem if needed.
        Component prevName = Component.text("<- Previous");
        Component nextName = Component.text("Next ->");

        this.previousPageItem =
                GuiItem.of(
                        ItemBuilder.of(Material.ARROW).name(prevName).build(), e -> previousPage());
        this.nextPageItem =
                GuiItem.of(ItemBuilder.of(Material.ARROW).name(nextName).build(), e -> nextPage());
        this.backgroundItem = GuiItem.background(Material.GRAY_STAINED_GLASS_PANE);
        this.borderItem = GuiItem.background(Material.BLACK_STAINED_GLASS_PANE);

        applyLayout();
    }

    /** Applies the navigation bar layout on the last row. */
    private void applyLayout() {
        int lastRow = rows - 1;
        for (int col = 0; col < 9; col++) {
            boolean isBorder = col == 0 || col == 8;
            super.setItem(lastRow * 9 + col, isBorder ? borderItem : backgroundItem);
        }
        super.setItem(lastRow * 9, previousPageItem);
        super.setItem(lastRow * 9 + 8, nextPageItem);
    }

    /** Creates a paged GUI with a plain-text title (no player-based localisation). */
    public static PagedGui of(String title) {
        return new PagedGui(null, Component.text(title));
    }

    /** Creates a paged GUI with a component title (no player-based localisation). */
    public static PagedGui of(Component title) {
        return new PagedGui(null, title);
    }

    /**
     * Creates a player-aware paged GUI with a plain-text title. Navigation buttons will be
     * localised to the player's locale.
     */
    public static PagedGui of(Player player, String title) {
        return new PagedGui(player, Component.text(title));
    }

    /**
     * Creates a player-aware paged GUI with a component title. Navigation buttons will be localised
     * to the player's locale.
     */
    public static PagedGui of(Player player, Component title) {
        return new PagedGui(player, title);
    }

    /** Adds a single item to the pageable item list. */
    public PagedGui addItem(GuiItem item) {
        allItems.add(item);
        return this;
    }

    /** Adds multiple items to the pageable item list. */
    public PagedGui addItems(GuiItem... items) {
        for (GuiItem item : items) {
            addItem(item);
        }
        return this;
    }

    /** Adds all items from a list to the pageable item list. */
    public PagedGui addItems(List<GuiItem> items) {
        allItems.addAll(items);
        return this;
    }

    /** Overrides the default "previous page" navigation item. */
    public PagedGui previousPageItem(GuiItem item) {
        this.previousPageItem = item;
        int lastRow = rows - 1;
        super.setItem(lastRow * 9, currentPage > 0 ? item : GuiItem.empty());
        return this;
    }

    /** Overrides the default "next page" navigation item. */
    public PagedGui nextPageItem(GuiItem item) {
        this.nextPageItem = item;
        int lastRow = rows - 1;
        super.setItem(lastRow * 9 + 8, currentPage < pages.size() - 1 ? item : GuiItem.empty());
        return this;
    }

    /** Sets the item used as the page indicator (displayed at the centre of the nav bar). */
    public PagedGui pageIndicatorItem(GuiItem item) {
        this.pageIndicatorItem = item;
        return this;
    }

    /** Overrides the default background item in the navigation row. */
    public PagedGui backgroundItem(GuiItem item) {
        this.backgroundItem = item;
        int lastRow = rows - 1;
        for (int col = 1; col < 8; col++) {
            super.setItem(lastRow * 9 + col, item);
        }
        return this;
    }

    /** Overrides the default border items in the navigation row. */
    public PagedGui borderItem(GuiItem item) {
        this.borderItem = item;
        int lastRow = rows - 1;
        super.setItem(lastRow * 9, item);
        super.setItem(lastRow * 9 + 8, item);
        return this;
    }

    /**
     * Finalises the paged GUI by splitting items into pages and rendering the first page.
     *
     * @return this
     */
    public PagedGui build() {
        buildPages();
        renderPage();
        return this;
    }

    /** Splits all items into pages of {@link #CONTENT_SIZE} capacity. */
    private void buildPages() {
        pages.clear();
        List<GuiItem> current = new ArrayList<>();
        for (GuiItem item : allItems) {
            if (current.size() >= CONTENT_SIZE) {
                pages.add(current);
                current = new ArrayList<>();
            }
            current.add(item);
        }
        if (!current.isEmpty() || pages.isEmpty()) {
            pages.add(current);
        }
    }

    /**
     * Renders the current page, placing items into the content area and updating navigation
     * buttons.
     */
    private void renderPage() {
        clearContent();

        if (pages.isEmpty()) return;

        int pageIndex = Math.min(currentPage, pages.size() - 1);
        List<GuiItem> page = pages.get(pageIndex);

        for (int i = 0; i < page.size() && i < CONTENT_SIZE; i++) {
            super.setItem(i, page.get(i));
        }

        int lastRow = rows - 1;
        super.setItem(lastRow * 9, currentPage > 0 ? previousPageItem : GuiItem.empty());
        super.setItem(
                lastRow * 9 + 8, currentPage < pages.size() - 1 ? nextPageItem : GuiItem.empty());

        if (pageIndicatorItem != null) {
            Component pageName = Component.text("Page " + (currentPage + 1) + "/" + pages.size());
            ItemStack indicator = pageIndicatorItem.itemStack().withCustomName(pageName);
            int centerSlot = lastRow * 9 + 4;
            super.setItem(centerSlot, indicator);
        }
    }

    /** Clears all content slots in the GUI. */
    private void clearContent() {
        for (int i = 0; i < CONTENT_SIZE; i++) {
            super.setItem(i, GuiItem.empty());
        }
    }

    /** Navigates to the previous page, if available. */
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
            if (player != null) GuiSounds.playClick(player);
        }
    }

    /** Navigates to the next page, if available. */
    private void nextPage() {
        if (currentPage < pages.size() - 1) {
            currentPage++;
            renderPage();
            if (player != null) GuiSounds.playClick(player);
        }
    }

    /** Places a GuiItem at the given slot and returns this for chaining. */
    @Override
    public PagedGui setItem(int slot, GuiItem guiItem) {
        super.setItem(slot, guiItem);
        return this;
    }

    /** Returns the complete list of all items across all pages. */
    public List<GuiItem> getAllItems() {
        return allItems;
    }

    /** Returns the current page index (0-based). */
    public int getCurrentPage() {
        return currentPage;
    }

    /** Returns the total number of pages. */
    public int getPageCount() {
        return pages.size();
    }
}

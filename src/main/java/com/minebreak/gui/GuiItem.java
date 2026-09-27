package com.minebreak.gui;

import java.util.function.Consumer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * A record representing an item in a {@link Gui} with an optional click handler.
 *
 * @param itemStack the item stack to display
 * @param clickHandler the handler invoked when the item is clicked (may be null)
 */
public record GuiItem(ItemStack itemStack, Consumer<ClickEvent> clickHandler) {

    /** Creates a GuiItem with the given stack and optional click handler. */
    public static GuiItem of(ItemStack itemStack, Consumer<ClickEvent> clickHandler) {
        return new GuiItem(itemStack, clickHandler);
    }

    /** Creates a GuiItem from a material with an optional click handler. */
    public static GuiItem of(Material material, Consumer<ClickEvent> clickHandler) {
        return new GuiItem(ItemStack.of(material), clickHandler);
    }

    /** Creates a non-interactive GuiItem from a material. */
    public static GuiItem of(Material material) {
        return new GuiItem(ItemStack.of(material), null);
    }

    /**
     * Creates a background GuiItem with an invisible name (space character). Background items are
     * non-interactive.
     */
    public static GuiItem background(Material material) {
        return new GuiItem(ItemBuilder.of(material).asBackground(), null);
    }

    /** Returns an empty (air) GuiItem, used to clear slots. */
    public static GuiItem empty() {
        return new GuiItem(ItemStack.AIR, null);
    }

    /** Returns true if this item has a click handler attached. */
    public boolean hasClickHandler() {
        return clickHandler != null;
    }
}

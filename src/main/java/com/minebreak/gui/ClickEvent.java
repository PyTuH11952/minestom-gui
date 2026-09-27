package com.minebreak.gui;

import net.minestom.server.entity.Player;

/**
 * Represents a click action within a {@link Gui}.
 *
 * @param player the player who clicked
 * @param slot the clicked inventory slot index
 * @param gui the GUI in which the click occurred
 * @param type the type of click performed
 */
public record ClickEvent(Player player, int slot, Gui gui, Type type) {

    /** Creates a ClickEvent from the given player, slot, GUI, and click type. */
    public static ClickEvent from(Player player, int slot, Gui gui, Type type) {
        return new ClickEvent(player, slot, gui, type);
    }

    /** The type of click performed within a GUI. */
    public enum Type {
        /** Left mouse button click. */
        LEFT_CLICK,
        /** Right mouse button click. */
        RIGHT_CLICK,
        /** Middle mouse button click (scroll wheel press). */
        MIDDLE_CLICK,
        /** Shift + left mouse button click. */
        SHIFT_LEFT_CLICK,
        /** Shift + right mouse button click. */
        SHIFT_RIGHT_CLICK,
        /** Mouse drag action. */
        DRAG,
        /** Any other click type not covered by the above. */
        OTHER
    }
}

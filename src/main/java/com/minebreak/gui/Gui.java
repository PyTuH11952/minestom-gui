package com.minebreak.gui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

/**
 * A flexible chest-GUI framework for Minestom.
 *
 * <p>Supports 1–6 rows of slots, pattern-based layouts, click handlers, background fills, and
 * border decorations. All mutating methods return {@code this} for chaining.
 */
public class Gui {

    private static final Map<Integer, InventoryType> ROW_MAP = new HashMap<>();

    static {
        ROW_MAP.put(1, InventoryType.CHEST_1_ROW);
        ROW_MAP.put(2, InventoryType.CHEST_2_ROW);
        ROW_MAP.put(3, InventoryType.CHEST_3_ROW);
        ROW_MAP.put(4, InventoryType.CHEST_4_ROW);
        ROW_MAP.put(5, InventoryType.CHEST_5_ROW);
        ROW_MAP.put(6, InventoryType.CHEST_6_ROW);
    }

    /** The underlying Minestom inventory backing this GUI. */
    protected final Inventory inventory;

    /** The array of {@link GuiItem} currently placed in the GUI slots. */
    protected final GuiItem[] items;

    /** The number of rows in the GUI (1–6). */
    protected final int rows;

    /** The total number of slots (rows × 9). */
    protected final int size;

    private Pattern pattern;
    private final Map<Character, List<Integer>> patternSlots = new HashMap<>();
    private final Map<UUID, PlayerLockListeners> lockListeners = new ConcurrentHashMap<>();
    private boolean lockPlayerInventory = false;

    /**
     * Bundles the click-lock and disconnect listeners installed on a player's event node while
     * their inventory is locked, so both can be removed together on close or disconnect.
     */
    private record PlayerLockListeners(
            EventListener<InventoryPreClickEvent> clickLock,
            EventListener<PlayerDisconnectEvent> disconnect) {}

    /**
     * Creates a new GUI with the given number of rows and title.
     *
     * @param rows number of rows (1–6)
     * @param title the inventory title
     */
    protected Gui(int rows, Component title) {
        InventoryType type = ROW_MAP.get(rows);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported rows: " + rows + " (1-6)");
        }
        this.rows = rows;
        this.size = rows * 9;
        this.inventory = new Inventory(type, title);
        this.items = new GuiItem[size];

        this.inventory.eventNode().addListener(InventoryPreClickEvent.class, this::handleClick);
        this.inventory
                .eventNode()
                .addListener(
                        InventoryCloseEvent.class,
                        event -> {
                            removeLockListener(event.getPlayer());
                            onClose(event.getPlayer());
                        });
    }

    /**
     * Handles inventory click events, cancelling them and dispatching to the appropriate {@link
     * GuiItem} click handler based on click type.
     */
    private void handleClick(InventoryPreClickEvent event) {
        Click click = event.getClick();

        if (click instanceof net.minestom.server.inventory.click.Click.Drag drag) {
            event.setCancelled(true);
            List<Integer> affected = new ArrayList<>();
            for (int affectedSlot : drag.slots()) {
                if (affectedSlot >= 0 && affectedSlot < size) {
                    affected.add(affectedSlot);
                }
            }
            if (!affected.isEmpty()) {
                MinecraftServer.getSchedulerManager()
                        .scheduleNextTick(
                                () -> {
                                    for (int slot : affected) {
                                        GuiItem expected = items[slot];
                                        inventory.setItemStack(
                                                slot,
                                                expected != null
                                                        ? expected.itemStack()
                                                        : ItemStack.AIR);
                                    }
                                });
            }
            for (int slot : affected) {
                GuiItem guiItem = items[slot];
                if (guiItem != null && guiItem.hasClickHandler()) {
                    guiItem.clickHandler()
                            .accept(
                                    ClickEvent.from(
                                            event.getPlayer(), slot, this, ClickEvent.Type.DRAG));
                }
            }
            return;
        }

        int slot = event.getSlot();

        event.setCancelled(true);

        if (slot < 0 || slot >= size) return;

        GuiItem guiItem = items[slot];
        if (guiItem == null) return;
        if (guiItem.itemStack().isAir() && !guiItem.hasClickHandler()) return;

        if (guiItem.hasClickHandler()) {
            ClickEvent.Type type;
            if (click instanceof net.minestom.server.inventory.click.Click.Left) {
                type = ClickEvent.Type.LEFT_CLICK;
            } else if (click instanceof net.minestom.server.inventory.click.Click.Right) {
                type = ClickEvent.Type.RIGHT_CLICK;
            } else if (click instanceof net.minestom.server.inventory.click.Click.Middle) {
                type = ClickEvent.Type.MIDDLE_CLICK;
            } else if (click instanceof net.minestom.server.inventory.click.Click.LeftShift) {
                type = ClickEvent.Type.SHIFT_LEFT_CLICK;
            } else if (click instanceof net.minestom.server.inventory.click.Click.RightShift) {
                type = ClickEvent.Type.SHIFT_RIGHT_CLICK;
            } else {
                type = ClickEvent.Type.OTHER;
            }
            guiItem.clickHandler().accept(ClickEvent.from(event.getPlayer(), slot, this, type));
        }
    }

    /** Opens the GUI for the given player. Calls {@link #onOpen(Player)} hook. */
    public void open(Player player) {
        if (lockPlayerInventory) {
            lockListeners.computeIfAbsent(
                    player.getUuid(),
                    uuid -> {
                        EventListener<InventoryPreClickEvent> clickLock =
                                EventListener.of(
                                        InventoryPreClickEvent.class,
                                        event -> {
                                            if (player.getOpenInventory() != inventory) return;
                                            event.setCancelled(true);
                                        });
                        EventListener<PlayerDisconnectEvent> disconnect =
                                EventListener.of(
                                        PlayerDisconnectEvent.class,
                                        event -> removeLockListener(event.getPlayer()));
                        player.eventNode().addListener(clickLock);
                        player.eventNode().addListener(disconnect);
                        return new PlayerLockListeners(clickLock, disconnect);
                    });
        }
        player.openInventory(inventory);
        onOpen(player);
    }

    /**
     * Removes the click-lock and disconnect listeners for the given player from its event node and
     * forgets them. Safe to call more than once.
     */
    private void removeLockListener(Player player) {
        PlayerLockListeners lock = lockListeners.remove(player.getUuid());
        if (lock != null) {
            player.eventNode().removeListener(lock.clickLock());
            player.eventNode().removeListener(lock.disconnect());
        }
    }

    /** When enabled, prevents items from being moved into this GUI from the player inventory. */
    public Gui setLockPlayerInventory(boolean lock) {
        this.lockPlayerInventory = lock;
        return this;
    }

    /** Closes the GUI for the given player. */
    public void close(Player player) {
        player.closeInventory();
    }

    /** Returns the underlying Minestom inventory. */
    public Inventory getInventory() {
        return inventory;
    }

    /** Returns the GuiItem at the given slot, or null if out of bounds. */
    public GuiItem getItem(int slot) {
        if (slot < 0 || slot >= size) return null;
        return items[slot];
    }

    /** Places a GuiItem at the given slot. Pass null to clear. */
    public Gui setItem(int slot, GuiItem guiItem) {
        if (slot < 0 || slot >= size) return this;
        items[slot] = guiItem;
        inventory.setItemStack(slot, guiItem != null ? guiItem.itemStack() : ItemStack.AIR);
        return this;
    }

    /** Places an item stack at the given slot with a click handler. */
    public Gui setItem(int slot, ItemStack itemStack, Consumer<ClickEvent> handler) {
        return setItem(slot, new GuiItem(itemStack, handler));
    }

    /** Places a material at the given slot with a click handler. */
    public Gui setItem(int slot, Material material, Consumer<ClickEvent> handler) {
        return setItem(slot, ItemStack.of(material), handler);
    }

    /** Places a non-interactive item stack at the given slot. */
    public Gui setItem(int slot, ItemStack itemStack) {
        return setItem(slot, new GuiItem(itemStack, null));
    }

    /** Places a non-interactive material at the given slot. */
    public Gui setItem(int slot, Material material) {
        return setItem(slot, ItemStack.of(material));
    }

    /**
     * Fills all empty slots with the given GuiItem. Slots that already have a non-air item are left
     * unchanged.
     */
    public Gui fill(GuiItem guiItem) {
        for (int i = 0; i < size; i++) {
            if (items[i] == null || items[i].itemStack().isAir()) {
                setItem(i, guiItem);
            }
        }
        return this;
    }

    /** Fills all empty slots with a background of the given material. */
    public Gui fill(Material material) {
        return fill(GuiItem.background(material));
    }

    /** Fills an entire row with the given GuiItem. */
    public Gui fillRow(int row, GuiItem guiItem) {
        int start = row * 9;
        int end = start + 9;
        for (int i = start; i < end; i++) {
            setItem(i, guiItem);
        }
        return this;
    }

    /** Fills an entire row with a background of the given material. */
    public Gui fillRow(int row, Material material) {
        return fillRow(row, GuiItem.background(material));
    }

    /** Fills the top, bottom, left, and right border slots with the given item. */
    public Gui fillBorders(GuiItem guiItem) {
        for (int i = 0; i < 9; i++) {
            setItem(i, guiItem);
            setItem(size - 1 - i, guiItem);
        }
        for (int i = 0; i < rows; i++) {
            setItem(i * 9, guiItem);
            setItem(i * 9 + 8, guiItem);
        }
        return this;
    }

    /** Fills borders with a background material. */
    public Gui fillBorders(Material material) {
        return fillBorders(GuiItem.background(material));
    }

    /**
     * Applies a {@link Pattern} to this GUI, enabling {@link #bind(char, GuiItem)}.
     *
     * @param pattern the ASCII layout pattern
     * @return this
     */
    public Gui applyPattern(Pattern pattern) {
        this.pattern = pattern;
        for (char key : pattern.keys()) {
            patternSlots.put(key, pattern.slots(key));
        }
        return this;
    }

    /** Binds a GuiItem to all slots matching the given pattern key. */
    public Gui bind(char key, GuiItem guiItem) {
        List<Integer> slots = patternSlots.get(key);
        if (slots != null) {
            for (int slot : slots) {
                setItem(slot, guiItem);
            }
        }
        return this;
    }

    /** Binds an item stack with a click handler to all slots matching the pattern key. */
    public Gui bind(char key, ItemStack itemStack, Consumer<ClickEvent> handler) {
        return bind(key, new GuiItem(itemStack, handler));
    }

    /** Binds a material with a click handler to all slots matching the pattern key. */
    public Gui bind(char key, Material material, Consumer<ClickEvent> handler) {
        return bind(key, ItemStack.of(material), handler);
    }

    /** Binds a non-interactive material to all slots matching the pattern key. */
    public Gui bind(char key, Material material) {
        return bind(key, GuiItem.background(material));
    }

    /** Returns all slot indices that match the given pattern key. */
    public List<Integer> getSlots(char key) {
        return patternSlots.getOrDefault(key, List.of());
    }

    /** Hook called when the GUI is opened for a player. Override to customise. */
    protected void onOpen(Player player) {}

    /** Hook called when the GUI is closed for a player. Override to customise. */
    protected void onClose(Player player) {}

    /** Creates a GUI with the given number of rows and a plain-text title. */
    public static Gui of(int rows, String title) {
        return new Gui(rows, Component.text(title));
    }

    /** Creates a GUI with the given number of rows and a component title. */
    public static Gui of(int rows, Component title) {
        return new Gui(rows, title);
    }

    /**
     * Creates a GUI from a Pattern with a plain-text title. The pattern determines the number of
     * rows.
     */
    public static Gui of(Pattern pattern, String title) {
        return of(pattern, Component.text(title));
    }

    /**
     * Creates a GUI from a Pattern with a component title. The pattern determines the number of
     * rows.
     */
    public static Gui of(Pattern pattern, Component title) {
        int rows = pattern.height();
        Gui gui = new Gui(rows, title);
        gui.applyPattern(pattern);
        return gui;
    }
}

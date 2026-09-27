package com.minebreak.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;

/**
 * Builder for creating {@link ItemStack} instances with custom names and lore.
 *
 * <p>Built items always get a non-italic custom name and non-italic lore lines, plus a {@code
 * TOOLTIP_DISPLAY} component that hides all extra vanilla tooltip sections (enchantments,
 * attributes, etc.).
 *
 * <p>All methods return {@code this} for method chaining.
 */
public final class ItemBuilder {

    private Material material;
    private int amount = 1;
    private Component displayName;
    private boolean glint = false;
    private String itemModelKey;
    private ResolvableProfile headProfile;
    private final List<Component> lore = new ArrayList<>();

    static final Set<DataComponent<?>> HIDDEN_TOOLTIP_COMPONENTS =
            Set.of(
                    DataComponents.ENCHANTMENTS,
                    DataComponents.STORED_ENCHANTMENTS,
                    DataComponents.CAN_PLACE_ON,
                    DataComponents.CAN_BREAK,
                    DataComponents.UNBREAKABLE,
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    DataComponents.DYED_COLOR,
                    DataComponents.POTION_CONTENTS,
                    DataComponents.POTION_DURATION_SCALE,
                    DataComponents.SUSPICIOUS_STEW_EFFECTS,
                    DataComponents.BUNDLE_CONTENTS,
                    DataComponents.CHARGED_PROJECTILES,
                    DataComponents.CONTAINER,
                    DataComponents.CONTAINER_LOOT,
                    DataComponents.JUKEBOX_PLAYABLE,
                    DataComponents.BANNER_PATTERNS,
                    DataComponents.BEES,
                    DataComponents.BLOCK_ENTITY_DATA,
                    DataComponents.BLOCK_STATE,
                    DataComponents.FIREWORK_EXPLOSION,
                    DataComponents.FIREWORKS,
                    DataComponents.INSTRUMENT,
                    DataComponents.MAP_ID,
                    DataComponents.PAINTING_VARIANT,
                    DataComponents.POT_DECORATIONS,
                    DataComponents.TROPICAL_FISH_PATTERN,
                    DataComponents.WRITTEN_BOOK_CONTENT);

    static final TooltipDisplay TOOLTIP_DISPLAY =
            new TooltipDisplay(false, HIDDEN_TOOLTIP_COMPONENTS);

    /**
     * @param material the base material for the item stack
     */
    private ItemBuilder(Material material) {
        this.material = material;
    }

    /** Creates a new builder for the given material. */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    /**
     * Creates a builder for a player head with a custom texture.
     *
     * @param textureBase64 the base64-encoded skin texture value
     * @return a new ItemBuilder for a player head with the given texture
     */
    public static ItemBuilder head(String textureBase64) {
        var builder = new ItemBuilder(Material.PLAYER_HEAD);
        builder.headProfile =
                new ResolvableProfile(
                        new GameProfile(
                                UUID.randomUUID(),
                                "MineBreak",
                                List.of(new GameProfile.Property("textures", textureBase64))));
        return builder;
    }

    /** Sets the stack size. */
    public ItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    /** Sets the custom display name as a Component (supports MiniMessage if pre-rendered). */
    public ItemBuilder name(Component name) {
        this.displayName = name;
        return this;
    }

    /** Sets the custom display name as plain text (no MiniMessage parsing). */
    public ItemBuilder name(String name) {
        return name(Component.text(name));
    }

    /** Makes the item glow with an enchantment glint. */
    public ItemBuilder glint(boolean glint) {
        this.glint = glint;
        return this;
    }

    /**
     * Sets the ITEM_MODEL component (e.g. {@code minebreak:pisun}) for custom resource-pack models.
     */
    public ItemBuilder model(String itemModelKey) {
        this.itemModelKey = itemModelKey;
        return this;
    }

    /** Appends a lore line as a Component. */
    public ItemBuilder lore(Component line) {
        this.lore.add(line);
        return this;
    }

    /** Appends all lore lines from the given list. */
    public ItemBuilder lore(List<Component> lines) {
        this.lore.addAll(lines);
        return this;
    }

    /** Appends multiple lore lines as plain text. */
    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            this.lore.add(Component.text(line));
        }
        return this;
    }

    /** Builds the ItemStack with the configured properties. */
    public ItemStack build() {
        ItemStack.Builder builder = ItemStack.builder(material).amount(amount);
        if (headProfile != null) {
            builder.set(DataComponents.PROFILE, headProfile);
        }
        if (displayName != null) {
            builder.set(
                    DataComponents.CUSTOM_NAME,
                    displayName.decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) {
            List<Component> plainLore = new ArrayList<>(lore.size());
            for (Component line : lore) {
                plainLore.add(line.decoration(TextDecoration.ITALIC, false));
            }
            builder.set(DataComponents.LORE, plainLore);
        }
        if (glint) {
            builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        if (itemModelKey != null && !itemModelKey.isBlank()) {
            builder.set(DataComponents.ITEM_MODEL, itemModelKey);
        }
        builder.set(DataComponents.TOOLTIP_DISPLAY, TOOLTIP_DISPLAY);
        return builder.build();
    }

    /** Builds a background item with an invisible name (space character). */
    public ItemStack asBackground() {
        return name(" ").build();
    }
}

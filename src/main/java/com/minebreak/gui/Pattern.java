package com.minebreak.gui;

import java.util.*;

/**
 * An ASCII-based layout pattern for arranging items in a {@link Gui}.
 *
 * <p>Each character in the pattern represents a "slot group" that can be bound to a {@link GuiItem}
 * via {@link Gui#bind(char, GuiItem)}. Spaces are ignored, allowing visual alignment in source
 * code.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Pattern.of(
 *     "# # # # # # # # #",
 *     "# . . . . . . . #",
 *     "# . . . . . . . #",
 *     "# < - - n - - > #"
 * );
 * }</pre>
 */
public final class Pattern {

    private final int width;
    private final int height;
    private final String data;
    private final Map<Character, List<Integer>> slotsByKey;

    /**
     * Constructs a new pattern with the given dimensions and flat character data.
     *
     * @param width the number of columns
     * @param height the number of rows
     * @param data the flat character data
     */
    private Pattern(int width, int height, String data) {
        this.width = width;
        this.height = height;
        this.data = data;

        Map<Character, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            map.computeIfAbsent(c, k -> new ArrayList<>()).add(i);
        }
        this.slotsByKey = map;
    }

    /**
     * Creates a pattern from ASCII row strings. Spaces are stripped; all rows must have the same
     * length after stripping.
     *
     * @param rows the pattern rows
     * @return the new pattern
     */
    public static Pattern of(String... rows) {
        if (rows.length == 0) {
            throw new IllegalArgumentException("Pattern must have at least one row");
        }
        int width = sanitize(rows[0]).length();
        int height = rows.length;
        StringBuilder sb = new StringBuilder();
        for (String row : rows) {
            String cleaned = sanitize(row);
            if (cleaned.length() != width) {
                throw new IllegalArgumentException("All pattern rows must have the same length");
            }
            sb.append(cleaned);
        }
        return new Pattern(width, height, sb.toString());
    }

    /**
     * Removes all spaces from a pattern row string.
     *
     * @param s the input string
     * @return the sanitised string without spaces
     */
    private static String sanitize(String s) {
        return s.replace(" ", "");
    }

    /** Returns the pattern width (number of columns). */
    public int width() {
        return width;
    }

    /** Returns the pattern height (number of rows). */
    public int height() {
        return height;
    }

    /** Returns the total number of slots (width * height). */
    public int size() {
        return width * height;
    }

    /** Returns the flat character data of the pattern. */
    public String data() {
        return data;
    }

    /** Returns all slot indices matching the given character key. */
    public List<Integer> slots(char key) {
        return slotsByKey.getOrDefault(key, List.of());
    }

    /** Returns the set of character keys used in this pattern. */
    public Set<Character> keys() {
        return slotsByKey.keySet();
    }

    /** Returns the character key at the given flat slot index. */
    public char keyAt(int slot) {
        if (slot < 0 || slot >= data.length()) return ' ';
        return data.charAt(slot);
    }
}

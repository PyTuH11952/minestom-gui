package com.minebreak.gui;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;

public final class GuiSounds {

    private GuiSounds() {}

    public static void playClick(Player player) {
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK, Sound.Source.MASTER, 1f, 1f));
    }

    public static void playSuccess(Player player) {
        player.playSound(
                Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 1f));
    }

    public static void playError(Player player) {
        player.playSound(
                Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 1f, 0.5f));
    }
}

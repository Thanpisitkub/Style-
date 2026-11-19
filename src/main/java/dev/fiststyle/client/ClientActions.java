package dev.fiststyle.client;

import dev.fiststyle.StyleConfig;
import dev.fiststyle.StyleNet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client-side helpers shared by the key bindings and the settings screen. */
public final class ClientActions {
    private ClientActions() {}

    public static boolean parryOn() {
        return StyleConfig.AUTO_PARRY.get();
    }

    public static boolean dodgeOn() {
        return StyleConfig.AUTO_DODGE.get();
    }

    public static void setParry(boolean on) {
        StyleConfig.AUTO_PARRY.set(on);
        saveAndSync();
    }

    public static void setDodge(boolean on) {
        StyleConfig.AUTO_DODGE.set(on);
        saveAndSync();
    }

    public static void saveAndSync() {
        try {
            StyleConfig.AUTO_PARRY.save();
            StyleConfig.AUTO_DODGE.save();
        } catch (Exception ignored) {
            // config file not ready - the value is still kept for this session
        }
        StyleNet.sendAuto(parryOn(), dodgeOn());
    }

    public static Component label(String key, boolean on) {
        return Component.translatable(key, Component.translatable(on ? "gui.style.on" : "gui.style.off"));
    }

    public static void toast(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }
}

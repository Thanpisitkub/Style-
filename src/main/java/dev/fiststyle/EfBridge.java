package dev.fiststyle;

import java.lang.reflect.Method;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Talks to Epic Fight by reflection so this mod needs no compile-time dependency on it.
 * Every signature used here was checked against epicfight-20.14.17-mc1.20.1-forge.jar.
 */
public final class EfBridge {
    private EfBridge() {}

    private static boolean initialised;
    private static boolean available;

    private static Method getEntityPatch;      // EpicFightCapabilities.getEntityPatch(Entity, Class)
    private static Class<?> serverPlayerPatch; // ServerPlayerPatch
    private static Method byKey;               // AnimationManager.byKey(String)
    private static Method playSynchronized;    // LivingEntityPatch.playAnimationSynchronized(AssetAccessor, float)

    private static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        try {
            Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            getEntityPatch = caps.getMethod("getEntityPatch", Entity.class, Class.class);
            serverPlayerPatch = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch");

            Class<?> manager = Class.forName("yesman.epicfight.api.animation.AnimationManager");
            byKey = manager.getMethod("byKey", String.class);

            Class<?> accessor = Class.forName("yesman.epicfight.api.asset.AssetAccessor");

            Class<?> living = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            playSynchronized = living.getMethod("playAnimationSynchronized", accessor, float.class);

            available = true;
        } catch (Throwable t) {
            FistFistStyleMod.LOGGER.error("[style] Could not link to Epic Fight - auto parry/dodge disabled", t);
        }
    }

    /** Plays an Epic Fight animation (e.g. "wom:biped/skill/dodgemaster_back") on the player. */
    public static boolean play(ServerPlayer player, String animationKey) {
        init();
        if (!available) {
            return false;
        }
        try {
            Object patch = getEntityPatch.invoke(null, player, serverPlayerPatch);
            if (patch == null) {
                return false;
            }
            Object accessor = byKey.invoke(null, animationKey);
            if (accessor == null) {
                return false;
            }
            playSynchronized.invoke(patch, accessor, 0.0F);
            return true;
        } catch (Throwable t) {
            FistFistStyleMod.LOGGER.warn("[style] Failed to play animation {}", animationKey, t);
            return false;
        }
    }
}

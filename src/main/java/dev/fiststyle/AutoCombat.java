package dev.fiststyle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Server-side logic:
 *  - Auto Parry / Auto Dodge (toggled from the client by key bindings or the settings screen)
 */
public class AutoCombat {
    private static final int PARRY_COOLDOWN_TICKS = 15;
    private static final int DODGE_COOLDOWN_TICKS = 25;

    private static final String[] PARRY_ANIMATIONS = {
            "wom:biped/skill/solar_guard_parry",
            "wom:biped/skill/herrscher_guard_parry"
    };
    private static final String[] DODGE_ANIMATIONS = {
            "wom:biped/skill/dodgemaster_back",
            "wom:biped/skill/dodgemaster_left",
            "wom:biped/skill/dodgemaster_right"
    };

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static final class State {
        boolean parry;
        boolean dodge;
        long parryReady;
        long dodgeReady;
    }

    public static void setState(ServerPlayer player, boolean parry, boolean dodge) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        state.parry = parry;
        state.dodge = dodge;
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ auto parry / dodge

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null || (!state.parry && !state.dodge)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == player || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return; // only real attacks from other entities (not fall, fire, void, /kill ...)
        }
        if (player.invulnerableTime > 10 || player.isSpectator()) {
            return;
        }

        long now = player.level().getGameTime();
        boolean projectile = source.getDirectEntity() != attacker;

        // melee: parry first, projectiles: dodge first. Falls back to the other one if it is on.
        for (int pass = 0; pass < 2; pass++) {
            boolean tryParry = projectile ? pass == 1 : pass == 0;
            if (tryParry && state.parry && now >= state.parryReady) {
                if (EfBridge.play(player, pick(PARRY_ANIMATIONS, player))) {
                    state.parryReady = now + PARRY_COOLDOWN_TICKS;
                    parryEffects(player, attacker);
                    event.setCanceled(true);
                    return;
                }
            } else if (!tryParry && state.dodge && now >= state.dodgeReady) {
                if (EfBridge.play(player, pick(DODGE_ANIMATIONS, player))) {
                    state.dodgeReady = now + DODGE_COOLDOWN_TICKS;
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    private static String pick(String[] options, ServerPlayer player) {
        return options[player.getRandom().nextInt(options.length)];
    }

    private static void parryEffects(ServerPlayer player, Entity attacker) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.2D, player.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.15D);
        if (attacker instanceof LivingEntity living) {
            living.knockback(0.5D, player.getX() - living.getX(), player.getZ() - living.getZ());
        }
    }
}

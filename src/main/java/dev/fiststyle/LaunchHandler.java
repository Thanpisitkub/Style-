package dev.fiststyle;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Launches the target upward when a player holding a Style item lands that style's combo-4 hit.
 *
 * <p>Epic Fight is not on the compile classpath, so the animation of an Epic Fight damage source is
 * read by reflection only. If the Epic Fight API differs from what is expected, one warning is
 * logged and the hook stays off for the rest of the session; the combos themselves are unaffected. An id
 * that is read fine but differs from the expected format throws nothing, so the first few distinct ids of
 * Epic Fight hits by a Style holder are logged at INFO next to the id expected for combo 4.
 *
 * <p>Vanilla knockback runs after {@link LivingHurtEvent}, so a launch applied there would be
 * overwritten. The target is only marked in {@link #onHurt} and launched on its next tick in
 * {@link #onTick}.
 */
public final class LaunchHandler {
    /** Upward speed in blocks per tick given to the launched target. */
    public static final double LAUNCH_VELOCITY = 0.9;
    /** Duration of the slow falling effect that lets combos 5-6 connect. */
    public static final int SLOW_FALL_TICKS = 25;
    /** Persistent-data key marking a target that is to be launched on its next tick. */
    public static final String FLAG = "fiststyle_launch";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SOURCE_CLASS = "yesman.epicfight.world.damagesource.EpicFightDamageSource";
    private static final String ACCESSOR_CLASS =
            "yesman.epicfight.api.animation.AnimationManager$AnimationAccessor";
    private static final String[] ID_METHODS = {"registryName", "getRegistryName"};

    /** Upper bound on the distinct animation ids {@link #logHit} writes to the log per session. */
    static final int MAX_LOGGED_IDS = 5;
    private static final String NO_ID = "<none>";
    private static final Set<String> LOGGED_IDS = new HashSet<>();

    /** Set once the hook has failed and been logged; afterwards {@link #animationIdOf} returns null. */
    private static final AtomicBoolean DISABLED = new AtomicBoolean(false);

    /** Source class to its public {@code getAnimation} method; empty when the source is not an Epic Fight one. */
    private static final ConcurrentHashMap<Class<?>, Optional<Method>> ANIMATION_GETTERS =
            new ConcurrentHashMap<>();
    /** Accessor class to its public id method; empty when the id has to come from {@code toString()}. */
    private static final ConcurrentHashMap<Class<?>, Optional<Method>> ID_GETTERS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onHurt(LivingHurtEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        DamageSource source = e.getSource();
        ServerPlayer attacker = attackerOf(source);
        if (attacker == null) {
            return;
        }
        Item held = attacker.getMainHandItem().getItem();
        if (!(held instanceof StyleItem style)) {
            return;
        }
        String expected = StyleStats.UPPERCUT[style.styleNumber - 1];
        String id = animationIdOf(source);
        boolean launch = id != null && idMatches(id, expected);
        if (id != null || isEpicFightSource(source)) {
            logHit(style.styleNumber, id, expected, launch);
        }
        if (launch) {
            target.getPersistentData().putBoolean(FLAG, true);
        }
    }

    @SubscribeEvent
    public void onTick(LivingEvent.LivingTickEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        if (!data.getBoolean(FLAG)) {
            return;
        }
        data.remove(FLAG);
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, LAUNCH_VELOCITY, motion.z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
    }

    /** True when reflection already resolved {@code source} as an Epic Fight source and the hook is still on. */
    private static boolean isEpicFightSource(DamageSource source) {
        Optional<Method> getter = ANIMATION_GETTERS.get(source.getClass());
        return !DISABLED.get() && getter != null && getter.isPresent();
    }

    /**
     * Logs what an Epic Fight hit by a Style holder looked like, so an id mismatch (which throws nothing
     * and so triggers no warning) can still be diagnosed from the log. Capped at {@link #MAX_LOGGED_IDS}
     * distinct ids per session; {@code id == null} means no id could be read from the Epic Fight source.
     */
    private static void logHit(int styleNumber, String id, String expected, boolean launch) {
        if (!firstSighting(id == null ? NO_ID : id)) {
            return;
        }
        if (id == null) {
            LOGGER.info("[Style] Epic Fight hit by Style {}: no animation id could be read "
                    + "(animation missing or unreadable), combo-4 id is '{}'", styleNumber, expected);
        } else {
            LOGGER.info("[Style] Epic Fight hit by Style {}: read animation id '{}', combo-4 id is '{}', match={}",
                    styleNumber, id, expected, launch);
        }
    }

    private static synchronized boolean firstSighting(String key) {
        return LOGGED_IDS.size() < MAX_LOGGED_IDS && LOGGED_IDS.add(key);
    }

    private static ServerPlayer attackerOf(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    /**
     * Returns the registry id of the Epic Fight animation behind {@code source}, or the accessor's
     * {@code toString()} when no id method exists. Returns {@code null} when the source is not an Epic
     * Fight damage source, carries no animation, or the Epic Fight API could not be read (logged once).
     */
    static String animationIdOf(DamageSource source) {
        if (source == null || DISABLED.get()) {
            return null;
        }
        try {
            Optional<Method> getter = ANIMATION_GETTERS.computeIfAbsent(source.getClass(), LaunchHandler::findAnimationGetter);
            if (getter.isEmpty()) {
                return null;
            }
            Object accessor = getter.get().invoke(source);
            if (accessor == null) {
                return null;
            }
            Optional<Method> idGetter = ID_GETTERS.computeIfAbsent(accessor.getClass(), LaunchHandler::findIdGetter);
            if (idGetter.isPresent()) {
                Object id = idGetter.get().invoke(accessor);
                if (id != null) {
                    return id.toString();
                }
            }
            return accessor.toString();
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    private static Optional<Method> findAnimationGetter(Class<?> sourceClass) {
        try {
            Class<?> epicFightSource = Class.forName(SOURCE_CLASS, false, LaunchHandler.class.getClassLoader());
            if (!epicFightSource.isAssignableFrom(sourceClass)) {
                return Optional.empty();
            }
            return Optional.of(epicFightSource.getMethod("getAnimation"));
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("Epic Fight damage source API not found", e);
        }
    }

    /**
     * Looks the id method up on the public accessor interface first, then on the runtime class. A method
     * whose declaring class is not public is skipped, since invoking it would throw IllegalAccessException.
     */
    private static Optional<Method> findIdGetter(Class<?> accessorClass) {
        Class<?> accessorInterface = null;
        try {
            Class<?> candidate = Class.forName(ACCESSOR_CLASS, false, LaunchHandler.class.getClassLoader());
            if (candidate.isAssignableFrom(accessorClass)) {
                accessorInterface = candidate;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            // Use the runtime class only.
        }
        for (String name : ID_METHODS) {
            for (Class<?> owner : new Class<?>[] {accessorInterface, accessorClass}) {
                if (owner == null) {
                    continue;
                }
                try {
                    Method method = owner.getMethod(name);
                    if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                        return Optional.of(method);
                    }
                } catch (NoSuchMethodException e) {
                    // Try the next owner, then the next name, then toString().
                }
            }
        }
        return Optional.empty();
    }

    /**
     * True when {@code found} is {@code expected}, or (for a {@code toString()} fallback) contains it as a
     * whole id, so {@code kick_1} does not match {@code kick_10}.
     */
    static boolean idMatches(String found, String expected) {
        if (expected.isEmpty()) {
            return false;
        }
        int from = 0;
        while (true) {
            int at = found.indexOf(expected, from);
            if (at < 0) {
                return false;
            }
            int end = at + expected.length();
            boolean startsId = at == 0 || !isIdChar(found.charAt(at - 1));
            boolean endsId = end == found.length() || !isIdChar(found.charAt(end));
            if (startsId && endsId) {
                return true;
            }
            from = at + 1;
        }
    }

    private static boolean isIdChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '/' || c == ':';
    }

    private static void disable(Throwable cause) {
        if (DISABLED.compareAndSet(false, true)) {
            LOGGER.warn("[Style] Launch hook disabled: could not read the Epic Fight animation from a damage source "
                    + "(the Epic Fight API probably differs from what this mod expects). Combo 4 will not launch "
                    + "targets; the combos themselves still work.", cause);
        }
    }
}

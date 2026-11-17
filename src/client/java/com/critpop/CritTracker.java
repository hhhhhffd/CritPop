package com.critpop;

import java.util.LinkedHashMap;

/** No visibility, playback, or prediction assumptions: require an attributed packet sequence. */
public final class CritTracker {
    public static final long ATTACK_WINDOW_MS = 2000;
    public static final long DAMAGE_WINDOW_MS = 225;
    private static final long SOUND_WINDOW_MS = 160;
    private static final int MAX_ATTEMPTS = 24;
    private final LinkedHashMap<Integer, Attempt> attempts = new LinkedHashMap<>();
    private Confirmation pending;

    public void clear() {
        attempts.clear();
        pending = null;
    }

    public void attack(int target, long now, double x, double y, double z) {
        attempts.values().removeIf(entry -> !within(now, entry.time, ATTACK_WINDOW_MS));
        // Update the aim and origin, but one target entry cannot generate more than one pop.
        attempts.remove(target);
        if (attempts.size() >= MAX_ATTEMPTS) attempts.pollFirstEntry();
        attempts.put(target, new Attempt(now, x, y, z));
    }

    public void damage(int target, boolean ownDirectMelee, long now) {
        if (pending != null && pending.target == target) pending = null;
        if (!ownDirectMelee) return;
        Attempt attempt = attempts.remove(target);
        if (attempt == null || !within(now, attempt.time, ATTACK_WINDOW_MS)) return;
        pending = new Confirmation(target, now, attempt);
    }

    public void sound(boolean critical, long now, double x, double y, double z,
                      double playerX, double playerY, double playerZ) {
        Confirmation hit = pending;
        if (hit == null) return;
        if (!within(now, hit.damageAt, DAMAGE_WINDOW_MS)) {
            pending = null;
            return;
        }
        boolean near = near(x, y, z, hit.attempt.x, hit.attempt.y, hit.attempt.z)
                    || near(x, y, z, playerX, playerY, playerZ);
        if (!near) return;
        if (critical) {
            hit.heard = true;
            hit.soundAt = now;
        } else {
            // Vanilla normal melee emits its weak/strong/sweep sound, not its critical sound.
            pending = null;
        }
    }

    public boolean critAnimation(int target, long now) {
        Confirmation hit = pending;
        if (hit == null || hit.target != target) return false;
        if (!within(now, hit.damageAt, DAMAGE_WINDOW_MS)) {
            pending = null;
            return false;
        }
        if (!hit.heard || !within(now, hit.soundAt, SOUND_WINDOW_MS)) return false;
        pending = null;
        return true;
    }

    private static boolean near(double x, double y, double z, double px, double py, double pz) {
        double dx = x - px;
        double dz = z - pz;
        return dx * dx + dz * dz <= 1.6 * 1.6 && Math.abs(y - py) <= 2.5;
    }

    private static boolean within(long now, long start, long limit) {
        return now >= start && now - start <= limit;
    }

    private record Attempt(long time, double x, double y, double z) { }

    private static final class Confirmation {
        final int target;
        final long damageAt;
        final Attempt attempt;
        boolean heard;
        long soundAt;
        Confirmation(int target, long damageAt, Attempt attempt) {
            this.target = target;
            this.damageAt = damageAt;
            this.attempt = attempt;
        }
    }
}

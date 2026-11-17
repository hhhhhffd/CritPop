package com.critpop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CritPop {
    private static final CritTracker TRACKER = new CritTracker();
    private static final PopOverlay OVERLAY = new PopOverlay();
    private static LocalPlayer player;
    private static ClientLevel level;

    private static Minecraft context() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player || mc.level != level) {
            player = mc.player;
            level = mc.level;
            TRACKER.clear();
            OVERLAY.clear();
        }
        return mc;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    public static void attack(Player attacker, Entity target) {
        context();
        if (level == null || player == null || attacker != player || !(target instanceof LivingEntity)) return;
        TRACKER.attack(target.getId(), now(), player.getX(), player.getY(), player.getZ());
    }

    public static void damage(ClientboundDamageEventPacket packet) {
        context();
        if (level == null || player == null) return;
        int self = player.getId();
        boolean own = packet.sourceCauseId() == self && packet.sourceDirectId() == self
                && (packet.sourceType().is(DamageTypes.PLAYER_ATTACK)
                    || packet.sourceType().is(DamageTypes.MACE_SMASH));
        TRACKER.damage(packet.entityId(), own, now());
    }

    public static void sound(ClientboundSoundPacket packet) {
        context();
        if (level == null || player == null || packet.getSource() != SoundSource.PLAYERS) return;
        SoundEvent sound = packet.getSound().value();
        boolean crit = SoundEvents.PLAYER_ATTACK_CRIT.equals(sound);
        if (crit || SoundEvents.PLAYER_ATTACK_STRONG.equals(sound)
                || SoundEvents.PLAYER_ATTACK_SWEEP.equals(sound)
                || SoundEvents.PLAYER_ATTACK_WEAK.equals(sound)
                || SoundEvents.PLAYER_ATTACK_NODAMAGE.equals(sound)) {
            TRACKER.sound(crit, now(), packet.getX(), packet.getY(), packet.getZ(),
                    player.getX(), player.getY(), player.getZ());
        }
    }

    public static void animate(ClientboundAnimatePacket packet) {
        context();
        if (level == null || player == null || packet.getAction() != ClientboundAnimatePacket.CRITICAL_HIT) return;
        long now = now();
        if (TRACKER.critAnimation(packet.getId(), now)) {
            CritPopConfig cfg = CritPopConfig.get();
            if (cfg.enabled && !cfg.phrases.isEmpty()) OVERLAY.pop(cfg.phrases, now);
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = context();
        if (level == null || player == null || mc.options.hideGui || mc.screen != null) return;
        if (CritPopConfig.get().enabled) OVERLAY.render(graphics, mc.font, now());
    }
}

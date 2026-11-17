package com.critpop.mixin;

import com.critpop.CritPop;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class CombatPacketMixin {
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void critpop$recordDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        CritPop.damage(packet);
    }

    @Inject(method = "handleSoundEvent", at = @At("TAIL"))
    private void critpop$recordAttackSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        CritPop.sound(packet);
    }

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void critpop$confirmCritical(ClientboundAnimatePacket packet, CallbackInfo ci) {
        CritPop.animate(packet);
    }
}

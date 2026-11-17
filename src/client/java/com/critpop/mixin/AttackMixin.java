package com.critpop.mixin;

import com.critpop.CritPop;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class AttackMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void critpop$recordAttack(Player attacker, Entity target, CallbackInfo ci) {
        CritPop.attack(attacker, target);
    }
}

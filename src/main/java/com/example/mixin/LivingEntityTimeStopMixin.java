package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 时停 LivingEntity 层：生物真正停住 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTimeStopMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeLivingTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }
}

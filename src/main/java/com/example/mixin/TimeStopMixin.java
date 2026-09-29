package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时停——Entity 层：
 * 只冻结 tick / setVelocity，不碰 setPos（保护实体生成）。
 * 适用于非生物实体（掉落物等）。
 */
@Mixin(Entity.class)
public abstract class TimeStopMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }

    @Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeVelocity(Vec3d velocity, CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }
}

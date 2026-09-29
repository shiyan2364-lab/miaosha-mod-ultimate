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
 * 真·时停（1.16.5）：
 * 1. tick → AI / 状态更新 / 掉落物物理停止
 * 2. setPos → 位置更新入口被冻结，实体无法移动（消除抽搐）
 * 3. setVelocity → 速度无法写入，击退/重力全失效
 * 玩家不受影响。
 */
@Mixin(Entity.class)
public abstract class TimeStopMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }

    @Inject(method = "setPos", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeSetPos(double x, double y, double z, CallbackInfo ci) {
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

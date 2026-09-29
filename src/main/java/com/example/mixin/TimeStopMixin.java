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
 * 1. 冻结 tick → 生物 AI / 动画状态 / 掉落物物理全部停止
 * 2. 冻结 move → 位置永久不动（消除身体抽搐）
 * 3. 冻结 setVelocity → 速度不能写入（击退/重力/漂移全失效）
 * 玩家不受影响，可正常行动。
 */
@Mixin(Entity.class)
public abstract class TimeStopMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeMove(Entity.MovementType type, Vec3d movement, CallbackInfo ci) {
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

package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时停——LivingEntity 层：
 * 拦截生物自己的 tick（Mixin 不会自动拦截子类覆写的方法）。
 * 只有这层被冻结，猪/僵尸/凋零这类生物才真正停住。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTimeStopMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeLivingTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }
}

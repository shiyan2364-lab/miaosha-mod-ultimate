package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * 1.16.5 安全版：只注入确定存在的方法
 * 被湮灭剑标记的实体：tick 掐掉/setHealth 拦截/getHealth 返回0/isAlive 返回false
 */
@Mixin(LivingEntity.class)
public abstract class EntityEraseMixin {

    @Unique
    private boolean miaosha$erased() {
        return MiaoShaMod.ERASED_ENTITIES
                .contains(((LivingEntity) (Object) this).getUuid());
    }

    @Unique
    private void miaosha$reflectZeroHealth() {
        try {
            Field f = LivingEntity.class.getDeclaredField("health");
            f.setAccessible(true);
            f.setFloat((LivingEntity) (Object) this, 0F);
        } catch (Exception ignored) {
        }
    }

    @Unique
    private void miaosha$reflectRemoved() {
        try {
            Field f = Entity.class.getDeclaredField("removed");
            f.setAccessible(true);
            f.setBoolean((Entity) (Object) this, true);
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$noTick(CallbackInfo ci) {
        if (miaosha$erased()) {
            miaosha$reflectZeroHealth();
            miaosha$reflectRemoved();
            ci.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void miaosha$noSetHealth(float health, CallbackInfo ci) {
        if (miaosha$erased()) ci.cancel();
    }

    @Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
    private void miaosha$zeroHealth(CallbackInfoReturnable<Float> cir) {
        if (miaosha$erased()) cir.setReturnValue(0F);
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void miaosha$notAlive(CallbackInfoReturnable<Boolean> cir) {
        if (miaosha$erased()) cir.setReturnValue(false);
    }
}

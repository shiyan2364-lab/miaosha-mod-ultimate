package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 时停 Entity 层：掉落物/箭矢停止，保留速度字段 → 朝向不丢 */
@Mixin(Entity.class)
public abstract class TimeStopMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeEntityTick(CallbackInfo ci) {
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }
}

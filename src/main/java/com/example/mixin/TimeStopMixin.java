package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TimeStopMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void miaosha$freezeTime(CallbackInfo ci) {
        // 时停开启，且当前实体不是玩家，则取消 tick（冻结）
        if (MiaoShaMod.TIME_STOPPED && !((Object) this instanceof PlayerEntity)) {
            ci.cancel();
        }
    }
}

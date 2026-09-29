package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void miaosha$attackErase(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld)) return;

        ItemStack held = self.getMainHandStack();
        Identifier id = Registries.ITEM.getId(held.getItem());
        if (id.equals(Identifier.of("miaosha-mod-ultimate", "miaosha_erase_sword"))) {
            if (!(target instanceof LivingEntity living) || living == self) return;
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;

            // 主目标硬抹除
            MiaoShaMod.hardErase(living);
            // 5 格连锁 + 落雷 + 反馈
            MiaoShaMod.chainErase(living, self);
            ci.cancel();
        } else if (id.equals(Identifier.of("miaosha-mod-ultimate", "miaosha_sword"))) {
            if (!(target instanceof LivingEntity living) || living == self) return;
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            // 秒杀剑：一击必杀，掉落保留
            living.setHealth(0.01f);
        }
    }
}

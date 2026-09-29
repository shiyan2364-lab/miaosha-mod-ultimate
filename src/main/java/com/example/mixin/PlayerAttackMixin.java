package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通杀核心：注入玩家"攻击"入口 PlayerEntity.attack(Entity)
 * 湮灭剑 → hardErase 硬抹除
 * 秒杀剑 → setHealth(0.01) 一击必杀（掉落保留）
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void miaosha$attackErase(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (!(self.world instanceof ServerWorld)) return;

        ItemStack held = self.getMainHandStack();
        Identifier id = Registry.ITEM.getId(held.getItem());

        boolean isErase = id.equals(new Identifier("miaosha-mod-ultimate", "miaosha_erase_sword"));
        boolean isKill  = id.equals(new Identifier("miaosha-mod-ultimate", "miaosha_sword"));
        if (!isErase && !isKill) return;

        if (!(target instanceof LivingEntity)) return;
        LivingEntity living = (LivingEntity) target;
        if (living == self) return;

        if (isErase) {
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            MiaoShaMod.hardErase(living);
            MiaoShaMod.LOGGER.info("{} UNIVERSAL-ERASED by attack", living.getDisplayName().getString());
        } else {
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            living.setHealth(0.01f);
        }
    }
}

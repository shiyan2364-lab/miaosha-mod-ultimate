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

import java.lang.reflect.Field;

/**
 * 通杀核心：注入玩家"攻击"入口 PlayerEntity.attack(Entity)
 * 无论目标是否无敌/锁血/覆写 damage/防移除，只要玩家挥刀打它 → 强制抹除
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void miaosha$attackErase(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        // 只在服务端处理，避免客户端重复
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
            // ===== 通杀：强制抹除，绕过一切无敌/保护机制 =====
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            MiaoShaMod.ERASED_ENTITIES.add(living.getUuid());

            // 反射清零血字段
            try {
                Field f = LivingEntity.class.getDeclaredField("health");
                f.setAccessible(true);
                f.setFloat(living, 0F);
            } catch (Exception ignored) {}
            // 反射设 removed=true
            try {
                Field f = Entity.class.getDeclaredField("removed");
                f.setAccessible(true);
                f.setBoolean(living, true);
            } catch (Exception ignored) {}
            // 常规手段兜底
            living.setHealth(0F);
            try { living.requestTeleport(living.getX(), -300D, living.getZ()); } catch (Exception ignored) {}
            living.remove();

            MiaoShaMod.LOGGER.info("{} UNIVERSAL-ERASED by attack", living.getDisplayName().getString());
            // 不 cancel，让攻击动作照常走完（击退/动画），目标已死
        } else {
            // ===== 秒杀剑：一击必杀（掉落保留） =====
            if (living instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            living.setHealth(0.01f);
        }
    }
}

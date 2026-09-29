package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Shadow public abstract boolean isDead();
    @Shadow public abstract void setHealth(float health);

    private static void reflectSetHealth(LivingEntity entity, float value) {
        try {
            Field f = LivingEntity.class.getDeclaredField("health");
            f.setAccessible(true);
            f.setFloat(entity, value);
        } catch (Exception ignored) {
            entity.setHealth(value);
        }
    }

    private static void reflectSetRemoved(Entity entity, boolean removed) {
        try {
            Field f = Entity.class.getDeclaredField("removed");
            f.setAccessible(true);
            f.setBoolean(entity, removed);
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void miaosha$instantKill(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof PlayerEntity)) return;

        PlayerEntity attacker = (PlayerEntity) source.getAttacker();
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.isDead() || self == attacker) return;

        ItemStack held = attacker.getMainHandStack();
        Identifier heldId = Registry.ITEM.getId(held.getItem());

        // ===== 湮灭之剑：深层硬抹除 =====
        if (heldId.equals(new Identifier("miaosha-mod-ultimate", "miaosha_erase_sword"))) {
            // 玩家目标：直接造成必死伤害（血条会归零消失），不 remove 玩家以免客户端怪异
            if (self instanceof PlayerEntity) {
                if (!MiaoShaMod.KILL_PLAYER) return;
                MiaoShaMod.ERASED_ENTITIES.add(self.getUuid());
                // 借用致命虚空伤害：血条直接清空，并强制走掉线式死亡
                self.setHealth(0F);
                self.kill();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            // 普通生物 / 凋灵：永久标记 + 反射清零 + 虚空 + remove + cancel
            MiaoShaMod.ERASED_ENTITIES.add(self.getUuid());
            reflectSetHealth(self, 0F);
            try {
                self.requestTeleport(self.getX(), -300D, self.getZ());
            } catch (Exception ignored) {
            }
            reflectSetRemoved(self, true);
            try {
                self.remove();
            } catch (Exception ignored) {
                self.kill();
            }
            cir.setReturnValue(true);
            cir.cancel();
            MiaoShaMod.LOGGER.info("{} was HARD-ERASED", self.getDisplayName().getString());
            return;
        }

        // ===== 秒杀之剑：一击必杀（掉落保留） =====
        if (heldId.equals(new Identifier("miaosha-mod-ultimate", "miaosha_sword"))) {
            if (self instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            reflectSetHealth(self, 0.01f);
        }
    }
}

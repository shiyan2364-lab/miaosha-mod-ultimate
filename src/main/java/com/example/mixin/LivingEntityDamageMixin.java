package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void miaosha$instantKill(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof PlayerEntity)) return;

        PlayerEntity attacker = (PlayerEntity) source.getAttacker();
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == attacker) return;

        ItemStack held = attacker.getMainHandStack();
        Identifier heldId = Registry.ITEM.getId(held.getItem());

        // 湮灭之剑：硬抹除
        if (heldId.equals(new Identifier("miaosha-mod-ultimate", "miaosha_erase_sword"))) {
            if (self instanceof PlayerEntity) {
                if (!MiaoShaMod.KILL_PLAYER) return;
                self.setHealth(0F);
                self.kill();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            MiaoShaMod.hardErase(self);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // 秒杀之剑：一击必杀（掉落保留）
        if (heldId.equals(new Identifier("miaosha-mod-ultimate", "miaosha_sword"))) {
            if (self instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            self.setHealth(0.01f);
        }
    }
}

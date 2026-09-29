package com.example.mixin;

import com.example.MiaoShaMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void miaosha$instantKill(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getAttacker() instanceof PlayerEntity attacker)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == attacker) return;

        ItemStack held = attacker.getMainHandStack();
        Identifier heldId = Registries.ITEM.getId(held.getItem());

        if (heldId.equals(Identifier.of("miaosha-mod-ultimate", "miaosha_erase_sword"))) {
            if (self instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            MiaoShaMod.hardErase(self);
            MiaoShaMod.chainErase(self, attacker);
            cir.setReturnValue(true);
            cir.cancel();
        } else if (heldId.equals(Identifier.of("miaosha-mod-ultimate", "miaosha_sword"))) {
            if (self instanceof PlayerEntity && !MiaoShaMod.KILL_PLAYER) return;
            self.setHealth(0.01f);
        }
    }
}

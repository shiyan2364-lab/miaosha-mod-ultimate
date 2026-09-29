package com.example.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.World;

import java.lang.reflect.Field;

public class UnkillableEntity extends PigEntity {
    public static final float MAX_HEALTH = 2000.0F;
    public static final float DAMAGE_CAP = 4.0F;

    public UnkillableEntity(EntityType<UnkillableEntity> type, World world) {
        super(type, world);
        this.setHealth(MAX_HEALTH);
    }

    @Override
    public void tick() {
        try {
            Field hf = LivingEntity.class.getDeclaredField("health");
            hf.setAccessible(true);
            if (hf.getFloat(this) <= 0.0F) hf.setFloat(this, MAX_HEALTH);
        } catch (Exception ignored) {}
        try {
            Field rf = Entity.class.getDeclaredField("removed");
            rf.setAccessible(true);
            if (rf.getBoolean(this)) rf.setBoolean(this, false);
        } catch (Exception ignored) {}
        super.tick();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(DamageTypes.OUT_OF_WORLD)
                || source.isOf(DamageTypes.IN_FIRE)
                || source.isOf(DamageTypes.ON_FIRE)
                || source.isOf(DamageTypes.LAVA)
                || source.isOf(DamageTypes.EXPLOSION)
                || source.isOf(DamageTypes.FALL)
                || source.isOf(DamageTypes.CACTUS)
                || source.isOf(DamageTypes.DROWN)
                || source.isOf(DamageTypes.LIGHTNING_BOLT)
                || source.isOf(DamageTypes.MAGIC)
                || source.isOf(DamageTypes.WITHER)
                || source.isOf(DamageTypes.STARVE)
                || source.isOf(DamageTypes.IN_WALL)) return false;
        return super.damage(source, Math.min(amount, DAMAGE_CAP));
    }

    @Override
    public void setHealth(float health) {
        if (health > 0.0F) super.setHealth(health);
    }

    @Override
    public float getHealth() {
        float real = super.getHealth();
        return real <= 0.0F ? 1.0F : real;
    }

    @Override
    public void kill() { }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED && this.isDead()) super.remove(reason);
    }
}

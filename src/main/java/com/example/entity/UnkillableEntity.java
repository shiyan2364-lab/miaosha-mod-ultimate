package com.example.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.World;

import java.lang.reflect.Field;

/**
 * 无敌之盾：无攻击能力，靠自身机制硬扛一切。
 * - 限伤：单次最多 4 点
 * - 环境伤害免疫（虚空/火/爆炸/摔落/溺水/魔法等）
 * - setHealth(<=0) 忽略
 * - getHealth() 至少返回 1，防止被判定死亡
 * - kill() 空实现，免疫/kill
 * - remove() 仅在自然死亡时生效（硬抹除会被拦截）
 * - isRemoved() 永远 false（引擎认为它一直在）
 * - tick() 每帧反射修复 health / removed 字段（被强改也拉回来）
 */
public class UnkillableEntity extends PigEntity {

    public static final float MAX_HEALTH = 2000.0F;
    public static final float DAMAGE_CAP = 4.0F;

    public UnkillableEntity(EntityType<? extends PigEntity> type, World world) {
        super(type, world);
        this.setHealth(MAX_HEALTH);
    }

    @Override
    public void tick() {
        try {
            Field hf = LivingEntity.class.getDeclaredField("health");
            hf.setAccessible(true);
            if (hf.getFloat(this) <= 0.0F) {
                hf.setFloat(this, MAX_HEALTH);
            }
        } catch (Exception ignored) {}
        try {
            Field rf = Entity.class.getDeclaredField("removed");
            rf.setAccessible(true);
            if (rf.getBoolean(this)) {
                rf.setBoolean(this, false);
            }
        } catch (Exception ignored) {}
        super.tick();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOutOfWorld() || source.isFire() || source.isExplosive()
                || source == DamageSource.FALL || source == DamageSource.CACTUS
                || source == DamageSource.DROWN || source == DamageSource.LIGHTNING_BOLT
                || source == DamageSource.MAGIC || source == DamageSource.WITHER
                || source == DamageSource.STARVE || source == DamageSource.IN_WALL) {
            return false;
        }
        return super.damage(source, Math.min(amount, DAMAGE_CAP));
    }

    @Override
    public void setHealth(float health) {
        if (health > 0.0F) {
            super.setHealth(health);
        }
    }

    @Override
    public float getHealth() {
        float real = super.getHealth();
        return real <= 0.0F ? 1.0F : real;
    }

    @Override
    public void kill() {
        // 免疫强制击杀
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 只有自然死亡（KILLED）才允许移除，其余全部拦截
        if (reason == Entity.RemovalReason.KILLED && this.isDead()) {
            super.remove(reason);
        }
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public void setRemoved(Entity.RemovalReason reason) {
        // 强移除也被拦截
    }

    @Override
    public boolean isAlive() {
        return true;
    }
}

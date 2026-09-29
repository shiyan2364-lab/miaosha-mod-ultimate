package com.example.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.world.World;

import java.lang.reflect.Field;

/**
 * 矛盾之盾生物：无攻击能力，但极难杀死。
 * 防御（完全不针对武器，只看自身机制）：
 * - 限伤：单次最多扣 4 点
 * - 环境伤害免疫（虚空/火/爆炸等）
 * - setHealth(<=0) 忽略
 * - getHealth 至少返回 1
 * - remove() 完全忽略
 * - isRemoved() 永远 false（防止引擎判定已移除）
 * - kill() 拒绝
 * - tick() 每帧反射修复 health/removed 字段
 * 血量 2000，纯被动无攻击。
 */
public class UnkillableEntity extends PigEntity {

    public static final float MAX_HEALTH = 2000.0F;
    public static final float DAMAGE_CAP = 4.0F;

    public UnkillableEntity(EntityType<? extends PigEntity> entityType, World world) {
        super(entityType, world);
        this.setHealth(MAX_HEALTH);
    }

    @Override
    public void tick() {
        // 反射防御：每帧修复被外部强制修改的字段
        try {
            Field hf = net.minecraft.entity.LivingEntity.class.getDeclaredField("health");
            hf.setAccessible(true);
            if (hf.getFloat(this) <= 0.0F) {
                hf.setFloat(this, MAX_HEALTH);
            }
        } catch (Exception ignored) {}

        try {
            Field rf = net.minecraft.entity.Entity.class.getDeclaredField("removed");
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
                || source == DamageSource.MAGIC || source == DamageSource.WITHER) {
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
        // 即使底层血量变成 0，对外读出来至少是 1，避免被判定死亡
        float real = super.getHealth();
        return real <= 0.0F ? 1.0F : real;
    }

    @Override
    public void remove() {
        // 完全忽略移除请求
    }

    /**
     * 注意：1.16.5 中 Entity 没有 public isRemoved() 可覆写方法（已删除）。
     * 本生物的防御由 remove() 空实现 + tick() 反射修复 removed 字段承担。
     */
    // （isRemoved 删除，移除防御由 remove() + tick 反射修复负责）

    @Override
    public void kill() {
        // 免疫 /kill
    }
}

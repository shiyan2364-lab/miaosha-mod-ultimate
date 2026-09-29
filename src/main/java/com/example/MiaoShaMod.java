package com.example;

import com.example.entity.UnkillableEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MiaoShaMod implements ModInitializer {

    public static final String MOD_ID = "miaosha-mod-ultimate";
    public static final Logger LOGGER = LogManager.getLogger("miaosha-mod-ultimate");

    public static final boolean KILL_PLAYER = true;
    public static final int ERASE_RADIUS = 50;

    public static Item MIAOSHA_SWORD;
    public static Item MIAOSHA_ERASE_SWORD;
    public static Item UNKILLABLE_SPAWN_EGG;

    public static EntityType<UnkillableEntity> UNKILLABLE_ENTITY_TYPE;

    public static final Set<UUID> ERASED_ENTITIES = new HashSet<>();

    /** 时停状态：true=已停止 */
    public static boolean TIME_STOPPED = false;

    @Override
    public void onInitialize() {
        MIAOSHA_SWORD = Registry.register(
                Registry.ITEM,
                new Identifier(MOD_ID, "miaosha_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -2.4F,
                        new Item.Settings().group(ItemGroup.COMBAT))
        );

        MIAOSHA_ERASE_SWORD = Registry.register(
                Registry.ITEM,
                new Identifier(MOD_ID, "miaosha_erase_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -0.4F,
                        new Item.Settings().group(ItemGroup.COMBAT))
        );

        UNKILLABLE_ENTITY_TYPE = Registry.register(
                Registry.ENTITY_TYPE,
                new Identifier(MOD_ID, "unkillable"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UnkillableEntity::new)
                        .dimensions(EntityDimensions.fixed(0.9F, 0.9F))
                        .trackRangeBlocks(128)
                        .build()
        );

        FabricDefaultAttributeRegistry.register(UNKILLABLE_ENTITY_TYPE,
                UnkillableEntity.createMobAttributes()
                        .add(EntityAttributes.GENERIC_MAX_HEALTH, UnkillableEntity.MAX_HEALTH)
                        .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D)
        );

        UNKILLABLE_SPAWN_EGG = Registry.register(
                Registry.ITEM,
                new Identifier(MOD_ID, "unkillable_spawn_egg"),
                new SpawnEggItem(UNKILLABLE_ENTITY_TYPE, 0xFFB6C1, 0xCD5C5C,
                        new Item.Settings().group(ItemGroup.MISC))
        );

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (world.isClient) return TypedActionResult.pass(held);
            Identifier heldId = Registry.ITEM.getId(held.getItem());
            if (heldId.equals(new Identifier(MOD_ID, "miaosha_erase_sword"))) {
                eraseArea(player);
                return TypedActionResult.success(held);
            }
            return TypedActionResult.pass(held);
        });

        LOGGER.info("MiaoSha Mod Ultimate loaded [MC 1.16.5] - True Paradox");
    }

    private static void eraseArea(PlayerEntity player) {
        if (player.getServer() == null) return;
        int r = ERASE_RADIUS;
        Box box = new Box(
                player.getX() - r, player.getY() - r, player.getZ() - r,
                player.getX() + r, player.getY() + r, player.getZ() + r
        );

        int cleared = 0;
        for (ServerWorld world : player.getServer().getWorlds()) {
            List<Entity> entities = world.getOtherEntities(player, box, e -> true);
            for (Entity e : entities) {
                boolean isMob = e instanceof LivingEntity && !(e instanceof PlayerEntity);
                boolean isItem = e instanceof ItemEntity;
                if (isMob || isItem) {
                    hardErase(e);
                    cleared++;
                }
            }
        }
        LOGGER.info("Area erase: {} entities removed in {}x{}", cleared, r * 2, r * 2);
    }

    /**
     * 终极抹除：多重手段硬移除（绕过一切覆写/防御）
     * 1. 反射清零血量
     * 2. 反射直接设 removed=true（绕过子类覆写）
     * 3. 反射调用 setRemoved(DISCARDED) 走引擎强制移除
     * 4. 从世界 EntityList 摘除
     * 5. 发包强制客户端移除
     */
    public static void hardErase(Entity entity) {
        if (entity == null) return;

        // 1. 反射清零血量
        if (entity instanceof LivingEntity) {
            try {
                Field f = LivingEntity.class.getDeclaredField("health");
                f.setAccessible(true);
                f.setFloat(entity, 0F);
            } catch (Exception ignored) {}
        }

        // 2. 反射直接设 removed=true（绕过所有覆写）
        try {
            Field rf = Entity.class.getDeclaredField("removed");
            rf.setAccessible(true);
            rf.setBoolean(entity, true);
        } catch (Exception ignored) {}

        // 3. 反射调用 setRemoved(DISCARDED)
        try {
            Method m = Entity.class.getDeclaredMethod("setRemoved", Entity.RemovalReason.class);
            m.setAccessible(true);
            m.invoke(entity, Entity.RemovalReason.DISCARDED);
        } catch (Exception ignored) {}

        // 4. 釜底抽薪：从世界底层容器中彻底摘除（不再 tick → 无法自我修复）
        if (entity.world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) entity.world;

            // 4.1 从服务端 tick 列表摘除（核心：连 tick 的机会都没有）
            try {
                Field tl = ServerWorld.class.getDeclaredField("entityTickList");
                tl.setAccessible(true);
                Object tickList = tl.get(serverWorld);
                if (tickList != null) {
                    Method remove = null;
                    try {
                        remove = tickList.getClass().getMethod("remove", Entity.class);
                    } catch (NoSuchMethodException ignored2) {}
                    if (remove == null) {
                        try {
                            remove = tickList.getClass().getDeclaredMethod("remove", Entity.class);
                        } catch (NoSuchMethodException ignored2) {}
                    }
                    if (remove != null) {
                        remove.setAccessible(true);
                        remove.invoke(tickList, entity);
                    }
                }
            } catch (Exception ignored) {}

            // 4.2 从世界 EntityList 摘除
            try {
                Field el = net.minecraft.world.World.class.getDeclaredField("entityList");
                el.setAccessible(true);
                Object entityList = el.get(serverWorld);
                if (entityList != null) {
                    Method remove = null;
                    try {
                        remove = entityList.getClass().getMethod("remove", Entity.class);
                    } catch (NoSuchMethodException ignored2) {}
                    if (remove == null) {
                        try {
                            remove = entityList.getClass().getDeclaredMethod("remove", Entity.class);
                        } catch (NoSuchMethodException ignored2) {}
                    }
                    if (remove != null) {
                        remove.setAccessible(true);
                        remove.invoke(entityList, entity);
                    }
                }
            } catch (Exception ignored) {}

            // 4.3 调用原版 onRemove 触发区块存储清理
            try {
                Method m = Entity.class.getDeclaredMethod("onRemove", Entity.RemovalReason.class);
                m.setAccessible(true);
                m.invoke(entity, Entity.RemovalReason.DISCARDED);
            } catch (Exception ignored) {}
        }

        // 5. 发包强制客户端移除
        if (entity.world instanceof ServerWorld && !entity.world.isClient) {
            ServerWorld serverWorld = (ServerWorld) entity.world;
            EntitiesDestroyS2CPacket packet = new EntitiesDestroyS2CPacket(entity.getId());
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player.networkHandler != null) {
                    player.networkHandler.sendPacket(packet);
                }
            }
        }
    }
}

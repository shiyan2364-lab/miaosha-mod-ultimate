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
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MiaoShaMod implements ModInitializer {

    public static final String MOD_ID = "miaosha-mod-ultimate";
    public static final Logger LOGGER = LogManager.getLogger("miaosha-mod-ultimate");
    public static final boolean KILL_PLAYER = true;
    public static Item MIAOSHA_SWORD;
    public static Item MIAOSHA_ERASE_SWORD;
    public static Item UNKILLABLE_SPAWN_EGG;
    public static EntityType<UnkillableEntity> UNKILLABLE_ENTITY_TYPE;
    public static final Set<UUID> ERASED_ENTITIES = new HashSet<>();
    public static boolean TIME_STOPPED = false;

    @Override
    public void onInitialize() {
        MIAOSHA_SWORD = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "miaosha_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -2.4F,
                        new Item.Settings().group(ItemGroup.COMBAT)));
        MIAOSHA_ERASE_SWORD = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "miaosha_erase_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -0.4F,
                        new Item.Settings().group(ItemGroup.COMBAT)));
        UNKILLABLE_ENTITY_TYPE = Registry.register(Registry.ENTITY_TYPE, new Identifier(MOD_ID, "unkillable"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UnkillableEntity::new)
                        .dimensions(EntityDimensions.fixed(0.9F, 0.9F))
                        .trackRangeBlocks(128)
                        .build());
        FabricDefaultAttributeRegistry.register(UNKILLABLE_ENTITY_TYPE, UnkillableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, UnkillableEntity.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D));
        UNKILLABLE_SPAWN_EGG = Registry.register(Registry.ITEM, new Identifier(MOD_ID, "unkillable_spawn_egg"),
                new SpawnEggItem(UNKILLABLE_ENTITY_TYPE, 0xFFB6C1, 0xCD5C5C,
                        new Item.Settings().group(ItemGroup.MISC)));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (world.isClient) return TypedActionResult.pass(held);
            if (Registry.ITEM.getId(held.getItem())
                    .equals(new Identifier(MOD_ID, "miaosha_erase_sword"))) {
                eraseAllLoadedChunks(player);
                return TypedActionResult.success(held);
            }
            return TypedActionResult.pass(held);
        });
        LOGGER.info("MiaoSha Mod Ultimate loaded [MC 1.16.5] - Global Thunder Erase!");
    }

    /** 清除所有已加载区块（视距内）中的生物与掉落物 */
    private static void eraseAllLoadedChunks(PlayerEntity player) {
        if (player.getServer() == null) return;
        int cleared = 0;
        List<String> names = new ArrayList<>();

        for (ServerWorld world : player.getServer().getWorlds()) {
            int viewDist = player.getServer().getPlayerManager().getViewDistance();
            int pcx = (int) Math.floor(player.getX()) >> 4;
            int pcz = (int) Math.floor(player.getZ()) >> 4;

            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    if (!world.isChunkLoaded(cx, cz)) continue;

                    Chunk chunk = world.getChunk(cx, cz);
                    Box box = new Box(cx * 16, 0, cz * 16, cx * 16 + 16, 256, cz * 16 + 16);

                    List<Entity> entities = world.getOtherEntities(player, box, e -> {
                        if (e instanceof PlayerEntity) return false;
                        return e instanceof LivingEntity || e instanceof ItemEntity;
                    });

                    for (Entity e : entities) {
                        names.add(e.getDisplayName().getString());
                        hardErase(e);
                        cleared++;

                        // 1.16.5 装饰闪电
                        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
                        if (bolt != null) {
                            bolt.refreshPositionAndAngles(e.getX(), e.getY(), e.getZ(), 0F, 0F);
                            bolt.setCosmetic(true);
                            world.spawnEntity(bolt);
                        }
                    }
                }
            }
        }

        if (!names.isEmpty()) {
            String msg;
            if (names.size() >= 3) {
                msg = "⚡ 已抹除 " + cleared + " 个目标";
            } else {
                msg = "⚡ 已抹除: " + String.join(", ", names);
            }
            player.sendMessage(new LiteralText(msg), false);
        }
    }

    /** 湮灭剑：1.16.5 可靠三板斧 + 末影龙原生死亡 */
    public static void hardErase(Entity entity) {
        if (entity == null) return;

        // 末影龙特判：必须走原生死亡流程，才能正常移除血条/开传送门
        if (entity instanceof EnderDragonEntity) {
            try { entity.kill(); } catch (Exception ignored) {}
            return;
        }

        if (entity instanceof LivingEntity) {
            try {
                Field f = LivingEntity.class.getDeclaredField("health");
                f.setAccessible(true);
                f.setFloat(entity, 0F);
                ((LivingEntity) entity).setHealth(0F);
                ((LivingEntity) entity).kill();
            } catch (Exception ignored) {}
        }
        try {
            Field rf = Entity.class.getDeclaredField("removed");
            rf.setAccessible(true);
            rf.setBoolean(entity, true);
        } catch (Exception ignored) {}
        try { entity.remove(); } catch (Exception ignored) {}
    }
}

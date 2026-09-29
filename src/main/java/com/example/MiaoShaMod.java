package com.example;

import com.example.entity.UnkillableEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MiaoShaMod implements ModInitializer {
    public static final String MOD_ID = "miaosha-mod-ultimate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final boolean KILL_PLAYER = true;
    public static final int CHAIN_RADIUS = 5;

    public static Item MIAOSHA_SWORD;
    public static Item MIAOSHA_ERASE_SWORD;
    public static Item UNKILLABLE_SPAWN_EGG;
    public static EntityType<UnkillableEntity> UNKILLABLE_ENTITY_TYPE;

    public static final Set<UUID> ERASED_ENTITIES = new HashSet<>();
    public static boolean TIME_STOPPED = false;

    @Override
    public void onInitialize() {
        MIAOSHA_SWORD = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "miaosha_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -2.4F, new Item.Settings()));
        MIAOSHA_ERASE_SWORD = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "miaosha_erase_sword"),
                new SwordItem(ToolMaterials.DIAMOND, 3, -0.4F, new Item.Settings()));

        UNKILLABLE_ENTITY_TYPE = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, "unkillable"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UnkillableEntity::new)
                        .dimensions(EntityDimensions.fixed(0.9F, 0.9F))
                        .trackRangeBlocks(128)
                        .build());
        FabricDefaultAttributeRegistry.register(UNKILLABLE_ENTITY_TYPE, UnkillableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, UnkillableEntity.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D));
        UNKILLABLE_SPAWN_EGG = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "unkillable_spawn_egg"),
                new SpawnEggItem(UNKILLABLE_ENTITY_TYPE, 0xFFB6C1, 0xCD5C5C, new Item.Settings()));

        // 加入创造物品栏（1.20.1 必须用 ItemGroupEvents）
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
            content.add(MIAOSHA_SWORD);
            content.add(MIAOSHA_ERASE_SWORD);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
            content.add(UNKILLABLE_SPAWN_EGG);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (world.isClient) return TypedActionResult.pass(held);
            if (Registries.ITEM.getId(held.getItem()).equals(Identifier.of(MOD_ID, "miaosha_erase_sword"))) {
                eraseAllLoadedChunks(player);
                return TypedActionResult.success(held);
            }
            return TypedActionResult.pass(held);
        });

        LOGGER.info("MiaoSha Mod Ultimate 1.20.1 loaded!");
    }

    public static void eraseAllLoadedChunks(PlayerEntity player) {
        if (player.getServer() == null) return;
        int cleared = 0;
        List<String> names = new ArrayList<>();
        for (ServerWorld world : player.getServer().getWorlds()) {
            int viewDist = player.getServer().getPlayerManager().getViewDistance();
            int pcx = (int) Math.floor(player.getX()) >> 4;
            int pcz = (int) Math.floor(player.getZ()) >> 4;
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!world.isChunkLoaded(cx, cz)) continue;
                    Box box = new Box(cx * 16, 0, cz * 16, cx * 16 + 16, 256, cz * 16 + 16);
                    for (Entity e : world.getOtherEntities(player, box,
                            e -> !(e instanceof PlayerEntity) && (e instanceof LivingEntity || e instanceof ItemEntity))) {
                        names.add(e.getDisplayName().getString());
                        hardErase(e);
                        cleared++;
                        spawnLightning(world, e.getX(), e.getY(), e.getZ());
                    }
                }
            }
        }
        if (!names.isEmpty()) {
            String msg = names.size() >= 3
                    ? "⚡ 已抹除 " + cleared + " 个目标"
                    : "⚡ 已抹除: " + String.join(", ", names);
            player.sendMessage(Text.literal(msg), false);
        }
    }

    public static void chainErase(Entity center, PlayerEntity attacker) {
        if (center.getWorld().isClient) return;
        if (!(center.getWorld() instanceof ServerWorld sw)) return;
        int count = 0;
        List<String> names = new ArrayList<>();
        double r = CHAIN_RADIUS;
        Box box = new Box(center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r, center.getY() + r, center.getZ() + r);
        for (Entity e : sw.getOtherEntities(center, box,
                e -> !(e instanceof PlayerEntity) && (e instanceof LivingEntity || e instanceof ItemEntity))) {
            if (e == center) continue;
            names.add(e.getDisplayName().getString());
            hardErase(e);
            count++;
            spawnLightning(sw, e.getX(), e.getY(), e.getZ());
        }
        if (count > 0 && attacker != null) {
            String msg = count >= 3
                    ? "☠ 连锁抹除 " + count + " 个目标"
                    : "☠ 连锁抹除: " + String.join(", ", names);
            attacker.sendMessage(Text.literal(msg), false);
        }
    }

    private static void spawnLightning(ServerWorld world, double x, double y, double z) {
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.refreshPositionAndAngles(x, y, z, 0F, 0F);
            bolt.setCosmetic(true);
            world.spawnEntity(bolt);
        }
    }

    public static void hardErase(Entity entity) {
        if (entity == null) return;
        if (entity instanceof EnderDragonEntity) {
            entity.kill();
            return;
        }
        if (entity instanceof LivingEntity living) {
            try {
                Field f = LivingEntity.class.getDeclaredField("health");
                f.setAccessible(true);
                f.setFloat(living, 0F);
            } catch (Exception ignored) {}
            living.setHealth(0F);
            living.kill();
        }
        entity.remove(Entity.RemovalReason.DISCARDED);
    }
}

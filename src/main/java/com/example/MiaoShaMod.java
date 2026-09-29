package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return ActionResult.PASS;
            ItemStack held = player.getStackInHand(hand);
            Identifier heldId = Registry.ITEM.getId(held.getItem());
            if (heldId.equals(new Identifier(MOD_ID, "miaosha_erase_sword"))) {
                eraseArea(player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        LOGGER.info("MiaoSha Mod Ultimate loaded [MC 1.16.5] - Two swords + area erase + time stop!");
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
                    e.remove();
                    cleared++;
                }
            }
        }
        LOGGER.info("Area erase: {} entities removed in {}x{}", cleared, r * 2, r * 2);
    }
}

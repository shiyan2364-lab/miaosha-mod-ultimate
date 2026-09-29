package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MiaoShaMod implements ModInitializer {

    public static final String MOD_ID = "miaosha-mod-ultimate";
    public static final Logger LOGGER = LogManager.getLogger("miaosha-mod-ultimate");

    public static final boolean KILL_PLAYER = true;

    public static Item MIAOSHA_SWORD;
    public static Item MIAOSHA_ERASE_SWORD;

    /** 被湮灭之剑永久抹杀的实体 UUID */
    public static final Set<UUID> ERASED_ENTITIES = new HashSet<>();

    @Override
    public void onInitialize() {
        // 使用原版「战斗」物品栏，绝对存在，绝不崩溃
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

        LOGGER.info("MiaoSha Mod Ultimate loaded [MC 1.16.5] - Two swords!");
    }
}

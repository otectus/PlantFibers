package com.otectus.plantfiber.registry;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.item.ConfigurableBedItem;
import com.otectus.plantfiber.item.GrassArmorItem;
import com.otectus.plantfiber.item.HealingConsumableItem;
import com.otectus.plantfiber.item.RopeBlockItem;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PlantFiberMod.MODID);

    public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register("plant_fiber",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLANT_MESH = ITEMS.register("plant_mesh",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REINFORCED_FIBERS = ITEMS.register("reinforced_fibers",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GRASS_BANDAGE = ITEMS.register("grass_bandage",
            () -> new HealingConsumableItem(new Item.Properties(),
                    MobEffects.REGENERATION,
                    () -> PlantFiberConfig.COMMON.enableBandage.get(),
                    () -> PlantFiberConfig.COMMON.bandageRegenDurationTicks.get(),
                    () -> PlantFiberConfig.COMMON.bandageRegenAmplifier.get(),
                    () -> PlantFiberConfig.COMMON.bandageCooldownTicks.get(),
                    24,
                    UseAnim.BOW));

    public static final RegistryObject<Item> GRASS_PLASTER = ITEMS.register("grass_plaster",
            () -> new HealingConsumableItem(new Item.Properties(),
                    MobEffects.REGENERATION,
                    () -> PlantFiberConfig.COMMON.enablePlaster.get(),
                    () -> PlantFiberConfig.COMMON.plasterRegenDurationTicks.get(),
                    () -> PlantFiberConfig.COMMON.plasterRegenAmplifier.get(),
                    () -> PlantFiberConfig.COMMON.plasterCooldownTicks.get(),
                    24,
                    UseAnim.BOW));

    public static final RegistryObject<Item> GRASS_HELMET = ITEMS.register("grass_helmet",
            () -> new GrassArmorItem(ModArmorMaterials.GRASS, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> GRASS_CHESTPLATE = ITEMS.register("grass_chestplate",
            () -> new GrassArmorItem(ModArmorMaterials.GRASS, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> GRASS_LEGGINGS = ITEMS.register("grass_leggings",
            () -> new GrassArmorItem(ModArmorMaterials.GRASS, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> GRASS_BOOTS = ITEMS.register("grass_boots",
            () -> new GrassArmorItem(ModArmorMaterials.GRASS, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> GRASS_FIBER_BLOCK_ITEM = ITEMS.register("grass_fiber_block",
            () -> new BlockItem(ModBlocks.GRASS_FIBER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> GRASS_BED_ITEM = ITEMS.register("grass_bed",
            () -> new ConfigurableBedItem(ModBlocks.GRASS_BED.get(), new Item.Properties()));

    public static final RegistryObject<Item> ROPE_ITEM = ITEMS.register("rope",
            () -> new RopeBlockItem(ModBlocks.ROPE.get(), new Item.Properties()));

    private ModItems() {
    }
}



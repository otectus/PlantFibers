package com.otectus.plantfiber;

import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.registry.ModBlockEntities;
import com.otectus.plantfiber.registry.ModBlocks;
import com.otectus.plantfiber.registry.ModCreativeTabs;
import com.otectus.plantfiber.registry.ModEntities;
import com.otectus.plantfiber.registry.ModItems;
import com.otectus.plantfiber.registry.ModLootModifiers;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PlantFiberMod.MODID)
public class PlantFiberMod {
    public static final String MODID = "plantfiber";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlantFiberMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PlantFiberConfig.SPEC);

    }
}



package com.otectus.plantfiber.registry;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.block.GrassBedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PlantFiberMod.MODID);

    public static final RegistryObject<BlockEntityType<GrassBedBlockEntity>> GRASS_BED_ENTITY =
            BLOCK_ENTITIES.register("grass_bed",
                    () -> BlockEntityType.Builder.of(GrassBedBlockEntity::new, ModBlocks.GRASS_BED.get()).build(null));

    private ModBlockEntities() {
    }
}

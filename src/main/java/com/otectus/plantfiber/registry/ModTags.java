package com.otectus.plantfiber.registry;

import com.otectus.plantfiber.PlantFiberMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> VEGETATION = TagKey.create(Registries.BLOCK, new ResourceLocation(PlantFiberMod.MODID, "vegetation"));
    public static final TagKey<Item> BANDAGE_PLANTS = TagKey.create(Registries.ITEM, new ResourceLocation(PlantFiberMod.MODID, "bandage_plants"));

    public static final TagKey<Block> ROPE_ANCHOR_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation(PlantFiberMod.MODID, "rope_anchor_blocks"));
    public static final TagKey<Block> ROPE_REPLACEABLE_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation(PlantFiberMod.MODID, "rope_replaceable_blocks"));
    public static final TagKey<Block> ROPE_FENCE_ANCHORS = TagKey.create(Registries.BLOCK, new ResourceLocation(PlantFiberMod.MODID, "rope_fence_anchors"));

    public static final TagKey<Item> FIBERS = TagKey.create(Registries.ITEM, new ResourceLocation(PlantFiberMod.MODID, "fibers"));
    public static final TagKey<Item> REINFORCED_FIBERS = TagKey.create(Registries.ITEM, new ResourceLocation(PlantFiberMod.MODID, "reinforced_fibers"));

    private ModTags() {
    }
}



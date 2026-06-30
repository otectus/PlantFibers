package com.otectus.plantfiber.block;

import com.otectus.plantfiber.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GrassBedBlockEntity extends BlockEntity {
    public GrassBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRASS_BED_ENTITY.get(), pos, state);
    }
}

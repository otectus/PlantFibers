package com.otectus.plantfiber.item;

import com.otectus.plantfiber.block.RopeBlock;
import com.otectus.plantfiber.config.PlantFiberConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class RopeBlockItem extends BlockItem {
    public RopeBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!PlantFiberConfig.COMMON.enableRope.get()) {
            return InteractionResult.PASS;
        }
        // All rope placement (extend column / fence anchor / underside) is handled by the block so
        // it can share the anchor-validation logic with its support checks.
        if (getBlock() instanceof RopeBlock ropeBlock) {
            return ropeBlock.tryPlaceFromContext(context);
        }
        return super.useOn(context);
    }
}

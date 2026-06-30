package com.otectus.plantfiber.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.registry.ModItems;
import com.otectus.plantfiber.registry.ModTags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class PlantFiberLootModifier extends LootModifier {
    public static final Codec<PlantFiberLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, PlantFiberLootModifier::new));

    public PlantFiberLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!PlantFiberConfig.COMMON.enablePlantFiberDrops.get()) {
            return generatedLoot;
        }

        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null) {
            return generatedLoot;
        }

        if (!state.is(ModTags.VEGETATION)) {
            return generatedLoot;
        }

        double chance = PlantFiberConfig.COMMON.plantFiberDropChance.get();
        double roll = context.getRandom().nextDouble();
        if (chance <= 0.0d || roll >= chance) {
            return generatedLoot;
        }

        generatedLoot.add(new ItemStack(ModItems.PLANT_FIBER.get()));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

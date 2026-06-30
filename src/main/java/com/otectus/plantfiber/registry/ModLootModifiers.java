package com.otectus.plantfiber.registry;

import com.mojang.serialization.Codec;
import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.loot.PlantFiberLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PlantFiberMod.MODID);

    public static final RegistryObject<Codec<PlantFiberLootModifier>> PLANT_FIBER_FROM_VEGETATION =
            LOOT_MODIFIERS.register("plant_fiber_from_vegetation", () -> PlantFiberLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }

    private ModLootModifiers() {
    }
}

package com.otectus.plantfiber.event;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.crafting.ConfigCondition;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = PlantFiberMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModSetup {
    private ModSetup() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CraftingHelper.register(ConfigCondition.Serializer.INSTANCE));
    }
}

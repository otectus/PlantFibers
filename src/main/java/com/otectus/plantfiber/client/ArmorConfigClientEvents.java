package com.otectus.plantfiber.client;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.EffectivePlantFiberConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drops the server's armor multiplier on disconnect so single-player never keeps a remote value. */
@Mod.EventBusSubscriber(modid = PlantFiberMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArmorConfigClientEvents {
    private ArmorConfigClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EffectivePlantFiberConfig.clearRemoteArmorDurabilityMultiplier();
    }
}

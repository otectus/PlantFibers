package com.otectus.plantfiber.event;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.network.ArmorConfigNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Delivers the armor multiplier snapshot to each joining player. */
@Mod.EventBusSubscriber(modid = PlantFiberMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArmorConfigEvents {
    private ArmorConfigEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArmorConfigNetwork.sendTo(player);
        }
    }
}

package com.otectus.plantfiber.registry;

import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.PlantFiberConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            new ResourceLocation("minecraft", "creative_mode_tab"), PlantFiberMod.MODID);

    public static final RegistryObject<CreativeModeTab> PLANTFIBER_TAB = TABS.register("plantfiber", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.plantfiber"))
                    .icon(() -> new ItemStack(ModItems.PLANT_FIBER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PLANT_FIBER.get());
                        output.accept(ModItems.PLANT_MESH.get());
                        if (PlantFiberConfig.COMMON.enableReinforcedFibers.get()) {
                            output.accept(ModItems.REINFORCED_FIBERS.get());
                        }
                        if (PlantFiberConfig.COMMON.enableRope.get()) {
                            output.accept(ModItems.ROPE_ITEM.get());
                        }
                        output.accept(ModItems.GRASS_BANDAGE.get());
                        output.accept(ModItems.GRASS_PLASTER.get());
                        output.accept(ModItems.GRASS_FIBER_BLOCK_ITEM.get());
                        if (PlantFiberConfig.COMMON.enableGrassBed.get()) {
                            output.accept(ModItems.GRASS_BED_ITEM.get());
                        }
                        if (PlantFiberConfig.COMMON.enableGrassArmor.get()) {
                            output.accept(ModItems.GRASS_HELMET.get());
                            output.accept(ModItems.GRASS_CHESTPLATE.get());
                            output.accept(ModItems.GRASS_LEGGINGS.get());
                            output.accept(ModItems.GRASS_BOOTS.get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}


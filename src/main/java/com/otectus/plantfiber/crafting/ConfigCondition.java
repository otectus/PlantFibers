package com.otectus.plantfiber.crafting;

import com.google.gson.JsonObject;
import com.otectus.plantfiber.PlantFiberMod;
import com.otectus.plantfiber.config.PlantFiberConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Recipe condition that enables/disables a recipe based on a named PlantFiberConfig boolean flag.
 * Usage in a recipe JSON:
 * <pre>
 * "conditions": [ { "type": "plantfiber:config", "flag": "enableRope" } ]
 * </pre>
 */
public class ConfigCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation(PlantFiberMod.MODID, "config");

    private static final Map<String, Supplier<ForgeConfigSpec.BooleanValue>> FLAGS = Map.of(
            "enableRope", () -> PlantFiberConfig.COMMON.enableRope,
            "enableReinforcedFibers", () -> PlantFiberConfig.COMMON.enableReinforcedFibers,
            "enablePlantMeshPaperRecipe", () -> PlantFiberConfig.COMMON.enablePlantMeshPaperRecipe
    );

    private final String flag;

    public ConfigCondition(String flag) {
        this.flag = flag;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        Supplier<ForgeConfigSpec.BooleanValue> value = FLAGS.get(flag);
        // Unknown flags default to enabled so a typo never silently hides content.
        return value == null || value.get().get();
    }

    @Override
    public String toString() {
        return "config(" + flag + ")";
    }

    public static class Serializer implements IConditionSerializer<ConfigCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigCondition value) {
            json.addProperty("flag", value.flag);
        }

        @Override
        public ConfigCondition read(JsonObject json) {
            return new ConfigCondition(GsonHelper.getAsString(json, "flag"));
        }

        @Override
        public ResourceLocation getID() {
            return ConfigCondition.ID;
        }
    }
}

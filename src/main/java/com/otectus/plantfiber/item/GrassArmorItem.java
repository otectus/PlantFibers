package com.otectus.plantfiber.item;

import com.otectus.plantfiber.config.EffectivePlantFiberConfig;
import com.otectus.plantfiber.config.PlantFiberConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GrassArmorItem extends ArmorItem {
    public GrassArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!PlantFiberConfig.COMMON.enableGrassArmor.get()) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
        if (!PlantFiberConfig.COMMON.enableGrassArmor.get()) {
            return false;
        }
        return super.canEquip(stack, armorType, entity);
    }

    /**
     * The registered maximum is the constant material baseline; the configured multiplier is applied
     * here so the value follows the effective (server) config without ever being baked into the item.
     */
    @Override
    public int getMaxDamage(ItemStack stack) {
        int baseline = this.getMaterial().getDurabilityForType(this.getType());
        return Math.max(1, (int) Math.floor(baseline * EffectivePlantFiberConfig.armorDurabilityMultiplier()));
    }

    // Lowering the multiplier can leave stored Damage above the new maximum. Presentation is clamped;
    // the stored damage is deliberately left alone so old armor is neither repaired nor deleted.

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = this.getMaxDamage(stack);
        int damage = Mth.clamp(stack.getDamageValue(), 0, max);
        return Math.round(13.0F - damage * 13.0F / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = this.getMaxDamage(stack);
        int damage = Mth.clamp(stack.getDamageValue(), 0, max);
        float remaining = Math.max(0.0F, (float) (max - damage) / max);
        return Mth.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F);
    }
}

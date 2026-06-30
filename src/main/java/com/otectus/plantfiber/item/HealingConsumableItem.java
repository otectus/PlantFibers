package com.otectus.plantfiber.item;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class HealingConsumableItem extends Item {
    private final MobEffect effect;
    private final BooleanSupplier enabled;
    private final IntSupplier durationTicks;
    private final IntSupplier amplifier;
    private final IntSupplier cooldownTicks;
    private final int useDuration;
    private final UseAnim useAnim;

    public HealingConsumableItem(Item.Properties properties,
                                 MobEffect effect,
                                 BooleanSupplier enabled,
                                 IntSupplier durationTicks,
                                 IntSupplier amplifier,
                                 IntSupplier cooldownTicks,
                                 int useDuration,
                                 UseAnim useAnim) {
        super(properties);
        this.effect = effect;
        this.enabled = enabled;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.cooldownTicks = cooldownTicks;
        this.useDuration = useDuration;
        this.useAnim = useAnim;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!enabled.getAsBoolean()) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                int duration = Math.max(0, durationTicks.getAsInt());
                int amp = Math.max(0, amplifier.getAsInt());
                if (duration > 0) {
                    player.addEffect(new MobEffectInstance(effect, duration, amp));
                }
                int cooldown = Math.max(0, cooldownTicks.getAsInt());
                if (cooldown > 0) {
                    player.getCooldowns().addCooldown(this, cooldown);
                }
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int duration = Math.max(0, durationTicks.getAsInt());
        int amp = Math.max(0, amplifier.getAsInt());
        int cooldown = Math.max(0, cooldownTicks.getAsInt());
        if (duration > 0) {
            tooltip.add(Component.translatable("tooltip.plantfiber.regen",
                    amp + 1, String.format("%.1f", duration / 20.0))
                    .withStyle(ChatFormatting.BLUE));
        }
        if (cooldown > 0) {
            tooltip.add(Component.translatable("tooltip.plantfiber.cooldown",
                    String.format("%.1f", cooldown / 20.0))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useDuration;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnim;
    }
}



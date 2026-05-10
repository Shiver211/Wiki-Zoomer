package com.shiver.wikizoomer.item;

import com.shiver.wikizoomer.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class ItemEntityBinder extends Item {

    public ItemEntityBinder() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isEntityBound(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!isEntityBound(stack)) {
            tooltip.add(Component.translatable("item.wikizoomer.entity_binder.desc").withStyle(ChatFormatting.GRAY));
        }
        Boolean isPlayer = stack.get(ModDataComponents.IS_PLAYER_ENTITY);
        CompoundTag entityTag = stack.get(ModDataComponents.ENTITY_TAG);
        if (entityTag != null) {
            boolean isPlayerEntity = isPlayer != null && isPlayer;
            Optional<EntityType<?>> optional = EntityType.by(entityTag);
            if (optional.isPresent()) {
                Component untranslated = isPlayerEntity
                        ? Component.translatable("entity.player.name").withStyle(ChatFormatting.GRAY)
                        : optional.get().getDescription();
                tooltip.add(untranslated);
            }
        }
    }

    public static boolean isEntityBound(ItemStack stack) {
        Boolean isPlayer = stack.get(ModDataComponents.IS_PLAYER_ENTITY);
        if (isPlayer != null && isPlayer) {
            return true;
        }
        CompoundTag entityTag = stack.get(ModDataComponents.ENTITY_TAG);
        if (entityTag != null) {
            Optional<EntityType<?>> optional = EntityType.by(entityTag);
            return optional.isPresent();
        }
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        CompoundTag entityTag = target.saveWithoutId(new CompoundTag());
        entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());

        ItemStack stackReplacement = new ItemStack(this);
        stackReplacement.set(ModDataComponents.IS_PLAYER_ENTITY, target instanceof Player);
        stackReplacement.set(ModDataComponents.ENTITY_TAG, entityTag);

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.swing(hand);
        if (!player.addItem(stackReplacement)) {
            ItemEntity itemEntity = player.drop(stackReplacement, false);
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setThrower(player);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

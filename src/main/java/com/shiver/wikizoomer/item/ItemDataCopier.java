package com.shiver.wikizoomer.item;

import com.shiver.wikizoomer.WikiZoomerUnofficial;
import com.shiver.wikizoomer.WikiZoomerUnofficialClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class ItemDataCopier extends Item {

    public ItemDataCopier() {
        super(new Properties().stacksTo(1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.wikizoomer.data_copier.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.wikizoomer.data_copier.desc2").withStyle(ChatFormatting.GRAY));
        if (WikiZoomerUnofficialClient.dataMimic != null) {
            tooltip.add(Component.translatable("item.wikizoomer.data_copier.tracking",
                    WikiZoomerUnofficialClient.dataMimic.getDisplayName()).withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (player.level().isClientSide) {
            WikiZoomerUnofficialClient.dataMimic = entity;
            player.displayClientMessage(Component.translatable("item.wikizoomer.data_copier.success", entity.getDisplayName()), true);
        }
        return InteractionResult.SUCCESS;
    }
}

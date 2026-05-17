package com.shiver.wikizoomer.block;

import com.mojang.serialization.MapCodec;
import com.shiver.wikizoomer.WikiZoomerUnofficial;
import com.shiver.wikizoomer.screen.GuiEntityZoomer;
import com.shiver.wikizoomer.screen.GuiItemZoomer;
import com.shiver.wikizoomer.tileentity.TileEntityEntityZoomer;
import com.shiver.wikizoomer.tileentity.TileEntityItemZoomer;
import com.shiver.wikizoomer.tileentity.TileEntityZoomerBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BlockZoomer extends BaseEntityBlock {
    private static final VoxelShape BASE_SHAPE = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape NECK_SHAPE = Block.box(4, 5, 4, 12, 16, 12);
    private static final VoxelShape JOINED_SHAPE = Shapes.join(BASE_SHAPE, NECK_SHAPE, BooleanOp.OR);
    private final boolean itemOrEntity;

    public BlockZoomer(Properties properties, boolean itemOrEntity) {
        super(properties.sound(SoundType.METAL).randomTicks());
        this.itemOrEntity = itemOrEntity;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        if (itemOrEntity) {
            return MapCodec.unit(() -> (BaseEntityBlock) WikiZoomerUnofficial.ITEM_ZOOMER_BLOCK.get());
        } else {
            return MapCodec.unit(() -> (BaseEntityBlock) WikiZoomerUnofficial.ENTITY_ZOOMER_BLOCK.get());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileEntityZoomerBase zoomer) {
                Containers.dropContents(level, pos, zoomer);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return JOINED_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof TileEntityZoomerBase zoomer) {
                if (!zoomer.getItem(0).isEmpty()) {
                    ItemEntity dropped = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, zoomer.getItem(0).copy());
                    level.addFreshEntity(dropped);
                    zoomer.clearContent();
                }
                InteractionHand handIn = player.getUsedItemHand();
                ItemStack heldItem = player.getItemInHand(handIn);
                ItemStack single = heldItem.copy();
                single.setCount(1);
                if (itemOrEntity || single.getItem() == WikiZoomerUnofficial.ENTITY_BINDER_ITEM.get()) {
                    zoomer.setItem(0, single);
                    if (!player.isCreative())
                        heldItem.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        } else {
            if (level.isClientSide) {
                if (itemOrEntity) {
                    Minecraft.getInstance().setScreen(new GuiItemZoomer((TileEntityZoomerBase) level.getBlockEntity(pos)));
                } else {
                    Minecraft.getInstance().setScreen(new GuiEntityZoomer((TileEntityEntityZoomer) level.getBlockEntity(pos)));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (itemOrEntity) {
            tooltip.add(Component.translatable("block.wikizoomer.item_zoomer.desc0").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.wikizoomer.item_zoomer.desc1").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("block.wikizoomer.entity_zoomer.desc0").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("block.wikizoomer.entity_zoomer.desc1").withStyle(ChatFormatting.GRAY));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return itemOrEntity ? new TileEntityItemZoomer(pos, state) : new TileEntityEntityZoomer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return itemOrEntity
                ? createTickerHelper(blockEntityType, WikiZoomerUnofficial.ITEM_ZOOMER_TE.get(), TileEntityItemZoomer::tick)
                : createTickerHelper(blockEntityType, WikiZoomerUnofficial.ENTITY_ZOOMER_TE.get(), TileEntityEntityZoomer::tick);
    }
}

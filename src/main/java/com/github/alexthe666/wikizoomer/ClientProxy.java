package com.github.alexthe666.wikizoomer;

import com.github.alexthe666.wikizoomer.client.GuiEntityZoomer;
import com.github.alexthe666.wikizoomer.client.GuiItemZoomer;
import com.github.alexthe666.wikizoomer.client.RenderEntityZoomer;
import com.github.alexthe666.wikizoomer.client.RenderItemZoomer;
import com.github.alexthe666.wikizoomer.client.ExportManager;
import com.github.alexthe666.wikizoomer.tileentity.TileEntityEntityZoomer;
import com.github.alexthe666.wikizoomer.tileentity.TileEntityRegistry;
import com.github.alexthe666.wikizoomer.tileentity.TileEntityZoomerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientProxy extends CommonProxy{
    public static Entity dataMimic = null;

    @Override
    public void setup() {
        BlockEntityRenderers.register(TileEntityRegistry.ITEM_ZOOMER_TE.get(), RenderItemZoomer::new);
        BlockEntityRenderers.register(TileEntityRegistry.ENTITY_ZOOMER_TE.get(), RenderEntityZoomer::new);
        ItemProperties.register(ItemAndBlockRegistry.ENTITY_BINDER_ITEM.get(), java.util.Objects.requireNonNull(ResourceLocation.tryParse("bound"), "bound"), (stack, a, b, c) -> ItemEntityBinder.isEntityBound(stack) ? 1 : 0);
    }

    @Override
    public void openItemZoomerGui(TileEntityZoomerBase tileEntity) {
        Minecraft.getInstance().setScreen(new GuiItemZoomer(tileEntity));
    }

    @Override
    public void openEntityZoomerGui(TileEntityEntityZoomer tileEntity) {
        Minecraft.getInstance().setScreen(new GuiEntityZoomer(tileEntity));
    }

    @Override
    public void onDataCopierUse(LivingEntity target) {
        this.dataMimic = target;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ExportManager.tick();
        }
    }

    @SubscribeEvent
    public void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        dataMimic = null;
    }

}

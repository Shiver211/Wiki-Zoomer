package com.shiver.wikizoomer;

import com.shiver.wikizoomer.client.ExportManager;
import com.shiver.wikizoomer.client.RenderEntityZoomer;
import com.shiver.wikizoomer.client.RenderItemZoomer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import javax.annotation.Nullable;

public class WikiZoomerUnofficialClient {
    @Nullable
    public static Entity dataMimic = null;

    // Mod bus events
    @EventBusSubscriber(modid = WikiZoomerUnofficial.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(WikiZoomerUnofficial.ITEM_ZOOMER_TE.get(), RenderItemZoomer::new);
            event.registerBlockEntityRenderer(WikiZoomerUnofficial.ENTITY_ZOOMER_TE.get(), RenderEntityZoomer::new);
        }
    }

    // Game bus events
    @EventBusSubscriber(modid = WikiZoomerUnofficial.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            ExportManager.tick();
        }

        @SubscribeEvent
        static void onClientLogout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            dataMimic = null;
        }
    }
}

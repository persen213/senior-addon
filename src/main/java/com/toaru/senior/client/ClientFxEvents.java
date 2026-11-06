package com.toaru.senior.client;

import com.toaru.senior.SeniorMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hooks the effects manager into the game's client events. Only loaded on the client. */
@Mod.EventBusSubscriber(modid = SeniorMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFxEvents {
    private ClientFxEvents() { }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        ClientFx.render(event.getPoseStack(), event.getCamera().getPosition(), event.getFrustum(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ClientFx.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFx.clear();
    }
}

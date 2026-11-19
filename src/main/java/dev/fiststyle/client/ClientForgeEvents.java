package dev.fiststyle.client;

import dev.fiststyle.StyleConfig;
import dev.fiststyle.FistStyleMod;
import dev.fiststyle.StyleNet;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FistFistStyleMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    private static boolean synced;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            synced = false; // re-send the saved toggles the next time we join a world
            return;
        }
        if (!synced) {
            synced = true;
            StyleNet.sendAuto(StyleConfig.AUTO_PARRY.get(), StyleConfig.AUTO_DODGE.get());
        }

        while (ClientModEvents.KEY_PARRY.consumeClick()) {
            ClientActions.setParry(!ClientActions.parryOn());
            ClientActions.toast(ClientActions.label("gui.style.parry", ClientActions.parryOn()));
        }
        while (ClientModEvents.KEY_DODGE.consumeClick()) {
            ClientActions.setDodge(!ClientActions.dodgeOn());
            ClientActions.toast(ClientActions.label("gui.style.dodge", ClientActions.dodgeOn()));
        }
        while (ClientModEvents.KEY_MENU.consumeClick()) {
            mc.setScreen(new AutoScreen());
        }
    }
}

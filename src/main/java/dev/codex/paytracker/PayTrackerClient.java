package dev.codex.paytracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class PayTrackerClient implements ClientModInitializer {
    private KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        PayTrackerStore.load();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.paytracker.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC
        ));
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                PayTrackerStore.recordPayment(message.getString().replaceAll("\\u00a7[0-9A-FK-ORa-fk-or]", "").trim()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) Minecraft.getInstance().setScreen(new PayTrackerScreen());
        });
    }
}

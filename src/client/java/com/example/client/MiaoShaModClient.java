package com.example.client;

import com.example.MiaoShaMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PigEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class MiaoShaModClient implements ClientModInitializer {

    private static KeyBinding timeStopKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(MiaoShaMod.UNKILLABLE_ENTITY_TYPE,
                PigEntityRenderer::new);

        timeStopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.miaosha-mod-ultimate.timestop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.miaosha-mod-ultimate"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (timeStopKey.wasPressed()) {
                MiaoShaMod.TIME_STOPPED = !MiaoShaMod.TIME_STOPPED;
                if (client.player == null) continue;
                client.player.sendMessage(
                        Text.literal(MiaoShaMod.TIME_STOPPED ? "⏸ 时停开启" : "▶ 时停关闭"), false);
            }
        });
    }
}

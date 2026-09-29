package com.example.client;

import com.example.MiaoShaMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PigEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class MiaoShaModClient implements ClientModInitializer {

    private static KeyBinding timeStopKey;

    @Override
    public void onInitializeClient() {
        // 注册实体渲染：无敌生物用猪的渲染器
        EntityRendererRegistry.INSTANCE.register(MiaoShaMod.UNKILLABLE_ENTITY_TYPE,
                PigEntityRenderer::new);

        // 注册 V 键
        timeStopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miaosha-mod-ultimate.timestop",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.miaosha-mod-ultimate"
        ));

        // 每帧检测按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (timeStopKey.wasPressed()) {
                MiaoShaMod.TIME_STOPPED = !MiaoShaMod.TIME_STOPPED;
                if (MiaoShaMod.TIME_STOPPED) {
                    client.player.sendMessage(new LiteralText("Time stopped!"), false);
                } else {
                    client.player.sendMessage(new LiteralText("Time resumed!"), false);
                }
            }
        });
    }
}

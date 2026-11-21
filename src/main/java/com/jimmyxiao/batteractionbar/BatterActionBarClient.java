package com.jimmyxiao.batteractionbar;

import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import com.jimmyxiao.batteractionbar.gui.ActionBarConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：读取配置、注册快捷键，并驱动额外动作栏的计时。
 * 装了 Mod Menu 时也可以直接从模组列表点「设置」。
 */
public class BatterActionBarClient implements ClientModInitializer {

	public static final String MOD_ID = "batteractionbar";

	private static KeyMapping openConfigKey;

	@Override
	public void onInitializeClient() {
		ActionBarConfig.load();

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.batteractionbar.open_config",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ActionBarConfigScreen(null));
				}
			}
			ActionBarMessages.tick();
			ActionBarHudRenderer.tick();
		});

		// 退出服务器后清空其它 Mod 注册的动作栏，避免残留文字
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BatterActionBarApi.clearAll());
	}
}

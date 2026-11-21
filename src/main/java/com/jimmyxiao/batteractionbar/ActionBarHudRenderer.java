package com.jimmyxiao.batteractionbar;

import com.jimmyxiao.batteractionbar.api.ActionBarSlot;
import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 绘制其它 Mod 通过 API 额外注册的动作栏。
 */
public final class ActionBarHudRenderer {

	private ActionBarHudRenderer() {
	}

	public static void renderExtraBars(GuiGraphics graphics) {
		if (!ActionBarConfig.get().enabled) {
			return;
		}
		for (ActionBarSlot slot : BatterActionBarApi.slots()) {
			slot.renderInternal(graphics);
		}
	}

	public static void tick() {
		for (ActionBarSlot slot : BatterActionBarApi.slots()) {
			slot.tickInternal();
		}
	}
}

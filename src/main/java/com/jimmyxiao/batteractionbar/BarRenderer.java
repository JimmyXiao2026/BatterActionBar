package com.jimmyxiao.batteractionbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 统一的动作栏文字绘制：位置以原版锚点 (屏幕宽/2, 屏幕高-68) 为原点，
 * 外观与原版一致（文字 + 背景板），可选整体缩放（以锚点为中心）。
 */
public final class BarRenderer {

	private BarRenderer() {
	}

	public static void draw(GuiGraphics graphics, Component text, int offsetX, int offsetY, int color) {
		draw(graphics, text, offsetX, offsetY, 1.0f, color);
	}

	public static void draw(GuiGraphics graphics, Component text, int offsetX, int offsetY, float scale, int color) {
		Font font = Minecraft.getInstance().font;
		int width = font.width(text);
		float safeScale = scale <= 0.0f ? 1.0f : scale;

		graphics.nextStratum();
		graphics.pose().pushMatrix();
		graphics.pose().translate(
			graphics.guiWidth() / 2.0f + (float) offsetX,
			graphics.guiHeight() - 68.0f + (float) offsetY
		);
		if (safeScale != 1.0f) {
			graphics.pose().scale(safeScale, safeScale);
		}
		graphics.drawStringWithBackdrop(font, text, -width / 2, -4, width, color);
		graphics.pose().popMatrix();
	}
}

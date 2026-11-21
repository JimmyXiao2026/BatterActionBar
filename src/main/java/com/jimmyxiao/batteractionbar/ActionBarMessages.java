package com.jimmyxiao.batteractionbar;

import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * 服务端动作栏与客户端动作栏的独立状态。
 *
 * 原版只有一个 overlayMessageString 槽位：谁后调用 setOverlayMessage 谁就顶掉前一条。
 * 服务器每 tick 下发动作栏时，客户端 Mod 自己的动作栏就会被挤掉（反之亦然）。
 * 这里把两种来源各存一份、各自计时，渲染时同时画出来，只是位置不同。
 */
public final class ActionBarMessages {

	/** 与原版一致：生存 60 tick，最后 20 tick 淡出。 */
	private static final int LIFETIME_TICKS = 60;
	private static final int FADE_TICKS = 20;

	private static volatile Component serverText;
	private static volatile int serverTicks;
	private static volatile boolean serverAnimate;

	private static volatile Component clientText;
	private static volatile int clientTicks;
	private static volatile boolean clientAnimate;

	private ActionBarMessages() {
	}

	public static void pushServer(Component text, boolean animate) {
		serverText = text;
		serverAnimate = animate;
		serverTicks = LIFETIME_TICKS;
	}

	public static void pushClient(Component text, boolean animate) {
		clientText = text;
		clientAnimate = animate;
		clientTicks = LIFETIME_TICKS;
	}

	/** 每客户端 tick 调用一次。 */
	public static void tick() {
		if (serverTicks > 0) {
			serverTicks--;
		}
		if (clientTicks > 0) {
			clientTicks--;
		}
	}

	public static boolean hasServerMessage() {
		return serverText != null && serverTicks > 0;
	}

	public static boolean hasClientMessage() {
		return clientText != null && clientTicks > 0;
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		ActionBarConfig config = ActionBarConfig.get();
		if (!config.enabled) {
			return;
		}
		renderChannel(graphics, deltaTracker, serverText, serverTicks, serverAnimate,
			ActionBarConfig.serverX(), ActionBarConfig.serverY(), ActionBarConfig.serverScale(), config.hideServer);
		renderChannel(graphics, deltaTracker, clientText, clientTicks, clientAnimate,
			ActionBarConfig.clientX(), ActionBarConfig.clientY(), ActionBarConfig.clientScale(), false);
	}

	private static void renderChannel(GuiGraphics graphics, DeltaTracker deltaTracker, Component text,
			int ticks, boolean animate, int offsetX, int offsetY, float scale, boolean hidden) {
		if (hidden || text == null || ticks <= 0) {
			return;
		}
		// 与原版 renderOverlayMessage 相同的透明度曲线
		float remaining = (float) ticks - deltaTracker.getGameTimeDeltaPartialTick(false);
		int alpha = (int) (remaining * 255.0F / FADE_TICKS);
		if (alpha > 255) {
			alpha = 255;
		}
		if (alpha <= 0) {
			return;
		}
		int color = animate
			? Mth.hsvToArgb(remaining / 50.0F, 0.7F, 0.6F, alpha)
			: ARGB.white(alpha);
		BarRenderer.draw(graphics, text, offsetX, offsetY, scale, color);
	}
}

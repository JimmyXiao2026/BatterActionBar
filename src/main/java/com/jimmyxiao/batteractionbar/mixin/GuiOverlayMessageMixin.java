package com.jimmyxiao.batteractionbar.mixin;

import com.jimmyxiao.batteractionbar.ActionBarMessages;
import com.jimmyxiao.batteractionbar.ActionBarTracker;
import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接管原版动作栏。
 *
 * 原版只有一份 overlayMessage 槽位，服务端每 tick 下发就会把客户端 Mod 的动作栏顶掉。
 * 这里在 setOverlayMessage 入口把消息按来源分别存进两条独立通道（各自计时），
 * 再取消原版绘制，由 ActionBarMessages 同时画出两条、各用各的坐标。
 */
@Mixin(Gui.class)
public abstract class GuiOverlayMessageMixin {

	@Inject(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
	private void batteractionbar$capture(Component message, boolean animate, CallbackInfo ci) {
		if (ActionBarTracker.classify(message) == ActionBarTracker.Origin.SERVER) {
			ActionBarMessages.pushServer(message, animate);
		} else {
			ActionBarMessages.pushClient(message, animate);
		}
	}

	@Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void batteractionbar$renderTwoChannels(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!ActionBarConfig.get().enabled) {
			// 总开关关闭：完全交给原版
			return;
		}
		ci.cancel();
		ActionBarMessages.render(graphics, deltaTracker);
	}
}

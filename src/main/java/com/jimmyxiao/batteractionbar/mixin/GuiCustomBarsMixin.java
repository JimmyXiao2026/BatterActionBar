package com.jimmyxiao.batteractionbar.mixin;

import com.jimmyxiao.batteractionbar.ActionBarHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在原版 HUD 绘制完之后，补画其它 Mod 通过 API 注册的额外动作栏。
 */
@Mixin(Gui.class)
public abstract class GuiCustomBarsMixin {

	@Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
	private void batteractionbar$renderExtraBars(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ActionBarHudRenderer.renderExtraBars(graphics);
	}
}

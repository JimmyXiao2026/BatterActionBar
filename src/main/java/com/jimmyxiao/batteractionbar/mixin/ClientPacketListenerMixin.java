package com.jimmyxiao.batteractionbar.mixin;

import com.jimmyxiao.batteractionbar.ActionBarTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在服务端动作栏的两个入站包里打标记，以便渲染时区分来源。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "setActionBarText", at = @At("HEAD"))
	private void batteractionbar$markActionBarPacket(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		ActionBarTracker.markServerMessage(packet.text());
	}

	@Inject(method = "setActionBarText", at = @At("RETURN"))
	private void batteractionbar$clearActionBarPacket(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
		ActionBarTracker.clearServerMessage();
	}

	@Inject(method = "handleSystemChat", at = @At("HEAD"))
	private void batteractionbar$markSystemChatPacket(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (packet.overlay()) {
			ActionBarTracker.markServerMessage(packet.content());
		}
	}

	@Inject(method = "handleSystemChat", at = @At("RETURN"))
	private void batteractionbar$clearSystemChatPacket(ClientboundSystemChatPacket packet, CallbackInfo ci) {
		if (packet.overlay()) {
			ActionBarTracker.clearServerMessage();
		}
	}
}

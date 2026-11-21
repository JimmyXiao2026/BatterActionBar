package com.jimmyxiao.batteractionbar;

import net.minecraft.network.chat.Component;

/**
 * 记录「当前显示的动作栏消息」到底来自服务端还是客户端。
 *
 * 服务端下发动作栏只有两条包路径：
 * ClientboundSetActionBarTextPacket（原版动作栏包）和
 * ClientboundSystemChatPacket(overlay = true)（聊天框伪装成动作栏）。
 * 其余途径（其它 Mod 直接调用 Gui#setOverlayMessage、LocalPlayer#displayClientMessage
 * 等）一律视为客户端动作栏。
 *
 * 判定方式：在包处理入口登记那条 Component 对象，之后 Gui#setOverlayMessage 收到
 * 同一个对象即判定为服务端消息。用对象同一性而不是布尔标记，可以避免误伤。
 */
public final class ActionBarTracker {

	public enum Origin {
		SERVER,
		CLIENT
	}

	private static Component pendingServerMessage;
	private static Origin current = Origin.CLIENT;

	private ActionBarTracker() {
	}

	/** 包处理入口登记：这条消息来自服务端。 */
	public static void markServerMessage(Component message) {
		pendingServerMessage = message;
	}

	/** 包处理结束清理登记（即使没走到 setOverlayMessage 也不会留下脏状态）。 */
	public static void clearServerMessage() {
		pendingServerMessage = null;
	}

	/** build 时解析这条消息的来源。 */
	public static Origin classify(Component message) {
		boolean fromServer = message != null && message == pendingServerMessage;
		if (fromServer) {
			pendingServerMessage = null;
		}
		current = fromServer ? Origin.SERVER : Origin.CLIENT;
		return current;
	}

	public static Origin current() {
		return current;
	}

	public static boolean isServer() {
		return current == Origin.SERVER;
	}
}

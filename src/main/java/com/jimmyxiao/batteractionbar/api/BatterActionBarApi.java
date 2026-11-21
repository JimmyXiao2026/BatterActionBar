package com.jimmyxiao.batteractionbar.api;

import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * 供其它 Mod 调用的公共接口：注册自己额外的动作栏。
 *
 * 在客户端初始化里调用一次即可，例如：
 *
 * <pre>{@code
 * ActionBarSlot myBar = BatterActionBarApi.registerSlot("mymod_status", Component.literal("状态栏"));
 * ...
 * myBar.show(Component.literal("正在采集"), 100);
 * }</code></pre>
 *
 * 注册后设置界面会自动多出一条可拖动的动作栏，位置写进模组配置，
 * 与内建的服务器/客户端动作栏互不影响。
 */
public final class BatterActionBarApi {

	private static final Map<String, ActionBarSlot> SLOTS = new LinkedHashMap<>();
	private static volatile List<ActionBarSlot> snapshot = List.of();

	private BatterActionBarApi() {
	}

	/** 注册一条动作栏，默认位置是原版动作栏所在点、默认原版大小。 */
	public static ActionBarSlot registerSlot(String id, Component displayName) {
		return registerSlot(id, displayName, 0, 0, ActionBarConfig.DEFAULT_SCALE);
	}

	public static ActionBarSlot registerSlot(String id, Component displayName, int defaultOffsetX, int defaultOffsetY) {
		return registerSlot(id, displayName, defaultOffsetX, defaultOffsetY, ActionBarConfig.DEFAULT_SCALE);
	}

	/**
	 * 注册一条动作栏。
	 *
	 * @param id      唯一标识，也是配置文件里的键；不可与 "server" / "client" 重名
	 * @param displayName 设置界面里显示的名字
	 * @param defaultOffsetX 默认 X 偏移（GUI 缩放像素）
	 * @param defaultOffsetY 默认 Y 偏移（GUI 缩放像素）
	 * @param defaultScale 默认缩放（1.0 = 原版大小，允许 0.25 ~ 4.0）
	 * @return 动作栏句柄；重复注册同一个 id 时返回已有的那个
	 */
	public static synchronized ActionBarSlot registerSlot(String id, Component displayName, int defaultOffsetX,
			int defaultOffsetY, float defaultScale) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(displayName, "displayName");
		String key = id.trim();
		if (key.isEmpty()) {
			throw new IllegalArgumentException("动作栏 id 不能为空");
		}
		if (ActionBarConfig.SERVER_ID.equals(key) || ActionBarConfig.CLIENT_ID.equals(key)) {
			throw new IllegalArgumentException("动作栏 id 与内建动作栏冲突: " + key);
		}
		ActionBarSlot existing = SLOTS.get(key);
		if (existing != null) {
			return existing;
		}
		float scale = ActionBarConfig.clampScale(defaultScale);
		ActionBarSlot slot = new ActionBarSlot(key, displayName, defaultOffsetX, defaultOffsetY, scale);
		SLOTS.put(key, slot);
		snapshot = List.copyOf(SLOTS.values());
		// 先把默认位置与大小落进配置，设置界面和保存都能直接看到
		ActionBarConfig.get().ensurePosition(key, defaultOffsetX, defaultOffsetY);
		ActionBarConfig.get().applyDefaultScale(key, scale);
		return slot;
	}

	/** 全部已注册的额外动作栏（按注册顺序）。 */
	public static List<ActionBarSlot> slots() {
		return snapshot;
	}

	public static synchronized ActionBarSlot getSlot(String id) {
		return SLOTS.get(id);
	}

	/** 断开服务器连接时清空所有额外动作栏的内容。 */
	public static void clearAll() {
		for (ActionBarSlot slot : snapshot) {
			slot.clear();
		}
	}
}

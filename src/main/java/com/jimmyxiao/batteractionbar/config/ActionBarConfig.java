package com.jimmyxiao.batteractionbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jimmyxiao.batteractionbar.api.ActionBarSlot;
import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模组配置：每条动作栏相对「原版动作栏位置」的偏移量与缩放。
 *
 * 内建两条：
 * - server：服务器下发的动作栏，默认上移 30 像素，让出原版位置
 * - client：客户端其它 Mod 的动作栏，默认不动
 *
 * 其它 Mod 通过 BatterActionBarApi 注册的动作栏也会按 id 存在 bars 里。
 */
public final class ActionBarConfig {

	public static final String SERVER_ID = "server";
	public static final String CLIENT_ID = "client";

	public static final int DEFAULT_SERVER_Y = -30;
	public static final int DEFAULT_CLIENT_Y = 0;
	public static final float DEFAULT_SCALE = 1.0f;

	public static final float MIN_SCALE = 0.25f;
	public static final float MAX_SCALE = 4.0f;

	private static final String FILE_NAME = "batteractionbar.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ActionBarConfig instance = new ActionBarConfig();

	/** 总开关：关闭时完全走原版渲染，额外动作栏也不画。 */
	public boolean enabled = true;

	/** 直接隐藏服务器下发的动作栏（不再绘制，也不再占用视觉空间）。 */
	public boolean hideServer = false;

	/** 每条动作栏的位置与缩放，键是动作栏 id。 */
	public Map<String, BarPosition> bars = new LinkedHashMap<>();

	public static final class BarPosition {
		public int x;
		public int y;
		public float scale = DEFAULT_SCALE;

		public BarPosition() {
		}

		public BarPosition(int x, int y, float scale) {
			this.x = x;
			this.y = y;
			this.scale = scale;
		}
	}

	public static ActionBarConfig get() {
		return instance;
	}

	// ------------------------------------------------------------------
	// 读写位置与缩放
	// ------------------------------------------------------------------

	/** 取位置，没有就按默认值建一条。 */
	public BarPosition ensurePosition(String id, int defaultX, int defaultY) {
		if (this.bars == null) {
			this.bars = new LinkedHashMap<>();
		}
		BarPosition position = this.bars.get(id);
		if (position == null) {
			position = new BarPosition(defaultX, defaultY, DEFAULT_SCALE);
			this.bars.put(id, position);
		}
		return position;
	}

	public int x(String id, int defaultX, int defaultY) {
		return ensurePosition(id, defaultX, defaultY).x;
	}

	public int y(String id, int defaultX, int defaultY) {
		return ensurePosition(id, defaultX, defaultY).y;
	}

	/** 读取缩放；配置里没有（例如旧配置没这个字段）时用默认值。 */
	public float scale(String id, float defaultScale) {
		BarPosition position = this.bars == null ? null : this.bars.get(id);
		if (position == null || position.scale <= 0.0f) {
			return clampScale(defaultScale);
		}
		return clampScale(position.scale);
	}

	public void setPosition(String id, int x, int y) {
		BarPosition position = ensurePosition(id, x, y);
		position.x = clampOffset(x);
		position.y = clampOffset(y);
	}

	public void resetPosition(String id, int defaultX, int defaultY) {
		setPosition(id, defaultX, defaultY);
	}

	public void setScale(String id, float scale) {
		ensurePosition(id, 0, 0).scale = clampScale(scale);
	}

	/** 只有当前没设过缩放时才写入默认值（注册新动作栏时用）。 */
	public void applyDefaultScale(String id, float defaultScale) {
		BarPosition position = ensurePosition(id, 0, 0);
		if (position.scale <= 0.0f) {
			position.scale = clampScale(defaultScale);
		}
	}

	/** 把所有动作栏（含其它 Mod 注册的）位置和缩放恢复成默认。 */
	public void resetAllPositions() {
		this.bars = new LinkedHashMap<>();
		ensurePosition(SERVER_ID, 0, DEFAULT_SERVER_Y);
		ensurePosition(CLIENT_ID, 0, DEFAULT_CLIENT_Y);
		for (ActionBarSlot slot : BatterActionBarApi.slots()) {
			ensurePosition(slot.id(), slot.defaultOffsetX(), slot.defaultOffsetY());
			applyDefaultScale(slot.id(), slot.defaultScale());
		}
	}

	/** 内建两条动作栏的便捷读取。 */
	public static int serverX() {
		return get().x(SERVER_ID, 0, DEFAULT_SERVER_Y);
	}

	public static int serverY() {
		return get().y(SERVER_ID, 0, DEFAULT_SERVER_Y);
	}

	public static float serverScale() {
		return get().scale(SERVER_ID, DEFAULT_SCALE);
	}

	public static int clientX() {
		return get().x(CLIENT_ID, 0, DEFAULT_CLIENT_Y);
	}

	public static int clientY() {
		return get().y(CLIENT_ID, 0, DEFAULT_CLIENT_Y);
	}

	public static float clientScale() {
		return get().scale(CLIENT_ID, DEFAULT_SCALE);
	}

	/** 把偏移量限制在合理范围内，避免拖到屏幕外面找不回来。 */
	public static int clampOffset(int value) {
		return Math.max(-2048, Math.min(2048, value));
	}

	public static float clampScale(float value) {
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
	}

	// ------------------------------------------------------------------
	// 持久化
	// ------------------------------------------------------------------

	public static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			resetToDefaultState();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			ActionBarConfig loaded = GSON.fromJson(reader, ActionBarConfig.class);
			if (loaded != null) {
				instance = loaded;
			}
		} catch (Exception ignored) {
			// 配置损坏时保留默认值，不打断游戏启动
		}
		if (instance.bars == null) {
			instance.bars = new LinkedHashMap<>();
		}
		instance.ensurePosition(SERVER_ID, 0, DEFAULT_SERVER_Y);
		instance.ensurePosition(CLIENT_ID, 0, DEFAULT_CLIENT_Y);
		for (ActionBarSlot slot : BatterActionBarApi.slots()) {
			instance.ensurePosition(slot.id(), slot.defaultOffsetX(), slot.defaultOffsetY());
			instance.applyDefaultScale(slot.id(), slot.defaultScale());
		}
	}

	private static void resetToDefaultState() {
		instance.ensurePosition(SERVER_ID, 0, DEFAULT_SERVER_Y);
		instance.ensurePosition(CLIENT_ID, 0, DEFAULT_CLIENT_Y);
	}

	public static void save() {
		Path path = configPath();
		try {
			Path parent = path.getParent();
			if (parent != null && !Files.exists(parent)) {
				Files.createDirectories(parent);
			}
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException ignored) {
		}
	}
}

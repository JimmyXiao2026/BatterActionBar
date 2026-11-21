package com.jimmyxiao.batteractionbar.api;

import com.jimmyxiao.batteractionbar.BarRenderer;
import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * 一个可被其它 Mod 调用的动作栏格子。
 *
 * 位置与原版动作栏一样以 (屏幕宽/2, 屏幕高-68) 为原点，
 * 单位是 GUI 缩放像素，偏移量保存在模组配置里，可以在设置界面里拖动。
 */
public final class ActionBarSlot {

	/** show(Component) 默认持续 60 tick（3 秒），和原版动作栏时长一致。 */
	public static final int DEFAULT_DURATION_TICKS = 60;

	private final String id;
	private final Component displayName;
	private final int defaultOffsetX;
	private final int defaultOffsetY;
	private final float defaultScale;

	private volatile Component text;
	private volatile int remainingTicks;
	private volatile boolean persistent;

	ActionBarSlot(String id, Component displayName, int defaultOffsetX, int defaultOffsetY, float defaultScale) {
		this.id = id;
		this.displayName = displayName;
		this.defaultOffsetX = defaultOffsetX;
		this.defaultOffsetY = defaultOffsetY;
		this.defaultScale = defaultScale;
	}

	public String id() {
		return this.id;
	}

	/** 设置界面里显示的名字。 */
	public Component displayName() {
		return this.displayName;
	}

	public int defaultOffsetX() {
		return this.defaultOffsetX;
	}

	public int defaultOffsetY() {
		return this.defaultOffsetY;
	}

	/** 注册时给的默认缩放（1.0 = 原版大小）。 */
	public float defaultScale() {
		return this.defaultScale;
	}

	/** 显示文本，默认 60 tick。 */
	public void show(Component text) {
		show(text, DEFAULT_DURATION_TICKS);
	}

	public void show(String text) {
		show(Component.literal(text));
	}

	/** 显示文本，ticks 后自动消失（最后 20 tick 淡出）。 */
	public void show(Component text, int ticks) {
		if (text == null) {
			return;
		}
		this.text = text;
		this.persistent = false;
		this.remainingTicks = Math.max(1, ticks);
	}

	/** 一直显示，直到下一次 show / clear。 */
	public void showPersistent(Component text) {
		if (text == null) {
			return;
		}
		this.text = text;
		this.persistent = true;
		this.remainingTicks = 0;
	}

	public void clear() {
		this.text = null;
		this.persistent = false;
		this.remainingTicks = 0;
	}

	public boolean isShowing() {
		return this.text != null && (this.persistent || this.remainingTicks > 0);
	}

	public Component currentText() {
		return this.text;
	}

	/** 当前 X 偏移（从配置实时读取）。 */
	public int offsetX() {
		return ActionBarConfig.get().x(this.id, this.defaultOffsetX, this.defaultOffsetY);
	}

	/** 当前 Y 偏移（从配置实时读取）。 */
	public int offsetY() {
		return ActionBarConfig.get().y(this.id, this.defaultOffsetX, this.defaultOffsetY);
	}

	/** 代码里直接设定位置（下次保存配置时生效）。 */
	public void setOffset(int x, int y) {
		ActionBarConfig.get().setPosition(this.id, x, y);
	}

	public void resetOffset() {
		ActionBarConfig.get().resetPosition(this.id, this.defaultOffsetX, this.defaultOffsetY);
	}

	/** 当前缩放（从配置实时读取，1.0 = 原版大小）。 */
	public float scale() {
		return ActionBarConfig.get().scale(this.id, this.defaultScale);
	}

	/** 代码里直接设定缩放（0.25 ~ 4.0），下次保存配置时生效。 */
	public void setScale(float scale) {
		ActionBarConfig.get().setScale(this.id, scale);
	}

	/** 恢复注册时给的默认缩放。 */
	public void resetScale() {
		ActionBarConfig.get().setScale(this.id, this.defaultScale);
	}

	// ------------------------------------------------------------------
	// 以下由模组内部调用，其它 Mod 不需要用
	// ------------------------------------------------------------------

	public void tickInternal() {
		if (this.persistent) {
			return;
		}
		if (this.remainingTicks > 0) {
			this.remainingTicks--;
		}
	}

	public void renderInternal(GuiGraphics graphics) {
		Component message = this.text;
		if (message == null) {
			return;
		}
		int alpha;
		if (this.persistent) {
			alpha = 255;
		} else {
			if (this.remainingTicks <= 0) {
				return;
			}
			alpha = Math.min(255, this.remainingTicks * 255 / 20);
		}

		BarRenderer.draw(graphics, message, offsetX(), offsetY(), scale(), ARGB.white(alpha));
	}
}

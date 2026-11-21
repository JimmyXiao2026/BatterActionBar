package com.jimmyxiao.batteractionbar.gui;

import com.jimmyxiao.batteractionbar.BarRenderer;
import com.jimmyxiao.batteractionbar.api.ActionBarSlot;
import com.jimmyxiao.batteractionbar.api.BatterActionBarApi;
import com.jimmyxiao.batteractionbar.config.ActionBarConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 设置界面：拖动内建的服务器/客户端动作栏，以及其它 Mod 通过 API 注册的额外动作栏。
 *
 * 预览位置与游戏内 HUD 完全对应：原版锚点在 (屏幕宽/2, 屏幕高-68)，文本再向上偏 4 像素。
 * 左键拖动，滚轮缩放，右键复位单条（位置 + 缩放），松手自动保存。
 */
public class ActionBarConfigScreen extends Screen {

	private static final int VANILLA_Y = 68;
	private static final float SCALE_STEP = 0.1f;
	private static final int SERVER_COLOR = 0xFF8FD6FF;
	private static final int CLIENT_COLOR = 0xFFFFFFFF;
	private static final int HIDDEN_COLOR = 0xFF6A6A6A;
	private static final int FRAME_COLOR = 0xFFFFD24A;
	private static final int TEXT_DIM = 0xFFA0A0A0;

	private static final class Entry {
		private final String id;
		private final Component name;
		private final int color;
		private final int defaultOffsetX;
		private final int defaultOffsetY;
		private final float defaultScale;
		private final String previewKey;
		private final ActionBarSlot slot;

		private Entry(String id, Component name, int color, int defaultOffsetX, int defaultOffsetY, float defaultScale,
				String previewKey, ActionBarSlot slot) {
			this.id = id;
			this.name = name;
			this.color = color;
			this.defaultOffsetX = defaultOffsetX;
			this.defaultOffsetY = defaultOffsetY;
			this.defaultScale = defaultScale;
			this.previewKey = previewKey;
			this.slot = slot;
		}

		private Component preview() {
			if (this.slot != null) {
				Component current = this.slot.currentText();
				return this.slot.isShowing() && current != null ? current : this.slot.displayName();
			}
			return Component.translatable(this.previewKey);
		}
	}

	private final Screen parent;

	private final List<Entry> entries = new ArrayList<>();
	private String draggingId;
	private double dragAccX;
	private double dragAccY;

	public ActionBarConfigScreen(Screen parent) {
		super(Component.translatable("screen.batteractionbar.title"));
		this.parent = parent;
	}

	private ActionBarConfig config() {
		return ActionBarConfig.get();
	}

	@Override
	protected void init() {
		this.draggingId = null;
		this.buildEntries();

		this.addRenderableWidget(Checkbox.builder(Component.translatable("screen.batteractionbar.enabled"), this.font)
			.pos(this.width / 2 - 150, 40)
			.selected(config().enabled)
			.onValueChange((checkbox, value) -> {
				config().enabled = value;
				ActionBarConfig.save();
			})
			.build());

		this.addRenderableWidget(Checkbox.builder(Component.translatable("screen.batteractionbar.hide_server"), this.font)
			.pos(this.width / 2 - 150, 62)
			.selected(config().hideServer)
			.onValueChange((checkbox, value) -> {
				config().hideServer = value;
				ActionBarConfig.save();
			})
			.build());

		int buttonWidth = 150;
		int gap = 8;
		int startX = this.width / 2 - buttonWidth - gap / 2;
		int buttonY = this.height - 34;

		this.addRenderableWidget(Button.builder(Component.translatable("screen.batteractionbar.reset_all"), b -> {
			config().resetAllPositions();
			ActionBarConfig.save();
		}).bounds(startX, buttonY, buttonWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("screen.batteractionbar.done"), b -> this.onClose())
			.bounds(startX + buttonWidth + gap, buttonY, buttonWidth, 20).build());
	}

	private void buildEntries() {
		this.entries.clear();
		// 内建两条
		this.entries.add(new Entry(ActionBarConfig.SERVER_ID,
			Component.translatable("screen.batteractionbar.name_server"),
			SERVER_COLOR, 0, ActionBarConfig.DEFAULT_SERVER_Y, ActionBarConfig.DEFAULT_SCALE,
			"screen.batteractionbar.preview_server", null));
		this.entries.add(new Entry(ActionBarConfig.CLIENT_ID,
			Component.translatable("screen.batteractionbar.name_client"),
			CLIENT_COLOR, 0, ActionBarConfig.DEFAULT_CLIENT_Y, ActionBarConfig.DEFAULT_SCALE,
			"screen.batteractionbar.preview_client", null));
		// 其它 Mod 注册的
		for (ActionBarSlot slot : BatterActionBarApi.slots()) {
			this.entries.add(new Entry(slot.id(), slot.displayName(), CLIENT_COLOR,
				slot.defaultOffsetX(), slot.defaultOffsetY(), slot.defaultScale(), null, slot));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("screen.batteractionbar.hint"),
			this.width / 2, this.height - 50, TEXT_DIM);

		// 原版动作栏所在位置（锚点线）
		int anchorY = this.height - VANILLA_Y;
		graphics.fill(8, anchorY, this.width - 8, anchorY + 1, 0x22FFFFFF);
		graphics.drawString(this.font, Component.translatable("screen.batteractionbar.vanilla_anchor"),
			10, anchorY + 3, 0x66FFFFFF);

		for (Entry entry : this.entries) {
			renderEntry(graphics, entry, mouseX, mouseY);
		}
	}

	private void renderEntry(GuiGraphics graphics, Entry entry, int mouseX, int mouseY) {
		ActionBarConfig config = config();
		int offsetX = config.x(entry.id, entry.defaultOffsetX, entry.defaultOffsetY);
		int offsetY = config.y(entry.id, entry.defaultOffsetX, entry.defaultOffsetY);
		float scale = config.scale(entry.id, entry.defaultScale);
		int centerX = this.width / 2 + offsetX;
		int anchorY = this.height - VANILLA_Y + offsetY;
		int topY = anchorY - Math.round(4.0f * scale);

		Component preview = entry.preview();
		int width = Math.max(1, Math.round(this.font.width(preview) * scale));
		int left = centerX - width / 2;
		int height = Math.max(1, Math.round(9.0f * scale));

		boolean hidden = entry.id.equals(ActionBarConfig.SERVER_ID) && config.hideServer;
		boolean active = entry.id.equals(this.draggingId);
		boolean hovered = entry.id.equals(targetAt(mouseX, mouseY));

		graphics.fill(left - 3, topY - 3, left + width + 3, topY + height + 3, 0x30000000);
		if (hovered || active) {
			graphics.renderOutline(left - 4, topY - 4, width + 8, height + 8, FRAME_COLOR);
		}

		BarRenderer.draw(graphics, preview, offsetX, offsetY, scale, hidden ? HIDDEN_COLOR : entry.color);

		graphics.drawCenteredString(this.font,
			Component.translatable("screen.batteractionbar.entry_label", entry.name, offsetX, offsetY,
				String.format(Locale.ROOT, "%.2fx", scale)),
			centerX, topY - 16, TEXT_DIM);
	}

	/** 鼠标当前压在哪条动作栏上（后画的优先）。 */
	private String targetAt(int mouseX, int mouseY) {
		ActionBarConfig config = config();
		for (int i = this.entries.size() - 1; i >= 0; i--) {
			Entry entry = this.entries.get(i);
			int centerX = this.width / 2 + config.x(entry.id, entry.defaultOffsetX, entry.defaultOffsetY);
			int anchorY = this.height - VANILLA_Y + config.y(entry.id, entry.defaultOffsetX, entry.defaultOffsetY);
			float scale = config.scale(entry.id, entry.defaultScale);
			int topY = anchorY - Math.round(4.0f * scale);
			int width = Math.max(1, Math.round(this.font.width(entry.preview()) * scale));
			int height = Math.max(1, Math.round(9.0f * scale));
			if (mouseX >= centerX - width / 2 - 6 && mouseX <= centerX + width / 2 + 6
				&& mouseY >= topY - 6 && mouseY <= topY + height + 6) {
				return entry.id;
			}
		}
		return null;
	}

	private Entry entryById(String id) {
		for (Entry entry : this.entries) {
			if (entry.id.equals(id)) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		String target = targetAt((int) event.x(), (int) event.y());
		if (target == null) {
			return false;
		}
		if (event.button() == 1) {
			// 右键复位这一条（位置 + 缩放）
			Entry entry = entryById(target);
			if (entry != null) {
				config().resetPosition(entry.id, entry.defaultOffsetX, entry.defaultOffsetY);
				config().setScale(entry.id, entry.defaultScale);
				ActionBarConfig.save();
			}
			return true;
		}
		if (event.button() != 0) {
			return false;
		}
		Entry entry = entryById(target);
		if (entry == null) {
			return false;
		}
		this.draggingId = target;
		this.dragAccX = config().x(target, entry.defaultOffsetX, entry.defaultOffsetY);
		this.dragAccY = config().y(target, entry.defaultOffsetX, entry.defaultOffsetY);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.draggingId == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		this.dragAccX += dragX;
		this.dragAccY += dragY;
		config().setPosition(this.draggingId,
			ActionBarConfig.clampOffset((int) Math.round(this.dragAccX)),
			ActionBarConfig.clampOffset((int) Math.round(this.dragAccY)));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.draggingId != null) {
			this.draggingId = null;
			ActionBarConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	/** 滚轮缩放：鼠标压在哪条动作栏上就调哪条，每次 0.1 倍。 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		String target = targetAt((int) mouseX, (int) mouseY);
		if (target == null || scrollY == 0.0) {
			return false;
		}
		Entry entry = entryById(target);
		if (entry == null) {
			return false;
		}
		float current = config().scale(entry.id, entry.defaultScale);
		float updated = current + (float) scrollY * SCALE_STEP;
		// 保留两位小数，避免浮点累积成一串 0.9999999
		updated = Math.round(updated * 100.0f) / 100.0f;
		config().setScale(entry.id, updated);
		ActionBarConfig.save();
		return true;
	}

	@Override
	public void onClose() {
		ActionBarConfig.save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}
}

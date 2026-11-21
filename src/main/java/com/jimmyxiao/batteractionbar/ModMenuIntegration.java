package com.jimmyxiao.batteractionbar;

import com.jimmyxiao.batteractionbar.gui.ActionBarConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu 集成：在模组列表里为本模组提供「设置」按钮。
 * Mod Menu 未安装时这个类不会被加载。
 */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ActionBarConfigScreen::new;
	}
}

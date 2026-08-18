package com.github.reviversmc.microdurability.rendertest;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderTestMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("RenderTest");

	@Override
	public void onInitializeClient() {
		LOGGER.info("[RenderTest] initialized - will auto-create world + equip damaged armor");
	}
}

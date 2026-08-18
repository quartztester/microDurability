package com.github.reviversmc.microdurability;

import java.util.function.Supplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Self-contained entry point for MC 26.x. MC 26.x is non-obfuscated, so this module
 * cannot reuse the yarn-namespace core; the version check and config loading are
 * re-implemented here.
 */
public class MicroDurability262 implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("MicroDurability");
	public static ModConfig262 config;
	public static Renderer262 renderer;

	static final Supplier<Boolean> IS_COMPATIBLE = () -> isWithin("26.2", "*");
	private static boolean initialized = false;

	@Override
	public void onInitialize() {
		if (initialized || !IS_COMPATIBLE.get()) {
			return;
		}

		initialized = true;

		renderer = new Renderer262();
		config = new ModConfig262();
	}

	private static boolean isWithin(String inclusiveLowerBounds, String inclusiveUpperBounds) {
		Version mcVersion = FabricLoader.getInstance()
				.getModContainer("minecraft")
				.get()
				.getMetadata()
				.getVersion();

		Version lowestSupportedVersion;
		Version highestSupportedVersion;

		try {
			lowestSupportedVersion = Version.parse(inclusiveLowerBounds);
			highestSupportedVersion = Version.parse(inclusiveUpperBounds);
		} catch (VersionParsingException e) {
			throw new RuntimeException("Failed to parse version bounds", e);
		}

		if (mcVersion.compareTo(lowestSupportedVersion) < 0) {
			return false;
		}

		if (inclusiveUpperBounds.equals("*") || mcVersion.compareTo(highestSupportedVersion) <= 0) {
			return true;
		}

		return false;
	}
}

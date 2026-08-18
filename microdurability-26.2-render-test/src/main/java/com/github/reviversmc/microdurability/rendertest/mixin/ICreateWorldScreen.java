package com.github.reviversmc.microdurability.rendertest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

@Mixin(CreateWorldScreen.class)
public interface ICreateWorldScreen {
	@Invoker("onCreate")
	void invokeOnCreate();
}

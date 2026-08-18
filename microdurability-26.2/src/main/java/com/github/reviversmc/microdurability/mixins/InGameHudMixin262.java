package com.github.reviversmc.microdurability.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;

import com.github.reviversmc.microdurability.MicroDurability262;

@Mixin(Hud.class)
public class InGameHudMixin262 {
	@Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
	private void renderArmorArea(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo callbackInfo) {
		if (MicroDurability262.renderer == null) {
			return;
		}

		MicroDurability262.renderer.renderArmorArea(context, ((Hud) (Object) this).getGuiTicks());
	}

	@Inject(method = "extractCrosshair", at = @At("RETURN"))
	private void renderHeldItemExclamationMark(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo callbackInfo) {
		if (MicroDurability262.renderer == null) {
			return;
		}

		MicroDurability262.renderer.renderHeldItemLowDurabilityWarning(context, ((Hud) (Object) this).getGuiTicks());
	}
}

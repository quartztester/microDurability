package com.github.reviversmc.microdurability.rendertest.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.github.reviversmc.microdurability.rendertest.RenderTestMod;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
	@Shadow
	@Nullable
	public LocalPlayer player;

	@Shadow
	@Nullable
	public ClientLevel level;

	@Shadow
	@Final
	public Gui gui;

	@Unique
	private boolean rt_openedCreate;
	@Unique
	private boolean rt_invokedCreate;
	@Unique
	private boolean rt_equipped;

	@Inject(method = "tick", at = @At("HEAD"))
	private void rt_tick(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;

		if (gui.overlay() == null) {
			if (!rt_openedCreate) {
				CreateWorldScreen.openFresh(mc, null);
				rt_openedCreate = true;
				RenderTestMod.LOGGER.info("[RenderTest] opened CreateWorldScreen");
			} else if (!rt_invokedCreate && gui.screen() instanceof ICreateWorldScreen cws) {
				cws.invokeOnCreate();
				rt_invokedCreate = true;
				RenderTestMod.LOGGER.info("[RenderTest] invoked onCreate");
			}
		}

		if (player != null && level != null && !rt_equipped) {
			player.setItemSlot(EquipmentSlot.HEAD, damaged(new ItemStack(Items.IRON_HELMET)));
			player.setItemSlot(EquipmentSlot.CHEST, damaged(new ItemStack(Items.IRON_CHESTPLATE)));
			player.setItemSlot(EquipmentSlot.LEGS, damaged(new ItemStack(Items.IRON_LEGGINGS)));
			player.setItemSlot(EquipmentSlot.FEET, damaged(new ItemStack(Items.IRON_BOOTS)));
			rt_equipped = true;
			RenderTestMod.LOGGER.info("[RenderTest] equipped damaged iron armor - render path should now fire");
		}
	}

	@Unique
	private ItemStack damaged(ItemStack stack) {
		int max = stack.getMaxDamage();

		if (max > 0) {
			stack.setDamageValue(max / 3);
		}

		return stack;
	}
}

package com.github.reviversmc.microdurability;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Self-contained renderer for MC 26.x (official Mojang names). MC 26.x is
 * non-obfuscated, so this cannot extend the yarn-namespace core {@code Renderer};
 * the render logic is re-implemented here. Raised/DoubleHotbar compat is omitted.
 */
public class Renderer262 {
	private static final Identifier microdurabilityTexture = Identifier.tryParse("microdurability:textures/gui/icons.png");
	// ponytail: icons.png is 256x256; blit needs the source texture size for UV scaling
	private static final int ICON_TEX_SIZE = 256;
	private final Minecraft mc;
	private static boolean debugLogged = false;

	protected Renderer262() {
		mc = Minecraft.getInstance();
	}

	private static String describe(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return "empty";
		return stack.getItem().getClass().getSimpleName() + (stack.isDamageableItem() ? "(damageable)" : "");
	}

	private boolean isStatusAreaVisible() {
		// The mixin only fires from Hud render methods, which run when the HUD is shown,
		// so the F1 "hide HUD" state is already handled upstream.
		return mc.player != null && !mc.player.isSpectator();
	}

	private boolean isTimeToShowWarning(int tick) {
		if (MicroDurability262.config.lowDurabilityWarning.blinkTime < 0.001) {
			return true;
		}

		return tick % (MicroDurability262.config.lowDurabilityWarning.blinkTime * 40f)
				> (MicroDurability262.config.lowDurabilityWarning.blinkTime * 20f);
	}

	public boolean shouldWarn(ItemStack stack) {
		if (stack == null || !stack.isDamageableItem()) {
			return false;
		}

		if (MicroDurability262.config.lowDurabilityWarning.onlyOnMendingItems && !hasMending(stack)) {
			return false;
		}

		int durability = stack.getMaxDamage() - stack.getDamageValue();
		boolean damageAbsoluteValueEnough = durability < MicroDurability262.config.lowDurabilityWarning.minDurabilityPointsBeforeWarning;
		boolean damagePercentageEnough = (durability * 100f / stack.getMaxDamage()) < MicroDurability262.config.lowDurabilityWarning.minDurabilityPercentageBeforeWarning;

		return damageAbsoluteValueEnough && damagePercentageEnough;
	}

	public void renderHeldItemLowDurabilityWarning(GuiGraphicsExtractor context, int tick) {
		if (!MicroDurability262.config.lowDurabilityWarning.displayWarningForTools
				|| !isStatusAreaVisible()
				|| !isTimeToShowWarning(tick)) {
			return;
		}

		int scaledWidth = mc.getWindow().getGuiScaledWidth();
		int scaledHeight = mc.getWindow().getGuiScaledHeight();

		for (ItemStack item : new ItemStack[]{mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
			if (!shouldWarn(item)) {
				continue;
			}

			// TODO: This doesn't align with the crosshair at some resolutions
			int warningX = scaledWidth/2 - 2;
			int warningY = scaledHeight/2 - 18;

			renderWarning(context, warningX, warningY);
			break;
		}
	}

	public void renderArmorArea(GuiGraphicsExtractor context, int tick) {
		if (!debugLogged) {
			debugLogged = true;
			ItemStack[] pieces = getArmorPieces();
			MicroDurability262.LOGGER.info("[microDurability 26.2] renderArmorArea fired. player={} visible={} sw={} sh={} feet={} legs={} chest={} head={}",
					mc.player != null, isStatusAreaVisible(),
					mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
					describe(pieces[0]), describe(pieces[1]), describe(pieces[2]), describe(pieces[3]));
		}

		if (!isStatusAreaVisible()) {
			return;
		}

		int scaledWidth = mc.getWindow().getGuiScaledWidth();
		int scaledHeight = mc.getWindow().getGuiScaledHeight();

		int x = scaledWidth/2 - 7;
		int y = scaledHeight - 30 - MicroDurability262.config.armorBars.yOffset;
		if (mc.player.experienceLevel > 0) y -= 6;

		boolean renderedWarning = MicroDurability262.config.lowDurabilityWarning.displayWarningForArmor
				&& isTimeToShowWarning(tick)
				&& renderArmorLowDurabilityWarning(context, x+5, y-12);

		if (!renderedWarning && MicroDurability262.config.armorBars.displayArmorBars) {
			renderArmorBars(context, x, y);
		}
	}

	public boolean renderArmorLowDurabilityWarning(GuiGraphicsExtractor context, int x, int y) {
		for (ItemStack armorPiece : getArmorPieces()) {
			if (!shouldWarn(armorPiece)) {
				continue;
			}

			renderWarning(context, x, y);
			return true;
		}

		return false;
	}

	private void renderArmorBars(GuiGraphicsExtractor context, int x, int y) {
		for (ItemStack armorPiece : getArmorPieces()) {
			renderBar(context, armorPiece, x, y -= 3);
		}
	}

	private ItemStack[] getArmorPieces() {
		return new ItemStack[]{
			mc.player.getItemBySlot(EquipmentSlot.FEET),
			mc.player.getItemBySlot(EquipmentSlot.LEGS),
			mc.player.getItemBySlot(EquipmentSlot.CHEST),
			mc.player.getItemBySlot(EquipmentSlot.HEAD)
		};
	}

	private void renderWarning(GuiGraphicsExtractor context, int x, int y) {
		// ponytail: blit UV/size semantics inferred from bytecode; verify icon placement in-game
		context.blit(RenderPipelines.GUI_TEXTURED, microdurabilityTexture, x, y, 0f, 0f, 3, 11, ICON_TEX_SIZE, ICON_TEX_SIZE);
	}

	private void renderBar(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
		if (stack == null || stack.isEmpty()) return;
		if (!MicroDurability262.config.armorBars.displayBarsForUndamagedArmor && !stack.isDamaged()) return;
		if (!stack.isDamageableItem()) return;

		int width = stack.getItem().getBarWidth(stack);
		context.fill(x, y, x + 13, y + 2, 0xFF000000);
		int red;
		int green;
		int blue;
		int alpha;

		if (!stack.isDamaged() && MicroDurability262.config.armorBars.useCustomBarColorForUndamagedArmor) {
			int argb = MicroDurability262.config.armorBars.customBarColorForUndamagedArmor;
			red = ((argb >> 16) & 0xFF) & 255;
			green = ((argb >> 8) & 0xFF) & 255;
			blue = (argb & 0xFF) & 255;
			alpha = ((argb >> 24) & 0xFF) & 255;
		} else {
			int color = stack.getItem().getBarColor(stack);
			red = color >> 16 & 255;
			green = color >> 8 & 255;
			blue = color & 255;
			alpha = 255;
		}

		context.fill(x, y, x + width, y + 1, (alpha << 24) | (red << 16) | (green << 8) | blue);
	}

	private boolean hasMending(ItemStack stack) {
		return EnchantmentHelper.has(stack, EnchantmentEffectComponents.REPAIR_WITH_XP);
	}
}

package com.github.reviversmc.microdurability;

// ponytail: plain POJO, no cloth-config/autoconfig. The 26.2 test instance uses YACL, not
// cloth-config, and a hard ConfigData reference crashes class-loading when cloth-config is
// absent. Config runs on defaults; add a real config GUI back if cloth-config support is wanted.
public class ModConfig262 {
	public ArmorBars armorBars = new ArmorBars();
	public LowDurabilityWarning lowDurabilityWarning = new LowDurabilityWarning();

	public static class ArmorBars {
		public boolean displayArmorBars = true;
		public boolean displayBarsForUndamagedArmor = true;
		public int yOffset = 0;
		public boolean useCustomBarColorForUndamagedArmor = false;
		public int customBarColorForUndamagedArmor = 0xFFFFFFFF;
	}

	public static class LowDurabilityWarning {
		public boolean displayWarningForTools = true;
		public boolean displayWarningForArmor = true;
		public boolean onlyOnMendingItems = true;
		public int minDurabilityPointsBeforeWarning = 100;
		public int minDurabilityPercentageBeforeWarning = 10;
		public float blinkTime = 1f;
	}
}

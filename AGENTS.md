# microDurability

Fabric mod that renders per-armor-piece durability bars. Multi-module: one
`microdurability-core` plus one compat module per Minecraft version
(`microdurability-1.21`, `microdurability-26.2`, ...). Each compat module is a
thin layer over the previous one (e.g. `Renderer262 extends Renderer1205`).

## Building

CI uses **JDK 21** (Temurin). Locally a newer JDK (25/26) runs Gradle but prints
`restricted method` warnings — harmless.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\latest\jdk-26"   # or jdk-21
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon <task> --console=plain > build.log 2>&1
Get-Content build.log -Tail 40
```

### Do NOT pipe Gradle output, and use `--no-daemon`

`gradlew.bat ... | Select-Object` (or any pipe) hangs until the tool timeout.
The Gradle **daemon** is a long-lived child that inherits the stdout handle and
keeps it open after the build ends, so the pipe never closes. Same problem with
`> file` redirect while a daemon is alive.

Fix:
- `--no-daemon` — Gradle runs in the client JVM, which exits and closes the handle.
- Redirect to `build.log` (no pipe), then read the file.

This returns in ~7s on failure instead of hanging.

## Version-matching gotcha

`fabric-loom` must match the Gradle wrapper. Loom declares a
`org.gradle.plugin.api-version`; if it's newer than the wrapper's Gradle, the
build fails at configuration with a "No matching variant ... api-version" error.
When bumping loom, bump `gradle/wrapper/gradle-wrapper.properties` to match.

Current: loom `1.18.0-alpha.16` requires **Gradle 9.7.0**.

## MC 26.x note — non-obfuscated, different loom plugin

MC 26.x is **non-obfuscated**: there is no yarn, no intermediary, and no official
Mojang mappings published for it (verified against the Fabric maven). So a 26.x
module is built differently from the 1.16–1.21 modules:

- Plugin: `net.fabricmc.fabric-loom` (the **base** plugin), NOT `fabric-loom-remap`.
- **No `mappings` dependency** at all.
- Use `implementation` / `runtimeOnly` (standard Gradle), NOT `modImplementation` /
  `modRuntimeOnly` (those only exist on the `-remap` plugin).

Verified working in an isolated project:
```groovy
plugins { id 'net.fabricmc.fabric-loom' version '1.18.0-alpha.16' }
dependencies {
    minecraft "com.mojang:minecraft:26.2"
    implementation "net.fabricmc:fabric-loader:0.16.7"
}
```

### Consequence: the 26.x module is fully self-contained

Every existing module (core + 1.16–1.21) is compiled against **yarn** names
(e.g. `net.minecraft.item.ItemStack`, `mc.options.hudHidden`). A 26.x module
compiles against **official** names (`net.minecraft.world.item.ItemStack`, ...).
The two namespaces don't line up, so a 26.x module **cannot** `api project(...)`
onto `microdurability-core` or any compat module, and cannot `extends Renderer1205`.
`microdurability-26.2` re-implements the render logic, entry point, mixin, config,
and version check in official names (no dependency on core or any compat module).

### 26.2 build specifics

- **Java 25+** (not 21): MC 26.2 targets JVM 25, so the module sets `options.release = 25`.
  CI must use JDK 25+ to build this module.
- **No config library**: `ModConfig262` is a plain POJO (no cloth-config/autoconfig). A hard
  `ConfigData` reference crashed class-loading on instances without cloth-config. Config runs
  on defaults; add a real config GUI back if needed.
- The root umbrella mod references it with `configuration: "default"` (the base plugin
  has no `namedElements` configuration, which only `-remap` creates).
- Gradle 9.x removed `base.archivesBaseName` — use `archivesName = archivesName.get() + '-suffix'`.

### 26.2 official-name translation (verified via javap on the deobf jar)

| 1.21 (yarn) | 26.2 (official) |
|---|---|
| `MinecraftClient` | `net.minecraft.client.Minecraft` |
| `mc.interactionManager` | `mc.gameMode` (`MultiPlayerGameMode`) |
| `getWindow().getScaledWidth/Height()` | `getWindow().getGuiScaledWidth/Height()` |
| `player.getHandItems()` | `player.getMainHandItem()` + `getOffhandItem()` |
| `player.getArmorItems()` | `player.getItemBySlot(EquipmentSlot.FEET/LEGS/CHEST/HEAD)` |
| `stack.isDamageable()` / `getDamage()` | `stack.isDamageableItem()` / `getDamageValue()` |
| `stack.getItemBarStep/Color()` | `stack.getItem().getBarWidth/Color(stack)` |
| `EnchantmentHelper.getEffect(s,X).isPresent()` | `EnchantmentHelper.has(s, EnchantmentEffectComponents.REPAIR_WITH_XP)` |
| `DrawContext` | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `InGameHud` (`renderHotbar`/`renderCrosshair`, `ticks`) | `Hud` (private `extractHotbarAndDecorations`/`extractCrosshair`, `getGuiTicks()`) |
| `RenderTickCounter` | `DeltaTracker` |
| `RenderLayer.getGuiOverlay()` + `fill(layer,…)` | `fill(x,y,x2,y2,color)` / `RenderPipelines.GUI_TEXTURED` |
| `drawTexture(tex,x,y,u,v,w,h)` | `blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, texW, texH)` |
| `ColorHelper.Argb.getArgb(a,r,g,b)` | inline `(a<<24)\|(r<<16)\|(g<<8)\|b` |

Inspect the deobf jar for more:
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar`

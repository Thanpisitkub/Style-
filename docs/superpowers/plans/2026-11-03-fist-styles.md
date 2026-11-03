# Style (Epic Fight Fist Styles) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Forge 1.20.1 mod "Style" that adds 10 hidden-in-hand fist-style items (Style 1–10) to Epic Fight, built into a `.jar` by GitHub Actions.

**Architecture:** Epic Fight data-driven weapon types (JSON, same shape as `effs:hand_combat`) generated from one table (`tools/styles.json`). Java only registers 10 items + a creative tab, sets attack damage/speed, and runs a reflection-based "launch on combo 4" hook. No Epic Fight classes are referenced at compile time.

**Tech Stack:** Forge 1.20.1 (47.x), ForgeGradle 6, Java 17, Gradle 8.1.1 (CI), Python 3 (generator + tests, Pillow for the texture), GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-03-fist-styles-design.md`

## Global Constraints

- Forge 1.20.1 (`1.20.1-47.3.0`), Java 17, ForgeGradle 6; Gradle 8.1.1 via `gradle/actions/setup-gradle@v4`.
- Mod id `fiststyle`, java package `dev.fiststyle`, display name "Style". Items `fiststyle:style_1` … `fiststyle:style_10`, names "Style 1" … "Style 10", creative tab titled "Style" (lang key `itemGroup.fiststyle`), tab icon = Style 1.
- Damage and speed per style (spec §7): S1 4.00/2.50, S2 4.50/2.40, S3 5.00/2.25, S4 5.50/2.10, S5 6.00/2.00, S6 6.25/1.90, S7 6.50/1.80, S8 7.00/1.70, S9 7.50/1.60, S10 8.00/1.50. Allowed range: damage 4.00–8.00, speed 1.50–2.50. Attribute values stored as damage − 1.0 and speed − 4.0 (main hand).
- `impact` = 0.5 + 0.15·(n−1) rounded to 2 decimals, `max_strikes` = 1, `armor_negation` = 0.0.
- Each combo list has exactly 8 entries: punch 1–6, sprint punch, jump punch (dash and air are always last two). Entry 4 (index 3) is the uppercut that launches.
- Idle/walk/run = `wom:biped/living/<name>_idle|_walk|_run`; `run` and `chase` omitted when the WOM animation does not exist (Style 2 has no `enderblaster_twohand_run`).
- `mods.toml` declares mandatory dependencies `forge [47,)`, `minecraft [1.20.1]`, `epicfight [20.14.3,)`, `wom`, `ef_cz`, `epicsanji` (ordering AFTER for the last four).
- Held item must be invisible: item model `display` scale `[0,0,0]` for `thirdperson_righthand`, `thirdperson_lefthand`, `firstperson_righthand`, `firstperson_lefthand`.
- Icon: one shared 16×16 texture, white letter S on black background.
- Work only on branch `claude/fist-styles`; never push `main`. Every commit message ends with:
  `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_015wjhFcFE6YDxN3YXACrjYD`.
- Verification limits: this environment cannot reach the Forge maven or run the game. The build gate is GitHub Actions (`gh run list/view`); in-game behaviour is tested by the user.

## Review Focus

1. A dependency jar missing or its mod id misspelled in `mods.toml` → the player gets a load-time crash; `tools/verify_jar.py` pins the exact ids (Task 3).
2. Style 2 has no `run` animation → the generator must omit `run`/`chase`, never reference a missing animation (Task 2).
3. Capability `type` not matching its type file name → style silently does nothing in game (Task 2).
4. A combo path not registered by the 3 animation mods → Epic Fight silently drops the weapon type; every path must be in `tools/known_animations.json` (Task 2).
5. Epic Fight API differs from what the reflection hook expects → no crash, one warning, combo still plays (Task 4).

## File Structure

```
settings.gradle, build.gradle, gradle.properties, .gitignore        Task 1
.github/workflows/build.yml                                         Task 1 (+ test steps in Task 3)
src/main/resources/META-INF/mods.toml, pack.mcmeta                  Task 1 (mods.toml deps final in Task 1)
tools/extract_animations.py, known_animations.json                  Task 2
tools/styles.json, generate.py, test_generate.py                    Task 2
src/main/resources/data/fiststyle/capabilities/weapons/{style_N.json, types/style_N.json}   Task 2 (generated)
src/main/resources/assets/fiststyle/{lang/en_us.json, models/item/style_N.json, textures/item/style_s.png}  Task 2 (generated)
src/main/java/dev/fiststyle/StyleStats.java                         Task 2 (generated)
src/main/java/dev/fiststyle/{FistStyleMod,StyleItem}.java           Task 1 (empty mod) / Task 3
tools/verify_jar.py, README.md                                      Task 3
src/main/java/dev/fiststyle/LaunchHandler.java                      Task 4
```

---

### Task 1: Gradle scaffold, empty mod, CI build and Release `latest`

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, `.github/workflows/build.yml`, `src/main/resources/META-INF/mods.toml`, `src/main/resources/pack.mcmeta`, `src/main/java/dev/fiststyle/FistStyleMod.java`

**Interfaces:**
- Produces: `dev.fiststyle.FistStyleMod` with `public static final String MOD_ID = "fiststyle"`, annotated `@Mod(FistStyleMod.MOD_ID)`, public no-arg constructor. The jar `build/libs/fiststyle-1.0.0.jar`.

- [ ] **Step 1: Scaffold the Gradle project** following the official Forge 1.20.1-47.x MDK layout: plugin `net.minecraftforge.gradle` `[6.0.16,6.2)` resolved from `https://maven.minecraftforge.net/` in `pluginManagement`; `group 'dev.fiststyle'`, `version '1.0.0'`, `base.archivesName = 'fiststyle'`; Java toolchain 17; `minecraft { mappings channel: 'official', version: '1.20.1' }`; dependency `minecraft "net.minecraftforge:forge:1.20.1-47.3.0"`; `jar.finalizedBy('reobfJar')`; `processResources` expands `mods.toml`.
- [ ] **Step 2: Write `mods.toml`** with the Global Constraints dependency block (modLoader `javafml`, `loaderVersion="[47,)"`, license `All Rights Reserved`, `displayName="Style"`) and `pack.mcmeta` (`pack_format` 15).
- [ ] **Step 3: Write `FistStyleMod`** (empty body) and `.gitignore` (`build/`, `.gradle/`, `run/`, `__pycache__/`).
- [ ] **Step 4: Write `.github/workflows/build.yml`**: triggers `push` (all branches) and `workflow_dispatch`; `permissions: contents: write`; steps `actions/checkout@v4`, `actions/setup-java@v4` (temurin 17), `gradle/actions/setup-gradle@v4` (`gradle-version: 8.1.1`), `gradle build --no-daemon`, `actions/upload-artifact@v4` (`build/libs/*.jar`), then a release step with `GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}` running `gh release delete latest --cleanup-tag --yes || true` followed by `gh release create latest build/libs/*.jar --prerelease --target "$GITHUB_SHA" --title "latest build" --notes "Built from $GITHUB_SHA ($GITHUB_REF_NAME)"`.
- [ ] **Step 5: Commit and push** (`git push -u origin claude/fist-styles`).
- [ ] **Step 6: Verify the CI build**

Run: `gh run list --repo Thanpisitkub/style- --branch claude/fist-styles --limit 1 --json status,conclusion` until `status` is `completed` (poll every 30 s; on failure read `gh run view <id> --log-failed`, fix the Gradle/FG config, push again).
Expected: `"conclusion":"success"`, and `gh release view latest --repo Thanpisitkub/style- --json assets` lists `fiststyle-1.0.0.jar`.

---

### Task 2: Style table, generator, generated data and tests

**Files:**
- Create: `tools/extract_animations.py`, `tools/known_animations.json`, `tools/styles.json`, `tools/generate.py`, `tools/test_generate.py`, plus all generated resource files listed in File Structure.

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces:
  - `tools/styles.json`: `{"styles":[{"n":int,"damage":float,"speed":float,"idle":str,"combos":[str×8]}]}`; combo strings are `"<mod>:biped/…"`; `combos[3]` is the uppercut. Table values come from spec §7 (copy the 10 rows of combos from the spec; mod prefixes W→`wom`, C→`ef_cz`, K→`epicsanji`, paths from the spec's short names resolved against `known_animations.json`).
  - `tools/generate.py`: `load_styles(path: Path) -> list[dict]`, `build_type(style: dict, known: set[str]) -> dict`, `build_capability(style: dict) -> dict`, `build_item_model() -> dict`, `build_lang(styles: list[dict]) -> dict`, `build_texture() -> PIL.Image.Image`, `build_stats_java(styles: list[dict]) -> str`, `write_all(root: Path, styles: list[dict], known: set[str]) -> None`.
  - Generated `dev.fiststyle.StyleStats`: `public static final double[] DAMAGE`, `public static final double[] SPEED`, `public static final String[] UPPERCUT` (animation id strings), index = style number − 1.

- [ ] **Step 1: Write `tools/extract_animations.py <jar>… -o tools/known_animations.json`**: for each jar open with `zipfile`, collect strings matching `[a-z_0-9]+/[a-z_0-9/]+` from `.class` files, keep those for which `assets/<ns>/animmodels/animations/<path>.json` exists in the same jar (`ns` = the asset namespace dir), output a sorted JSON list of `"<ns>:<path>"`. Run it on the four attached jars (`/mnt/user-data/attachments/a3c82683-2137-57ea-869a-ee4e3ee3e358/*.jar`). Expected: file contains e.g. `wom:biped/skill/kick_1`, `ef_cz:biped/mk/kabal/kabal_auto1`, `epicsanji:biped/combat/sanji_auto_1`.
- [ ] **Step 2: Write `tools/styles.json`** from the spec §7 tables. Resolve each short name (`W:`→`wom`, `C:`→`ef_cz`, `K:`→`epicsanji`) to the one id in `known_animations.json` whose namespace matches the tag and whose last path segment equals the short name; stop and report if a name resolves to zero or more than one id.
- [ ] **Step 3: Write failing tests in `tools/test_generate.py`** (`unittest`, import `generate`):
  - `test_stats_in_range_and_ordered`: 10 styles; S1 == (4.0, 2.5), S10 == (8.0, 1.5); all damage in [4.0, 8.0], speed in [1.5, 2.5]; damage non-decreasing, speed non-increasing.
  - `test_combo_shape`: for every style `build_type(s, known)["combos"]["one_hand"] == s["combos"]`, `["two_hand"]` identical, `len == 8`, `["mount"] == ["epicfight:biped/combat/sword_mount_attack"]`.
  - `test_all_animation_ids_known`: every id in every combo is in `known_animations.json`.
  - `test_living_motion`: `build_type(s, known)["livingmotion_modifier"]["one_hand"]["idle"] == f"wom:biped/living/{s['idle']}_idle"`; for style 2 `"run"` and `"chase"` are not in that dict; for style 1 both are present.
  - `test_capability_type_matches_file`: `build_capability(s)["type"] == f"fiststyle:style_{n}"`, `attributes.common == {"armor_negation":0.0,"impact":round(0.5+0.15*(n-1),2),"max_strikes":1}`, and after `write_all` into a temp dir `data/fiststyle/capabilities/weapons/types/style_{n}.json` exists for every n.
  - `test_item_model_hides_held_item`: the four `display` keys in Global Constraints have `scale == [0,0,0]`; `textures.layer0 == "fiststyle:item/style_s"`; `parent == "item/handheld"`.
  - `test_texture`: size `(16,16)`, every pixel is `(0,0,0,255)` or `(255,255,255,255)`, ≥ 10 white pixels, pixel (0,0) black.
  - `test_lang`: `item.fiststyle.style_10 == "Style 10"`, `itemGroup.fiststyle == "Style"`.
  - `test_stats_java`: output contains `package dev.fiststyle;`, a `DAMAGE` array of 10 values, and `UPPERCUT` listing each style's `combos[3]`.
  - `test_write_all_idempotent`: two `write_all` runs into two temp dirs yield byte-identical trees.
- [ ] **Step 4: Run tests, expect failure**: `python3 -m unittest discover -s tools -p 'test_*.py' -v` → import error / failures.
- [ ] **Step 5: Implement `tools/generate.py`** to the signatures above. `build_type` mirrors `effs:hand_combat` key-for-key: `category "sword"`, hit/swing sounds `epicfight:entity.hit.blunt` / `epicfight:entity.weapon.whoosh_hard`, `styles` (case `epicfight:offhand_category` sword → `two_hand`, default `one_hand`), `offhand_item_compatible_predicate` (greatsword), `collider {number 3, center [0,0,-0.75], size [0.4,0.4,0.8]}`, `combos`, `innate_skills` both styles `epicfight:relentless_combo`, and `livingmotion_modifier` for both styles with `block "epicfight:biped/skill/guard_uchigatana"`, `idle`, `walk`, `run`, `chase`(=run) per the omission rule. The texture draws a centered 7×11 S glyph from a hard-coded bitmap on a black 16×16 image using Pillow (`pip install pillow --break-system-packages` if missing). JSON written with 2-space indent and trailing newline; Java written with a `// GENERATED by tools/generate.py — do not edit` header.
- [ ] **Step 6: Run tests, expect pass**: same command → `OK`.
- [ ] **Step 7: Generate and commit**: `python3 tools/generate.py` (writes into the repo tree), then `git add tools src && git commit -m "feat: style table, generator and generated Epic Fight data"` with the attribution lines.

---

### Task 3: Items, creative tab, jar verification, README

**Files:**
- Create: `src/main/java/dev/fiststyle/StyleItem.java`, `tools/verify_jar.py`, `README.md`
- Modify: `src/main/java/dev/fiststyle/FistStyleMod.java`, `.github/workflows/build.yml`

**Interfaces:**
- Consumes: `StyleStats.DAMAGE/SPEED/UPPERCUT` (Task 2), `FistStyleMod.MOD_ID` (Task 1).
- Produces: `StyleItem(int styleNumber)` with `public final int styleNumber`; `FistStyleMod.STYLE_ITEMS: List<RegistryObject<Item>>` (10 entries, index = n − 1); `FistStyleMod.ITEMS`, `FistStyleMod.TABS` (`DeferredRegister`s).

- [ ] **Step 1: Write failing `tools/verify_jar.py <jar>`** (exit 1 with a message on the first failed check): inside the jar assert `META-INF/mods.toml` contains `modId="fiststyle"` and mandatory dependency ids `epicfight`, `wom`, `ef_cz`, `epicsanji`; for n in 1..10 the entries `data/fiststyle/capabilities/weapons/style_n.json`, `.../types/style_n.json`, `assets/fiststyle/models/item/style_n.json` exist and parse as JSON; `assets/fiststyle/textures/item/style_s.png` starts with the PNG signature; `assets/fiststyle/lang/en_us.json` has `item.fiststyle.style_1`…`style_10` and `itemGroup.fiststyle`; classes `dev/fiststyle/FistStyleMod.class`, `StyleItem.class`, `StyleStats.class` exist. Run it against the Task 1 jar (download via `gh release download latest -p '*.jar'`) → expect failure on missing items/classes.
- [ ] **Step 2: Implement `StyleItem extends Item`**: constructor `StyleItem(int styleNumber)` calls `super(new Item.Properties().stacksTo(1))`; builds one immutable multimap with `Attributes.ATTACK_DAMAGE` = `StyleStats.DAMAGE[n-1] - 1.0` (UUID `Item.BASE_ATTACK_DAMAGE_UUID`, name "Weapon modifier", `Operation.ADDITION`) and `Attributes.ATTACK_SPEED` = `StyleStats.SPEED[n-1] - 4.0` (UUID `Item.BASE_ATTACK_SPEED_UUID`); `getDefaultAttributeModifiers(EquipmentSlot slot)` returns that map for `MAINHAND`, otherwise `super`.
- [ ] **Step 3: Implement `FistStyleMod`**: `ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)`, `TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)`; register `style_1`…`style_10` into `STYLE_ITEMS`; one tab `"style"` with `.title(Component.translatable("itemGroup.fiststyle"))`, icon = Style 1, `displayItems` adds all ten; constructor registers both on the mod event bus.
- [ ] **Step 4: Update the workflow**: before `gradle build` add `pip install pillow` and `python3 -m unittest discover -s tools -p 'test_*.py'` and `python3 tools/generate.py && git diff --exit-code -- src/main/java src/main/resources/data src/main/resources/assets/fiststyle/lang src/main/resources/assets/fiststyle/models` (fails if committed generated text files are stale; the PNG is excluded because Pillow versions encode PNGs differently); after the build add `python3 tools/verify_jar.py build/libs/fiststyle-1.0.0.jar`.
- [ ] **Step 5: Write `README.md`** (Thai): what the mod is, required mods, how to download the jar from Releases on a phone, the 10-style damage/speed table, how to swap a combo animation (edit `tools/styles.json`, run `python3 tools/generate.py`, push), troubleshooting (send the game log lines).
- [ ] **Step 6: Commit, push, verify CI**: same `gh run list` check as Task 1 Step 6.
Expected: `"conclusion":"success"` including the unittest and `verify_jar.py` steps; on failure fix from `gh run view <id> --log-failed`.

---

### Task 4: Launch hook (combo 4 uppercut)

**Files:**
- Create: `src/main/java/dev/fiststyle/LaunchHandler.java`
- Modify: `src/main/java/dev/fiststyle/FistStyleMod.java` (register the handler on `MinecraftForge.EVENT_BUS` in the constructor), `tools/verify_jar.py` (also require `dev/fiststyle/LaunchHandler.class`)

**Interfaces:**
- Consumes: `StyleItem.styleNumber`, `StyleStats.UPPERCUT` (Tasks 2–3).
- Produces: `public final class LaunchHandler` with `@SubscribeEvent public void onHurt(LivingHurtEvent e)`, `@SubscribeEvent public void onTick(LivingEvent.LivingTickEvent e)`, `static String animationIdOf(DamageSource source)` (returns `null` when unresolved), constants `LAUNCH_VELOCITY = 0.9`, `SLOW_FALL_TICKS = 25`, `FLAG = "fiststyle_launch"`.

- [ ] **Step 1: Extend `tools/verify_jar.py`** to require the class, run it on the Task 3 jar → fails.
- [ ] **Step 2: Implement `animationIdOf`** with reflection only: find a public no-arg `getAnimation` on `source.getClass()`, call it, then read the id via the first present no-arg method of `registryName`, `getRegistryName`, else `toString()`. Cache the resolved `Method` per class in a `ConcurrentHashMap`. Any `Throwable` is caught: log one `LOGGER.warn` (guard with an `AtomicBoolean`) saying the launch hook is disabled, return `null`.
- [ ] **Step 3: Implement `onHurt`** (server side only): if the direct/true attacker is a `ServerPlayer` whose main-hand stack item is a `StyleItem` and `animationIdOf(source)` equals `StyleStats.UPPERCUT[styleNumber - 1]`, set `target.getPersistentData().putBoolean(FLAG, true)`. **Implement `onTick`** (server side only): if the flag is set, remove it, then `target.setDeltaMovement(x, LAUNCH_VELOCITY, z)` (keep current x and z), `target.hurtMarked = true`, and add `MobEffects.SLOW_FALLING` for `SLOW_FALL_TICKS` ticks. The one-tick delay is deliberate: vanilla knockback runs after the hurt event and would overwrite an immediate launch.
- [ ] **Step 4: Register the handler** in `FistStyleMod`'s constructor.
- [ ] **Step 5: Commit, push, verify CI** as in Task 3 Step 6; then confirm `gh release view latest` shows the new jar.
- [ ] **Step 6: Hand over for in-game testing**: send the user the Release link and this checklist (also in `README.md`): (a) all 10 items appear in the "Style" tab with the S icon and hold invisibly; (b) tooltip shows the Style N damage/speed; (c) each style plays punch 1–6, sprint punch, jump punch; (d) hit 4 launches the target and 5–6 connect; (e) log lines to send if anything fails.

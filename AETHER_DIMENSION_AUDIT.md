# Aether Dimension Integration Audit

Branch: `aether-integration`
Audited against the current deobfuscated EaglercraftX 1.8 source.

## Verified lifecycle constraints

- `WorldProvider.getProviderForDimension(int)` currently constructs providers only for dimensions -1, 0, and 1. Dimension 2 must get a dedicated provider.
- `MinecraftServer.loadAllWorlds` currently allocates exactly three `WorldServer` slots and maps them by index: 0 -> Overworld (0), 1 -> Nether (-1), 2 -> End (1).
- `MinecraftServer.worldServerForDimension` independently hard-codes the same three-way mapping. Both allocation and mapping must change together; appending a fourth slot without updating this mapping would route dimension 2 to Overworld.
- The server's world tick, save, difficulty, shutdown, and entity lookup paths iterate over `worldServers`. These loops are length-driven, but the shutdown flush currently dereferences every slot without a null check. Initialization must populate every configured slot before shutdown can occur.
- `ServerConfigurationManager.setPlayerManager` uses slot 0 for the shared player save manager and world-border listener. It does not require a fixed array length, but its Overworld assumptions must remain intact.
- `WorldServerMulti` accepts the dimension ID and delegates shared map storage/scoreboard state to the Overworld. Its provider-specific storage suffix must be unique (`_aether`) to avoid collisions with Nether/End data.
- `EntityPlayerMP.travelToDimension` delegates actual transfer to `ServerConfigurationManager.transferPlayerToDimension`.
- `ServerConfigurationManager.transferPlayerToDimension` resolves source and destination through `worldServerForDimension`, then calls `transferEntityToWorld`. Aether travel therefore depends on correct server mapping and a safe destination spawn.
- The existing `Teleporter` is Nether-portal-specific: it searches/creates vanilla portal blocks and has special End behavior. It must not be reused as the Aether portal implementation.
- `WorldProviderEnd` is a useful provider reference for sky-world behavior, but its `ChunkProviderEnd` creates End terrain and must not be used as the Aether generator.

## Required implementation order

1. Add a dedicated Aether provider and a genuinely Aether-specific chunk generator before exposing dimension 2.
2. Add dimension 2 to world creation with an explicit index-to-dimension mapping, then update `worldServerForDimension`.
3. Confirm unique provider suffix and save isolation for the Aether world.
4. Add a dedicated portal block/activation/travel path; leave vanilla Nether portal behavior unchanged.
5. Add client sky/fog/rendering, localization, textures, and audio only after server-side world creation and transfer are stable.
6. Run the offline JavaScript build and inspect the produced artifact; then perform an actual browser gameplay smoke test.

## Current status

Native Aether dimension, generator, and portal foundations are implemented on `aether-integration`. Selected outer islands now also receive deterministic stone-brick ruin markers with glowstone caps and a central Aether Crystal. The crystal is a registered, light-emitting block with a quartz-based vanilla texture and a crafting recipe using four quartz. This remains an incomplete gameplay implementation: runtime travel, rendering, and saving have not been browser-tested.

## Additional transfer-path finding

- `ServerConfigurationManager.transferEntityToWorld` contains vanilla coordinate scaling rules for Nether/Overworld and otherwise uses the destination provider's spawn coordinate. For destinations other than End (dimension 1), it then calls `parWorldServer2.getDefaultTeleporter().placeInPortal(...)`. Aether must therefore supply a safe provider spawn coordinate and a dedicated teleporter strategy (or a carefully isolated transfer override) so the generic Nether teleporter cannot create/search vanilla Nether portals in the Aether world.
- The transfer routine's coordinate scaling is dimension-specific and only models dimension -1 versus dimension 0. Aether travel should preserve or deliberately map coordinates rather than accidentally inheriting the Nether's 8:1 scaling behavior.
- The current code's fallback branch treats any non-1 destination similarly for spawn selection, so dimension 2 cannot be considered complete merely by adding it to `worldServerForDimension`.


## Implementation checkpoint — 2026-11-20

The following native source changes are now present on `aether-integration`:

- Dimension ID 2 is registered through `WorldProviderAether`.
- Server world arrays and dimension lookup include a fourth world slot for Aether while preserving the vanilla slot mapping for Overworld, Nether, and End.
- `ChunkProviderAether` generates deterministic floating islands around a central spawn island, with sky biome data. Selected outer islands now include small deterministic stone-brick ruin structures with glowstone caps and a central Aether Crystal; the central spawn island is kept clear.
- Added registered `aether_crystal` block ID 199, with light emission and a vanilla quartz texture to avoid requiring external assets.
- Added a shaped recipe for Aether Crystal using four quartz.
- A dedicated `aether_portal` block is registered separately from the vanilla Nether portal.
- A glowstone frame (4 blocks wide by 5 high, with a 2 by 3 opening) activates when water is placed in the opening. The interior becomes Aether portal blocks.
- Entity portal handling routes Aether portal collisions between dimension 0 and dimension 2.
- Aether transfers use the destination spawn and deliberately bypass the vanilla Nether teleporter and its 8:1 coordinate scaling.
- The Aether provider uses the sky biome, a pale-blue fog, a low cloud height, a unique `_aether` storage suffix, and an Aether-specific spawn coordinate. Its surface-render flag is enabled so the client draws the normal sky, sun, moon, and stars while the dimension remains non-respawnable.

### Build verification

- Latest offline build including both the altar structures and cool-toned portal particles: GitHub Actions run `44792582232`, commit `30e3acf563cb860a1a9656c43600475f6ec6b9c2`, result `completed / success`. Artifact ID `14656748582`, 37,285,633 bytes.
- The preceding altar-only build also completed successfully: run `44792487638`, artifact ID `14656728681`.
- The latest artifact ZIP was downloaded and passed `unzip -t` integrity validation (87 entries). `classes.js` contains the Aether provider/generator/portal, crystal, particle, and ambience markers and passed `node --check`.
- This verifies compilation and packaging only; it does not replace the still-pending in-browser gameplay test.
- Latest offline JavaScript build: GitHub Actions run `44785371957`, commit `44ee0d9a15b0e069c7f0936ac55b52d6c90604ef`, result `completed / success`. It includes the Aether Crystal inventory registration and the fix preventing Endermen from spawning in Aether. Resource repacking was explicitly re-triggered so the custom blockstate/model/item model and English localization are present inside the offline EPK.
- Previous verified build: GitHub Actions run `44774687307` completed successfully with artifact ID `14648562483`.
- GitHub Actions run `44774588795` completed successfully for the corrected Aether Crystal Java implementation. The full build including the bundled blockstate and model resources is run `44774687307` for commit `93738189b6ba968e432cca043388fbaf72bc9899`; result: `completed / success`.
- Latest artifact: `EaglercraftX-1.8-Offline`, artifact ID `14653791967`, 37,260,230 bytes. Downloaded ZIP passed integrity inspection (87 entries); the embedded `assets.epk` contains the Aether Crystal blockstate, block model, item model, and English localization. `classes.js` contains the Aether provider, generator, portal, and crystal identifiers and passed a JavaScript syntax check. The crystal block is registered as an inventory ItemBlock.
- Previous verified build: GitHub Actions run 22: `44766437918`
- Result: `completed / success`
- Task: `:target_teavm_javascript:makeMainOfflineDownload`
- Artifact: `EaglercraftX-1.8-Offline`, ID `14646545726`, 37,283,077 bytes.
- The downloaded ZIP passed archive integrity inspection; `classes.js` contains the Aether provider, generator, and portal identifiers.

### Remaining work / not yet verified

- Browser gameplay smoke test was attempted with local headless Chromium, but the environment blocked navigation with `ERR_BLOCKED_BY_ADMINISTRATOR`; therefore no in-game behavior is claimed as verified. A successful Gradle/TeaVM build proves compilation and packaging, not that portal activation, chunk generation, rendering, travel, and saving all behave correctly at runtime.
- The implementation is a native Aether foundation, not a full port of the original Aether mod.
- Still missing: additional Aether-specific blocks/items, full structures/dungeons, Aether-specific creatures, dedicated Aether sound assets, and a dedicated blue portal texture. The portal now emits cool-toned spell particles and a quieter, higher-pitched portal ambience using existing sounds. The Aether sky now uses the standard surface celestial renderer with its pale-blue fog; a custom skybox remains future work.
- To return from Aether with the current implementation, build another glowstone-and-water portal in the Aether dimension.
- Keep PR #1 as Draft until those runtime checks and remaining content are complete.

## Review pass — 2026-11-20 (latest build)

- Reviewed the built offline artifact from workflow run `44795923097`, commit `f46bd9120c03d686a574ca8697c0df004ea9130b`, result `completed / success`.
- Artifact `EaglercraftX-1.8-Offline`, ID `14657683521`, size 37,260,591 bytes. ZIP integrity check passed (87 files).
- The artifact contains the full offline English and International Eaglercraft HTML clients. The English offline HTML embeds its compiled client code and EPK asset data directly; it is not merely a prototype page.
- `classes.js` passed `node --check`; the compiled output contains the Aether provider, floating-island generator, portal, and crystal identifiers.
- Decompressed the EPK v2 resource payload and confirmed the Aether Crystal blockstate, block model, item model, and English localization are packaged.
- Review found the earlier ruin platform inherited per-column island height variation. The generator now flattens a 7x7 footprint before placing the level 5x5 altar and corner pillars. This correction is included in the successful build above.
- Attempted a local Chromium smoke test, but Chromium did not complete even a blank-page headless launch in this environment. Therefore actual menu startup, world creation, portal activation/travel, and save/reload remain unverified.

### Scope clarification

This is a modified native Eaglercraft 1.8.8 offline client with an Aether dimension foundation, not a complete port of the original Aether mod. Still outstanding: Aether-specific creatures, full dungeons/interiors/loot, additional Aether blocks/items, dedicated Aether sound assets, and a dedicated blue portal texture/custom skybox. The current portal uses blue-tinted particles and an existing portal sound; the crystal uses the vanilla quartz texture. The PR remains Draft pending gameplay validation and content completion.


## Latest verification pass — 2026-11-20

- Reviewed source on branch `aether-integration`, commit `73add4a241bd526f22eb3a0c9b6c5e970c8385e0`.
- Found and corrected a spawn-height issue: the central island surface generated between Y=89 and Y=91, while the previous spawn Y=90 could intersect the surface. `WorldProviderAether.getSpawnCoordinate()` now uses Y=93.
- A deterministic calculation check covered 5,003 representative world seeds: central-island top remained in the expected 89–91 range, and Y=93 stayed above it. A focused portal-offset check confirmed that the activation search covers all six cells of the 2×3 opening.
- GitHub Actions run `44797264427` completed successfully for this exact commit. Build, offline-output verification, and artifact upload steps all passed.
- Artifact `EaglercraftX-1.8-Offline`, ID `14659251985`, size 37,260,595 bytes. SHA-256: `18437415668b151f276dec2526a7e564f04ac42913e1cb99162d64ab84ba286a`.
- Downloaded artifact ZIP passed integrity validation (87 entries); the SHA-256 matched GitHub's artifact digest. Both offline HTML clients are present. `classes.js` passed `node --check` and contains Aether dimension/generator/portal/crystal markers. The packaged EPK v2 file is present.
- A local Chromium headless attempt against the full offline HTML did not complete before the environment timeout; actual menu startup and in-game portal/travel/save-reload remain unverified.
- CodeRabbit CLI was absent; its official installer could not be reached because DNS resolution for `cli.coderabbit.ai` failed. No CodeRabbit review result is claimed. CircleCI CLI/integration was also unavailable in this environment; GitHub Actions provided the successful CI build instead.
- The PR currently has no submitted review entries and remains Draft.

### Current scope
This is a modified native Eaglercraft 1.8.8 offline client with an Aether dimension foundation, not a complete port of the original Aether mod. Full dungeons/interiors/loot, Aether-specific creatures, additional Aether items/blocks, dedicated Aether audio, a unique blue portal texture, and a custom skybox remain unfinished. The current portal has cool-toned particles but still uses the existing portal texture and sound.

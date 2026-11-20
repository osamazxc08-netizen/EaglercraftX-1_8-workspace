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

- A fresh offline JavaScript build is running for the Aether sky-rendering fix.
- Previous verified build: GitHub Actions run `44774687307` completed successfully with artifact ID `14648562483`.
- GitHub Actions run `44774588795` completed successfully for the corrected Aether Crystal Java implementation. The full build including the bundled blockstate and model resources is run `44774687307` for commit `93738189b6ba968e432cca043388fbaf72bc9899`; result: `completed / success`.
- Latest artifact: `EaglercraftX-1.8-Offline`, artifact ID `14648562483`, 37,238,217 bytes. Downloaded ZIP passed integrity inspection (87 entries); `classes.js` contains the Aether provider, generator, portal, and crystal identifiers.
- Previous verified build: GitHub Actions run 22: `44766437918`
- Result: `completed / success`
- Task: `:target_teavm_javascript:makeMainOfflineDownload`
- Artifact: `EaglercraftX-1.8-Offline`, ID `14646545726`, 37,283,077 bytes.
- The downloaded ZIP passed archive integrity inspection; `classes.js` contains the Aether provider, generator, and portal identifiers.

### Remaining work / not yet verified

- No browser gameplay smoke test has been performed. A successful Gradle/TeaVM build proves compilation and packaging, not that portal activation, chunk generation, rendering, travel, and saving all behave correctly at runtime.
- The implementation is a native Aether foundation, not a full port of the original Aether mod.
- Still missing: additional Aether-specific blocks/items, full structures/dungeons, creatures, sound set, and a dedicated blue portal texture/particles. The Aether sky now uses the standard surface celestial renderer with its pale-blue fog; a custom skybox remains future work.
- To return from Aether with the current implementation, build another glowstone-and-water portal in the Aether dimension.
- Keep PR #1 as Draft until those runtime checks and remaining content are complete.

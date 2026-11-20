# Aether Integration Plan (EaglercraftX 1.8.8)

## Goal
Integrate an offline-playable Aether-style dimension into the existing EaglercraftX 1.8.8 client/server codebase. The Overworld remains the normal starting dimension. The Aether is entered only after the player constructs and activates the custom portal.

This is an implementation plan, not a claim that the dimension or portal is already playable.

## Baseline verified in source
- `WorldProvider.getProviderForDimension(int)` currently maps only -1 (Nether), 0 (Overworld), and 1 (End).
- `MinecraftServer` allocates exactly three `worldServers` and a parallel `timeOfLastDimensionTick` array.
- `MinecraftServer.worldServerForDimension(int)` currently maps the three vanilla dimension IDs by array position.
- Server world creation, initialization, save, shutdown, ticking, player-manager setup, and status reporting iterate over `worldServers`.
- `WorldProviderEnd` demonstrates the provider contract; `ChunkProviderEnd` demonstrates a dedicated generator.
- `EntityPlayerMP.travelToDimension` and `Teleporter` contain vanilla dimension-transition assumptions, including special handling for the End.
- `BlockPortal` is the Nether portal and should not be repurposed; Aether portal validation/activation must be separate.

## Architecture decisions
- Reserve dimension ID **2** for Aether; preserve vanilla IDs -1, 0, and 1.
- Keep the existing offline Eaglercraft integrated-server architecture. No external server is required.
- Give Aether its own provider, chunk generator, and dimension save identity.
- Implement portal detection/activation separately from Nether portal behavior. The requested frame/activation design is a custom Aether mechanic and must not silently change vanilla Nether portals.
- Keep the Overworld as the initial spawn dimension.
- Prioritize deterministic, seed-based island generation and modest structure density to avoid excessive memory/CPU use on tablets.

## Required implementation phases

### Phase 1 — Dimension lifecycle (blocking foundation)
1. Add `WorldProviderAether` with dimension ID 2, sky/fog/cloud behavior, Aether biome manager, and stable save suffix.
2. Expand server dimension storage without relying on dimension ID as an array index.
3. Update world creation and initialization, player manager registration, world ticking, save/shutdown, status reporting, and `worldServerForDimension(2)`.
4. Audit every `worldServers[index]` access and every assumption that there are exactly three dimensions.
5. Confirm world save isolation and reload behavior before adding portal travel.

### Phase 2 — World generation
1. Add an Aether chunk generator that creates floating islands from the world seed.
2. Add an Aether biome with safe, non-hostile initial exploration content.
3. Add a small set of structures and resource blocks/items, with bounded generation cost.
4. Add deterministic tests/checks for chunk repeatability and chunk boundaries.

### Phase 3 — Portal and travel
1. Add a dedicated portal block/validator/activation item or fluid mechanic, preserving the requested player-built frame.
2. Route entity travel through the server's dimension lookup; define safe arrival coordinates and return behavior.
3. Handle portal cooldown, player state, world loading, and save persistence.
4. Test entering/leaving repeatedly and reloading a world while in either dimension.

### Phase 4 — Client presentation and content
1. Add textures, block/item registration, localization, ambient audio, particles, sky/fog rendering, and recipe-book entries.
2. Ensure first/third-person camera modes remain vanilla-compatible.
3. Keep the feature offline and package all required assets into the offline build.

### Phase 5 — Validation and release
1. Run the existing offline JavaScript build workflow.
2. Add focused tests for provider mapping, server world lookup, dimension ticking/saving, portal validation, and deterministic generation where the project test setup permits.
3. Download and inspect the produced artifact; separately smoke-test the HTML client in a supported browser.
4. Do not describe a successful compile as proof of gameplay correctness.

## Immediate risks / open technical checks
- Dimension lifecycle is spread across `MinecraftServer`, `ServerConfigurationManager`, `WorldServer`, player transfer, save handlers, and Eaglercraft integrated-server code. All call sites must be audited before changing the world array.
- The client and integrated server share deobfuscated Minecraft code, but renderer, packet, and asset registration paths need separate inspection.
- This workspace is a vanilla EaglercraftX base; it does not contain the original Forge Aether mod implementation or its assets. The feature must be implemented as a native Eaglercraft port, not described as a direct Forge-mod import.
- Tablet performance and browser storage limits require conservative generation and asset budgets.

## Acceptance criteria
- Existing Overworld/Nether/End behavior remains unchanged.
- Aether dimension ID 2 is created, ticked, saved, and loaded independently.
- Player-built Aether portal activates only when the specified structure is valid.
- Player can travel both ways; world state persists after closing/reopening the offline client.
- Offline build includes all required code and assets and passes a browser smoke test.


## Progress update — 2026-11-20

### Implemented on the integration branch
- Native dimension provider and deterministic floating-island chunk generator.
- Player-built glowstone/water portal and dimension travel routing.
- Simple deterministic stone-brick ruin markers on selected outer islands.
- Bundled Aether Crystal blockstate, block model, and item model resources.
- Aether Crystal inventory registration, making the existing quartz-based crafting recipe usable.
- Dedicated Aether sky presentation changes are present in the branch.
- Aether mob spawning is disabled until Aether-specific creatures are implemented; this avoids inheriting Enderman spawns from the reused vanilla End biome.

### Current validation boundary
- Offline TeaVM builds have passed for earlier commits on this branch.
- A fresh build is running for the crystal item registration and spawn-safety changes.
- No actual browser gameplay test has been completed. Portal activation/travel, chunk rendering, and save/reload still require runtime verification.
- This is a native Aether-inspired implementation, not a complete port of the original Aether mod.

### Remaining work
- Implement Aether-specific peaceful/ambient creatures and safe spawn rules.
- Add dedicated Aether portal texture and particles, plus ambient audio.
- Expand the simple ruin markers into full structures with interiors and loot.
- Verify portal travel in both directions, save isolation, and save/reload in browser.

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

This commit is an audit/documentation checkpoint only. It does not add a playable Aether dimension, portal, generator, mobs, or assets. The CI run associated with the current branch was still in progress at the time of this audit; a successful build must be confirmed separately.

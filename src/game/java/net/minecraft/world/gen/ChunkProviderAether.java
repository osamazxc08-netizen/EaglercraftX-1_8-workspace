package net.minecraft.world.gen;

import java.util.List;
import java.util.Collections;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Lightweight deterministic floating-island generator for the native Aether
 * dimension. Islands are generated from world coordinates, so neighboring
 * chunks join without seams and the central island always contains the spawn.
 */
public class ChunkProviderAether implements IChunkProvider {

	private static final int ISLAND_SPACING = 240;
	private final World world;

	public ChunkProviderAether(World worldIn) {
		this.world = worldIn;
	}

	private static int hash(int x, int z, long seed) {
		long value = seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
		value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
		value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
		value ^= value >>> 31;
		return (int) (value & 0x7fffffff);
	}

	private void generateColumn(ChunkPrimer primer, int worldX, int worldZ, int localX, int localZ) {
		int cellX = Math.floorDiv(worldX, ISLAND_SPACING);
		int cellZ = Math.floorDiv(worldZ, ISLAND_SPACING);
		double bestDistance = Double.MAX_VALUE;
		int islandTop = 88;
		int islandThickness = 20;
		int bestCenterX = 0;
		int bestCenterZ = 0;
		int bestHash = 0;

		for (int cx = cellX - 1; cx <= cellX + 1; ++cx) {
			for (int cz = cellZ - 1; cz <= cellZ + 1; ++cz) {
				int h = hash(cx, cz, this.world.getSeed());
				int centerX = cx * ISLAND_SPACING + (cx == 0 && cz == 0 ? 0 : (h % 57) - 28);
				int centerZ = cz * ISLAND_SPACING + (cx == 0 && cz == 0 ? 0 : ((h >>> 7) % 57) - 28);
				int radiusX = cx == 0 && cz == 0 ? 88 : 48 + (h % 27);
				int radiusZ = cx == 0 && cz == 0 ? 88 : 45 + ((h >>> 5) % 29);
				double dx = (double) (worldX - centerX) / (double) radiusX;
				double dz = (double) (worldZ - centerZ) / (double) radiusZ;
				double distance = dx * dx + dz * dz;
				if (distance < bestDistance) {
					bestDistance = distance;
					bestCenterX = centerX;
					bestCenterZ = centerZ;
					bestHash = h;
					islandTop = 86 + (int) (Math.sin(worldX * 0.045D) * 3.0D
							+ Math.cos(worldZ * 0.041D) * 3.0D) + ((h >>> 12) % 3);
					islandThickness = 9 + (int) ((1.0D - Math.min(distance, 1.0D)) * 18.0D);
				}
			}
		}

		if (bestDistance > 1.0D) {
			return;
		}

		int bottom = islandTop - islandThickness;
		for (int y = Math.max(1, bottom); y <= islandTop; ++y) {
			if (y == islandTop) {
				primer.setBlockState(localX, y, localZ, Blocks.grass.getDefaultState());
			} else if (y >= islandTop - 3) {
				primer.setBlockState(localX, y, localZ, Blocks.dirt.getDefaultState());
			} else {
				primer.setBlockState(localX, y, localZ, Blocks.stone.getDefaultState());
			}
		}

		// Small deterministic stone ruins appear on selected outer islands.
		// The central spawn island is intentionally kept clear.
		if ((bestHash % 4) == 0 && (bestCenterX != 0 || bestCenterZ != 0)) {
			int dx = worldX - bestCenterX;
			int dz = worldZ - bestCenterZ;
			boolean corner = (Math.abs(dx) == 3 && Math.abs(dz) == 3);
			if (dx == 0 && dz == 0) {
				primer.setBlockState(localX, islandTop + 1, localZ, Blocks.aether_crystal.getDefaultState());
			}
			if (corner) {
				primer.setBlockState(localX, islandTop + 1, localZ, Blocks.stonebrick.getDefaultState());
				primer.setBlockState(localX, islandTop + 2, localZ, Blocks.stonebrick.getDefaultState());
				primer.setBlockState(localX, islandTop + 3, localZ, Blocks.glowstone.getDefaultState());
			}
			if ((dx == 0 && Math.abs(dz) <= 3) || (dz == 0 && Math.abs(dx) <= 3)) {
				if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
					primer.setBlockState(localX, islandTop + 1, localZ, Blocks.stonebrick.getDefaultState());
				}
			}
		}
	}

	@Override
	public Chunk provideChunk(int chunkX, int chunkZ) {
		ChunkPrimer primer = new ChunkPrimer();
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				this.generateColumn(primer, chunkX * 16 + x, chunkZ * 16 + z, x, z);
			}
		}
		Chunk chunk = new Chunk(this.world, primer, chunkX, chunkZ);
		byte[] biomes = chunk.getBiomeArray();
		for (int i = 0; i < biomes.length; ++i) {
			biomes[i] = (byte) BiomeGenBase.sky.biomeID;
		}
		chunk.generateSkylightMap();
		return chunk;
	}

	@Override
	public Chunk provideChunk(BlockPos pos) {
		return this.provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
	}

	@Override
	public Chunk getLoadedChunk(int x, int z) {
		return this.provideChunk(x, z);
	}

	@Override
	public boolean chunkExists(int x, int z) {
		return true;
	}

	@Override
	public void populate(IChunkProvider provider, int x, int z) {
		// Aether islands are fully defined during chunk creation.
	}

	@Override
	public boolean func_177460_a(IChunkProvider provider, Chunk chunk, int x, int z) {
		return false;
	}

	@Override
	public boolean saveChunks(boolean all, IProgressUpdate progress) {
		return true;
	}

	@Override
	public void saveExtraData() {
	}

	@Override
	public boolean unloadQueuedChunks() {
		return false;
	}

	@Override
	public boolean canSave() {
		return true;
	}

	@Override
	public String makeString() {
		return "AetherFloatingIslands";
	}

	@Override
	public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
		// The vanilla sky biome is also used by The End and includes Endermen.
		// Keep the Aether calm until its own creature roster is implemented.
		return Collections.emptyList();
	}

	@Override
	public BlockPos getStrongholdGen(World worldIn, String structureName, BlockPos position) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return 0;
	}

	@Override
	public void recreateStructures(Chunk chunk, int x, int z) {
	}
}

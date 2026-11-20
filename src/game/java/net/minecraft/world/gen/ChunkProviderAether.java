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
 * dimension. Candidate island data is calculated once per chunk rather than
 * once per block column to reduce CPU work on low-end devices.
 */
public class ChunkProviderAether implements IChunkProvider {

	private static final int ISLAND_SPACING = 240;
	private static final int CHUNK_SIZE = 16;
	private static final IslandData[] NO_ISLANDS = new IslandData[0];

	private static final class IslandData {
		final int centerX;
		final int centerZ;
		final int radiusX;
		final int radiusZ;
		final int hash;
		final boolean central;

		IslandData(int centerX, int centerZ, int radiusX, int radiusZ, int hash, boolean central) {
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.radiusX = radiusX;
			this.radiusZ = radiusZ;
			this.hash = hash;
			this.central = central;
		}
	}

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

	private IslandData[] prepareIslands(int chunkX, int chunkZ) {
		// A compact Aether archipelago: one central spawn island and six outer islands.
		// The fixed layout keeps exploration bounded and makes the world lighter to generate.
		final int[] offsetsX = { 0, 240, 120, -120, -240, -120, 120 };
		final int[] offsetsZ = { 0, 0, 208, 208, 0, -208, -208 };
		IslandData[] islands = new IslandData[7];
		long seed = this.world.getSeed();

		for (int i = 0; i < islands.length; ++i) {
			int h = hash(i, i * 31, seed);
			boolean central = i == 0;
			int jitterX = central ? 0 : (h % 17) - 8;
			int jitterZ = central ? 0 : ((h >>> 7) % 17) - 8;
			int radiusX = central ? 88 : 54 + (h % 12);
			int radiusZ = central ? 88 : 50 + ((h >>> 5) % 14);
			islands[i] = new IslandData(offsetsX[i] + jitterX, offsetsZ[i] + jitterZ,
					radiusX, radiusZ, h, central);
		}
		return islands;
	}

	/**
	 * Small integer triangle wave: visually smooth enough for island contours,
	 * but avoids calling sin/cos hundreds of times for every generated chunk.
	 */
	private static int heightWave(int coordinate, int period, int amplitude) {
		int phase = Math.floorMod(coordinate, period);
		int half = period >> 1;
		int triangle = phase < half ? phase : period - phase;
		return (triangle * amplitude * 2 / half) - amplitude;
	}

	private void generateColumn(ChunkPrimer primer, int worldX, int worldZ, int localX, int localZ,
			IslandData[] islands) {
		int bestDistance = Integer.MAX_VALUE;
		IslandData bestIsland = null;
		int bestHeight = 88;
		int bestThickness = 20;

		for (int i = 0; i < islands.length; ++i) {
			IslandData island = islands[i];
			int dx = worldX - island.centerX;
			int dz = worldZ - island.centerZ;
			int nx = dx * 256 / island.radiusX;
			int nz = dz * 256 / island.radiusZ;
			int distance = nx * nx + nz * nz;
			if (distance < bestDistance) {
				bestDistance = distance;
				bestIsland = island;
				bestHeight = 86 + heightWave(worldX, 140, 3) + heightWave(worldZ, 128, 3)
						+ ((island.hash >>> 12) % 3);
				bestThickness = 9 + (int) ((1.0D - Math.min(distance / 65536.0D, 1.0D)) * 18.0D);
			}
		}

		if (bestIsland == null || bestDistance > 65536) {
			return;
		}

		int bottom = bestHeight - bestThickness;
		for (int y = Math.max(1, bottom); y <= bestHeight; ++y) {
			if (y == bestHeight) {
				primer.setBlockState(localX, y, localZ, Blocks.grass.getDefaultState());
			} else if (y >= bestHeight - 3) {
				primer.setBlockState(localX, y, localZ, Blocks.dirt.getDefaultState());
			} else {
				primer.setBlockState(localX, y, localZ, Blocks.stone.getDefaultState());
			}
		}

		if ((bestIsland.hash % 4) == 0 && !bestIsland.central) {
			int dx = worldX - bestIsland.centerX;
			int dz = worldZ - bestIsland.centerZ;
			int ax = Math.abs(dx);
			int az = Math.abs(dz);
			int ruinTop = 89 + ((bestIsland.hash >>> 12) % 3);

			if (ax <= 3 && az <= 3) {
				for (int y = Math.max(1, bottom); y <= ruinTop; ++y) {
					if (y == ruinTop) {
						primer.setBlockState(localX, y, localZ, Blocks.grass.getDefaultState());
					} else if (y >= ruinTop - 3) {
						primer.setBlockState(localX, y, localZ, Blocks.dirt.getDefaultState());
					} else {
						primer.setBlockState(localX, y, localZ, Blocks.stone.getDefaultState());
					}
				}
				for (int y = ruinTop + 1; y <= ruinTop + 4; ++y) {
					primer.setBlockState(localX, y, localZ, Blocks.air.getDefaultState());
				}
			}

			if (ax <= 2 && az <= 2) {
				primer.setBlockState(localX, ruinTop + 1, localZ, Blocks.stonebrick.getDefaultState());
			}
			if (dx == 0 && dz == 0) {
				primer.setBlockState(localX, ruinTop + 2, localZ, Blocks.aether_crystal.getDefaultState());
			}
			if (ax == 3 && az == 3) {
				primer.setBlockState(localX, ruinTop + 1, localZ, Blocks.stonebrick.getDefaultState());
				primer.setBlockState(localX, ruinTop + 2, localZ, Blocks.stonebrick.getDefaultState());
				primer.setBlockState(localX, ruinTop + 3, localZ, Blocks.glowstone.getDefaultState());
			}
		}
	}

	@Override
	public Chunk provideChunk(int chunkX, int chunkZ) {
		ChunkPrimer primer = new ChunkPrimer();
		IslandData[] islands = this.prepareIslands(chunkX, chunkZ);
		int baseX = chunkX * CHUNK_SIZE;
		int baseZ = chunkZ * CHUNK_SIZE;
		for (int x = 0; x < CHUNK_SIZE; ++x) {
			for (int z = 0; z < CHUNK_SIZE; ++z) {
				this.generateColumn(primer, baseX + x, baseZ + z, x, z, islands);
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

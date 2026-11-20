package net.minecraft.world;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderAether;

/**
 * Native Eaglercraft Aether dimension provider.
 * Dimension 2 is kept separate from the vanilla Nether and End providers.
 */
public class WorldProviderAether extends WorldProvider {

	@Override
	protected void registerWorldChunkManager() {
		this.worldChunkMgr = new WorldChunkManagerHell(BiomeGenBase.sky, 0.0F);
		this.dimensionId = 2;
		this.hasNoSky = false;
		this.isHellWorld = false;
	}

	@Override
	public IChunkProvider createChunkGenerator() {
		return new ChunkProviderAether(this.worldObj);
	}

	@Override
	public boolean canRespawnHere() {
		return false;
	}

	@Override
	public boolean isSurfaceWorld() {
		// RenderGlobal uses this flag to draw the normal sky, sun, moon, and stars.
		// Keep Aether non-respawnable, but render its sky like a surface dimension.
		return true;
	}

	@Override
	public float getCloudHeight() {
		return 8.0F;
	}

	@Override
	public BlockPos getSpawnCoordinate() {
		return new BlockPos(0, 93, 0);
	}

	@Override
	public boolean canCoordinateBeSpawn(int x, int z) {
		return this.worldObj.getBlockState(new BlockPos(x, 88, z)).getBlock().getMaterial().blocksMovement();
	}

	@Override
	public boolean doesXZShowFog(int x, int z) {
		return false;
	}

	@Override
	public Vec3 getFogColor(float celestialAngle, float partialTicks) {
		return new Vec3(0.72D, 0.86D, 1.0D);
	}

	@Override
	public String getDimensionName() {
		return "The Aether";
	}

	@Override
	public String getInternalNameSuffix() {
		return "_aether";
	}
}

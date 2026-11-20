package net.minecraft.block;

import net.lax1dude.eaglercraft.v1_8.EaglercraftRandom;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

/**
 * A separate glowstone-framed water portal. It deliberately does not use
 * the vanilla obsidian/Nether portal frame or vanilla portal travel logic.
 */
public class BlockAetherPortal extends BlockPortal {

	public BlockAetherPortal() {
		super();
		this.setBlockUnbreakable();
		this.setLightLevel(0.75F);
	}

	public static boolean tryCreatePortal(World world, BlockPos waterPos) {
		if (world.isRemote || world.getBlockState(waterPos).getBlock().getMaterial() != Material.water) {
			return false;
		}
		if (tryCreatePortal(world, waterPos, EnumFacing.Axis.X)) {
			return true;
		}
		return tryCreatePortal(world, waterPos, EnumFacing.Axis.Z);
	}

	private static boolean tryCreatePortal(World world, BlockPos waterPos, EnumFacing.Axis axis) {
		int ux = axis == EnumFacing.Axis.X ? 1 : 0;
		int uz = axis == EnumFacing.Axis.Z ? 1 : 0;
		for (int widthOffset = 0; widthOffset < 2; ++widthOffset) {
			for (int heightOffset = 0; heightOffset < 3; ++heightOffset) {
				BlockPos base = waterPos.add(-ux * widthOffset, -heightOffset, -uz * widthOffset);
				if (!isValidFrame(world, base, ux, uz)) {
					continue;
				}
				IBlockState portalState = Blocks.aether_portal.getDefaultState().withProperty(AXIS, axis);
				for (int x = 0; x < 2; ++x) {
					for (int y = 0; y < 3; ++y) {
						world.setBlockState(base.add(ux * x, y, uz * x), portalState, 2);
					}
				}
				return true;
			}
		}
		return false;
	}

	private static boolean isValidFrame(World world, BlockPos base, int ux, int uz) {
		boolean hasWater = false;
		for (int x = -1; x <= 2; ++x) {
			for (int y = -1; y <= 3; ++y) {
				BlockPos pos = base.add(ux * x, y, uz * x);
				boolean frame = x == -1 || x == 2 || y == -1 || y == 3;
				Material material = world.getBlockState(pos).getBlock().getMaterial();
				if (frame) {
					if (world.getBlockState(pos).getBlock() != Blocks.glowstone) {
						return false;
					}
				} else {
					if (material != Material.air && material != Material.water) {
						return false;
					}
					if (material == Material.water) {
						hasWater = true;
					}
				}
			}
		}
		return hasWater;
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, EaglercraftRandom random) {
		// Aether gateways do not use the Nether portal's random spawn behavior.
	}

	@Override
	public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighbor) {
		// Portal integrity is checked during activation; keep the active gateway stable.
	}

	@Override
	public void randomDisplayTick(World world, BlockPos pos, IBlockState state, EaglercraftRandom random) {
		if (random.nextInt(120) == 0) {
			world.playSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D,
					(double) pos.getZ() + 0.5D, "portal.portal", 0.35F,
					1.15F + random.nextFloat() * 0.25F, false);
		}
		for (int i = 0; i < 2; ++i) {
			double x = (double) pos.getX() + random.nextFloat();
			double y = (double) pos.getY() + random.nextFloat();
			double z = (double) pos.getZ() + random.nextFloat();
			world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, y, z, 0.25D, 0.75D, 1.0D);
		}
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state,
			net.minecraft.entity.Entity entity) {
		if (entity.ridingEntity == null && entity.riddenByEntity == null) {
			entity.func_181015_d(pos);
		}
	}
}

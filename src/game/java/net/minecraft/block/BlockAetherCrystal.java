package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 * Decorative light-emitting crystal found in Aether ruins.
 * Uses the vanilla quartz texture through the bundled block/item model JSON.
 */
public class BlockAetherCrystal extends Block {

	public BlockAetherCrystal() {
		super(Material.glass);
		this.setHardness(1.5F);
		this.setResistance(4.0F);
		this.setLightLevel(0.8F);
		this.setStepSound(soundTypeGlass);
		this.setUnlocalizedName("aetherCrystal");
		this.setCreativeTab(CreativeTabs.tabBlock);
	}
}

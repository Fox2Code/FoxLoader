package com.fox2code.foxloader.client.mixins;

import com.fox2code.foxloader.client.registry.RegisteredBlockImpl;
import com.fox2code.foxloader.registry.RegisteredBlock;
import com.fox2code.foxloader.registry.RegisteredItem;
import com.fox2code.foxloader.registry.RegisteredItemStack;
import com.fox2code.foxloader.utils.BlockFace;
import net.minecraft.src.client.renderer.block.icon.Icon;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.level.IBlockAccess;
import net.minecraft.src.game.level.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class MixinBlock implements RegisteredBlock<IBlockAccess, World>, RegisteredBlockImpl {
	@Shadow
	public abstract int getBlockID();
	@Shadow @Final
	public int blockID;

	@Shadow protected abstract Block setHardness(float hardness);
	@Shadow protected abstract Block setResistance(float resistance);

	@Shadow public abstract Icon getIcon(int blockFace, int metadata);
	@Shadow public abstract void setTexture(Icon icon, int blockFace, int metadata);
	@Shadow public abstract boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int blockFace);
	@Shadow public abstract Icon getBlockTextureFromSide(int blockFace);
	@Shadow public abstract boolean canPlaceBlockOnSide(World world, int x, int y, int z, int blockFace);
	@Shadow public abstract void onBlockPlaced(World world, int x, int y, int z, int metadata);
	@Shadow public abstract boolean isIndirectlyPoweringTo(World world, int x, int y, int z, int blockFace);
	@Shadow public abstract boolean isPoweringTo(IBlockAccess blockAccess, int x, int y, int z, int blockFace);

	@Shadow public abstract int onBlockPlacedWithOffset(World world, int x, int y, int z, int facing, float xVec, float yVec, float zVec, int par9);

	@Override
	public RegisteredItem asRegisteredItem() {
		return (RegisteredItem) Item.itemsList[this.getBlockID()];
	}

	@Override
	public RegisteredItemStack newRegisteredItemStack() {
		return (RegisteredItemStack) (Object) new ItemStack((Block) (Object) this);
	}

	@Override
	public int getRegisteredBlockId() {
		return this.blockID;
	}

	@Override
	public void setRegisteredHardness(float hardness) {
		this.setHardness(hardness);
	}

	@Override
	public void setRegisteredResistance(float resistance) {
		this.setResistance(resistance);
	}

	/**
	 * Calls {@link #shouldSideBeRendered(IBlockAccess, int, int, int, int)}
	 */
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, BlockFace blockFace) {
		return this.shouldSideBeRendered(blockAccess, x, y, z, blockFace.ordinal());
	}

	/**
	 * Calls {@link #getBlockTextureFromSide(int)}
	 */
	@Override
	public Icon getIcon(BlockFace blockFace) {
		return this.getBlockTextureFromSide(blockFace.ordinal());
	}

	/**
	 * Calls {@link #getIcon(int, int)}
	 */
	@Override
	public Icon getIcon(BlockFace blockFace, int metadata) {
		return this.getIcon(blockFace.ordinal(), metadata);
	}

	/**
	 * Calls {@link #setTexture(Icon, int, int)}
	 */
	@Override
	public void setIcon(Icon icon, BlockFace blockFace, int metadata) {
		this.setTexture(icon, blockFace.ordinal(), metadata);
	}

	/**
	 * Calls {@link #canPlaceBlockOnSide(World, int, int, int, int)}
	 */
	@Override
	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, BlockFace blockFace) {
		return canPlaceBlockOnSide(world, x, y, z, blockFace.ordinal());
	}

	/**
	 * Calls {@link #onBlockPlaced(World, int, int, int, int)}
	 */
	@Override
	public void onBlockPlaced(World world, int x, int y, int z, BlockFace blockFace) {
		onBlockPlaced(world, x, y, z, blockFace.ordinal());
	}

	/**
	 * Calls {@link #isPoweringTo(IBlockAccess, int, int, int, int)}
	 */
	@Override
	public boolean isPoweringTo(IBlockAccess blockAccess, int x, int y, int z, BlockFace blockFace) {
		return isPoweringTo(blockAccess, x, y, z, blockFace.ordinal());
	}

	/**
	 * Calls {@link #isIndirectlyPoweringTo(World, int, int, int, int)}
	 */
	@Override
	public boolean isIndirectlyPoweringTo(World world, int x, int y, int z, BlockFace blockFace) {
		return isIndirectlyPoweringTo(world, x, y, z, blockFace.ordinal());
	}

	/**
	 * Calls {@link #onBlockPlacedWithOffset(World, int, int, int, int, float, float, float, int)}
	 */
	@Override
	public int onBlockPlacedWithOffset(World world, int x, int y, int z, BlockFace blockFace, float xVec, float yVec, float zVec, int defaultReturn) {
		return onBlockPlacedWithOffset(world, x, y, z, blockFace.ordinal(), xVec, yVec, zVec, defaultReturn);
	}

	/**
	 * Only implemented on the client.
	 *
	 * @deprecated use {@link #setIcon(Icon, BlockFace, int)}
	 */
	@Deprecated
	@Override
	public final void setTexture(Icon icon, BlockFace blockFace, int metadata) {
		RegisteredBlockImpl.super.setTexture(icon, blockFace, metadata);
	}

	/**
	 * Only implemented on client
	 *
	 * @deprecated use {@link #getIcon(BlockFace)}
	 */
	@Deprecated
	@Override
	public final Icon getBlockTextureFromSide(BlockFace blockFace) {
		return this.getIcon(blockFace);
	}

	/**
	 * Only implemented on the client.
	 *
	 * @deprecated use {@link #getIcon(BlockFace, int)}
	 */
	@Deprecated
	@Override
	public final Icon getTexture(BlockFace blockFace, int metadata) {
		return this.getIcon(blockFace, 0);
	}
}

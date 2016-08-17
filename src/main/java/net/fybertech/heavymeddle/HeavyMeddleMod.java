package net.fybertech.heavymeddle;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddleapi.ConfigFile;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MainOrOffHand;
import net.minecraft.world.World;

public class HeavyMeddleMod 
{
	public static boolean allAxesHeavy = false;	
	public static int maxBlocks = 300;
	public static int maxGirth = 10;
	public static int maxHeight = 100;
	public static boolean ignoreLeaves = false;
	
	private void loadConfig()
	{
		ConfigFile config = new ConfigFile(new File(Meddle.getConfigDir(), "heavymeddle.cfg"));
		config.load();
		
		allAxesHeavy = config.get(ConfigFile.key("general", "allAxesHeavy", false, "Makes all axes destroy logs like the Heavy Axe."));
		maxBlocks = config.get(ConfigFile.key("general", "maxBlocks", 300, "Maximum number of blocks that can be destroyed in a single break."));
		maxGirth = config.get(ConfigFile.key("general", "maxGirth", 10, "Maximum radius to search for blocks in the tree, e.g., 0 = 1x1, 1 = 3x3, 2 = 5x5, etc."));
		maxHeight = config.get(ConfigFile.key("general", "maxHeight", 100, "Maximum height to search for blocks in the tree."));
		ignoreLeaves = config.get(ConfigFile.key("general", "ignoreLeaves", false, "Disables leaf block detection used when determining whether to destroy multiple logs."));
		
		if (config.hasChanged()) config.save();		
	}
	
	
	public void init()
	{
		loadConfig();
		
		ItemStack heavyAxe = new ItemStack(Items.iron_axe);
		NBTTagCompound tag = new NBTTagCompound();
		
		NBTTagCompound display = new NBTTagCompound();
		display.setString("Name", "Heavy Axe");
		
		NBTTagList lore = new NBTTagList();
		lore.appendTag(new NBTTagString("Embrace your inner lumberjack."));
		display.setTag("Lore", lore);
		
		NBTTagList ench = new NBTTagList();
		NBTTagCompound enchUnbreaking = new NBTTagCompound();
		enchUnbreaking.setInteger("id", 34);		
		enchUnbreaking.setInteger("lvl", 2);
		ench.appendTag(enchUnbreaking);	
		
		tag.setTag("display", display);
		tag.setTag("ench",  ench);
		tag.setByte("isHeavyAxe", (byte)1);
		
		heavyAxe.setTagCompound(tag);
		CraftingManager.getInstance().addRecipe(heavyAxe, "II ", "IS ", " S ", 'I', Blocks.iron_block, 'S', Items.stick);
		CraftingManager.getInstance().addRecipe(heavyAxe, " II", " SI", " S ", 'I', Blocks.iron_block, 'S', Items.stick);
	}
	
	/**
	 * Recursively adds neighboring log blocks from the current position, up to the maximum
	 * girth, height, and block count provided in the configuration.
	 * @param block
	 * @param world
	 * @param startPos
	 * @param currentPos
	 * @param player
	 * @param currentTree
	 * @return Whether or not any leaf blocks were found next to the tree.
	 */
	public static boolean addNeighbors(Block block, World world, BlockPos startPos, BlockPos currentPos, EntityPlayer player, HashSet<BlockPos> currentTree) {
		
		boolean hasLeaves = false; 
		for (int z = currentPos.getZ() - 1; z <= currentPos.getZ() + 1; z++) {
			for (int x = currentPos.getX() - 1; x <= currentPos.getX() + 1; x++) {
				for (int y = 0; y <= 1; y++) {
					
					BlockPos newPos = new BlockPos(x, currentPos.getY() + y, z);
					if (currentTree.contains(newPos)) continue;
					
					IBlockState upState = world.getBlockState(newPos);
					if (upState == null) continue;				
					
					if (upState.getBlock() instanceof BlockLeaves) {
						hasLeaves = true;
					}
					
					if (upState.getBlock() == block) {
						if (Math.abs(x - startPos.getX()) <= maxGirth && Math.abs(z - startPos.getZ()) <= maxGirth
								&& y - startPos.getY() <= maxHeight && currentTree.size() <= maxBlocks) {
							currentTree.add(newPos);
							if (addNeighbors(block, world, startPos, newPos, player, currentTree))
								hasLeaves = true;
						}
					}
				}
			}
		}
		return hasLeaves;
		
	}
	
	/**
	 * Destroys a single log block in the world.
	 * @param block
	 * @param world
	 * @param pos
	 * @param player
	 * @return Whether the block was successfully destroyed by the player.
	 */
	public static boolean destroyBlock(Block block, World world, BlockPos pos, EntityPlayer player) {
		// TODO - Change stackSize to getStackSize() later.  Mapping doesn't exist currently for 16w32b, so
		//        we'll use an access transformer for now.
		if (player.getHeldMainHandItem() == null || player.getHeldMainHandItem().stackSize < 1) return false;
		
		IBlockState upState = world.getBlockState(pos);
		if (upState == null) return false;				
		
		if (upState.getBlock() != null && upState.getBlock() == block) 
		{
			world.setBlockState(pos, Blocks.air.getDefaultState(), 3);					
			player.getHeldMainHandItem().damageItem(1, player);
			
			if (player.getHeldMainHandItem().stackSize < 1) {							
				player.setHeldItem(MainOrOffHand.MAIN_HAND, (ItemStack)null);
			}					
			
			block.harvestBlock(world, player, pos, upState, null, player.getHeldMainHandItem() == null ? null : player.getHeldMainHandItem().copy());
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * Old method, destroys all log blocks attached to the current position, regardless
	 * of whether they are player placed, and with no limit.
	 * @param block
	 * @param world
	 * @param pos
	 * @param player
	 */
	public static void destroyNeighbors(Block block, World world, BlockPos pos, EntityPlayer player)
	{		
		for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
			for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
				for (int y = 0; y <= 1; y++) {
					
					BlockPos newPos = new BlockPos(x, pos.getY() + y, z);
					if (destroyBlock(block, world, newPos, player))
						destroyNeighbors(block, world, newPos, player);
					
				}				
			}
		}
	}
	
	
	public static boolean onBlockDestroyedHook(boolean result, ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity)
	{
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (!tag.hasKey("isHeavyAxe", 1) && !HeavyMeddleMod.allAxesHeavy) return result;
		}
		else if (!HeavyMeddleMod.allAxesHeavy) return result;	
		
		if (entity.isSneaking()) return result;
		
		Block block = state.getBlock();
		if (block instanceof BlockLog && entity instanceof EntityPlayer) {
			HashSet<BlockPos> tree = new HashSet<BlockPos>();
			boolean hasLeaves = addNeighbors(block, world, pos, pos, (EntityPlayer)entity, tree);
			if (hasLeaves || ignoreLeaves) {
				for (BlockPos currentPos : tree) {
					if (!destroyBlock(block, world, currentPos, (EntityPlayer)entity))
						break;
				}
			}
		}
		
		return result;
	}
	
}

package net.fybertech.heavymeddle;

import java.io.File;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddleapi.ConfigFile;
import net.minecraft.block.Block;
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
	
	
	private void loadConfig()
	{
		ConfigFile config = new ConfigFile(new File(Meddle.getConfigDir(), "heavymeddle.cfg"));
		config.load();
		
		allAxesHeavy = config.get(ConfigFile.key("general", "allAxesHeavy", false, "Makes all axes destroy logs like the Heavy Axe."));
		
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
	
		
	public static void destroyNeighbors(Block block, World world, BlockPos pos, EntityPlayer player)
	{		
		for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
			for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
				for (int y = 0; y <= 1; y++) {
					if (player.getHeldMainHandItem() == null || player.getHeldMainHandItem().stackSize < 1) return;				
					
					BlockPos newPos = new BlockPos(x, pos.getY() + y, z);
					IBlockState upState = world.getBlockState(newPos);
					if (upState == null) continue;				
					
					if (upState.getBlock() != null && upState.getBlock() == block) 
					{
						world.setBlockState(newPos, Blocks.air.getDefaultState(), 3);					
						player.getHeldMainHandItem().damageItem(1, player);
						
						if (player.getHeldMainHandItem().stackSize < 1) {							
							player.setHeldItem(MainOrOffHand.MAIN_HAND, (ItemStack)null);
						}					
						
						block.harvestBlock(world, player, newPos, upState, null, player.getHeldMainHandItem() == null ? null : player.getHeldMainHandItem().copy());
						
						destroyNeighbors(block, world, newPos, player);
					}
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
			destroyNeighbors(block, world, pos, (EntityPlayer)entity);
		}
		
		return result;
	}
	
}

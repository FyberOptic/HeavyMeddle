package net.fybertech.heavymeddle;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.minecraft.launchwrapper.IClassTransformer;

public class HeavyTransformer implements IClassTransformer
{
	String itemAxe = DynamicMappings.getClassMapping("net/minecraft/item/ItemAxe");
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) 
	{		
		if (itemAxe != null && name.equals(itemAxe)) return transformAxe(bytes);
		
		return bytes;
	}

	
	private byte[] failGracefully(String error, byte[] bytes)
	{
		Meddle.LOGGER.error("[Meddle/HeavyMeddle] " + error);
		return bytes;
	}
	
	
	private byte[] transformAxe(byte[] bytes)
	{
		ClassReader reader = new ClassReader(bytes);
		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);
		
		String mapping = DynamicMappings.getMethodMapping("net/minecraft/item/Item onBlockDestroyed (Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/entity/EntityLivingBase;)Z");
		if (mapping == null) return failGracefully("Couldn't find mapping for Item.onBlockDestroyed!", bytes);
		MethodNode onBlockDestroyed = DynamicMappings.getMethodNodeFromMapping(cn, mapping);
		if (onBlockDestroyed != null) return failGracefully("ItemAxe.onBlockDestroyed already exists!", bytes);
		
		String[] split = mapping.split(" ");
		
		onBlockDestroyed = new MethodNode(Opcodes.ACC_PUBLIC, split[1], split[2], null, null);
		
		InsnList list = onBlockDestroyed.instructions;
		
		list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ALOAD, 3));
		list.add(new VarInsnNode(Opcodes.ALOAD, 4));
		list.add(new VarInsnNode(Opcodes.ALOAD, 5));		
		list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, cn.superName, split[1], split[2], false));
		
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ALOAD, 3));
		list.add(new VarInsnNode(Opcodes.ALOAD, 4));
		list.add(new VarInsnNode(Opcodes.ALOAD, 5));		
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/heavymeddle/HeavyMeddleMod", "onBlockDestroyedHook", split[2].replace("(", "(Z"), false));		
		
		list.add(new InsnNode(Opcodes.IRETURN));
		
		cn.methods.add(onBlockDestroyed);
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		cn.accept(writer);
		return writer.toByteArray();
	}
}

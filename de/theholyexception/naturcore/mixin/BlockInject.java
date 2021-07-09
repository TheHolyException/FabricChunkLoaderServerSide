package de.theholyexception.naturcore.mixin;

import de.theholyexception.naturcore.ChunkLoaderBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.UUID;

//@Mixin(Block.class)
//public class BlockInject {
	/*@Inject(at = @At("HEAD"), method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", cancellable = true)
	private void onBlockPlace(World world, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack itemStack, CallbackInfo ci) {
		String itemID = itemStack.getName().toString().split(",")[0].split("'")[1];
		if (itemID.equals("block.minecraft.lodestone")) {
			player.sendSystemMessage(new LiteralText("§cYou have created an Chunloader"), Util.NIL_UUID);
		}
	}*/
//}

@Mixin(Blocks.class)
public class BlockInject {
	//@Inject(at = @At("HEAD"), method = "register()V")
	@Inject(at = @At("HEAD"), method = "register(Ljava/lang/String;Lnet/minecraft/block/Block;)Lnet/minecraft/block/Block;", cancellable = true)
	private static void register(String id, Block block, CallbackInfoReturnable info) {
		try {
			//return null;
			//System.out.println("Trying to inject Chunkloader : " + id);
			if (id.equals("lodestone")) {
				Block b = new ChunkLoaderBlock(AbstractBlock.Settings.of(Material.REPAIR_STATION).requiresTool().strength(3.5F).sounds(BlockSoundGroup.LODESTONE));
				info.setReturnValue(Registry.register(Registry.BLOCK, id, b));
				//System.out.println(Registry.BLOCK.containsId(new Identifier(id)));
				Iterator var0 = Registry.BLOCK.iterator();
				while(var0.hasNext()) {
					Block bl = (Block)var0.next();
					if(bl == b) System.out.println("[NaturCore] Successfully injected Chunkloader Functions in the Lodestone Block");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}


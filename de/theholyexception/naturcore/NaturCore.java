package de.theholyexception.naturcore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class NaturCore implements ModInitializer {

	private static final Block ChunkLoaderBlock = Blocks.LODESTONE;


	@Override
	public void onInitialize() {
		System.out.println("Setting up NaturCore");
	}


}

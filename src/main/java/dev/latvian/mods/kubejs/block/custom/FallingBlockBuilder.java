package dev.latvian.mods.kubejs.block.custom;

import com.mojang.serialization.MapCodec;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.color.KubeColor;
import dev.latvian.mods.kubejs.color.SimpleColor;
import dev.latvian.mods.rhino.util.ReturnsSelf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

@ReturnsSelf
public class FallingBlockBuilder extends BlockBuilder {
	private KubeColor dustColor = new SimpleColor(0x807C7B);

	public FallingBlockBuilder(ResourceLocation i) {
		super(i);
	}

	@Override
	public Block createObject() {
		return new KubeJSFallingBlock(createProperties());
	}

	public FallingBlockBuilder dustColor(KubeColor color) {
		dustColor = color;
		return this;
	}

	static class KubeJSFallingBlock extends FallingBlock {
		private static final MapCodec<KubeJSFallingBlock> CODEC = simpleCodec(KubeJSFallingBlock::new);

		public KubeJSFallingBlock(Properties p) {
			super(p);
		}

		@Override
		protected MapCodec<KubeJSFallingBlock> codec() {
			return CODEC;
		}

		@Override
		public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
			return ((FallingBlockBuilder) kjs$getBlockBuilder()).dustColor.kjs$getARGB();
		}
	}
}

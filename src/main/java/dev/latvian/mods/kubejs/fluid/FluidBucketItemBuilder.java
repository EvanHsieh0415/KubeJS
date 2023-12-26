package dev.latvian.mods.kubejs.fluid;

import dev.architectury.core.item.ArchitecturyBucketItem;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.world.item.BucketItem;

public class FluidBucketItemBuilder extends ItemBuilder {
	public final FluidBuilder fluidBuilder;

	public FluidBucketItemBuilder(FluidBuilder b) {
		super(b.newID("", "_bucket"));
		fluidBuilder = b;
		maxStackSize(1);
	}

	@Override
	public BucketItem createObject() {
		return new ArchitecturyBucketItem(fluidBuilder, createItemProperties());
	}

	@Override
	public void generateAssetJsons(AssetJsonGenerator generator) {
		generator.itemModel(id, m -> m.parent("kubejs:item/generated_bucket"));
	}
}

package dev.latvian.mods.kubejs.recipe.filter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class TypeFilter implements RecipeFilter {
	private final ResourceKey<RecipeSerializer<?>> type;

	public TypeFilter(ResourceLocation t) {
		type = ResourceKey.create(Registries.RECIPE_SERIALIZER, t);
	}

	@Override
	public boolean test(RecipeMatchContext cx) {
		return cx.recipe().kjs$getTypeKey() == type;
	}

	@Override
	public String toString() {
		return "TypeFilter{" + type.location() + '}';
	}
}

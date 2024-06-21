package dev.latvian.mods.kubejs.integration.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.latvian.mods.kubejs.item.ItemPredicate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;

public class EMIIntegration {
	public static EmiStack fluid(FluidStack stack) {
		return EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount());
	}

	public static Predicate<EmiStack> predicate(ItemPredicate ingredient) {
		return emiStack -> {
			var is = emiStack.getItemStack();
			return !is.isEmpty() && ingredient.test(is);
		};
	}

	public static Predicate<EmiStack> predicate(FluidIngredient ingredient) {
		var set = new HashSet<>(Arrays.stream(ingredient.getStacks()).map(EMIIntegration::fluid).toList());
		return set::contains;
	}
}

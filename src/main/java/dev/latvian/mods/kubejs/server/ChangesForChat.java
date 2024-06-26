package dev.latvian.mods.kubejs.server;

import dev.latvian.mods.kubejs.bindings.TextIcons;
import dev.latvian.mods.kubejs.util.TimeJS;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ChangesForChat {
	public static int recipesAdded = 0;
	public static int recipesRemoved = 0;
	public static int recipesModified = 0;
	public static long recipesMs = 0L;

	public static void print(Consumer<Component> out) {
		if (recipesAdded != 0 || recipesRemoved != 0 || recipesModified != 0 || recipesMs != 0L) {
			out.accept(Component.empty()
				.append(TextIcons.crafting().kjs$hover(Component.literal("Recipe Changes")))
				.append(Component.empty()
					.kjs$hover(Component.literal("Added Recipes"))
					.append(" ")
					.append(TextIcons.icons("+."))
					.append(String.valueOf(recipesAdded))
					.append(" ")
				)
				.append("|")
				.append(Component.empty()
					.kjs$hover(Component.literal("Removed Recipes"))
					.append(" ")
					.append(TextIcons.icons("-."))
					.append(String.valueOf(recipesRemoved))
					.append(" ")
				)
				.append("|")
				.append(Component.empty()
					.kjs$hover(Component.literal("Modified Recipes"))
					.append(" ")
					.append(TextIcons.icons("~."))
					.append(String.valueOf(recipesModified))
					.append(" ")
				)
				.append("| " + TimeJS.msToString(recipesMs))

			);
		}
	}
}
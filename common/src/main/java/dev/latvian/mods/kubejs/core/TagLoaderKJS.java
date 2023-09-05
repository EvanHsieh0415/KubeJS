package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import dev.latvian.mods.kubejs.item.ingredient.TagContext;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dev.latvian.mods.kubejs.server.TagEventJS;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface TagLoaderKJS<T> {
	default void kjs$customTags(Map<ResourceLocation, List<TagLoader.EntryWithSource>> map) {
		TagContext.INSTANCE.setValue(TagContext.EMPTY);
		var reg = kjs$getRegistry();

		if (reg == null) {
			return;
		}
		var regInfo = RegistryInfo.MAP.get(reg.key());

		if ((regInfo != null && regInfo.hasDefaultTags) || ServerEvents.TAGS.hasListeners(reg.key())) {
			var dir = kjs$getDirectory();
			new TagEventJS<>(dir, map, reg).post(ServerScriptManager.instance == null ? null : ServerScriptManager.instance.tagEventHolders.get(reg.key()));
		}
	}

	void kjs$setRegistry(Registry<T> registry);

	@Nullable
	Registry<T> kjs$getRegistry();

	String kjs$getDirectory();
}
package dev.latvian.mods.kubejs.recipe.viewer.server;

import dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper;
import dev.latvian.mods.kubejs.recipe.viewer.GroupEntriesKubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ServerGroupItemEntriesKubeEvent implements GroupEntriesKubeEvent {
	private final List<ItemData.Group> list;

	public ServerGroupItemEntriesKubeEvent(List<ItemData.Group> list) {
		this.list = list;
	}

	@Override
	public void group(Context cx, Object filter, ResourceLocation groupId, Component description) {
		list.add(new ItemData.Group(IngredientWrapper.wrap(cx, filter), groupId, description));
	}
}

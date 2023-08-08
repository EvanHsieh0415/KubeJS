package dev.latvian.mods.kubejs.core;

import com.google.gson.JsonElement;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import dev.latvian.mods.kubejs.loot.*;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.DataExport;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public interface LootTablesKJS {
	default void kjs$apply0(Map<ResourceLocation, JsonElement> map, BiConsumer<ResourceLocation, JsonElement> action) {
		// part 1: modifying loot tables
		Map<ResourceLocation, JsonElement> map1 = new HashMap<>(map);
		ServerEvents.GENERIC_LOOT_TABLES.post(ScriptType.SERVER, new GenericLootEventJS(map1));
		ServerEvents.BLOCK_LOOT_TABLES.post(ScriptType.SERVER, new BlockLootEventJS(map1));
		ServerEvents.ENTITY_LOOT_TABLES.post(ScriptType.SERVER, new EntityLootEventJS(map1));
		ServerEvents.GIFT_LOOT_TABLES.post(ScriptType.SERVER, new GiftLootEventJS(map1));
		ServerEvents.FISHING_LOOT_TABLES.post(ScriptType.SERVER, new FishingLootEventJS(map1));
		ServerEvents.CHEST_LOOT_TABLES.post(ScriptType.SERVER, new ChestLootEventJS(map1));

		// part 2: add loot tables to export
		for (var entry : map1.entrySet()) {
			try {
				action.accept(entry.getKey(), entry.getValue());

				if (DataExport.export != null) {
					DataExport.export.addJson("loot_tables/" + entry.getKey().toString() + ".json", entry.getValue());
				}
			} catch (Exception ex) {
				ConsoleJS.SERVER.error("Failed to load loot table " + entry.getKey() + ": " + ex + "\nJson: " + entry.getValue());
			}
		}

		// if (DataExport.dataExport != null) {
		// 	DataExport.dataExport.add("loot_tables", export);
		// }

		// part 3: export data
		DataExport.exportData();

		// part 4: complete reload
		if (UtilsJS.staticServer != null && CommonProperties.get().announceReload && !CommonProperties.get().hideServerScriptErrors) {
			if (ScriptType.SERVER.errors.isEmpty()) {
				UtilsJS.staticServer.kjs$tell(Component.literal("Reloaded with no KubeJS errors!").withStyle(ChatFormatting.GREEN));
			} else {
				UtilsJS.staticServer.kjs$tell(ScriptType.SERVER.errorsComponent("/kubejs errors"));
			}
		}
	}
}

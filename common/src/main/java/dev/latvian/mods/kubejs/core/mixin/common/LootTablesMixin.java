package dev.latvian.mods.kubejs.core.mixin.common;

import com.google.gson.JsonElement;
import dev.latvian.mods.kubejs.core.LootTablesKJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(LootDataManager.class)
public abstract class LootTablesMixin implements LootTablesKJS {

	// TODO: (low priority) Replace with a less destructive mixin type
	@Redirect(method = "apply*", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V", ordinal = 0))
	private void kjs$apply(Map<ResourceLocation, JsonElement> map, BiConsumer<ResourceLocation, JsonElement> action) {
		kjs$apply0(map, action);
	}
}

package dev.latvian.mods.kubejs;

import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.data.ExportablePackResources;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KubeJSCommon {
	public void init() {
	}

	public void clientSetup() {
	}

	public void reloadClientInternal() {
	}

	public void clientBindings(BindingsEvent event) {
	}

	public void clientTypeWrappers(TypeWrappers typeWrappers) {
	}

	public void handleDataFromServerPacket(String channel, @Nullable CompoundTag data) {
	}

	@Nullable
	public Player getClientPlayer() {
		return null;
	}

	public void paint(CompoundTag tag) {
	}

	public Level getClientLevel() {
		throw new IllegalStateException("Can't access client level from server side!");
	}

	public void reloadTextures() {
	}

	public void reloadLang() {
	}

	public boolean isClientButNotSelf(Player player) {
		return false;
	}

	public void generateTypings(CommandSourceStack source) {
	}

	public void reloadConfig() {
		CommonProperties.reload();
		DevProperties.reload();
	}

	public void export(List<ExportablePackResources> packs) {
	}
}
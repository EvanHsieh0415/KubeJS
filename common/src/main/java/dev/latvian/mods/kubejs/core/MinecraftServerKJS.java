package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.net.SendDataFromServerMessage;
import dev.latvian.mods.kubejs.player.AdvancementJS;
import dev.latvian.mods.kubejs.player.EntityArrayList;
import dev.latvian.mods.kubejs.server.IScheduledEventCallback;
import dev.latvian.mods.kubejs.server.ScheduledEvent;
import dev.latvian.mods.kubejs.util.TickDuration;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.TemporalAmount;

@RemapPrefixForJS("kjs$")
public interface MinecraftServerKJS extends WithAttachedData<MinecraftServer>, MessageSenderKJS, WithPersistentData, DataSenderKJS {
	default MinecraftServer kjs$self() {
		return (MinecraftServer) this;
	}

	MinecraftServer.ReloadableResources kjs$getReloadableResources();

	ServerLevel kjs$getOverworld();

	ScheduledEvent kjs$schedule(TemporalAmount timer, IScheduledEventCallback event);

	default ScheduledEvent kjs$scheduleInTicks(long ticks, IScheduledEventCallback event) {
		return kjs$schedule(new TickDuration(ticks), event);
	}

	@Override
	default Component kjs$getName() {
		return Component.literal(kjs$self().name());
	}

	@Override
	default Component kjs$getDisplayName() {
		return kjs$self().createCommandSourceStack().getDisplayName();
	}

	@Override
	default void kjs$tell(Component message) {
		kjs$self().sendSystemMessage(message);

		for (var player : kjs$self().getPlayerList().getPlayers()) {
			player.kjs$tell(message);
		}
	}

	@Override
	default void kjs$setStatusMessage(Component message) {
		for (var player : kjs$self().getPlayerList().getPlayers()) {
			player.kjs$setStatusMessage(message);
		}
	}

	@Override
	default int kjs$runCommand(String command) {
		return kjs$self().getCommands().performPrefixedCommand(kjs$self().createCommandSourceStack(), command);
	}

	@Override
	default int kjs$runCommandSilent(String command) {
		return kjs$self().getCommands().performPrefixedCommand(kjs$self().createCommandSourceStack().withSuppressedOutput(), command);
	}

	default ServerLevel kjs$getLevel(ResourceLocation dimension) {
		return kjs$self().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
	}

	@Nullable
	default ServerPlayer kjs$getPlayer(PlayerSelector selector) {
		return selector.getPlayer(kjs$self());
	}

	default EntityArrayList kjs$getPlayers() {
		return new EntityArrayList(kjs$self().overworld(), kjs$self().getPlayerList().getPlayers());
	}

	default EntityArrayList kjs$getEntities() {
		var list = new EntityArrayList(kjs$self().overworld(), 10);

		for (var level : kjs$self().getAllLevels()) {
			list.addAllIterable(level.getAllEntities());
		}

		return list;
	}

	@Nullable
	default AdvancementJS kjs$getAdvancement(ResourceLocation id) {
		var a = kjs$self().getAdvancements().getAdvancement(id);
		return a == null ? null : new AdvancementJS(a);
	}

	@Override
	default void kjs$sendData(String channel, @Nullable CompoundTag data) {
		new SendDataFromServerMessage(channel, data).sendToAll(kjs$self());
	}
}

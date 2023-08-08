package dev.latvian.mods.kubejs.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.latvian.mods.kubejs.CommonProperties;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.KubeJSPaths;
import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.net.PaintMessage;
import dev.latvian.mods.kubejs.platform.IngredientPlatformHelper;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.VirtualKubeJSDataPack;
import dev.latvian.mods.kubejs.server.CustomCommandEventJS;
import dev.latvian.mods.kubejs.server.DataExport;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class KubeJSCommands {
	public static final DynamicCommandExceptionType NO_REGISTRY = new DynamicCommandExceptionType((id) ->
			Component.literal("No builtin or static registry found for " + id)
	);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var cmd = dispatcher.register(Commands.literal("kubejs")
				.then(Commands.literal("help")
						.executes(context -> help(context.getSource()))
				)
				.then(Commands.literal("custom_command")
						.then(Commands.argument("id", StringArgumentType.word())
								.executes(context -> customCommand(context.getSource(), StringArgumentType.getString(context, "id")))
						)
				)
				.then(Commands.literal("hand")
						.executes(context -> hand(context.getSource().getPlayerOrException(), InteractionHand.MAIN_HAND))
				)
				.then(Commands.literal("offhand")
						.executes(context -> hand(context.getSource().getPlayerOrException(), InteractionHand.OFF_HAND))
				)
				.then(Commands.literal("inventory")
						.executes(context -> inventory(context.getSource().getPlayerOrException()))
				)
				.then(Commands.literal("hotbar")
						.executes(context -> hotbar(context.getSource().getPlayerOrException()))
				)
				.then(Commands.literal("errors")
						.executes(context -> errors(context.getSource()))
				)
				.then(Commands.literal("warnings")
						.executes(context -> warnings(context.getSource()))
				)
				.then(Commands.literal("reload")
						.then(Commands.literal("config")
								.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
								.executes(context -> reloadConfig(context.getSource()))
						)
						.then(Commands.literal("startup_scripts")
								.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
								.executes(context -> reloadStartup(context.getSource()))
						)
						.then(Commands.literal("server_scripts")
								.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
								.executes(context -> reloadServer(context.getSource()))
						)
						.then(Commands.literal("client_scripts")
								.requires(source -> true)
								.executes(context -> reloadClient(context.getSource()))
						)
						.then(Commands.literal("textures")
								.requires(source -> true)
								.executes(context -> reloadTextures(context.getSource()))
						)
						.then(Commands.literal("lang")
								.requires(source -> true)
								.executes(context -> reloadLang(context.getSource()))
						)
				)
				.then(Commands.literal("export")
						.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
						.executes(context -> export(context.getSource()))
				)
				.then(Commands.literal("export_virtual_data")
						.requires(source -> source.getServer().isSingleplayer() || source.hasPermission(2))
						.executes(context -> exportVirtualData(context.getSource()))
				)
				/*
				.then(Commands.literal("output_recipes")
						.executes(context -> outputRecipes(context.getSource().getPlayerOrException()))
				)
				.then(Commands.literal("input_recipes")
						.executes(context -> inputRecipes(context.getSource().getPlayerOrException()))
				)
				.then(Commands.literal("check_recipe_conflicts")
						.executes(context -> checkRecipeConflicts(context.getSource().getPlayerOrException()))
				)
				 */
				.then(Commands.literal("list_tag")
						.then(Commands.argument("registry", ResourceLocationArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										ctx.getSource().registryAccess()
												.registries()
												.map(entry -> entry.key().location().toString()), builder)
								)
								.executes(ctx -> listTagsFor(ctx.getSource(), registry(ctx, "registry")))
								.then(Commands.argument("tag", ResourceLocationArgument.id())
										.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
												allTags(ctx.getSource(), registry(ctx, "registry"))
														.map(TagKey::location)
														.map(ResourceLocation::toString), builder)
										)
										.executes(ctx -> tagObjects(ctx.getSource(), TagKey.create(registry(ctx, "registry"),
												ResourceLocationArgument.getId(ctx, "tag")))
										)
								)
						)
				)
				.then(Commands.literal("dump_registry")
						.then(Commands.argument("registry", ResourceLocationArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										ctx.getSource().registryAccess()
												.registries()
												.map(entry -> entry.key().location().toString()), builder)
								)
								.executes(ctx -> dumpRegistry(ctx.getSource(), registry(ctx, "registry")))
						)
				)
				.then(Commands.literal("stages")
						.then(Commands.literal("add")
								.then(Commands.argument("player", EntityArgument.players())
										.then(Commands.argument("stage", StringArgumentType.string())
												.executes(context -> addStage(context.getSource(), EntityArgument.getPlayers(context, "player"), StringArgumentType.getString(context, "stage")))
										)
								)
						)
						.then(Commands.literal("remove")
								.then(Commands.argument("player", EntityArgument.players())
										.then(Commands.argument("stage", StringArgumentType.string())
												.executes(context -> removeStage(context.getSource(), EntityArgument.getPlayers(context, "player"), StringArgumentType.getString(context, "stage")))
										)
								)
						)
						.then(Commands.literal("clear")
								.then(Commands.argument("player", EntityArgument.players())
										.executes(context -> clearStages(context.getSource(), EntityArgument.getPlayers(context, "player")))
								)
						)
						.then(Commands.literal("list")
								.then(Commands.argument("player", EntityArgument.players())
										.executes(context -> listStages(context.getSource(), EntityArgument.getPlayers(context, "player")))
								)
						)
				)
				.then(Commands.literal("painter")
						.then(Commands.argument("player", EntityArgument.players())
								.then(Commands.argument("object", CompoundTagArgument.compoundTag())
										.executes(context -> painter(context.getSource(), EntityArgument.getPlayers(context, "player"), CompoundTagArgument.getCompoundTag(context, "object")))
								)
						)
				)
				.then(Commands.literal("generate_typings")
						.requires(source -> source.getServer().isSingleplayer())
						.executes(context -> generateTypings(context.getSource()))
				)
				.then(Commands.literal("packmode")
						.executes(context -> packmode(context.getSource(), ""))
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> packmode(context.getSource(), StringArgumentType.getString(context, "name")))
						)
				)
		);

		dispatcher.register(Commands.literal("kjs").redirect(cmd));
	}

	private static <T> ResourceKey<Registry<T>> registry(CommandContext<CommandSourceStack> ctx, String arg) {
		return ResourceKey.createRegistryKey(ResourceLocationArgument.getId(ctx, arg));
	}

	private static <T> Stream<TagKey<T>> allTags(CommandSourceStack source, ResourceKey<Registry<T>> registry) throws CommandSyntaxException {
		return source.registryAccess().registry(registry)
				.orElseThrow(() -> NO_REGISTRY.create(registry.location()))
				.getTagNames();
	}

	private static Component copy(String s, ChatFormatting col, String info) {
		var component = Component.literal("- ");
		component.setStyle(component.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)));
		component.setStyle(component.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s)));
		component.setStyle(component.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(info + " (Click to copy)"))));
		component.append(Component.literal(s).withStyle(col));
		return component;
	}

	private static void link(CommandSourceStack source, ChatFormatting color, String name, String url) {
		source.sendSuccess(() -> Component.literal("• ").append(Component.literal(name).withStyle(color).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)))), false);
	}

	private static int help(CommandSourceStack source) {
		link(source, ChatFormatting.GOLD, "Wiki", "https://kubejs.com/?" + KubeJS.QUERY);
		link(source, ChatFormatting.GREEN, "Support", "https://kubejs.com/support?" + KubeJS.QUERY);
		link(source, ChatFormatting.BLUE, "Changelog", "https://kubejs.com/changelog?" + KubeJS.QUERY);
		return Command.SINGLE_SUCCESS;
	}

	private static int customCommand(CommandSourceStack source, String id) {
		if (ServerEvents.CUSTOM_COMMAND.hasListeners()) {
			var result = ServerEvents.CUSTOM_COMMAND.post(new CustomCommandEventJS(source.getLevel(), source.getEntity(), BlockPos.containing(source.getPosition()), id), id);

			if (result.type() == EventResult.Type.ERROR) {
				source.sendFailure(Component.literal(result.value().toString()));
				return 0;
			}

			return 1;
		}

		return 0;
	}

	private static int hand(ServerPlayer player, InteractionHand hand) {
		player.sendSystemMessage(Component.literal("Item in hand:"));
		var stack = player.getItemInHand(hand);
		player.sendSystemMessage(copy(ItemStackJS.toItemString(stack), ChatFormatting.GREEN, "Item ID"));

		List<ResourceLocation> tags = new ArrayList<>(stack.kjs$getTags());
		tags.sort(null);

		for (var id : tags) {
			player.sendSystemMessage(copy("'#" + id + "'", ChatFormatting.YELLOW, "Item Tag [" + IngredientPlatformHelper.get().tag(id.toString()).kjs$getStacks().size() + " items]"));
		}

		player.sendSystemMessage(copy("'@" + stack.kjs$getMod() + "'", ChatFormatting.AQUA, "Mod [" + IngredientPlatformHelper.get().mod(stack.kjs$getMod()).kjs$getStacks().size() + " items]"));

		/*var cat = stack.getItem().getItemCategory();

		if (cat != null) {
			player.sendSystemMessage(copy("'%" + cat.getRecipeFolderName() + "'", ChatFormatting.LIGHT_PURPLE, "Item Group [" + IngredientPlatformHelper.get().creativeTab(cat).kjs$getStacks().size() + " items]"));
		}*/

		return 1;
	}

	private static int inventory(ServerPlayer player) {
		return dump(player.getInventory().items, player, "Inventory");
	}

	private static int hotbar(ServerPlayer player) {
		return dump(player.getInventory().items.subList(0, 9), player, "Hotbar");
	}

	private static int dump(List<ItemStack> stacks, ServerPlayer player, String name) {
		var dump = stacks.stream().filter(is -> !is.isEmpty()).map(ItemStackJS::toItemString).toList();
		player.sendSystemMessage(copy(dump.toString(), ChatFormatting.WHITE, name + " Item List"));
		return 1;
	}

	private static int errors(CommandSourceStack source) {
		var lines = ScriptType.SERVER.errors.toArray(new String[0]);

		if (lines.length == 0) {
			source.sendSuccess(() -> Component.literal("No errors found!").withStyle(ChatFormatting.GREEN), false);

			if (!ScriptType.SERVER.warnings.isEmpty()) {
				source.sendSuccess(() -> ScriptType.SERVER.warningsComponent("/kubejs warnings"), false);
			}
			return 1;
		}

		for (var i = 0; i < lines.length; i++) {
			var component = Component.literal((i + 1) + ") ").append(Component.literal(lines[i]).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED);
			source.sendSuccess(() -> component, false);
		}

		source.sendSuccess(() -> Component.literal("More info in ")
						.append(Component.literal("'logs/kubejs/server.log'")
								.kjs$clickOpenFile(ScriptType.SERVER.getLogFile().toString())
								.kjs$hover(Component.literal("Click to open"))).withStyle(ChatFormatting.DARK_RED),
				false);

		if (!ScriptType.SERVER.warnings.isEmpty()) {
			source.sendSuccess(() -> ScriptType.SERVER.warningsComponent("/kubejs warnings"), false);
		}

		return 1;
	}

	private static int warnings(CommandSourceStack source) {
		var lines = ScriptType.SERVER.warnings.toArray(new String[0]);

		if (lines.length == 0) {
			source.sendSuccess(() -> Component.literal("No warnings found!").withStyle(ChatFormatting.GREEN), false);
			return 1;
		}

		for (var i = 0; i < lines.length; i++) {
			var component = Component.literal((i + 1) + ") ").append(Component.literal(lines[i]).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFA500))).withStyle(ChatFormatting.RED));
			source.sendSuccess(() -> component, false);
		}

		return 1;
	}

	private static int reloadConfig(CommandSourceStack source) {
		KubeJS.PROXY.reloadConfig();
		source.sendSuccess(() -> Component.literal("Done!"), false);
		return 1;
	}

	private static int reloadStartup(CommandSourceStack source) {
		KubeJS.getStartupScriptManager().reload(null);
		source.sendSuccess(() -> Component.literal("Done!"), false);
		return 1;
	}

	private static int reloadServer(CommandSourceStack source) {
		ServerScriptManager.instance.reloadScriptManager(source.getServer().kjs$getReloadableResources().resourceManager());
		source.sendSuccess(() -> Component.literal("Done! To reload recipes, tags, loot tables and other datapack things, run ")
						.append(Component.literal("'/reload'")
								.kjs$clickRunCommand("/reload")
								.kjs$hover(Component.literal("Click to run"))),
				false);
		return 1;
	}

	private static int reloadClient(CommandSourceStack source) {
		KubeJS.PROXY.reloadClientInternal();
		source.sendSuccess(() -> Component.literal("Done! To reload textures, models and other assets, press F3 + T"), false);
		return 1;
	}

	private static int reloadTextures(CommandSourceStack source) {
		KubeJS.PROXY.reloadTextures();
		return 1;
	}

	private static int reloadLang(CommandSourceStack source) {
		KubeJS.PROXY.reloadLang();
		return 1;
	}

	private static int export(CommandSourceStack source) {
		if (DataExport.export != null) {
			return 0;
		}

		DataExport.export = new DataExport();
		DataExport.export.source = source;
		source.sendSuccess(() -> Component.literal("Reloading server and exporting data..."), false);

		var minecraftServer = source.getServer();
		var packRepository = minecraftServer.getPackRepository();
		var worldData = minecraftServer.getWorldData();
		var collection = packRepository.getSelectedIds();
		packRepository.reload();
		Collection<String> collection2 = Lists.newArrayList(collection);
		Collection<String> collection3 = worldData.getDataConfiguration().dataPacks().getDisabled();

		for (var string : packRepository.getAvailableIds()) {
			if (!collection3.contains(string) && !collection2.contains(string)) {
				collection2.add(string);
			}
		}

		ReloadCommand.reloadPacks(collection2, source);
		return 1;
	}

	private static int exportVirtualData(CommandSourceStack source) {
		return source.getServer().getResourceManager()
				.listPacks()
				.filter(pack -> pack instanceof VirtualKubeJSDataPack)
				.map(pack -> (VirtualKubeJSDataPack) pack)
				.mapToInt(pack -> {
							var path = KubeJSPaths.EXPORT.resolve(pack.packId() + ".zip");
							try {
								Files.deleteIfExists(path);
								try (var fs = FileSystems.newFileSystem(path, Map.of("create", true))) {
									pack.export(fs);
								}
								source.sendSuccess(() -> Component.literal("Successfully exported %s to %s".formatted(pack, path)).withStyle(ChatFormatting.GREEN), false);
								return 1;
							} catch (IOException e) {
								e.printStackTrace();
								source.sendFailure(Component.literal("Failed to export %s!".formatted(pack)).withStyle(style ->
										style.withColor(ChatFormatting.RED)
												.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage())))));
								return 0;
							}
						}
				).sum();
	}

	private static int outputRecipes(ServerPlayer player) {
		player.sendSystemMessage(Component.literal("WIP!"));
		return Command.SINGLE_SUCCESS;
	}

	private static int inputRecipes(ServerPlayer player) {
		player.sendSystemMessage(Component.literal("WIP!"));
		return Command.SINGLE_SUCCESS;
	}

	private static int checkRecipeConflicts(ServerPlayer player) {
		player.sendSystemMessage(Component.literal("WIP!"));
		return Command.SINGLE_SUCCESS;
	}

	private static <T> int listTagsFor(CommandSourceStack source, ResourceKey<Registry<T>> registry) throws CommandSyntaxException {
		var tags = allTags(source, registry);

		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("List of all Tags for " + registry.location() + ":"), false);
		source.sendSuccess(() -> Component.empty(), false);

		var size = tags.map(TagKey::location).map(tag -> Component.literal("- %s".formatted(tag)).withStyle(Style.EMPTY
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kubejs list_tag %s %s".formatted(registry.location(), tag)))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("[Show all entries for %s]".formatted(tag))))
		)).mapToLong(msg -> {
			source.sendSuccess(() -> msg, false);
			return 1;
		}).sum();

		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("Total: %d tags".formatted(size)), false);
		source.sendSuccess(() -> Component.literal("(Click on any of the above tags to list their contents!)"), false);
		source.sendSuccess(() -> Component.empty(), false);

		return Command.SINGLE_SUCCESS;
	}

	private static <T> int tagObjects(CommandSourceStack source, TagKey<T> key) throws CommandSyntaxException {
		var registry = source.registryAccess()
				.registry(key.registry())
				.orElseThrow(() -> NO_REGISTRY.create(key.registry().location()));

		var tag = registry.getTag(key);

		if (tag.isEmpty()) {
			source.sendFailure(Component.literal("Tag not found or empty!"));
			return 0;
		}
		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("Contents of #" + key.location() + " [" + key.registry().location() + "]:"), false);
		source.sendSuccess(() -> Component.empty(), false);

		var items = tag.get();

		for (var holder : items) {
			var id = holder.unwrap().map(o -> o.location().toString(), o -> o + " (unknown ID)");
			source.sendSuccess(() -> Component.literal("- " + id), false);
		}

		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("Total: " + items.size() + " elements"), false);
		source.sendSuccess(() -> Component.empty(), false);
		return Command.SINGLE_SUCCESS;
	}

	private static <T> int dumpRegistry(CommandSourceStack source, ResourceKey<Registry<T>> registry) throws CommandSyntaxException {
		var ids = source.registryAccess().registry(registry)
				.orElseThrow(() -> NO_REGISTRY.create(registry.location()))
				.holders();

		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("List of all entries for registry " + registry.location() + ":"), false);
		source.sendSuccess(() -> Component.empty(), false);

		var size = ids.map(holder -> {
			var id = holder.key().location();
			return Component.literal("- %s".formatted(id)).withStyle(Style.EMPTY
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("%s [%s]".formatted(holder.value(), holder.value().getClass().getName()))))
			);
		}).mapToLong(msg -> {
			source.sendSuccess(() -> msg, false);
			return 1;
		}).sum();

		source.sendSuccess(() -> Component.empty(), false);
		source.sendSuccess(() -> Component.literal("Total: %d entries".formatted(size)), false);
		source.sendSuccess(() -> Component.empty(), false);


		return 1;
	}

	private static int addStage(CommandSourceStack source, Collection<ServerPlayer> players, String stage) {
		for (var p : players) {
			if (p.kjs$getStages().add(stage)) {
				source.sendSuccess(() -> Component.literal("Added '" + stage + "' stage for " + p.getScoreboardName()), false);
			}
		}

		return 1;
	}

	private static int removeStage(CommandSourceStack source, Collection<ServerPlayer> players, String stage) {
		for (var p : players) {
			if (p.kjs$getStages().remove(stage)) {
				source.sendSuccess(() -> Component.literal("Removed '" + stage + "' stage for " + p.getScoreboardName()), false);
			}
		}

		return 1;
	}

	private static int clearStages(CommandSourceStack source, Collection<ServerPlayer> players) {
		for (var p : players) {
			if (p.kjs$getStages().clear()) {
				source.sendSuccess(() -> Component.literal("Cleared stages for " + p.getScoreboardName()), false);
			}
		}

		return 1;
	}

	private static int listStages(CommandSourceStack source, Collection<ServerPlayer> players) {
		for (var p : players) {
			source.sendSuccess(() -> Component.literal(p.getScoreboardName() + " stages:"), false);
			p.kjs$getStages().getAll().stream().sorted().forEach(s -> source.sendSuccess(() -> Component.literal("- " + s), false));
		}

		return 1;
	}

	private static int painter(CommandSourceStack source, Collection<ServerPlayer> players, CompoundTag object) {
		new PaintMessage(object).sendTo(players);
		return 1;
	}

	private static int generateTypings(CommandSourceStack source) {
		if (!source.getServer().isSingleplayer()) {
			source.sendFailure(Component.literal("You can only run this command in singleplayer!"));
			return 0;
		}

		KubeJS.PROXY.generateTypings(source);
		return 1;
	}

	private static int packmode(CommandSourceStack source, String packmode) {
		if (packmode.isEmpty()) {
			source.sendSuccess(() -> Component.literal("Current packmode: " + CommonProperties.get().packMode), false);
		} else {
			CommonProperties.get().setPackMode(packmode);
			source.sendSuccess(() -> Component.literal("Set packmode to: " + packmode), false);
		}

		return 1;
	}
}
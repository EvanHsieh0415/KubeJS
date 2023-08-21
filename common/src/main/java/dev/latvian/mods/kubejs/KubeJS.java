package dev.latvian.mods.kubejs;

import com.google.common.base.Stopwatch;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.utils.EnvExecutor;
import dev.latvian.mods.kubejs.bindings.event.StartupEvents;
import dev.latvian.mods.kubejs.block.KubeJSBlockEventHandler;
import dev.latvian.mods.kubejs.client.KubeJSClient;
import dev.latvian.mods.kubejs.entity.KubeJSEntityEventHandler;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.item.KubeJSItemEventHandler;
import dev.latvian.mods.kubejs.level.KubeJSWorldEventHandler;
import dev.latvian.mods.kubejs.net.KubeJSNet;
import dev.latvian.mods.kubejs.platform.MiscPlatformHelper;
import dev.latvian.mods.kubejs.player.KubeJSPlayerEventHandler;
import dev.latvian.mods.kubejs.recipe.KubeJSRecipeEventHandler;
import dev.latvian.mods.kubejs.recipe.schema.RecipeNamespace;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.registry.RegistryEventJS;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.*;
import dev.latvian.mods.kubejs.server.KubeJSServerEventHandler;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import dev.latvian.mods.kubejs.util.KubeJSBackgroundThread;
import dev.latvian.mods.kubejs.util.KubeJSPlugins;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;

public class KubeJS {
	public static final String MOD_ID = "kubejs";
	public static final String MOD_NAME = "KubeJS";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	public static final int MC_VERSION_NUMBER = 2001;
	public static final String MC_VERSION_STRING = "1.20.1";
	public static String QUERY;

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static KubeJS instance;
	private static Path gameDirectory;

	public static KubeJSCommon PROXY;

	private static ScriptManager startupScriptManager, clientScriptManager;

	public static ScriptManager getStartupScriptManager() {
		return startupScriptManager;
	}

	public static ScriptManager getClientScriptManager() {
		return clientScriptManager;
	}

	public static Mod thisMod;

	public KubeJS() throws Throwable {
		instance = this;
		gameDirectory = Platform.getGameFolder().normalize().toAbsolutePath();
		Locale.setDefault(Locale.US);

		if (Files.notExists(KubeJSPaths.README)) {
			try {
				Files.writeString(KubeJSPaths.README, """
						Find out more info on the website: https://kubejs.com/
										
						Directory information:
										
						assets - Acts as a resource pack, you can put any client resources in here, like textures, models, etc. Example: assets/kubejs/textures/item/test_item.png
						data - Acts as a datapack, you can put any server resources in here, like loot tables, functions, etc. Example: data/kubejs/loot_tables/blocks/test_block.json
										
						startup_scripts - Scripts that get loaded once during game startup - Used for adding items and other things that can only happen while the game is loading (Can be reloaded with /kubejs reload_startup_scripts, but it may not work!)
						server_scripts - Scripts that get loaded every time server resources reload - Used for modifying recipes, tags, loot tables, and handling server events (Can be reloaded with /reload)
						client_scripts - Scripts that get loaded every time client resources reload - Used for JEI events, tooltips and other client side things (Can be reloaded with F3+T)
										
						config - KubeJS config storage. This is also the only directory that scripts can access other than world directory
						exported - Data dumps like texture atlases end up here
										
						You can find type-specific logs in logs/kubejs/ directory
						""".trim()
				);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		PROXY = EnvExecutor.getEnvSpecific(() -> KubeJSClient::new, () -> KubeJSCommon::new);

		if (!MiscPlatformHelper.get().isDataGen()) {
			new KubeJSBackgroundThread().start();
		}

		var pluginTimer = Stopwatch.createStarted();
		LOGGER.info("Looking for KubeJS plugins...");
		thisMod = Platform.getMod(MOD_ID);
		var allMods = new ArrayList<>(Platform.getMods());
		allMods.remove(thisMod);
		allMods.add(0, thisMod);
		KubeJSPlugins.load(allMods);
		LOGGER.info("Done in " + pluginTimer.stop());

		KubeJSPlugins.forEachPlugin(KubeJSPlugin::init);
		KubeJSPlugins.forEachPlugin(KubeJSPlugin::registerEvents);

		startupScriptManager = new ScriptManager(ScriptType.STARTUP, KubeJSPaths.STARTUP_SCRIPTS);
		clientScriptManager = new ScriptManager(ScriptType.CLIENT, KubeJSPaths.CLIENT_SCRIPTS);

		startupScriptManager.reload(null);

		KubeJSPlugins.forEachPlugin(KubeJSPlugin::initStartup);

		KubeJSWorldEventHandler.init();
		KubeJSPlayerEventHandler.init();
		KubeJSEntityEventHandler.init();
		KubeJSBlockEventHandler.init();
		KubeJSItemEventHandler.init();
		KubeJSServerEventHandler.init();
		KubeJSRecipeEventHandler.init();

		PROXY.init();

		for (var extraId : StartupEvents.REGISTRY.findUniqueExtraIds(ScriptType.STARTUP)) {
			if (extraId instanceof ResourceKey<?> key) {
				var info = RegistryInfo.of(UtilsJS.cast(key));
				var event = new RegistryEventJS(info);
				StartupEvents.REGISTRY.post(event, key);
				event.created.forEach(BuilderBase::createAdditionalObjects);
			}
		}
	}

	public static void loadScripts(ScriptPack pack, Path dir, String path) {
		if (!path.isEmpty() && !path.endsWith("/")) {
			path += "/";
		}

		final var pathPrefix = path;

		try {
			for (var file : Files.walk(dir, 10, FileVisitOption.FOLLOW_LINKS).filter(Files::isRegularFile).toList()) {
				var fileName = dir.relativize(file).toString().replace(File.separatorChar, '/');

				if (fileName.endsWith(".js")) {
					pack.info.scripts.add(new ScriptFileInfo(pack.info, pathPrefix + fileName));
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static String appendModId(String id) {
		return id.indexOf(':') == -1 ? (MOD_ID + ":" + id) : id;
	}

	public static Path getGameDirectory() {
		return gameDirectory;
	}

	public static Path verifyFilePath(Path path) throws IOException {
		if (!path.normalize().toAbsolutePath().startsWith(gameDirectory)) {
			throw new IOException("You can't access files outside Minecraft directory!");
		}

		return path;
	}

	public void setup() {
		KubeJSNet.init();
		StartupEvents.INIT.post(ScriptType.STARTUP, new StartupEventJS());
		// KubeJSRegistries.chunkGenerators().register(new ResourceLocation(KubeJS.MOD_ID, "flat"), () -> KJSFlatLevelSource.CODEC);
	}

	public void loadComplete() {
		KubeJSPlugins.forEachPlugin(KubeJSPlugin::afterInit);
		ScriptsLoadedEvent.EVENT.invoker().run();
		StartupEvents.POST_INIT.post(ScriptType.STARTUP, new StartupEventJS());
		UtilsJS.postModificationEvents();
		RecipeNamespace.getAll();

		if (!ScriptType.STARTUP.errors.isEmpty()) {
			var list = new ArrayList<String>();
			list.add("Startup script errors:");

			var lines = ScriptType.STARTUP.errors.toArray(new String[0]);

			for (int i = 0; i < lines.length; i++) {
				list.add((i + 1) + ") " + lines[i]);
			}

			LOGGER.error(String.join("\n", list));

			ConsoleJS.STARTUP.flush(true);
			throw new RuntimeException("There were KubeJS startup script syntax errors! See logs/kubejs/startup.log for more info");
		}

		QUERY = "source=kubejs&mc=" + MC_VERSION_NUMBER + "&loader=" + PlatformWrapper.getName() + "&v=" + URLEncoder.encode(thisMod.getVersion(), StandardCharsets.UTF_8);

		var updater = new Thread(() -> {
			try {
				var response = HttpClient.newBuilder()
						.followRedirects(HttpClient.Redirect.ALWAYS)
						.connectTimeout(Duration.ofSeconds(5L))
						.build()
						.send(HttpRequest.newBuilder().uri(URI.create("https://kubejs.com/update-check?" + QUERY)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() == 200) {
					var body = response.body().trim();

					if (!body.isEmpty()) {
						ConsoleJS.STARTUP.info("Update available: " + body);
					}
				}
			} catch (Exception ignored) {
			}
		});

		updater.setDaemon(true);
		updater.start();
	}
}
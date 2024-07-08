package dev.latvian.mods.kubejs.registry;

import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class RegistryKubeEvent<T> implements KubeStartupEvent, AdditionalObjectRegistry {
	private final ResourceKey<Registry<T>> registryKey;
	private final BuilderTypeRegistryHandler.Info<T> builderInfo;
	public final List<BuilderBase<? extends T>> created;

	public RegistryKubeEvent(ResourceKey<Registry<T>> registryKey) {
		this.registryKey = registryKey;
		this.builderInfo = BuilderTypeRegistryHandler.info(registryKey);
		this.created = new LinkedList<>();
	}

	public BuilderBase<? extends T> create(Context cx, String id, String type) {
		var sourceLine = SourceLine.of(cx);
		var t = builderInfo.namedType(type);

		if (t == null) {
			throw new KubeRuntimeException("Unknown type '" + type + "' for object '" + id + "'!").source(sourceLine);
		}

		var b = t.factory().createBuilder(ID.kjs(id));

		if (b == null) {
			throw new KubeRuntimeException("Unknown type '" + type + "' for object '" + id + "'!").source(sourceLine);
		} else if (builderInfo.directCodec() != null) {
			throw new KubeRuntimeException("Type '" + type + "' for object '" + id + "' is a datapack registry type!").source(sourceLine);
		} else {
			b.sourceLine = sourceLine;
			b.registryKey = registryKey;
			addBuilder(b);
			created.add(b);
		}

		return b;
	}

	public BuilderBase<? extends T> create(Context cx, String id) {
		var sourceLine = SourceLine.of(cx);
		var t = builderInfo.defaultType();

		if (t == null) {
			throw new KubeRuntimeException("Registry '" + registryKey.location() + "' doesn't have a default type registered!").source(sourceLine);
		}

		var b = t.factory().createBuilder(ID.kjs(id));

		if (b == null) {
			throw new KubeRuntimeException("Unknown type '" + t.type() + "' for object '" + id + "'!").source(sourceLine);
		} else {
			b.sourceLine = sourceLine;
			b.registryKey = registryKey;
			addBuilder(b);
			created.add(b);
		}

		return b;
	}

	public CustomBuilderObject createCustom(Context cx, String id, Supplier<Object> object) {
		var sourceLine = SourceLine.of(cx);

		if (object == null) {
			throw new KubeRuntimeException("Tried to register a null object with id: " + id).source(sourceLine);
		}

		var rl = ID.kjs(id);
		var b = new CustomBuilderObject(rl, object);
		b.sourceLine = sourceLine;
		b.registryKey = registryKey;
		addBuilder(b);
		created.add(b);
		return b;
	}

	@Override
	public void afterPosted(EventResult result) {
		for (var c : created) {
			c.createAdditionalObjects(this);
		}
	}

	@Override
	public <R> void add(ResourceKey<Registry<R>> registry, BuilderBase<? extends R> builder) {
		builder.registryKey = (ResourceKey) registry;
		addBuilder(builder);
	}

	private <R> void addBuilder(BuilderBase<? extends R> builder) {
		if (builder == null) {
			throw new IllegalArgumentException("Can't add null builder in registry '" + builder.registryKey.location() + "'!");
		}

		if (DevProperties.get().logRegistryEventObjects) {
			ConsoleJS.STARTUP.info("~ " + builder.registryKey.location() + " | " + builder.id);
		}

		var objStorage = RegistryObjectStorage.of(builder.registryKey);

		if (objStorage.objects.containsKey(builder.id)) {
			throw new IllegalArgumentException("Duplicate key '" + builder.id + "' in registry '" + builder.registryKey.location() + "'!");
		}

		objStorage.objects.put(builder.id, (BuilderBase) builder);
		RegistryObjectStorage.ALL_BUILDERS.add(builder);

		// registry.deferredRegister.register()
	}
}
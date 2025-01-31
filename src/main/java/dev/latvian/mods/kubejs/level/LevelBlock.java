package dev.latvian.mods.kubejs.level;

import dev.latvian.mods.kubejs.core.BlockProviderKJS;
import dev.latvian.mods.kubejs.core.InventoryKJS;
import dev.latvian.mods.kubejs.player.EntityArrayList;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.BlockWrapper;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RemapPrefixForJS("kjs$")
public interface LevelBlock extends BlockProviderKJS {
	Level getLevel();

	BlockPos getPos();

	@Override
	default Block kjs$getBlock() {
		return getBlockState().getBlock();
	}

	@HideFromJS
	default LevelBlock cache(BlockState state) {
		return this;
	}

	@HideFromJS
	default LevelBlock cache(BlockEntity entity) {
		return this;
	}

	default ResourceKey<Level> getDimensionKey() {
		return getLevel().dimension();
	}

	default ResourceLocation getDimension() {
		return getDimensionKey().location();
	}

	default int getX() {
		return getPos().getX();
	}

	default int getY() {
		return getPos().getY();
	}

	default int getZ() {
		return getPos().getZ();
	}

	default double getCenterX() {
		return getX() + 0.5D;
	}

	default double getCenterY() {
		return getY() + 0.5D;
	}

	default double getCenterZ() {
		return getZ() + 0.5D;
	}

	default LevelBlock offset(Direction f, int d) {
		return getLevel().kjs$getBlock(getPos().relative(f, d));
	}

	default LevelBlock offset(Direction f) {
		return offset(f, 1);
	}

	default LevelBlock offset(int x, int y, int z) {
		return getLevel().kjs$getBlock(getPos().offset(x, y, z));
	}

	default LevelBlock getDown() {
		return offset(Direction.DOWN);
	}

	default LevelBlock getUp() {
		return offset(Direction.UP);
	}

	default LevelBlock getNorth() {
		return offset(Direction.NORTH);
	}

	default LevelBlock getSouth() {
		return offset(Direction.SOUTH);
	}

	default LevelBlock getWest() {
		return offset(Direction.WEST);
	}

	default LevelBlock getEast() {
		return offset(Direction.EAST);
	}

	default BlockState getBlockState() {
		return getLevel().getBlockState(getPos());
	}

	default void setBlockState(BlockState state, int flags) {
		getLevel().setBlock(getPos(), state, flags);
	}

	default void setBlockState(BlockState state) {
		setBlockState(state, Block.UPDATE_ALL);
	}

	default void set(Block block, Map<?, ?> properties, int flags) {
		var state = block.defaultBlockState();

		if (!properties.isEmpty() && state.getBlock() != Blocks.AIR) {
			state = BlockWrapper.withProperties(state, properties);

			var pmap = new HashMap<String, Property<?>>();

			for (var property : state.getProperties()) {
				pmap.put(property.getName(), property);
			}

			for (var entry : properties.entrySet()) {
				var property = pmap.get(String.valueOf(entry.getKey()));

				if (property != null) {
					state = state.setValue(property, Cast.to(property.getValue(String.valueOf(entry.getValue())).orElseThrow()));
				}
			}
		}

		setBlockState(state, flags);
	}

	default void set(Block block, Map<?, ?> properties) {
		set(block, properties, 3);
	}

	default void set(Block block) {
		set(block, Collections.emptyMap());
	}

	default Map<String, String> getProperties() {
		Map<String, String> map = new HashMap<>();
		var state = getBlockState();

		for (Property property : state.getProperties()) {
			map.put(property.getName(), property.getName(state.getValue(property)));
		}

		return map;
	}

	@Nullable
	default BlockEntity getEntity() {
		return getLevel().getBlockEntity(getPos());
	}

	default String getEntityId() {
		var entity = getEntity();
		return entity == null ? "minecraft:air" : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString();
	}

	@Nullable
	default CompoundTag getEntityData() {
		var entity = getEntity();

		if (entity != null) {
			return entity.saveWithoutMetadata(getLevel().registryAccess());
		}

		return null;
	}

	default void setEntityData(@Nullable CompoundTag tag) {
		if (tag != null) {
			var entity = getEntity();

			if (entity != null) {
				entity.loadWithComponents(tag, getLevel().registryAccess());
			}
		}
	}

	default void mergeEntityData(@Nullable CompoundTag tag) {
		var t = getEntityData();

		if (t == null) {
			setEntityData(tag);
		} else if (tag != null && !tag.isEmpty()) {
			for (var s : tag.getAllKeys()) {
				t.put(s, tag.get(s));
			}
		}

		setEntityData(t);
	}

	default int getLight() {
		return getLevel().getMaxLocalRawBrightness(getPos());
	}

	default int getSkyLight() {
		return getLevel().getBrightness(LightLayer.SKY, getPos()) - getLevel().getSkyDarken();
	}

	default int getBlockLight() {
		return getLevel().getBrightness(LightLayer.BLOCK, getPos());
	}

	default boolean getCanSeeSky() {
		return getLevel().canSeeSky(getPos());
	}

	default boolean canSeeSkyFromBelowWater() {
		return getLevel().canSeeSkyFromBelowWater(getPos());
	}

	default Explosion explode(ExplosionProperties properties) {
		return getLevel().kjs$explode(getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D, properties);
	}

	@Nullable
	default Entity createEntity(EntityType<?> type) {
		var entity = getLevel().kjs$createEntity(type);

		if (entity != null) {
			entity.kjs$setPosition(this);
		}

		return entity;
	}

	default void spawnLightning(boolean effectOnly, @Nullable ServerPlayer player) {
		getLevel().kjs$spawnLightning(getCenterX(), getCenterY(), getCenterZ(), effectOnly, player);
	}

	default void spawnLightning(boolean effectOnly) {
		spawnLightning(effectOnly, null);
	}

	default void spawnLightning() {
		spawnLightning(false);
	}

	default void spawnFireworks(Fireworks fireworks, int lifetime) {
		getLevel().kjs$spawnFireworks(getCenterX(), getCenterY(), getCenterZ(), fireworks, lifetime);
	}

	@Nullable
	default InventoryKJS getInventory() {
		return getInventory(Direction.UP);
	}

	@Nullable
	default InventoryKJS getInventory(Direction facing) {
		var entity = getEntity();

		if (entity != null) {
			var c = getLevel().getCapability(Capabilities.ItemHandler.BLOCK, getPos(), getBlockState(), getEntity(), facing);

			if (c instanceof InventoryKJS inv) {
				return inv;
			} else if (entity instanceof InventoryKJS inv) {
				return inv;
			}
		}

		return null;
	}

	default ItemStack getItem() {
		var state = getBlockState();
		return state.getBlock().getCloneItemStack(getLevel(), getPos(), state);
	}

	default List<ItemStack> getDrops() {
		return getDrops(null, ItemStack.EMPTY);
	}

	default List<ItemStack> getDrops(@Nullable Entity entity, ItemStack heldItem) {
		if (getLevel() instanceof ServerLevel s) {
			return Block.getDrops(getBlockState(), s, getPos(), getEntity(), entity, heldItem);
		}

		return List.of();
	}

	default void popItem(ItemStack item) {
		Block.popResource(getLevel(), getPos(), item);
	}

	default void popItemFromFace(ItemStack item, Direction dir) {
		Block.popResourceFromFace(getLevel(), getPos(), dir, item);
	}

	default EntityArrayList getPlayersInRadius(double radius) {
		var list = new EntityArrayList(1);

		double cx = getCenterX();
		double cy = getCenterY();
		double cz = getCenterZ();

		for (var entity : getLevel().getEntities((Entity) null, new AABB(cx - 0.5D - radius, cy - 0.5D - radius, cz - 0.5D - radius, cx + 0.5D + radius, cy + 0.5D + radius, cz + 0.5D + radius), EntityArrayList.ALWAYS_TRUE_PREDICATE)) {
			if (entity.distanceToSqr(cx, cy, cz) <= radius * radius && entity instanceof Player p && !p.isFakePlayer()) {
				list.add(p);
			}
		}

		return list;
	}

	default EntityArrayList getPlayersInRadius() {
		return getPlayersInRadius(8D);
	}

	default ResourceLocation getBiomeId() {
		var k = getLevel().getBiome(getPos()).getKey();
		return k == null ? Biomes.PLAINS.location() : k.location();
	}

	default String toBlockStateString() {
		var id = kjs$getId();
		var properties = getProperties();

		if (properties.isEmpty()) {
			return id;
		}

		var builder = new StringBuilder(id);
		builder.append('[');

		var first = true;

		for (var entry : properties.entrySet()) {
			if (first) {
				first = false;
			} else {
				builder.append(',');
			}

			builder.append(entry.getKey());
			builder.append('=');
			builder.append(entry.getValue());
		}

		builder.append(']');
		return builder.toString();
	}
}

package dev.latvian.mods.kubejs.platform.forge;

import dev.latvian.mods.kubejs.core.InventoryKJS;
import dev.latvian.mods.kubejs.platform.LevelPlatformHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class LevelForgeHelper implements LevelPlatformHelper {
	@Override
	@Nullable
	public InventoryKJS getInventoryFromBlockEntity(BlockEntity tileEntity, Direction facing) {
		var handler = tileEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, facing).orElse(null);

		if (handler instanceof InventoryKJS inv) {
			return inv;
		}

		return null;
	}

	@Override
	public boolean areCapsCompatible(ItemStack a, ItemStack b) {
		return a.areCapsCompatible(b);
	}

	@Override
	public double getReachDistance(LivingEntity livingEntity) {
		return livingEntity.getAttribute(ForgeMod.ENTITY_REACH.get()).getValue();
	}
}

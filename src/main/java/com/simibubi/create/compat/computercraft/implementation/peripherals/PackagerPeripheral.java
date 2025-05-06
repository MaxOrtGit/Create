package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

public class PackagerPeripheral extends SyncedPeripheral<PackagerBlockEntity> {

	public PackagerPeripheral(PackagerBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void attach(@NotNull IComputerAccess computer) {
		super.attach(computer);
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}

	@Override
	public void detach(@NotNull IComputerAccess computer) {
		super.detach(computer);
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}

	@LuaFunction(mainThread = true)
	public final boolean makePackage() {
		if (!blockEntity.heldBox.isEmpty())
			return false;
		blockEntity.activate(); // activate() doesn't return a value so i'm walking around it
		if (blockEntity.heldBox.isEmpty())
			return false;
		return true;
	}
  
	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> list() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAvailableItems().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getBasicDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

  @LuaFunction(mainThread = true)
  public final Map<String, ?> getItemDetail(int slot) throws LuaException {
    List<BigItemStack> stacks = blockEntity.getAvailableItems().getStacks();
    if (slot < 1) { // All positive can technically be valid
      throw new LuaException("Slot out of range (1 or greater)");
    }

    if (slot > stacks.size()) {
      return null;
    }

    BigItemStack entry = stacks.get(slot - 1);
    Map<String, Object> details = new HashMap<>(
        VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
    details.put("count", entry.count);
    return details;
  }
  
	@LuaFunction(mainThread = true)
	public final String getAddress() {
		return blockEntity.signBasedAddress;
	}

	@LuaFunction(mainThread = true)
	public final void setAddress(Optional<String> argument) {
		if (argument.isPresent()) {
			blockEntity.customComputerAddress = argument.get();
			blockEntity.signBasedAddress = argument.get();
			blockEntity.hasCustomComputerAddress = true;
		} else {
			blockEntity.customComputerAddress = "";
			blockEntity.hasCustomComputerAddress = false;
		}
	}

  @LuaFunction(mainThread = true)
  public final PackageLuaObject getPackage() {
    ItemStack box = blockEntity.heldBox;
    if (box.isEmpty())
      return null;

    return new PackageLuaObject(blockEntity, box);
  }

	@NotNull
	@Override
	public String getType() {
		return "Create_Packager";
	}

}
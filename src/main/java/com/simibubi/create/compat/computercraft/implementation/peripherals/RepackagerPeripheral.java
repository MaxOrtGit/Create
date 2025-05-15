package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.Optional;

import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.events.PackageReceiveEvent;
import com.simibubi.create.compat.computercraft.events.PackageSendEvent;
import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;

import dan200.computercraft.api.peripheral.IComputerAccess;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;
import dan200.computercraft.api.lua.LuaFunction;

public class RepackagerPeripheral extends SyncedPeripheral<RepackagerBlockEntity> {

	public RepackagerPeripheral(RepackagerBlockEntity blockEntity) {
		super(blockEntity);
	}

	@Override
	public void onFirstAttach() {
		super.onFirstAttach();
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}

	@Override
	public void onLastDetach() {
		super.onLastDetach();
		// Ephemeral nature of address, should not be set on load until a computer
		// explicitly calls setAddress again on the BE.
		blockEntity.hasCustomComputerAddress = false;
	}
  
  @Override
	public void prepareComputerEvent(@NotNull ComputerEvent event) {
		if (event instanceof PackageReceiveEvent pre) {
      // block entity is null so package is always valid
			queueEvent("package_receive", new PackageLuaObject(null, pre.box));
		}
    else if (event instanceof PackageSendEvent pse) {
      queueEvent("package_send", new PackageLuaObject(pse.blockEntity, pse.box));
    }
	}
  
	@LuaFunction(mainThread = true)
	public final boolean makePackage() {
		if (!blockEntity.heldBox.isEmpty())
			return false;
		blockEntity.activate();
		if (blockEntity.heldBox.isEmpty())
			return false;
		return true;
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
		return "Create_Repackager";
	}

}

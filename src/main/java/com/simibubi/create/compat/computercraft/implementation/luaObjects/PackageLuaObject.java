package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageOrderLuaObject;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class PackageLuaObject {

  private PackagerBlockEntity blockEntity;
  private ItemStack box;

  public PackageLuaObject(PackagerBlockEntity blockEntity, ItemStack box) {
    this.blockEntity = blockEntity;
    this.box = box;
  }

  @LuaFunction(mainThread = true)
  public final boolean isValid() {
    return !blockEntity.heldBox.isEmpty() && blockEntity.heldBox == box;
  }

  public final void checkValid() throws LuaException {
    if (!isValid())
      throw new LuaException("Invalid package object");
  }

  @LuaFunction(mainThread = true)
  public final String getAddress() throws LuaException {
    checkValid();
    return PackageItem.getAddress(box);
  }

	@LuaFunction(mainThread = true)
	public final void setAddress(String argument) throws LuaException {
    checkValid();
    PackageItem.addAddress(box, argument);
	}
  
  @LuaFunction(mainThread = true)
  public final Integer getOrderID() throws LuaException {
		checkValid();
    return PackageItem.getOrderId(box);
  }
  
	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> list() throws LuaException {
		checkValid();

		ItemStackHandler results = PackageItem.getContents(box);
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		for (int i = 0; i < results.getSlots(); i++) {
			ItemStack stack = results.getStackInSlot(i);
			if (!stack.isEmpty()) {
				Map<String, Object> details = new HashMap<>(
          VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
				result.put(i + 1, details); // +1 because lua
			}
		}
		return result;
	}

  @LuaFunction(mainThread = true)
  public final Map<String, ?> getItemDetail(int slot) throws LuaException {
    checkValid();

    if (slot < 1 || slot > PackageItem.SLOTS) {
      throw new LuaException("Slot out of range (between 1 and " + PackageItem.SLOTS + ")");
    }

    ItemStackHandler results = PackageItem.getContents(box);
    ItemStack stack = results.getStackInSlot(slot - 1);
    if (stack.isEmpty())
      return null;

    return new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
  }

  @LuaFunction(mainThread = true)
  public final PackageOrderLuaObject getOrderContext() throws LuaException {
    checkValid();

    PackageOrderWithCrafts context = PackageItem.getOrderContext(box);
    if (context == null)
      return null;

    return new PackageOrderLuaObject(this, context);
  }

}

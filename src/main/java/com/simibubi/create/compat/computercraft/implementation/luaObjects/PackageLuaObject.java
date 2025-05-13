package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class PackageLuaObject implements LuaComparable {

  public PackagerBlockEntity blockEntity;
  public ItemStack box;

  public PackageLuaObject(PackagerBlockEntity blockEntity, ItemStack box) {
    this.blockEntity = blockEntity;
    this.box = box;
  }

  @LuaFunction(mainThread = true)
  public final boolean isValid() {
    return blockEntity == null || (!blockEntity.heldBox.isEmpty() && blockEntity.heldBox == box);
  }

  public final void checkValid() throws LuaException {
    if (!isValid())
      throw new LuaException("Invalid package object");
  }

  public final void checkEditable() throws LuaException {
    if (blockEntity == null)
      throw new LuaException("Package is not editable");
  }

  @LuaFunction(mainThread = true)
  public final String getAddress() throws LuaException {
    checkValid();
    return PackageItem.getAddress(box);
  }

	@LuaFunction(mainThread = true)
	public final void setAddress(String argument) throws LuaException {
    checkValid();
    checkEditable();
    PackageItem.addAddress(box, argument);
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
  
  public boolean hasOrderData() {
    return PackageItem.hasFragmentData(box);
  }

  @LuaFunction(mainThread = true)
  public final PackageOrderLuaObject getOrderData() throws LuaException {
    checkValid();

    if (!hasOrderData())
      return null;

    return new PackageOrderLuaObject(this);
  }
  
  public final List<LuaItemStack> getLuaItemStacks() {
		ItemStackHandler results = PackageItem.getContents(box);
		List<LuaItemStack> result = new ArrayList<>();

		for (int i = 0; i < results.getSlots(); i++) {
			ItemStack stack = results.getStackInSlot(i);
      if (!stack.isEmpty()) {
        result.add(new LuaItemStack(stack));
      }
    }
    
    return result;
	}

  @Override
  public Map<?,?> getTableRepresentation() {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put("address", getAddress());
      // Lazy getter so we don't need to get the contents if we don't need to
      map.put("contents", getLuaItemStacks());

      if (hasOrderData())
        map.put("orderData", getOrderData());
      return map;

    } catch (LuaException e) {
      return null; // Should never happen
    }
  }

}

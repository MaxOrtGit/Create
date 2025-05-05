package com.simibubi.create.compat.computercraft.implementation.peripherals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraftforge.items.ItemStackHandler;

import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.content.logistics.BigItemStack;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
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
	public final int getItemCount() {
		return blockEntity.getAvailableItems().getTotalCount();
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
	public final String getAddress() throws LuaException {
		return blockEntity.signBasedAddress;
	}

  @LuaFunction(mainThread = true)
  public final String getPackageAddress() {
    if (!blockEntity.heldBox.isEmpty())
      return PackageItem.getAddress(blockEntity.heldBox);
    return null;
  }

	@LuaFunction(mainThread = true)
	public final boolean setPackageAddress(Optional<String> argument) {
		if (!blockEntity.heldBox.isEmpty()) {
			if (argument.isPresent()) {
				PackageItem.addAddress(blockEntity.heldBox, argument.get());
			} else {
				PackageItem.addAddress(blockEntity.heldBox, "");
			}
			return true;
		}
		return false;
	}
  
  @LuaFunction(mainThread = true)
  public final Integer getPackageOrderID() {
		if (!blockEntity.heldBox.isEmpty()) {
			return PackageItem.getOrderId(blockEntity.heldBox);
		}
    return null;
  }
  
	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> getPackageList() {
		ItemStack box = blockEntity.heldBox;
		if (box.isEmpty())
			return null;

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
  public final Map<String, ?> getPackageItemDetail(int slot) throws LuaException {
    if (slot < 1 || slot > PackageItem.SLOTS) {
      throw new LuaException("Slot out of range (between 1 and " + PackageItem.SLOTS + ")");
    }

    ItemStack box = blockEntity.heldBox;
    if (box.isEmpty())
      return null;

    ItemStackHandler results = PackageItem.getContents(box);
    ItemStack stack = results.getStackInSlot(slot - 1);
    if (stack.isEmpty())
      return null;

    return new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(stack));
  }

  @LuaFunction(mainThread = true)
  public final CreateLuaTable getPackageOrderedStacksList()
  {
    ItemStack box = blockEntity.heldBox;
    if (box.isEmpty())
      return null;
      
    PackageOrderWithCrafts context = PackageItem.getOrderContext(box);
    if (context == null)
      return null;
    
    CreateLuaTable stacks = new CreateLuaTable();
    
    int i = 0;
    for (BigItemStack bis : context.stacks()) {
      i++;
      Map<String, Object> details = new HashMap<>(
          VanillaDetailRegistries.ITEM_STACK.getBasicDetails(bis.stack));
      details.put("count", bis.count); // Use bis count
      stacks.put(i, details);
    }

    return stacks;
  }

  @LuaFunction(mainThread = true)
  public final CreateLuaTable getPackageOrderedStacksItemDetail(int slot) throws LuaException {
    if (slot < 1) { // All positive can technically be valid
      throw new LuaException("Slot out of range (1 or greater)");
    }

    ItemStack box = blockEntity.heldBox;
    if (box.isEmpty())
      return null;

    PackageOrderWithCrafts context = PackageItem.getOrderContext(box);
    if (context == null)
      return null;

    List<BigItemStack> stacks = context.stacks();
    if (slot > stacks.size()) {
      return null;
    }

    BigItemStack bis = stacks.get(slot - 1);
    Map<String, Object> details = new HashMap<>(
        VanillaDetailRegistries.ITEM_STACK.getDetails(bis.stack));
    details.put("count", bis.count); // Use bis count

    return new CreateLuaTable(details);
  }

  @LuaFunction(mainThread = true)
  public final CreateLuaTable getPackageOrderedCrafts() {
    ItemStack box = blockEntity.heldBox;
    if (box.isEmpty())
      return null;

    PackageOrderWithCrafts context = PackageItem.getOrderContext(box);
    if (context == null)
      return null;

    CreateLuaTable crafts = new CreateLuaTable();
    
    int i = 0;
    for (CraftingEntry entry : context.orderedCrafts()) {
      CreateLuaTable craft = new CreateLuaTable();
      craft.put("count", entry.count());

      CreateLuaTable recipe = new CreateLuaTable();
      int j = 0;
      for (BigItemStack bis : entry.pattern().stacks()) {
        j++;
        // Not sure if this is the best way to get the in game ID for the item, if there is please let me know
        String name = VanillaDetailRegistries.ITEM_STACK.getBasicDetails(bis.stack).get("name").toString();
        recipe.put(j, name.equals("minecraft:air") ? null : name);
      }
      i++;
      craft.put("recipe", recipe);
      crafts.put(i, craft);
    }

    return crafts;
  }



	@NotNull
	@Override
	public String getType() {
		return "Create_Packager";
	}

}

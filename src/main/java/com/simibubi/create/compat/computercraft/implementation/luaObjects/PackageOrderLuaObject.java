package com.simibubi.create.compat.computercraft.implementation.luaObjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.compat.computercraft.implementation.luaObjects.PackageLuaObject;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public class PackageOrderLuaObject {
  
  private PackageLuaObject packageLuaObject;
  private PackageOrderWithCrafts context;

  public PackageOrderLuaObject(PackageLuaObject packageLuaObject, PackageOrderWithCrafts context) {
    this.packageLuaObject = packageLuaObject;
    this.context = context;
  }
  
  @LuaFunction(mainThread = true)
  public final CreateLuaTable list() throws LuaException {
    packageLuaObject.checkValid();
    
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
  public final CreateLuaTable getItemDetail(int slot) throws LuaException {
    packageLuaObject.checkValid();
    
    if (slot < 1) { // All positive can technically be valid
      throw new LuaException("Slot out of range (1 or greater)");
    }

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
  public final CreateLuaTable getCrafts() throws LuaException {
    packageLuaObject.checkValid();
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

}

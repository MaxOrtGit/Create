package com.simibubi.create.compat.computercraft.events;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;

import net.minecraft.world.item.ItemStack;

public class PackageReceiveEvent implements ComputerEvent {
  
  public PackagerBlockEntity blockEntity;
  public ItemStack box;

  public PackageReceiveEvent(PackagerBlockEntity blockEntity, ItemStack box) {
    this.blockEntity = blockEntity;
    this.box = box;
  }

}

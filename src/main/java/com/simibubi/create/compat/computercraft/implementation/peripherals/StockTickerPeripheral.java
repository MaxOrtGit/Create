package com.simibubi.create.compat.computercraft.implementation.peripherals;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.compat.computercraft.implementation.ComputerUtil;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

import org.jetbrains.annotations.Nullable;

public class StockTickerPeripheral extends SyncedPeripheral<StockTickerBlockEntity> {

	// private final ScrollValueBehaviour targetSpeed;

	public StockTickerPeripheral(StockTickerBlockEntity blockEntity) {
		super(blockEntity);
		// this.targetSpeed = targetSpeed;
	}

	@LuaFunction(mainThread = true)
	public final int getItemCount() {
		return blockEntity.getAccurateSummary().getTotalCount();
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> list() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getBasicDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<Integer, Map<String, ?>> listDetailed() {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		int i = 0;
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
			i++;
			Map<String, Object> details = new HashMap<>(
					VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
			details.put("count", entry.count);
			result.put(i, details);
		}
		return result;
	}

	/*
	 * for every item in the netowrk, this will compare that item to the CC args
	 * filter, a table that looks something like this:
	 * {
	 * name = "minecraft:jungle_log",
	 * tags = {
	 * ["minecraft:logs"] = true
	 * },
	 * count = 5
	 * },
	 * and the second optional String arg which is the address:
	 * "home_address"
	 * (default value "")
	 *
	 * It then adds items that match the name if provided, nbt if provided, have all
	 * of the tags if provided, has all the enchants if provided and
	 * stops looking after adding items equal to count or finishing
	 * going through the summary.
	 * filter of {} requests all items from the network trollface.jpeg
	 */
	@LuaFunction(mainThread = true)
	public final int requestFiltered(IArguments arguments) throws LuaException {
		if (!(arguments.get(0) instanceof Map<?, ?> filterTable))
			throw new LuaException("Filter must be a table");

    for (Object key : filterTable.keySet())
      if (!(key instanceof String)) 
        throw new LuaException("Filter keys must be strings");

		@SuppressWarnings("unchecked")
    Map<String, Object> filter = (Map<String, Object>) filterTable;
    
    int itemsRequested = Integer.MAX_VALUE;
    if (arguments.get(2) instanceof Number) {
      itemsRequested = ((Number) arguments.get(2)).intValue();
      if (itemsRequested < 1)
        throw new LuaException("Count must be a positive number or nil for all");
    }
    int itemsSent = 0;
    
		String address;
		// Computercraft has forced my hand to make this dollar store filter algo
		List<BigItemStack> validItems = new ArrayList<>();
		for (BigItemStack entry : blockEntity.getAccurateSummary().getStacks()) {
      int foundItems = ComputerUtil.bigItemStackToLuaTableFilter(entry, filter);
			if (foundItems > 0) {
				int toTake = Math.min(foundItems, itemsRequested);
        itemsRequested -= toTake;
        itemsSent += toTake;
        entry.count = toTake;
				validItems.add(entry);
			}
      if (itemsRequested <= 0) break;
		}
		if (arguments.get(1) instanceof String)
			address = arguments.getString(1);
		else
			address = "";

		PackageOrder order = new PackageOrder(validItems);
		blockEntity.broadcastPackageRequest(RequestType.RESTOCK, order, null, address);

		/*
		 * CatnipServices.NETWORK
		 * .sendToServer(new PackageOrderRequestPacket(blockEntity.getBlockPos(), new
		 * PackageOrder(itemsToOrder),
		 * address, false, new PackageOrder(stacks);
		 */
		return itemsSent;
	}

	@LuaFunction(mainThread = true)
	public Map<Integer, Map<String, ?>> listPaymentInventory() {
		return ComputerUtil.list(blockEntity.getReceivedPaymentsHandler());
	}

	@NotNull
	@Override
	public String getType() {
		return "Create_StockTicker";
	}

	@Override
	public @Nullable Object getTarget() {
		return blockEntity.getReceivedPaymentsHandler();
	}
}

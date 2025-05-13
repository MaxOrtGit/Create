package com.simibubi.create.compat.computercraft.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.simibubi.create.compat.computercraft.implementation.luaObjects.LuaComparable;
import com.simibubi.create.content.logistics.BigItemStack;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import net.minecraftforge.items.IItemHandler;

public class ComputerUtil {

  public static int bigItemStackToLuaTableFilter(BigItemStack entry, Map<String,Object> filter) throws LuaException {

    Map<String,Object> details =
      new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(entry.stack));
    //details.put("count", entry.count);

    // If name is in filter and doesn't have : in it add minecraft: namespace
    if (filter.containsKey("name") && filter.get("name") instanceof String name) {
      if (!name.contains(":")) {
        details.put("name", "minecraft:" + name);
      }
    }

    // If filter is a list, check if the item is in the list
    if (!(filter instanceof Map<?,?>)) 
      throw new LuaException("Filter must be a map");

    if (!deepEquals(filter, details))
      return 0;
    return entry.count;
  }

  private static boolean deepEquals(Object fVal, Object iVal) throws LuaException {
    // Checks all non number, Map, and List types
    if (Objects.equals(iVal, fVal)) return true;

    // Lua Objects can implement LuaComparable to provide a table representation for lazy filtering
    if (iVal instanceof LuaComparable iStack) {
      return deepEquals(fVal, iStack.getTableRepresentation());
    } 

    // If both are numbers, compare them as doubles because lua numbers are always doubles
    if (fVal instanceof Number fn && iVal instanceof Number in)
      return Double.compare(fn.doubleValue(), in.doubleValue()) == 0;

    // Convert to collections
    Collection fColl = Collection.of(fVal);
    Collection iColl = Collection.of(iVal);
    // If one is not a collection, return false
    if (fColl == null || iColl == null)
      return false;
    
    // Compare as list or map
    if (iColl.isList() && fColl.isList()) return matchList(fColl, iColl);
    if (iColl.isMap()  && fColl.isMap())  return matchMap (fColl, iColl);
    return false;                                         
  }

  private static boolean matchList(Collection f, Collection i) throws LuaException {
    switch (f.mode) {
      case EXACT -> {
        if (f.list.size() != i.list.size()) return false;
        for (int k = 0; k < f.list.size(); k++)
          if (!deepEquals(f.list.get(k), i.list.get(k))) 
            return false;
        return true;
      }
      case CONTAINS   -> {
        outer: for (Object fVal : f.list) {
          for (Iterator<?> it = i.list.iterator(); it.hasNext();) {
            Object iVal = it.next();
            if (deepEquals(fVal, iVal)) { it.remove(); continue outer; }
          }
          return false;
        }
        return true;
      }
      case CONTAINED  -> 
      {
        outer: for (Object iVal : i.list) {
          for (Iterator<?> it = f.list.iterator(); it.hasNext();) {
            Object fVal = it.next();
            if (deepEquals(fVal, iVal)) { it.remove(); continue outer; }
          }
          return false;
        }
        return true;
      }
    }
    return false;
  }

  private static boolean matchMap(Collection f, Collection i) throws LuaException {
    switch (f.mode) {
      case EXACT -> {
        if (!f.map.keySet().equals(i.map.keySet())) return false;
        for (var e : f.map.entrySet()) {
          if (!deepEquals(e.getValue(), i.map.get(e.getKey())))
            return false;
        }
        return true;
      }
      case CONTAINS -> {
        for (var e : f.map.entrySet()) {
          if (!i.map.containsKey(e.getKey())
              || !deepEquals(e.getValue(), i.map.get(e.getKey())))
            return false;
        }
        return true;
      }
      case CONTAINED -> {
        for (var e : i.map.entrySet()) {
          if (!f.map.containsKey(e.getKey())
              || !deepEquals(f.map.get(e.getKey()), e.getValue()))
            return false;
        }
        return true;
      }
    }
    return false;
  }

  // Is a wrapper for lua tables so they can be processed as lists or maps
  private record Collection(MatchMode mode, List<?> list, Map<?, ?> map) {
    boolean isList() { return list != null; }
    boolean isMap()  { return map != null; }

    // Returns null if not a list or map
    static Collection of(Object o) throws LuaException {
      if (o instanceof Map<?,?> m) {
        MatchMode mode = MatchMode.parse(m.get("_mode"));
        m.remove("_mode");
        if (isArrayLike(m)) {
          List<Object> lst = toOrderedList(m);
          return new Collection(mode, lst, m);
        }
        return new Collection(mode, null, m);
      }
      // List for CC, never from filter
      if (o instanceof List<?> raw) {
        return new Collection(MatchMode.CONTAINS, raw, null);
      }
      return null;
    }
  }

  // Allows user to specify match mode in filter for the specific list or map
  // Exact: 1:1 match, list must be same size and order
  // Contains: All elements in filter must be in the item, order doesn't matter, args removed from filter as they are found
  // Contained: All elements in item must be in the filter, order doesn't matter, args removed from item as they are found
  private enum MatchMode { EXACT, CONTAINS, CONTAINED;
    static MatchMode parse(Object t) throws LuaException {
      if (!(t instanceof String s)) return CONTAINS;
      return switch (s.toLowerCase()) {
        case "exact"     -> EXACT;
        case "contains"  -> CONTAINS;
        case "contained" -> CONTAINED;
        default          -> throw new LuaException(
          "Invalid match mode: " + s + ", expected 'exact', 'contained' or 'contains'");
      };
    }
  }

  // All arrays from lua are passed as maps so we check if it is an array-like map
  private static boolean isArrayLike(Map<?,?> map) {
    int n = map.size();
    if (n == 0) return true;

    boolean[] seen = new boolean[n];
    for (Object keyObj : map.keySet()) {
      if (!(keyObj instanceof Number)) return false;
      int k = ((Number) keyObj).intValue() - 1;
      if (k != Math.floor(k)) return false; // not an whole number
      if (k < 0 || k >= n || seen[k]) return false;
      seen[k] = true;
    }

    for (boolean ok : seen)  
      if (!ok) return false;

    return true;
  }

  private static List<Object> toOrderedList(Map<?,?> m) {
    int n = m.size();
    List<Object> out = new ArrayList<>(Collections.nCopies(n, null));
    for (var e : m.entrySet())
      out.set(((Number) e.getKey()).intValue() - 1, e.getValue());
    return out;
  }
  
	public static Map<Integer, Map<String, ?>> list(IItemHandler inventory) {
		Map<Integer, Map<String, ?>> result = new HashMap<>();
		var size = inventory.getSlots();
		for (var i = 0; i < size; i++) {
			var stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) result.put(i + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
		}

		return result;
	}
}

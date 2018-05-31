// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.infinity.resource.bcs.ScriptInfo;
import org.infinity.resource.bcs.Signatures;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

public class IdsMap
{
  private final TreeMap<Long, IdsMapEntry> idsMap = new TreeMap<>();
  private final HashMap<String, Long> symbolMap = new HashMap<>();
  private final ResourceEntry entry;
  private final boolean caseSensitive;

  public IdsMap(ResourceEntry entry)
  {
    this.entry = entry;
    this.caseSensitive = IdsMapCache.isCaseSensitiveMatch(entry.toString());
    try {
      if (entry.getExtension().equalsIgnoreCase("IDS")) {
        parseIDS();
      } else if (entry.getExtension().equalsIgnoreCase("2DA")) {
        parse2DA();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString()
  {
    if (entry != null) {
      return entry.toString();
    } else {
      return "null";
    }
  }

  /** Returns the number of entries in the map. */
  public int size()
  {
    return idsMap.size();
  }

  /** Returns a copy of the values contained in the IDS map. */
  public List<IdsMapEntry> getAllValues()
  {
    return new ArrayList<IdsMapEntry>(idsMap.values());
  }

  /** Returns a formatted string list of all available symbols and their associated key values. */
  public List<String> getAllStringValues()
  {
    ArrayList<String> retVal = new ArrayList<>(idsMap.size() * 3 / 2);

    for (final IdsMapEntry e: idsMap.values()) {
      long id = e.getID();
      Iterator<String> iter = e.getSymbols();
      while (iter.hasNext()) {
        String symbol = iter.next();
        retVal.add(IdsMapEntry.toString(id, symbol));
      }
    }

    return retVal;
  }

  /** Returns a copy of the keys contained in the IDS map as a sorted set. */
  public SortedSet<Long> getKeys()
  {
    return new TreeSet<Long>(idsMap.keySet());
  }

  /** Returns the entry structure defined by the specified IDS value, or {@code null} otherwise. */
  public IdsMapEntry get(long value)
  {
    return idsMap.get(Long.valueOf(normalizedKey(value)));
  }

  private void put(long key, IdsMapEntry value)
  {
    if (value != null) {
      idsMap.put(Long.valueOf(normalizedKey(key)), value);
    }
  }

  /**
   * Attempts to find the entry containing the specified symbol.
   * Symbol will be compared case-sensitive except for triggers, actions and objects.
   * @param symbol The symbolic name.
   * @return Matching entry structure, or {@code null} otherwise.
   */
  public IdsMapEntry lookup(String symbol)
  {
    return lookup(symbol, caseSensitive);
  }

  /**
   * Attempts to find the entry containing the specified symbol.
   * @param symbol The symbolic name.
   * @param exact Whether to compare case-sensitive.
   * @return Matching entry structure, or {@code null} otherwise.
   */
  public IdsMapEntry lookup(String symbol, boolean exact)
  {
    IdsMapEntry retVal = null;
    String symbolNorm = normalizedString(symbol);
    if (!symbolNorm.isEmpty() && !symbolNorm.equals("0")) {
      Long key = symbolMap.get(normalizedString(symbolNorm));
      if (key != null) {
        IdsMapEntry e = idsMap.get(key);
        if (exact && e != null) {
          Iterator<String> iter = e.getSymbols();
          while (iter.hasNext() && retVal == null) {
            String s = iter.next();
            if (s.equals(symbol)) {
              retVal = e;
            }
          }
        } else {
          retVal = e;
        }
      }
    }
    return retVal;
  }

  /** Returns whether symbols from this IDS resource are matched case-sensitive. */
  public boolean isCaseSensitiveMatch()
  {
    return caseSensitive;
  }

  private void parse2DA() throws Exception
  {
    StringTokenizer st = new StringTokenizer(new PlainTextResource(entry).getText(), "\r\n");
    // 3 uninteresting lines
    for (int i = 0; i < 3 && st.hasMoreTokens(); i++) {
      st.nextToken();
    }
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      try {
        extract2DA(token);
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Error interpreting " + entry + ": " + token,
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void parseIDS() throws Exception
  {
    // parsing regular IDS content
    StringTokenizer st = new StringTokenizer(new PlainTextResource(entry).getText(), "\r\n");
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      try {
        extractIDS(token);
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Error interpreting " + entry + ": " + token,
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }

    // parsing hardcoded entries
    List<String> list = null;
    if (entry.toString().equalsIgnoreCase("TRIGGER.IDS")) {
      list = ScriptInfo.getInfo().getFunctionDefinitions(Signatures.Function.FunctionType.TRIGGER);
    } else if (entry.toString().equalsIgnoreCase("ACTION.IDS")) {
      list = ScriptInfo.getInfo().getFunctionDefinitions(Signatures.Function.FunctionType.ACTION);
    }
    if (list != null) {
      for (String token: list) {
        try {
          extractIDS(token);
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(null, "Error interpreting " + entry + ": " + token,
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  private void extract2DA(String line)
  {
    StringTokenizer st = new StringTokenizer(line);
    long id = normalizedKey(Long.parseLong(st.nextToken()));
    String resource = st.nextToken();
    while (st.hasMoreTokens()) {
      resource = st.nextToken();
    }
    IdsMapEntry value = get(id);
    if (value == null) {
      value = new IdsMapEntry(id, resource);
      put(id, value);
    }
  }

  private void extractIDS(String line)
  {
    if (line.contains("WatchersKeep")) {
      if (true) {
      }
    }
    line = line.trim();
    int p = Math.min(line.indexOf(' ') & Integer.MAX_VALUE, line.indexOf('\t') & Integer.MAX_VALUE);
    if (p == Integer.MAX_VALUE) {
      return;
    }

    String istr = line.substring(0, p).trim();
    if (istr.equalsIgnoreCase("IDS")) {
      return;
    }

    if (!istr.isEmpty()) {
      int radix = 10;
      if (istr.length() > 2 && istr.substring(0, 2).equalsIgnoreCase("0x")) {
        istr = istr.substring(2);
        radix = 16;
      }
      long key = normalizedKey(Long.parseLong(istr, radix));

      String vstr = line.substring(p).trim();
      p = vstr.indexOf("//");
      if (p >= 0) {
        vstr = vstr.substring(0, p).trim();
      }
      if (!vstr.isEmpty()) {
        IdsMapEntry value = get(key);
        if (value != null) {
          value.addSymbol(vstr);
        } else {
          value = new IdsMapEntry(key, vstr);
          put(key, value);
        }

        symbolMap.put(normalizedString(vstr), Long.valueOf(key));
      }
    }
  }

  private long normalizedKey(long key)
  {
    return key & 0xffffffffL;
  }

  private String normalizedString(String s)
  {
    return (s != null) ? s.trim().toUpperCase(Locale.ENGLISH) : "";
  }
}

// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.infinity.resource.StructEntry;
import org.infinity.util.IdsMapEntry;

/**
 * Specialized IdsBitmap type for properly handling KIT.IDS in BG and BG2.
 */
public class KitIdsBitmap extends IdsBitmap
{

  public KitIdsBitmap(ByteBuffer buffer, int offset, String name)
  {
    this(null, buffer, offset, name);
  }

  public KitIdsBitmap(StructEntry parent, ByteBuffer buffer, int offset, String name)
  {
    super(parent, buffer, offset, 4, name, "KIT.IDS");
    init();
  }

  public KitIdsBitmap(ByteBuffer buffer, int offset, String name, int idsStart)
  {
    this(null, buffer, offset, name, idsStart);
  }

  public KitIdsBitmap(StructEntry parent, ByteBuffer buffer, int offset, String name, int idsStart)
  {
    super(parent, buffer, offset, 4, name, "KIT.IDS", idsStart);
    init();
  }

//--------------------- Begin Interface Writeable ---------------------

 @Override
 public void write(OutputStream os) throws IOException
 {
   writeLong(os, swapWords(getValue()));
 }

//--------------------- End Interface Writeable ---------------------

  private void init()
  {
    // adding "No Kit" value if needed
    addIdsMapEntry(new IdsMapEntry(0L, "NO_KIT"));

    // fixing word order of kit id value
    setValue(swapWords(getValue()));
  }

  // Swaps position of the two lower words
  private static long swapWords(long value)
  {
    return ((value >>> 16) & 0xffffL) | ((value & 0xffffL) << 16);
  }
}

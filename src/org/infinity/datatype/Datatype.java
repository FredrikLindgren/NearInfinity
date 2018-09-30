// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.infinity.resource.StructEntry;
import org.infinity.util.io.ByteBufferOutputStream;
import org.infinity.util.io.StreamUtils;

public abstract class Datatype implements StructEntry
{
  protected static final Dimension DIM_WIDE = new Dimension(800, 100);
  protected static final Dimension DIM_BROAD = new Dimension(650, 100);
  protected static final Dimension DIM_MEDIUM = new Dimension(450, 100);

  private final List<UpdateListener> listeners = new ArrayList<>();
  private final int length;

  private String name;
  private int offset;
  private StructEntry parent;

  protected Datatype(int offset, int length, String name)
  {
    this(null, offset, length, name);
  }

  protected Datatype(StructEntry parent, int offset, int length, String name)
  {
    this.parent = parent;
    this.offset = offset;
    this.length = length;
    this.name = name;
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(StructEntry o)
  {
    return offset - o.getOffset();
  }

// --------------------- End Interface Comparable ---------------------


// --------------------- Begin Interface StructEntry ---------------------

  @Override
  public Datatype clone() throws CloneNotSupportedException
  {
    return (Datatype)super.clone();
  }

  @Override
  public void copyNameAndOffset(StructEntry entry)
  {
    name = entry.getName();
    offset = entry.getOffset();
  }

  @Override
  public StructEntry getParent()
  {
    return parent;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public void setName(String newName)
  {
    if (newName != null) {
      name = newName;
    } else {
      throw new NullPointerException("Name of struct field must not be null");
    }
  }

  @Override
  public int getOffset()
  {
    return offset;
  }

  @Override
  public int getSize()
  {
    return length;
  }

  @Override
  public List<StructEntry> getStructChain()
  {
    final List<StructEntry> list = new ArrayList<>();
    StructEntry e = this;
    while (e != null) {
      list.add(0, e);
      e = e.getParent();
      if (list.contains(e)) {
        // avoid infinite loops
        break;
      }
    }
    return list;
  }

  @Override
  public void setOffset(int newoffset)
  {
    offset = newoffset;
  }

  @Override
  public void setParent(StructEntry parent)
  {
    this.parent = parent;
  }

  @Override
  public ByteBuffer getDataBuffer()
  {
    ByteBuffer bb = StreamUtils.getByteBuffer(getSize());
    try (ByteBufferOutputStream bbos = new ByteBufferOutputStream(bb)) {
      write(bbos);
    } catch (IOException e) {
      e.printStackTrace();
    }
    bb.position(0);
    return bb;
  }

// --------------------- End Interface StructEntry ---------------------

  /**
   * Adds the specified update listener to receive update events from this object.
   * If listener l is null, no exception is thrown and no action is performed.
   * @param l The update listener
   */
  public void addUpdateListener(UpdateListener l)
  {
    if (l != null) {
      listeners.add(l);
    }
  }

  /**
   * Returns an array of all update listeners registered on this object.
   * @return All of this object's update listener or an empty array if no listener is registered.
   */
  public UpdateListener[] getUpdateListeners()
  {
    UpdateListener[] ar = new UpdateListener[listeners.size()];
    for (int i = 0; i < listeners.size(); i++) {
      ar[i] = listeners.get(i);
    }
    return ar;
  }

  /**
   * Removes the specified update listener, so that it no longer receives update events
   * from this object.
   * @param l The update listener
   */
  public void removeUpdateListener(UpdateListener l)
  {
    if (l != null) {
      listeners.remove(l);
    }
  }

  /**
   * Notifies all listeners that the value of this Datatype object may have changed.
   */
  protected void fireValueUpdated(UpdateEvent event)
  {
    if (event != null && event.getStructure() != null) {
      // don't lose current selection
      if (event.getStructure().getViewer() != null) {
        event.getStructure().getViewer().storeCurrentSelection();
      }
      boolean retVal = false;
      for (final UpdateListener l: listeners) {
        retVal |= l.valueUpdated(event);
      }
      if (retVal) {
        event.getStructure().fireTableDataChanged();
      }
      if (event.getStructure().getViewer() != null) {
        event.getStructure().getViewer().restoreCurrentSelection();
      }
    }
  }


  void writeInt(OutputStream os, int value) throws IOException
  {
    final int size = getSize();
    switch (size) {
      case 4:
        StreamUtils.writeInt(os, value);
        break;
      case 3:
        StreamUtils.writeInt24(os, value);
        break;
      case 2:
        StreamUtils.writeShort(os, (short)value);
        break;
      case 1:
        StreamUtils.writeByte(os, (byte)value);
        break;
      default:
        throw new IllegalArgumentException("Field '"+name+"' of class "+getClass()+" has unsupported size; expected one of 1-4, but it has "+size);
    }
  }

  void writeLong(OutputStream os, long value) throws IOException
  {
    writeInt(os, (int)value);
  }
}


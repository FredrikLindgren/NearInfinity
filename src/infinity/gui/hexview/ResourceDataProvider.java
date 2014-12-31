// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.hexview;

import java.util.ArrayList;
import java.util.HashMap;

import infinity.resource.key.ResourceEntry;
import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;

/**
 * Provides data as byte array from the associated ResourceEntry instance to be used in
 * JHexView components.
 *
 * @author argent77
 */
public class ResourceDataProvider implements IDataProvider
{
  private final ArrayList<IDataChangedListener> listeners = new ArrayList<IDataChangedListener>();
  private final HashMap<Integer, Byte> modifiedMap = new HashMap<Integer, Byte>();
  private final ResourceEntry entry;

  private int size;

  public ResourceDataProvider(ResourceEntry entry)
  {
    if (entry == null) {
      throw new NullPointerException("entry is null");
    }
    this.entry = entry;
    size = -1;    // initialized when needed
  }

//--------------------- Begin Interface IDataProvider ---------------------

  @Override
  public void addListener(IDataChangedListener listener)
  {
    if (listener != null && listeners.indexOf(listener) < 0) {
      listeners.add(listener);
    }
  }

  @Override
  public byte[] getData(long offset, int length)
  {
    if (offset+length > getDataLength()) {
      length = getDataLength() - (int)offset;
    }

    if (length > 0) {
      try {
        byte[] buffer = getResourceEntry().getResourceData();
        byte[] retVal = new byte[length];
        for (int i = 0; i < length; i++) {
          if (isModifiedData((int)offset+i)) {
            retVal[i] = getModifiedData((int)offset+i);
          } else {
            retVal[i] = buffer[(int)offset+i];
          }
        }
        return retVal;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public int getDataLength()
  {
    if (size < 0) {
      size = 0;
      try {
        byte[] buffer = getResourceEntry().getResourceData();
        if (buffer != null) {
          size = buffer.length;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return size;
  }

  @Override
  public boolean hasData(long start, int length)
  {
    return (start >= 0 && start+length <= getDataLength());
  }

  @Override
  public boolean isEditable()
  {
    // to be fully implemented
    return true;
  }

  @Override
  public boolean keepTrying()
  {
    return false;
  }

  @Override
  public void removeListener(IDataChangedListener listener)
  {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  @Override
  public void setData(long offset, byte[] data)
  {
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        addModifiedData((int)offset, data[i]);
      }
      if (data.length > 0) {
        fireDataChanged();
      }
    }
  }

//--------------------- End Interface IDataProvider ---------------------

  /** Returns the attached ResourceEntry instance. */
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

  /** Removes all modified data information from the provider. */
  public void clear()
  {
    clearModifiedData();
  }

  protected void fireDataChanged()
  {
    if (!listeners.isEmpty()) {
      DataChangedEvent event = new DataChangedEvent(this);
      for (int i = listeners.size()-1; i >= 0; i--) {
        listeners.get(i).dataChanged(event);
      }
    }
  }

  private boolean isModifiedData(int offset)
  {
    return modifiedMap.containsKey(Integer.valueOf(offset));
  }

  private byte getModifiedData(int offset)
  {
    Byte b = modifiedMap.get(Integer.valueOf(offset));
    if (b != null) {
      return b.byteValue();
    } else {
      return 0;
    }
  }

  private void addModifiedData(int offset, byte value)
  {
    if (offset >= 0 && offset < getDataLength()) {
      modifiedMap.put(Integer.valueOf(offset), Byte.valueOf(value));
    }
  }

//  private void removeModifiedData(int offset)
//  {
//    modifiedMap.remove(Integer.valueOf(offset));
//  }

  // Removes all modified data from the map.
  private void clearModifiedData()
  {
    modifiedMap.clear();
  }
}

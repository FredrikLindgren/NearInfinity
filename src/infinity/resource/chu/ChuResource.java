// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

public final class ChuResource extends AbstractStruct implements Resource, HasViewerTabs //, HasAddRemovable
{
  private List<Pair<Integer>> listControls;
  private int ofsPanels, numPanels, sizePanels, ofsControls, numControls;
  private Viewer detailViewer;

  public ChuResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Window)
        ((Window)o).writeControlsTable(os);
    }
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Window)
        ((Window)o).writeControls(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (detailViewer == null) {
      detailViewer = new Viewer(this);
    }
    return detailViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------

   // Write 'size' number of zeros to the output stream
  void writeGap(OutputStream os, int startOfs, int endOfs) throws IOException
  {
    while (startOfs < endOfs) {
      os.write(0);
      startOfs++;
    }
  }

  /** Returns the starting offset of available panels. */
  public int getPanelsOffset()
  {
    return ofsPanels;
  }

  /** Returns the number of available panels. */
  public int getPanelCount()
  {
    return numPanels;
  }

  /** Returns the starting offset of the control table. */
  public int getControlsOffset()
  {
    return ofsControls;
  }

  /** Returns the number of available controls. */
  public int getControlCount()
  {
    return numControls;
  }

  /** Returns the absolute starting offset of the control at the given index. */
  public int getControlOffset(int index)
  {
    if (index >= 0 && index < listControls.size()) {
      return listControls.get(index).getFirst();
    } else {
      return 0;
    }
  }

  /** Returns the size of the control at the given index. */
  public int getControlSize(int index)
  {
    if (index >= 0 && index < listControls.size()) {
      return listControls.get(index).getSecond();
    } else {
      return 0;
    }
  }

  /** Returns the panel size in bytes. */
  public int getPanelSize()
  {
    return sizePanels;
  }

  /** Returns the given panel. */
  public Window getPanel(int index)
  {
    if (index >= 0 && index < getPanelCount()) {
      return (Window)getAttribute(String.format(Window.FMT_NAME, index));
    } else {
      return null;
    }
  }

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    initData(buffer, offset);

    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new SectionCount(buffer, offset + 8, 4, "# panels", Window.class));
    list.add(new SectionOffset(buffer, offset + 12, "Controls offset", Control.class));
    list.add(new SectionOffset(buffer, offset + 16, "Panels offset", Window.class));
    offset += 20;

    // handling optional gap between header and panels section
    int endoffset = Math.min(getPanelsOffset(), getControlsOffset());
    if (offset < endoffset) {
      list.add(new Unknown(buffer, offset, endoffset - offset, "Unused"));
    }

    offset = endoffset;
    for (int i = 0; i < getPanelCount(); i++) {
      Window window = new Window(this, buffer, offset, i);
      offset = window.getEndOffset();
      endoffset = Math.max(endoffset, window.readControls(buffer));
      list.add(window);
    }

    // handling optional gap between panels section and controls section
    if (offset < ofsControls) {
      list.add(new Unknown(buffer, offset, ofsControls - offset, "Unused"));
    }

    return Math.max(offset, endoffset);
  }

  // initialize data required to reconstruct the original resource on save
  private void initData(byte[] buffer, int offset)
  {
    // loading header data
    numPanels = DynamicArray.getInt(buffer, offset + 8);
    ofsControls = DynamicArray.getInt(buffer, offset + 12);
    ofsPanels = DynamicArray.getInt(buffer, offset + 16);
    sizePanels = Math.abs(ofsControls - ofsPanels) / numPanels;
    if (sizePanels >= 36) sizePanels = 36; else if (sizePanels >= 28) sizePanels = 28;

    // loading controls data
    numControls = 0;
    int curOfs = ofsPanels;
    for (int i = 0; i < numPanels; i++) {
      int numCtrl = DynamicArray.getShort(buffer, curOfs + 14);
      int startIdx = DynamicArray.getShort(buffer, curOfs + 24);
      numControls = Math.max(numControls, startIdx + numCtrl);
      curOfs += sizePanels;
    }

    // Adding offset/size pairs for each control
    curOfs = ofsControls;
    if (listControls == null) {
      listControls = new ArrayList<Pair<Integer>>();
    }
    int ofs = 0, len = 0;
    for (int i = 0; i < numControls; i++, curOfs += 8) {
      ofs = DynamicArray.getInt(buffer, curOfs);
      len = DynamicArray.getInt(buffer, curOfs + 4);
      listControls.add(new Pair<Integer>(Integer.valueOf(ofs), Integer.valueOf(len)));
    }

    // adding virtual entry for determining the true size of the last control entry
    ofs = Math.max(ofs + len, buffer.length);
    listControls.add(new Pair<Integer>(Integer.valueOf(ofs), Integer.valueOf(0)));
  }
}


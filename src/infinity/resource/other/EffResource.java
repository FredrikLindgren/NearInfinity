// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import infinity.datatype.EffectType;
import infinity.datatype.TextString;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.Effect2;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

public final class EffResource extends AbstractStruct implements Resource, HasViewerTabs
{
  private HexViewer hexViewer;

  public EffResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    addField(new TextString(buffer, offset + 4, 4, "Version"));
    addField(new TextString(buffer, offset + 8, 4, "Signature 2"));
    addField(new TextString(buffer, offset + 12, 4, "Version 2"));
    EffectType type = new EffectType(buffer, offset + 16, 4);
    addField(type);
    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 20, list);
    addToList(getList().size() - 1, list);

    list.clear();
    Effect2.readCommon(list, buffer, offset);
    addToList(getList().size() - 1, list);

    return offset + 216;
  }

//--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      hexViewer = new HexViewer(this, new BasicColorMap(this, true));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        EffResource eff = new EffResource(entry);
        boolean retVal = true;
        String key;
        Object o;

        String[] keyList = new String[]{SearchOptions.EFF_Effect, SearchOptions.EFF_Param1,
                                        SearchOptions.EFF_Param2, SearchOptions.EFF_TimingMode,
                                        SearchOptions.EFF_Duration};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct;
            if (SearchOptions.isResourceByOffset(key)) {
              int ofs = SearchOptions.getResourceIndex(key);
              struct = eff.getAttribute(ofs);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key));
            }
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        if (retVal) {
          key = SearchOptions.EFF_SaveType;
          o = searchOptions.getOption(key);
          StructEntry struct = eff.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchFlags(struct, o);
        }

        keyList = new String[]{SearchOptions.EFF_Resource1, SearchOptions.EFF_Resource2,
                               SearchOptions.EFF_Resource3};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct;
            if (SearchOptions.isResourceByOffset(key)) {
              int ofs = SearchOptions.getResourceIndex(key);
              struct = eff.getAttribute(ofs);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key));
            }
            retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.EFF_Custom1, SearchOptions.EFF_Custom2,
                               SearchOptions.EFF_Custom3, SearchOptions.EFF_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(eff, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }

}


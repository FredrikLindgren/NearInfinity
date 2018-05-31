// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.List;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;

/**
 * Manages entrance layer objects.
 */
public class LayerEntrance extends BasicLayer<LayerObjectEntrance>
{
  private static final String AvailableFmt = "Entrances: %d";

  public LayerEntrance(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ENTRANCE, viewer);
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      close();
      List<LayerObjectEntrance> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute(AreResource.ARE_OFFSET_ENTRANCES);
        SectionCount sc = (SectionCount)are.getAttribute(AreResource.ARE_NUM_ENTRANCES);
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Entrance.class);
          for (int i = 0, size = listStruct.size(); i < size; i++) {
            LayerObjectEntrance obj = new LayerObjectEntrance(are, (Entrance)listStruct.get(i));
            setListeners(obj);
            list.add(obj);
          }
          setInitialized(true);
        }
      }
      return list.size();
    }
    return 0;
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }
}

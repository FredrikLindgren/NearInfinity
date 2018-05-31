// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.SpawnPoint;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Spawn Point
 */
public class LayerObjectSpawnPoint extends LayerObject
{
  private static final Image[] ICON = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_SPAWN_POINT_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_SPAWN_POINT_2)};
  private static final Point CENTER = new Point(22, 22);

  private final SpawnPoint sp;
  private final Point location = new Point();

  private IconLayerItem item;
  private Flag scheduleFlags;


  public LayerObjectSpawnPoint(AreResource parent, SpawnPoint sp)
  {
    super(ViewerConstants.RESOURCE_ARE, "Spawn Point", SpawnPoint.class, parent);
    this.sp = sp;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return sp;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{sp};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
    } else {
      return false;
    }
  }


  private void init()
  {
    if (sp != null) {
      String msg = "";
      try {
        location.x = ((DecNumber)sp.getAttribute(SpawnPoint.ARE_SPAWN_LOCATION_X)).getValue();
        location.y = ((DecNumber)sp.getAttribute(SpawnPoint.ARE_SPAWN_LOCATION_Y)).getValue();

        scheduleFlags = ((Flag)sp.getAttribute(SpawnPoint.ARE_SPAWN_ACTIVE_AT));

        msg = ((TextString)sp.getAttribute(SpawnPoint.ARE_SPAWN_NAME)).toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Using cached icons
      Image[] icon;
      String keyIcon = String.format("%s%s", SharedResourceCache.createKey(ICON[0]),
                                                 SharedResourceCache.createKey(ICON[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.ICON, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.ICON, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon);
      } else {
        icon = ICON;
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      item = new IconLayerItem(location, sp, msg, msg, icon[0], CENTER);
      item.setLabelEnabled(Settings.ShowLabelSpawnPoints);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}

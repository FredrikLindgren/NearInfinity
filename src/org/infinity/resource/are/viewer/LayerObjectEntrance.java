// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Entrance;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Entrance
 */
public class LayerObjectEntrance extends LayerObject
{
  private static final Image[] ICON = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ENTRANCE_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ENTRANCE_2)};
  private static final Point CENTER = new Point(11, 18);

  private final Entrance entrance;
  private final Point location = new Point();

  private IconLayerItem item;

  public LayerObjectEntrance(AreResource parent, Entrance entrance)
  {
    super(ViewerConstants.RESOURCE_ARE, "Entrance", Entrance.class, parent);
    this.entrance = entrance;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return entrance;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{entrance};
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

  private void init()
  {
    if (entrance != null) {
      String info = "";
      String msg = "";
      try {
        location.x = ((DecNumber)entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_X)).getValue();
        location.y = ((DecNumber)entrance.getAttribute(Entrance.ARE_ENTRANCE_LOCATION_Y)).getValue();
        int o = ((Bitmap)entrance.getAttribute(Entrance.ARE_ENTRANCE_ORIENTATION)).getValue();
        if (o < 0) o = 0; else if (o >= Actor.s_orientation.length) o = Actor.s_orientation.length - 1;
        info = ((TextString)entrance.getAttribute(Entrance.ARE_ENTRANCE_NAME)).toString();
        msg = String.format("%s (%s)", ((TextString)entrance.getAttribute(Entrance.ARE_ENTRANCE_NAME)).toString(),
                            Actor.s_orientation[o]);
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

      item = new IconLayerItem(location, entrance, msg, info, icon[0], CENTER);
      item.setLabelEnabled(Settings.ShowLabelEntrances);
      item.setName(getCategory());
      item.setToolTipText(info);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}

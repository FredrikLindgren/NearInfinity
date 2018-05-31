// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.Ambient;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Ambient Sound and Ambient Sound Range
 * Note: Ambient returns two layer items: 0=icon, 1=range (if available)
 */
public class LayerObjectAmbient extends LayerObject
{
  private static final Image[] ICON_GLOBAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_G_1),
                                              Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_G_2)};
  private static final Image[] ICON_LOCAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_L_1),
                                             Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_L_2)};
  private static final Point CENTER = new Point(16, 16);
  private static final Color[] COLOR_RANGE = {new Color(0xA0000080, true), new Color(0xA0000080, true),
                                              new Color(0x00204080, true), new Color(0x004060C0, true)};

  private final Ambient ambient;
  private final Point location = new Point();

  private IconLayerItem itemIcon;   // for sound icon
  private ShapedLayerItem itemShape;  // for sound range
  private int radiusLocal, volume;
  private Flag scheduleFlags;


  public LayerObjectAmbient(AreResource parent, Ambient ambient)
  {
    super(ViewerConstants.RESOURCE_ARE, "Sound", Ambient.class, parent);
    this.ambient = ambient;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return ambient;
  }

  @Override
  public Viewable[] getViewables()
  {
    if (isLocal()) {
      return new Viewable[]{ambient, ambient};
    } else {
      return new Viewable[]{ambient};
    }
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return itemIcon;
  }

  /**
   * Returns the layer item of specified type.
   * @param type The type of the item to return (either {@code ViewerConstants.AMBIENT_ITEM_ICON} or
   *              {@code ViewerConstants.AMBIENT_ITEM_RANGE}).
   * @return The layer item of specified type.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (type == ViewerConstants.AMBIENT_ITEM_RANGE && isLocal()) {
      return itemShape;
    } else if (type == ViewerConstants.AMBIENT_ITEM_ICON) {
      return itemIcon;
    } else {
      return null;
    }
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    if (isLocal()) {
      return new AbstractLayerItem[]{itemIcon, itemShape};
    } else {
      return new AbstractLayerItem[]{itemIcon};
    }
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    int x = (int)(location.x*zoomFactor + (zoomFactor / 2.0));
    int y = (int)(location.y*zoomFactor + (zoomFactor / 2.0));

    if (itemIcon != null) {
      itemIcon.setItemLocation(x, y);
    }

    if (isLocal()) {
      Shape circle = createShape(zoomFactor);
      Rectangle rect = circle.getBounds();
      itemShape.setItemLocation(x, y);
      itemShape.setCenterPosition(new Point(rect.width / 2, rect.height / 2));
      itemShape.setShape(circle);
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
    return new Point[]{location, location};
  }

  /**
   * Returns whether the ambient sound uses a local sound radius.
   */
  public boolean isLocal()
  {
    return (itemShape != null);
  }

  /**
   * Returns the local radius of the ambient sound (if any).
   */
  public int getRadius()
  {
    return radiusLocal;
  }

  /**
   * Returns the volume of the ambient sound.
   */
  public int getVolume()
  {
    return volume;
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
    if (ambient != null) {
      String msg = "";
      Image[] icon = ICON_GLOBAL;
      Shape circle = null;
      Color[] color = new Color[COLOR_RANGE.length];
      try {
        location.x = ((DecNumber)ambient.getAttribute(Ambient.ARE_AMBIENT_ORIGIN_X)).getValue();
        location.y = ((DecNumber)ambient.getAttribute(Ambient.ARE_AMBIENT_ORIGIN_Y)).getValue();
        radiusLocal = ((DecNumber)ambient.getAttribute(Ambient.ARE_AMBIENT_RADIUS)).getValue();
        volume = ((DecNumber)ambient.getAttribute(Ambient.ARE_AMBIENT_VOLUME)).getValue();
        if (((Flag)ambient.getAttribute(Ambient.ARE_AMBIENT_FLAGS)).isFlagSet(2)) {
          icon = ICON_GLOBAL;
          radiusLocal = 0;
        } else {
          icon = ICON_LOCAL;
        }

        scheduleFlags = ((Flag)ambient.getAttribute(Ambient.ARE_AMBIENT_ACTIVE_AT));

        msg = ((TextString)ambient.getAttribute(Ambient.ARE_AMBIENT_NAME)).toString();
        if (icon == ICON_LOCAL) {
          circle = createShape(1.0);
          double minAlpha = 0.0, maxAlpha = 64.0;
          double alphaF = minAlpha + Math.sqrt((double)volume) / 10.0 * (maxAlpha - minAlpha);
          int alpha = (int)alphaF & 0xff;
          color[0] = COLOR_RANGE[0];
          color[1] = COLOR_RANGE[1];
          color[2] = new Color(COLOR_RANGE[2].getRGB() | (alpha << 24), true);
          color[3] = new Color(COLOR_RANGE[3].getRGB() | (alpha << 24), true);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Using cached icons
      String keyIcon = String.format("%s%s", SharedResourceCache.createKey(icon[0]),
                                                 SharedResourceCache.createKey(icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.ICON, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.ICON, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon);
      } else {
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      // creating sound item
      itemIcon = new IconLayerItem(location, ambient, msg, msg, icon[0], CENTER);
      itemIcon.setLabelEnabled(Settings.ShowLabelSounds);
      itemIcon.setName(getCategory());
      itemIcon.setToolTipText(msg);
      itemIcon.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      itemIcon.setVisible(isVisible());

      // creating sound range item
      if (icon == ICON_LOCAL) {
        itemShape = new ShapedLayerItem(location, ambient, msg, msg, circle, new Point(radiusLocal, radiusLocal));
        itemShape.setName(getCategory());
        itemShape.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        itemShape.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        itemShape.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        itemShape.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        itemShape.setStrokeWidth(AbstractLayerItem.ItemState.NORMAL, 2);
        itemShape.setStrokeWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
        itemShape.setStroked(true);
        itemShape.setFilled(true);
        itemShape.setVisible(isVisible());
      }
    }
  }

  private Shape createShape(double zoomFactor)
  {
    if (ambient != null && itemShape != null && radiusLocal > 0) {
      float diameter = (float)(radiusLocal*zoomFactor + (zoomFactor / 2.0)) * 2.0f;
      return new Ellipse2D.Float(0.0f, 0.0f, diameter, diameter);
    }
    return null;
  }
}

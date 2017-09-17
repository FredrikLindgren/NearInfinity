// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.infinity.resource.are.viewer.ViewerConstants.LayerStackingType;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;

/**
 * Manages global area viewer settings.
 */
public class Settings
{
  // Default layer order on map
  public static final ViewerConstants.LayerStackingType[] DefaultLayerOrder = {
      ViewerConstants.LayerStackingType.ACTOR,
      ViewerConstants.LayerStackingType.ENTRANCE,
      ViewerConstants.LayerStackingType.AMBIENT,
      ViewerConstants.LayerStackingType.ANIMATION,
      ViewerConstants.LayerStackingType.AUTOMAP,
      ViewerConstants.LayerStackingType.SPAWN_POINT,
      ViewerConstants.LayerStackingType.PRO_TRAP,
      ViewerConstants.LayerStackingType.CONTAINER,
      ViewerConstants.LayerStackingType.REGION,
      ViewerConstants.LayerStackingType.DOOR,
      ViewerConstants.LayerStackingType.DOOR_POLY,
      ViewerConstants.LayerStackingType.WALL_POLY,
      ViewerConstants.LayerStackingType.AMBIENT_RANGE,
      ViewerConstants.LayerStackingType.TRANSITION
  };
  public static final String[] LabelZoomFactor = {"Auto-fit", "25%", "33%", "50%", "100%", "200%", "300%", "400%"};
  public static final double[] ItemZoomFactor = {0.0, 0.25, 1.0/3.0, 0.5, 1.0, 2.0, 3.0, 4.0};
  public static final int ZoomFactorIndexAuto = 0;       // points to the auto-fit zoom factor
  public static final int ZoomFactorIndexDefault = 4;    // points to the default zoom factor (1x)

  // Defines stacking order of layer items on the map
  public static final List<ViewerConstants.LayerStackingType> ListLayerOrder = getDefaultLayerOrder();
  // Indicates whether to store settings on disk
  public static boolean StoreVisualSettings = getDefaultStoreVisualSettings();
  // Current open/closed state of door tiles and structures
  public static boolean DrawClosed = getDefaultDrawClosed();
  // Current visibility state of overlays
  public static boolean DrawOverlays = getDefaultDrawOverlays();
  // Current visibility state of the tile grid
  public static boolean DrawGrid = getDefaultDrawGrid();
  // Current visibility state of ambient range items
  public static boolean ShowAmbientRanges = getDefaultAmbientRanges();
  // Defines whether to ignore time schedules on layer items
  public static boolean EnableSchedules = getDefaultEnableSchedules();
  // Defines whether to ignore the "Is shown" flag of background animations
  public static boolean OverrideAnimVisibility = getDefaultOverrideAnimVisibility();
  // Bitmask that controls the collapsed/expanded state of the sidebar controls
  public static int SidebarControls = getDefaultSidebarControls();
  // Indicates whether to show frames around real background animations all the time
  public static int ShowFrame = getDefaultShowFrame();
  // Interpolation state of map tileset
  public static int InterpolationMap = getDefaultInterpolationMap();
  // Interpolation state of real background animations
  public static int InterpolationAnim = getDefaultInterpolationAnim();
  // Bitmask defining the enabled state of layer items
  public static int LayerFlags = getDefaultLayerFlags();
  // The visibility state of real background animations (icons/still/animated)
  public static int ShowRealAnimations = getDefaultShowRealAnimations();
  // The current time of day (in hours)
  public static int TimeOfDay = getDefaultTimeOfDay();
  // The current zoom level of the map (as combobox item index)
  public static int ZoomLevel = getDefaultZoomLevel();
  // The frame rate for animated overlays
  public static double FrameRateOverlays = getDefaultFrameRateOverlays();
  // The frame rate for animated background animations
  public static double FrameRateAnimations = getDefaultFrameRateAnimations();
  // The alpha transparency for mini map overlays (search/height/light maps), range: [0.0, 1.0]
  public static double MiniMapAlpha = getDefaultMiniMapAlpha();
  // One of the MAP_XXX constants for minimaps
  public static int MiniMap = getDefaultMiniMap();
  // Show label for various layer item types
  public static boolean ShowLabelActorsAre = getDefaultLabelActorsAre();
  public static boolean ShowLabelActorsIni = getDefaultLabelActorsIni();
//  public static boolean ShowLabelRegions = getDefaultLabelRegions();
  public static boolean ShowLabelEntrances = getDefaultLabelEntrances();
//  public static boolean ShowLabelContainers = getDefaultLabelContainers();
  public static boolean ShowLabelSounds = getDefaultLabelSounds();
//  public static boolean ShowLabelDoors = getDefaultLabelDoors();
  public static boolean ShowLabelAnimations = getDefaultLabelAnimations();
  public static boolean ShowLabelMapNotes = getDefaultLabelMapNotes();
  public static boolean ShowLabelSpawnPoints = getDefaultLabelSpawnPoints();

  // Preferences keys for specific settings
  private static final String PREFS_STORESETTINGS           = "StoreSettings";
  private static final String PREFS_DRAWCLOSED              = "DrawClosed";
  private static final String PREFS_DRAWOVERLAYS            = "DrawOverlays";
  private static final String PREFS_DRAWGRID                = "DrawGrid";
  private static final String PREFS_SIDEBARCONTROLS         = "SidebarControls";
  private static final String PREFS_SHOWFRAME               = "ShowFrame";
  private static final String PREFS_SHOWAMBIENT             = "ShowAmbientRanges";
  private static final String PREFS_ENABLESCHEDULES         = "EnableSchedules";
  private static final String PREFS_OVERRIDEANIMVISIBILITY  = "OverrideAnimVisibility";
  private static final String PREFS_LAYERFLAGS              = "LayerFlags";
  private static final String PREFS_SHOWREALANIMS           = "ShowRealAnimations";
  private static final String PREFS_TIMEOFDAY               = "TimeOfDay";
  private static final String PREFS_ZOOMLEVEL               = "ZoomLevel";
  private static final String PREFS_LAYERZORDER_FMT         = "LayerZOrder%1$d";
  private static final String PREFS_INTERPOLATION_MAP       = "InterpolationMap";
  private static final String PREFS_INTERPOLATION_ANIMS     = "InterpolationAnims";
  private static final String PREFS_FRAMERATE_OVERLAYS      = "FrameRateOverlays";
  private static final String PREFS_FRAMERATE_ANIMS         = "FrameRateAnims";
  private static final String PREFS_MINIMAP_ALPHA           = "MiniMapAlpha";
  private static final String PREFS_MINIMAP                 = "MiniMap";
  private static final String PREFS_LABEL_ACTOR_ARE         = "LabelActorAre";
  private static final String PREFS_LABEL_ACTOR_INI         = "LabelActorIni";
//  private static final String PREFS_LABEL_REGIONS           = "LabelRegions";
  private static final String PREFS_LABEL_ENTRANCES         = "LabelEntrances";
//  private static final String PREFS_LABEL_CONTAINERS        = "LabelContainers";
  private static final String PREFS_LABEL_SOUNDS            = "LabelSounds";
//  private static final String PREFS_LABEL_DOORS             = "LabelDoors";
  private static final String PREFS_LABEL_ANIMATIONS        = "LabelAnimations";
  private static final String PREFS_LABEL_MAPNOTES          = "LabelMapNotes";
  private static final String PREFS_LABEL_SPAWNPOINTS       = "LabelSpawnPoints";

  private static boolean SettingsLoaded = false;

  /**
   * Loads stored viewer settings from disk if available and the store settings flag is enabled.
   * @param force If true, overrides the store settings flag and always loads settings from disk.
   */
  public static void loadSettings(boolean force)
  {
    if (!SettingsLoaded || force) {
      Preferences prefs = Preferences.userNodeForPackage(AreaViewer.class);

      // loading required settings
      StoreVisualSettings = prefs.getBoolean(PREFS_STORESETTINGS, getDefaultStoreVisualSettings());
      OverrideAnimVisibility = prefs.getBoolean(PREFS_OVERRIDEANIMVISIBILITY, getDefaultOverrideAnimVisibility());
      ShowFrame = prefs.getInt(PREFS_SHOWFRAME, getDefaultShowFrame());
      InterpolationMap = prefs.getInt(PREFS_INTERPOLATION_MAP, getDefaultInterpolationMap());
      InterpolationAnim = prefs.getInt(PREFS_INTERPOLATION_ANIMS, getDefaultInterpolationAnim());
      FrameRateOverlays = prefs.getDouble(PREFS_FRAMERATE_OVERLAYS, getDefaultFrameRateOverlays());
      FrameRateAnimations = prefs.getDouble(PREFS_FRAMERATE_ANIMS, getDefaultFrameRateAnimations());
      MiniMapAlpha = prefs.getDouble(PREFS_MINIMAP_ALPHA, getDefaultMiniMapAlpha());
      ShowLabelActorsAre = prefs.getBoolean(PREFS_LABEL_ACTOR_ARE, getDefaultLabelActorsAre());
      ShowLabelActorsIni = prefs.getBoolean(PREFS_LABEL_ACTOR_INI, getDefaultLabelActorsIni());
//      ShowLabelRegions = prefs.getBoolean(PREFS_LABEL_REGIONS, getDefaultLabelRegions());
      ShowLabelEntrances = prefs.getBoolean(PREFS_LABEL_ENTRANCES, getDefaultLabelEntrances());
//      ShowLabelContainers = prefs.getBoolean(PREFS_LABEL_CONTAINERS, getDefaultLabelContainers());
      ShowLabelSounds = prefs.getBoolean(PREFS_LABEL_SOUNDS, getDefaultLabelSounds());
//      ShowLabelDoors = prefs.getBoolean(PREFS_LABEL_DOORS, getDefaultLabelDoors());
      ShowLabelAnimations = prefs.getBoolean(PREFS_LABEL_ANIMATIONS, getDefaultLabelAnimations());
      ShowLabelMapNotes = prefs.getBoolean(PREFS_LABEL_MAPNOTES, getDefaultLabelMapNotes());
      ShowLabelSpawnPoints = prefs.getBoolean(PREFS_LABEL_SPAWNPOINTS, getDefaultLabelSpawnPoints());

      // loading layer z-order
      ListLayerOrder.clear();
      for (int i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
        int idx = prefs.getInt(String.format(PREFS_LAYERZORDER_FMT, i), -1);
        if (idx >= 0 && idx < ViewerConstants.LayerStackingType.values().length) {
          ListLayerOrder.add(ViewerConstants.LayerStackingType.values()[idx]);
        } else {
          ListLayerOrder.add(DefaultLayerOrder[i]);
        }
      }

      // loading optional settings
      if (StoreVisualSettings || force) {
        EnableSchedules = prefs.getBoolean(PREFS_ENABLESCHEDULES, getDefaultEnableSchedules());
        DrawClosed = prefs.getBoolean(PREFS_DRAWCLOSED, getDefaultDrawClosed());
        DrawOverlays = prefs.getBoolean(PREFS_DRAWOVERLAYS, getDefaultDrawOverlays());
        DrawGrid = prefs.getBoolean(PREFS_DRAWGRID, getDefaultDrawGrid());
        ShowAmbientRanges = prefs.getBoolean(PREFS_SHOWAMBIENT, getDefaultAmbientRanges());
        SidebarControls = prefs.getInt(PREFS_SIDEBARCONTROLS, getDefaultSidebarControls());
        LayerFlags = prefs.getInt(PREFS_LAYERFLAGS, getDefaultLayerFlags());
        ShowRealAnimations = prefs.getInt(PREFS_SHOWREALANIMS, getDefaultShowRealAnimations());
        TimeOfDay = prefs.getInt(PREFS_TIMEOFDAY, getDefaultTimeOfDay());
        ZoomLevel = prefs.getInt(PREFS_ZOOMLEVEL, getDefaultZoomLevel());
        MiniMap = prefs.getInt(PREFS_MINIMAP, getDefaultMiniMap());
      }
      validateSettings();
      SettingsLoaded = true;
    }
  }

  /**
   * Stores current global viewer settings on disk only if storing settings is enabled.
   * @param force If true, always stores settings to disk, ignoring the store settings flag.
   */
  public static void storeSettings(boolean force)
  {
    validateSettings();
    Preferences prefs = Preferences.userNodeForPackage(AreaViewer.class);

    // storing basic settings
    prefs.putBoolean(PREFS_STORESETTINGS, StoreVisualSettings);
    prefs.putBoolean(PREFS_OVERRIDEANIMVISIBILITY, OverrideAnimVisibility);
    prefs.putInt(PREFS_SHOWFRAME, ShowFrame);
    prefs.putInt(PREFS_INTERPOLATION_MAP, InterpolationMap);
    prefs.putInt(PREFS_INTERPOLATION_ANIMS, InterpolationAnim);
    prefs.putDouble(PREFS_FRAMERATE_OVERLAYS, FrameRateOverlays);
    prefs.putDouble(PREFS_FRAMERATE_ANIMS, FrameRateAnimations);
    prefs.putDouble(PREFS_MINIMAP_ALPHA, MiniMapAlpha);
    prefs.putBoolean(PREFS_LABEL_ACTOR_ARE, ShowLabelActorsAre);
    prefs.putBoolean(PREFS_LABEL_ACTOR_INI, ShowLabelActorsIni);
//    prefs.putBoolean(PREFS_LABEL_REGIONS, ShowLabelRegions);
    prefs.putBoolean(PREFS_LABEL_ENTRANCES, ShowLabelEntrances);
//    prefs.putBoolean(PREFS_LABEL_CONTAINERS, ShowLabelContainers);
    prefs.putBoolean(PREFS_LABEL_SOUNDS, ShowLabelSounds);
//    prefs.putBoolean(PREFS_LABEL_DOORS, ShowLabelDoors);
    prefs.putBoolean(PREFS_LABEL_ANIMATIONS, ShowLabelAnimations);
    prefs.putBoolean(PREFS_LABEL_MAPNOTES, ShowLabelMapNotes);
    prefs.putBoolean(PREFS_LABEL_SPAWNPOINTS, ShowLabelSpawnPoints);

    // storing layer z-order
    for (int i = 0; i < ListLayerOrder.size(); i++) {
      prefs.putInt(String.format(PREFS_LAYERZORDER_FMT, i), getLayerStackingTypeIndex(ListLayerOrder.get(i)));
    }

    // storing optional settings
    if (StoreVisualSettings || force) {
      prefs.putBoolean(PREFS_ENABLESCHEDULES, EnableSchedules);
      prefs.putBoolean(PREFS_DRAWCLOSED, DrawClosed);
      prefs.putBoolean(PREFS_DRAWOVERLAYS, DrawOverlays);
      prefs.putBoolean(PREFS_DRAWGRID, DrawGrid);
      prefs.putBoolean(PREFS_SHOWAMBIENT, ShowAmbientRanges);
      prefs.putInt(PREFS_SIDEBARCONTROLS, SidebarControls);
      prefs.putInt(PREFS_LAYERFLAGS, LayerFlags);
      prefs.putInt(PREFS_SHOWREALANIMS, ShowRealAnimations);
      prefs.putInt(PREFS_TIMEOFDAY, TimeOfDay);
      prefs.putInt(PREFS_ZOOMLEVEL, ZoomLevel);
      prefs.putInt(PREFS_MINIMAP, MiniMap);
    }
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      e.printStackTrace();
    }
  }

  // Makes sure that all settings are valid
  private static void validateSettings()
  {
    int mask = (1 << LayerManager.getLayerTypeCount()) - 1;
    LayerFlags &= mask;

    SidebarControls &= (ViewerConstants.SIDEBAR_VISUALSTATE |
                        ViewerConstants.SIDEBAR_LAYERS |
                        ViewerConstants.SIDEBAR_MINIMAPS);
    ShowRealAnimations = Math.min(Math.max(ShowRealAnimations, ViewerConstants.ANIM_SHOW_NONE),
                                  ViewerConstants.ANIM_SHOW_ANIMATED);
    TimeOfDay = Math.min(Math.max(TimeOfDay, ViewerConstants.TIME_0), ViewerConstants.TIME_23);
    ZoomLevel = Math.min(Math.max(ZoomLevel, 0), ItemZoomFactor.length - 1);
    InterpolationMap = Math.min(Math.max(InterpolationMap, ViewerConstants.FILTERING_AUTO),
                                ViewerConstants.FILTERING_BILINEAR);
    InterpolationAnim = Math.min(Math.max(InterpolationAnim, ViewerConstants.FILTERING_AUTO),
                                 ViewerConstants.FILTERING_BILINEAR);
    FrameRateOverlays = Math.min(Math.max(FrameRateOverlays, 1.0), 30.0);
    FrameRateAnimations = Math.min(Math.max(FrameRateAnimations, 1.0), 30.0);
    MiniMapAlpha = Math.min(Math.max(MiniMapAlpha, 0.0), 1.0);
    MiniMap = Math.min(Math.max(MiniMap, ViewerConstants.MAP_NONE), ViewerConstants.MAP_HEIGHT);

    // validating layers z-order
    mask = 0;
    // 1. checking for duplicates
    int i = 0;
    while (i < ListLayerOrder.size()) {
      int bit = 1 << getLayerStackingTypeIndex(ListLayerOrder.get(i));
      if ((mask & bit) != 0) {
        ListLayerOrder.remove(i);
        continue;
      } else {
        mask |= bit;
      }
      i++;
    }
    // 2. adding missing layers
    for (i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
      int bit = 1 << i;
      if ((mask & bit) == 0) {
        ListLayerOrder.add(ViewerConstants.LayerStackingType.values()[i]);
      }
    }
  }

  public static List<ViewerConstants.LayerStackingType> getDefaultLayerOrder()
  {
    List<ViewerConstants.LayerStackingType> list = new ArrayList<ViewerConstants.LayerStackingType>();
    Collections.addAll(list, DefaultLayerOrder);
    return list;
  }

  public static boolean getDefaultStoreVisualSettings()
  {
    return false;
  }

  public static boolean getDefaultDrawClosed()
  {
    return false;
  }

  public static boolean getDefaultDrawOverlays()
  {
    return true;
  }

  public static boolean getDefaultDrawGrid()
  {
    return false;
  }

  public static boolean getDefaultAmbientRanges()
  {
    return false;
  }

  public static boolean getDefaultEnableSchedules()
  {
    return false;
  }

  public static boolean getDefaultOverrideAnimVisibility()
  {
    return false;
  }

  public static int getDefaultSidebarControls()
  {
    return ViewerConstants.SIDEBAR_VISUALSTATE | ViewerConstants.SIDEBAR_LAYERS | ViewerConstants.SIDEBAR_MINIMAPS;
  }

  public static int getDefaultShowFrame()
  {
    return ViewerConstants.FRAME_AUTO;
  }

  public static int getDefaultInterpolationMap()
  {
    return ViewerConstants.FILTERING_AUTO;
  }

  public static int getDefaultInterpolationAnim()
  {
    return ViewerConstants.FILTERING_AUTO;
  }

  public static int getDefaultLayerFlags()
  {
    return 0;
  }

  public static int getDefaultShowRealAnimations()
  {
    return ViewerConstants.ANIM_SHOW_NONE;
  }

  public static int getDefaultTimeOfDay()
  {
    return ViewerConstants.getHourOf(ViewerConstants.LIGHTING_DAY);
  }

  public static int getDefaultZoomLevel()
  {
    return ZoomFactorIndexDefault;
  }

  public static double getDefaultFrameRateOverlays()
  {
    return 7.5;
  }

  public static double getDefaultFrameRateAnimations()
  {
    return 15.0;
  }

  public static double getDefaultMiniMapAlpha()
  {
    return 0.5;
  }

  public static int getDefaultMiniMap()
  {
    return ViewerConstants.MAP_NONE;
  }

  public static boolean getDefaultLabelActorsAre()
  {
    return true;
  }

  public static boolean getDefaultLabelActorsIni()
  {
    return true;
  }

//  public static boolean getDefaultLabelRegions()
//  {
//    return false;
//  }

  public static boolean getDefaultLabelEntrances()
  {
    return false;
  }

//  public static boolean getDefaultLabelContainers()
//  {
//    return false;
//  }

  public static boolean getDefaultLabelSounds()
  {
    return false;
  }

//  public static boolean getDefaultLabelDoors()
//  {
//    return false;
//  }

  public static boolean getDefaultLabelAnimations()
  {
    return false;
  }

  public static boolean getDefaultLabelMapNotes()
  {
    return false;
  }

  public static boolean getDefaultLabelSpawnPoints()
  {
    return false;
  }

  // Converts values from LayerStackingType to LayerType
  public static LayerType stackingToLayer(LayerStackingType type)
  {
    switch (type) {
      case ACTOR:
        return LayerType.ACTOR;
      case AMBIENT:
      case AMBIENT_RANGE:
        return LayerType.AMBIENT;
      case ANIMATION:
        return LayerType.ANIMATION;
      case AUTOMAP:
        return LayerType.AUTOMAP;
      case CONTAINER:
        return LayerType.CONTAINER;
      case DOOR:
        return LayerType.DOOR;
      case DOOR_POLY:
        return LayerType.DOOR_POLY;
      case ENTRANCE:
        return LayerType.ENTRANCE;
      case PRO_TRAP:
        return LayerType.PRO_TRAP;
      case REGION:
        return LayerType.REGION;
      case SPAWN_POINT:
        return LayerType.SPAWN_POINT;
      case TRANSITION:
        return LayerType.TRANSITION;
      case WALL_POLY:
        return LayerType.WALL_POLY;
      default:
        return null;
    }
  }

  // Converts values from LayerType to LayerStackingType (ignoring AmbientRange)
  public static LayerStackingType layerToStacking(LayerType type)
  {
    switch (type)
    {
      case ACTOR:
        return LayerStackingType.ACTOR;
      case AMBIENT:
        return LayerStackingType.AMBIENT;
      case ANIMATION:
        return LayerStackingType.ANIMATION;
      case AUTOMAP:
        return LayerStackingType.AUTOMAP;
      case CONTAINER:
        return LayerStackingType.CONTAINER;
      case DOOR:
        return LayerStackingType.DOOR;
      case DOOR_POLY:
        return LayerStackingType.DOOR_POLY;
      case ENTRANCE:
        return LayerStackingType.ENTRANCE;
      case PRO_TRAP:
        return LayerStackingType.PRO_TRAP;
      case REGION:
        return LayerStackingType.REGION;
      case SPAWN_POINT:
        return LayerStackingType.SPAWN_POINT;
      case TRANSITION:
        return LayerStackingType.TRANSITION;
      case WALL_POLY:
        return LayerStackingType.WALL_POLY;
      default:
        return null;
    }
  }

  // Returns the index of the specified enum type
  public static int getLayerStackingTypeIndex(ViewerConstants.LayerStackingType type)
  {
    for (int i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
      if (type == ViewerConstants.LayerStackingType.values()[i]) {
        return i;
      }
    }
    return -1;
  }
}

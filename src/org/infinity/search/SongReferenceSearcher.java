// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.datatype.Song2daBitmap;
import org.infinity.datatype.ResourceBitmap.RefEntry;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Song;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.io.StreamUtils;

public class SongReferenceSearcher extends AbstractReferenceSearcher
{
  private final List<Pattern> scriptActions = new ArrayList<Pattern>();

  private long songId;
  private ResourceEntry songEntry;

  public SongReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "BCS", "BS", "DLG"},
          new boolean[]{true, true, false, true}, parent);
    init(targetEntry);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof AreResource) {
      searchAre(entry, (AreResource)resource);
    } else if (resource instanceof BcsResource) {
      searchBcs(entry, (BcsResource)resource);
    } else if (resource instanceof DlgResource) {
      searchDlg(entry, (DlgResource)resource);
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    }
  }

  private void searchBcs(ResourceEntry entry, BcsResource bcs)
  {
//    Decompiler decompiler = new Decompiler(bcs.getCode(), true);
    Decompiler decompiler = new Decompiler(bcs.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(false);
    try {
      String text = decompiler.decompile();
      searchText(entry, null, text);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void searchDlg(ResourceEntry entry, DlgResource dlg)
  {
    List<StructEntry> list = dlg.getList();
    for (final StructEntry e: list) {
      if (e instanceof AbstractCode) {
        String text = ((AbstractCode)e).getText();
        searchText(entry, e, text);
      }
    }
  }

  private void searchText(ResourceEntry entry, StructEntry res, String text)
  {
    for (final Pattern pattern: scriptActions) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        addHit(entry, matcher.group(), res);
      }
    }
  }

  private void searchAre(ResourceEntry entry, AreResource are)
  {
    StructEntry se = are.getAttribute(Song.ARE_SONGS);
    if (se instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)se);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    List<StructEntry> list = struct.getFlatList();
    for (final StructEntry e: list) {
      if (e instanceof Song2daBitmap) {
        int v = ((Song2daBitmap)e).getValue();
        if (v == songId) {
          addHit(entry, String.format("%s (%d)", songEntry.getResourceName(), songId), e);
        }
      }
    }
  }

  private void init(ResourceEntry targetEntry)
  {
    songEntry = targetEntry;

    songId = -1L;
    Song2daBitmap songBitmap = new Song2daBitmap(StreamUtils.getByteBuffer(4), 0, 4);
    List<RefEntry> resList = songBitmap.getResourceList();
    for (final RefEntry refEntry: resList) {
      ResourceEntry entry = refEntry.getResourceEntry();
      if (entry != null && entry.equals(targetEntry)) {
        songId = refEntry.getValue();
        break;
      }
    }

    if (songId >= 0) {
      if (Profile.getGame() != Profile.Game.PST) {
        scriptActions.add(Pattern.compile("StartMusic\\(" + Long.toString(songId) + ",.+\\)"));
        IdsMap map = null;
        if (ResourceFactory.resourceExists("SONGLIST.IDS")) {
          map = IdsMapCache.get("SONGLIST.IDS");
        } else if (Profile.getGame() == Profile.Game.IWD2) {
          map = IdsMapCache.get("MUSIC.IDS");
        }
        if (map != null && map.get(songId) != null) {
          String musicId = map.get(songId).getSymbol();
          scriptActions.add(Pattern.compile("SetMusic\\(.+?," + Long.toString(songId) + "\\)"));
          if (musicId != null && !musicId.isEmpty()) {
            scriptActions.add(Pattern.compile("SetMusic\\(.+?," + musicId + "\\)"));
          }
        }
      }
    }
  }
}

// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;

public final class TohResource extends AbstractStruct implements Resource
{
  // TOH-specific field labels
  public static final String TOH_NUM_ENTRIES    = "# strref entries";
  public static final String TOH_OFFSET_ENTRIES = "Strref entries offset";
  public static final String TOH_LANGUAGE_TYPE  = "Language type";

  public TohResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public void close() throws Exception
  {
    // don't save changes
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    int startOffset = offset;
    boolean isEnhanced = Profile.isEnhancedEdition() && (buffer.getInt(offset + 4) == 2);
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new DecNumber(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new DecNumber(buffer, offset + 8, 4, TOH_LANGUAGE_TYPE));
    SectionCount scStrref = new SectionCount(buffer, offset + 12, 4, TOH_NUM_ENTRIES, StrRefEntry.class);
    addField(scStrref);
    SectionOffset soStrref = null;
    if (isEnhanced) {
      soStrref = new SectionOffset(buffer, offset + 16, TOH_OFFSET_ENTRIES, StrRefEntry.class);
      addField(soStrref);
    } else {
      addField(new Unknown(buffer, offset + 16, 4));
    }

    List<Integer> ofsList = null;
    offset = 20;
    if (isEnhanced) {
      offset = soStrref.getValue();
      ofsList = new ArrayList<Integer>(scStrref.getValue());
    }
    for (int i = 0; i < scStrref.getValue(); i++) {
      if (isEnhanced) {
        // storing string offset for later
        int ofs = soStrref.getValue() + buffer.getInt(offset + 4);
        ofsList.add(ofs);
        // adding strref entries structure
        StrRefEntry2 entry = new StrRefEntry2(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      } else {
        StrRefEntry entry = new StrRefEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      }
    }

    if (isEnhanced) {
      for (int i = 0; i < scStrref.getValue(); i++) {
        StringEntry2 entry = new StringEntry2(this, buffer, startOffset + ofsList.get(i), i);
        addField(entry);
        offset += entry.getEndOffset();
      }
    }

    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.SAVE);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }
}

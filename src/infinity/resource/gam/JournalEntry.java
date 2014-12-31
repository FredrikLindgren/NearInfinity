// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.ResourceFactory;
import infinity.util.LongIntegerHashMap;

final class JournalEntry extends AbstractStruct implements AddRemovable
{
  private static final LongIntegerHashMap<String> chapter = new LongIntegerHashMap<String>();
  private static final String s_section[] = new String[]{"User notes", "Quests", "Done quests",
                                                         "Journal"};

  static {
    chapter.put(new Long(0x1f), "From talk override");
    chapter.put(new Long(0xff), "From dialog.tlk");
  }


  JournalEntry() throws Exception
  {
    super(null, "Journal entry", new byte[12], 0);
  }

  JournalEntry(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Journal entry " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, "Text"));
    addField(new DecNumber(buffer, offset + 4, 4, "Time (ticks)"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.isEnhancedEdition()) {
      addField(new UnsignDecNumber(buffer, offset + 8, 1, "Chapter"));
      addField(new Unknown(buffer, offset + 9, 1));
      addField(new Flag(buffer, offset + 10, 1, "Section", s_section));
      addField(new HashBitmap(buffer, offset + 11, 1, "Text source", chapter));
    }
    else {
      addField(new DecNumber(buffer, offset + 8, 2, "Chapter"));
      addField(new Unknown(buffer, offset + 10, 2));
    }
    return offset + 12;
  }
}


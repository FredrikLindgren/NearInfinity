// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Transition extends AbstractStruct implements AddRemovable
{
  public static final String FMT_NAME = "Response %1$d";

  private static final String[] s_flag = {"No flags set", "Text associated", "Trigger", "Action",
                                          "Terminates dialogue", "Journal entry", "", "Add unsolved quest",
                                          "Add journal note", "Add solved quest"};
  private int nr;

  Transition() throws Exception
  {
    super(null, "Response", new byte[32], 0);
  }

  Transition(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, String.format(FMT_NAME, nr), buffer, offset);
    this.nr = nr;
  }

  public int getActionIndex()
  {
    if (getFlag().isFlagSet(2)) {
      return ((DecNumber)getAttribute("Action index")).getValue();
    } else {
      return -1;
    }
  }

  public StringRef getAssociatedText()
  {
    return (StringRef)getAttribute("Associated text");
  }

  public Flag getFlag()
  {
    return (Flag)getAttribute("Flags");
  }

  public StringRef getJournalEntry()
  {
    return (StringRef)getAttribute("Journal entry");
  }

  public ResourceRef getNextDialog()
  {
    return (ResourceRef)getAttribute("Next dialogue");
  }

  public int getNextDialogState()
  {
    return ((DecNumber)getAttribute("Next dialogue state")).getValue();
  }

  public int getNumber()
  {
    return nr;
  }

  public int getTriggerIndex()
  {
    if (getFlag().isFlagSet(1)) {
      return ((DecNumber)getAttribute("Trigger index")).getValue();
    } else {
      return -1;
    }
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
    addField(new Flag(buffer, offset, 4, "Flags", s_flag));
    addField(new StringRef(buffer, offset + 4, "Associated text"));
    addField(new StringRef(buffer, offset + 8, "Journal entry"));
    addField(new DecNumber(buffer, offset + 12, 4, "Trigger index"));
    addField(new DecNumber(buffer, offset + 16, 4, "Action index"));
    addField(new ResourceRef(buffer, offset + 20, "Next dialogue", "DLG"));
    addField(new DecNumber(buffer, offset + 28, 4, "Next dialogue state"));
    return offset + 32;
  }
}


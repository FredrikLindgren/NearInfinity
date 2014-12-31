// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.ResourceFactory;

public final class Item extends AbstractStruct implements AddRemovable
{
  private static final String[] s_itemflag = {"No flags set", "Identified", "Not stealable", "Stolen",
                                              "Undroppable"};

  public Item() throws Exception
  {
    super(null, "Item", new byte[20], 0);
  }

  public Item(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Item " + nr, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, "Item", "ITM"));
    if (ResourceFactory.isEnhancedEdition()) {
      addField(new UnsignDecNumber(buffer, offset + 8, 2, "Duration"));
    } else {
      addField(new Unknown(buffer, offset + 8, 2));
    }
    addField(new DecNumber(buffer, offset + 10, 2, "Quantity/Charges 1"));
    addField(new DecNumber(buffer, offset + 12, 2, "Quantity/Charges 2"));
    addField(new DecNumber(buffer, offset + 14, 2, "Quantity/Charges 3"));
    addField(new Flag(buffer, offset + 16, 4, "Flags", s_itemflag));
    return offset + 20;
  }
}


// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Entrance extends AbstractStruct implements AddRemovable
{
  Entrance() throws Exception
  {
    super(null, "Entrance", new byte[104], 0);
  }

  Entrance(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Entrance " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new DecNumber(buffer, offset + 32, 2, "Location: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Location: Y"));
    addField(new Bitmap(buffer, offset + 36, 4, "Orientation", Actor.s_orientation));
    addField(new Unknown(buffer, offset + 40, 64));
    return offset + 104;
  }
}


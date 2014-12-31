// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;

final class UnknownSection2 extends AbstractStruct
{
  UnknownSection2(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Unknown section", buffer, offset);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new Unknown(buffer, offset, 20));
    return offset + 20;
  }
}


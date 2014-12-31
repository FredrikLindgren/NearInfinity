// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.StructEntry;
import infinity.resource.vertex.Vertex;

public abstract class Polygon extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  public static final String[] s_flags = { "No flags set", "Shade wall", "Semi transparent",
                                            "Hovering wall", "Cover animations", "Unknown",
                                            "Unknown", "Unknown", "Is door" };

  public Polygon(AbstractStruct superStruct, String name, byte buffer[], int offset) throws Exception
  {
    super(superStruct, name, buffer, offset, 8);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Vertex()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Vertex) {
      int index = ((DecNumber)getAttribute("Vertex index")).getValue();
      index += ((DecNumber)getAttribute("# vertices")).getValue();
      AbstractStruct superStruct = getSuperStruct();
      while (superStruct.getSuperStruct() != null)
        superStruct = superStruct.getSuperStruct();
      int offset = ((HexNumber)superStruct.getAttribute("Vertices offset")).getValue();
      datatype.setOffset(offset + 4 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  public void readVertices(byte buffer[], int offset) throws Exception
  {
    DecNumber firstVertex = (DecNumber)getAttribute("Vertex index");
    DecNumber numVertices = (DecNumber)getAttribute("# vertices");
    for (int i = 0; i < numVertices.getValue(); i++) {
      addField(new Vertex(this, buffer, offset + 4 * (firstVertex.getValue() + i), i));
    }
  }

  public int updateVertices(int offset, int startIndex)
  {
    ((DecNumber)getAttribute("Vertex index")).setValue(startIndex);
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof Vertex) {
        entry.setOffset(offset);
        ((AbstractStruct)entry).realignStructOffsets();
        offset += 4;
        count++;
      }
    }
    ((DecNumber)getAttribute("# vertices")).setValue(count);
    return count;
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new DecNumber(buffer, offset, 4, "Vertex index"));
    addField(new SectionCount(buffer, offset + 4, 4, "# vertices", Vertex.class));
    addField(new Flag(buffer, offset + 8, 1, "Polygon flags", s_flags));
    addField(new Unknown(buffer, offset + 9, 1));
    addField(new DecNumber(buffer, offset + 10, 2, "Minimum coordinate: X"));
    addField(new DecNumber(buffer, offset + 12, 2, "Maximum coordinate: X"));
    addField(new DecNumber(buffer, offset + 14, 2, "Minimum coordinate: Y"));
    addField(new DecNumber(buffer, offset + 16, 2, "Maximum coordinate: Y"));
    return offset + 18;
  }
}


// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import java.util.List;

import infinity.datatype.DecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public class Vertex extends AbstractStruct implements AddRemovable
{
  public Vertex() throws Exception
  {
    super(null, "Vertex", new byte[4], 0, 2);
  }

  Vertex(AbstractStruct superStruct, String name, byte buffer[], int offset) throws Exception
  {
    super(superStruct, name, buffer, offset, 2);
  }

  public Vertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Vertex " + nr, buffer, offset, 2);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset)
  {
    addField(new DecNumber(buffer, offset, 2, "X"));
    addField(new DecNumber(buffer, offset + 2, 2, "Y"));
    return offset + 4;
  }


  /** Returns the x coordinate of the vertex. */
  public int getX()
  {
    DecNumber dn = (DecNumber)getAttribute(getOffset(), false);
    if (dn != null) {
      return dn.getValue();
    } else {
      return 0;
    }
  }

  /** Assigns a new x coordinate to the vertex. */
  public void setX(int value)
  {
    DecNumber dn = (DecNumber)getAttribute(getOffset(), false);
    if (dn != null) {
      dn.setValue(value);
    }
  }

  /** Returns the y coordinate of the vertex. */
  public int getY()
  {
    DecNumber dn = (DecNumber)getAttribute(getOffset() + 2, false);
    if (dn != null) {
      return dn.getValue();
    } else {
      return 0;
    }
  }

  /** Assigns a new y coordinate to the vertex. */
  public void setY(int value)
  {
    DecNumber dn = (DecNumber)getAttribute(getOffset() + 2, false);
    if (dn != null) {
      dn.setValue(value);
    }
  }

  /** Assigns new x and y coordinates to the vertex. */
  public void setPoint(int x, int y)
  {
    setX(x);
    setY(y);
  }

  /**
   * Sorts the entries defined in the specified array of vertices.
   * @param vertices The Vertex array to sort.
   * @return Returns the sorted Vertex array.
   */
  public static Vertex[] sortVertices(Vertex[] vertices)
  {
    if (vertices != null && vertices.length > 1) {
      int index = -1;

      // 1. determining bottom-left most point
      int curX = Integer.MAX_VALUE, curY = Integer.MIN_VALUE;
      for (int i = 0; i < vertices.length; i++) {
        int y = vertices[i].getY();
        if (y >= curY) {
          int x = vertices[i].getX();
          if (x < curX) {
            index = i;
          }
        }
      }

      // 2. shifting elements
      while (index > 0) {
        for (int idx = 1; idx < vertices.length; idx++) {
          Vertex v = vertices[idx-1];
          vertices[idx-1] = vertices[idx];
          vertices[idx] = v;
        }
        index--;
      }
    }
    return vertices;
  }

  /**
   * Sorts the entries defined in the specified list of vertices.
   * @param vertices The list of Vertex objects to sort.
   * @return Returns the sorted Vertex list.
   */
  public static List<Vertex> sortVertices(List<Vertex> vertices)
  {
    if (vertices != null && vertices.size() > 1) {
      int index = -1;

      // 1. determining bottom-left most point
      int curX = Integer.MAX_VALUE, curY = Integer.MIN_VALUE;
      for (int i = 0, size = vertices.size(); i < size; i++) {
        Vertex v = vertices.get(i);
        int y = v.getY();
        if (y >= curY) {
          int x = v.getX();
          if (x < curX) {
            index = i;
          }
        }
      }

      // 2. shifting elements
      while (index > 0) {
        for (int idx = 1, size = vertices.size(); idx < size; idx++) {
          Vertex v = vertices.get(idx-1);
          vertices.set(idx-1, vertices.get(idx));
          vertices.set(idx, v);
        }
        index--;
      }
    }
    return vertices;
  }
}


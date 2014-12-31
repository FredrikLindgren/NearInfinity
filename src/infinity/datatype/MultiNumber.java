// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.BrowserMenuBar;
import infinity.gui.StructViewer;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

/**
 * A Number object consisting of multiple values of a given number of bits.
 * @author argent77
 */
public class MultiNumber extends Datatype implements Editable
{
  private int value;
  private ValueTableModel mValues;
  private JTable tValues;

  /**
   * Constructs a Number object consisting of multiple values of a given number of bits.
   * @param buffer The buffer containing resource data for this type.
   * @param offset Resource offset
   * @param length Resource length in bytes. Supported lengths: 1, 2, 3, 4
   * @param name Field name
   * @param numBits Number of bits for each value being part of the Number object.
   * @param numValues Number of values to consider. Supported range: [1, length*8/numBits]
   * @param valueNames List of individual field names for each contained value.
   */
  public MultiNumber(byte[] buffer, int offset, int length, String name,
                     int numBits, int numValues, String[] valueNames)
  {
    this(null, buffer, offset, length, name, numBits, numValues, valueNames);
  }

  /**
   * Constructs a Number object consisting of multiple values of a given number of bits.
   * @param parent A parent structure containing to this datatype object.
   * @param buffer The buffer containing resource data for this type.
   * @param offset Resource offset
   * @param length Resource length in bytes. Supported lengths: 1, 2, 3, 4
   * @param name Field name
   * @param numBits Number of bits for each value being part of the Number object.
   * @param numValues Number of values to consider. Supported range: [1, length*8/numBits]
   * @param valueNames List of individual field names for each contained value.
   */
  public MultiNumber(StructEntry parent, byte[] buffer, int offset, int length, String name,
                     int numBits, int numValues, String[] valueNames)
  {
    super(offset, length, name);

    read(buffer, offset);

    if (numBits < 1 || numBits > (length*8)) numBits = length*8;

    if (numValues < 1) {
      numValues = 1;
    } else if (numValues > (length*8/numBits)) {
      numValues = length*8/numBits;
    }

    mValues = new ValueTableModel(value, numBits, numValues, valueNames);
  }

//--------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    tValues = new JTable(mValues);
    tValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tValues.setFont(BrowserMenuBar.getInstance().getScriptFont());
    tValues.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    tValues.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.GRAY));
    tValues.getTableHeader().setReorderingAllowed(false);
    tValues.getTableHeader().setResizingAllowed(true);
    tValues.setPreferredScrollableViewportSize(tValues.getPreferredSize());
    JScrollPane scroll = new JScrollPane(tValues);
    scroll.setBorder(BorderFactory.createEmptyBorder());

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(scroll, gbc);

    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    panel.add(bUpdate, gbc);

    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  @Override
  public void select()
  {
    mValues.setValue(value);
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    value = mValues.getValue();
    return true;
  }

//--------------------- End Interface Editable ---------------------

//--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeInt(os, value);
  }

//--------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        value = DynamicArray.getByte(buffer, offset);
        break;
      case 2:
        value = DynamicArray.getShort(buffer, offset);
        break;
      case 3:
        value = DynamicArray.getInt24(buffer, offset);
        break;
      case 4:
        value = DynamicArray.getInt(buffer, offset);
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getValueCount(); i++) {
      sb.append(String.format("%1$s: %2$d", getValueName(i), getValue(i)));
      if (i+1 < getValueCount())
        sb.append(", ");
    }
    return sb.toString();
  }

  /** Returns number of bits per value. */
  public int getBits()
  {
    return mValues.getBitsPerValue();
  }

  /** Returns number of values stored in the Number object. */
  public int getValueCount()
  {
    return mValues.getValueCount();
  }

  /** Returns the label associated with the specified value. */
  public String getValueName(int idx)
  {
    return mValues.getValueName(idx);
  }

  /** Returns value of the whole Number object. */
  public int getValue()
  {
    return value;
  }

  /** Returns the specified value. */
  public int getValue(int idx)
  {
    if (idx >= 0 && idx < mValues.getValueCount()) {
      if (getBits() < 32) {
        return (value >>> (idx*getBits())) & ((1 << getBits()) - 1);
      } else {
        return getValue();
      }
    }
    return 0;
  }

  /** Set the value for the whole Number object. */
  public void setValue(int value)
  {
    mValues.setValue(value);
    this.value = mValues.getValue();
  }

  /** Sets the specified value. */
  public void setValue(int idx, int value)
  {
    mValues.setValue(idx, value);
    this.value = mValues.getValue();
  }


//-------------------------- INNER CLASSES --------------------------

  // Manages a fixed two columns table with a given number of rows
  private static class ValueTableModel extends AbstractTableModel
  {
    private static final int ATTRIBUTE = 0;
    private static final int VALUE = 1;

    private final Object[][] data;

    private int bits;
    private int numValues;

    public ValueTableModel(Integer value, int bits, int numValues, String[] labels)
    {
      if (bits < 1) bits = 1; else if (bits > 32) bits = 32;
      if (numValues < 1 || numValues > (32 / bits)) numValues = 32 / bits;

      this.bits = bits;
      this.numValues = numValues;
      data = new Object[2][numValues];
      for (int i = 0; i < numValues; i++) {
        if (labels != null && i < labels.length && labels[i] != null) {
          data[ATTRIBUTE][i] = labels[i];
        } else {
          data[ATTRIBUTE][i] = "Value " + Integer.toString(i+1);
        }
        data[VALUE][i] = Integer.valueOf((value >>> (i*bits)) & ((1 << bits) - 1));
      }
    }

//--------------------- Begin Class AbstractTableModel ---------------------

    @Override
    public int getRowCount()
    {
      return numValues;
    }

    @Override
    public int getColumnCount()
    {
      return 2;   // fixed
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      if (rowIndex >= 0 && rowIndex < numValues && columnIndex >= 0 && columnIndex < 2) {
        return data[columnIndex][rowIndex];
      }
      return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
      if (columnIndex == 1) {
        if (rowIndex >= 0 && rowIndex < numValues) {
          try {
            int newVal = Integer.parseInt(aValue.toString());
            if (newVal < 0) newVal = 0;
            if (newVal >= (1 << bits)) newVal = (1 << bits) - 1;
            data[VALUE][rowIndex] = Integer.valueOf(newVal);
            fireTableCellUpdated(rowIndex, columnIndex);
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        }
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return (columnIndex == VALUE);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
      if (columnIndex >= 0 && columnIndex < 2) {
        return getValueAt(0, columnIndex).getClass();
      } else {
        return Object.class;
      }
    }

    @Override
    public String getColumnName(int columnIndex)
    {
      switch (columnIndex) {
        case ATTRIBUTE:
          return "Attribute";
        case VALUE:
          return "Value";
        default:
          return "";
      }
    }

  //--------------------- End Class AbstractTableModel ---------------------

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < numValues; i++) {
        sb.append(String.format("%1$s: %2$d", (String)data[ATTRIBUTE][i], ((Integer)data[VALUE][i]).intValue()));
        if (i+1 < numValues)
          sb.append(", ");
      }
      return sb.toString();
    }

    public int getValue()
    {
      int retVal = 0;
      for (int i = 0; i < numValues; i++) {
        retVal |= (getValue(i) & ((1 << bits) - 1)) << (i*bits);
      }
      return retVal;
    }

    public int getValue(int rowIndex)
    {
      if (rowIndex >= 0 && rowIndex < numValues) {
        return ((Integer)data[VALUE][rowIndex]).intValue();
      }
      return 0;
    }

    public void setValue(int v)
    {
      for (int i = 0; i < numValues; i++, v >>>= bits) {
        data[VALUE][i] = Integer.valueOf(v & ((1 << bits) - 1));
      }
    }

    public void setValue(int rowIndex, int v)
    {
      if (rowIndex >= 0 && rowIndex < numValues) {
        data[VALUE][rowIndex] = Integer.valueOf(v & ((1 << bits) - 1));
      }
    }

    public int getValueCount()
    {
      return numValues;
    }

    public int getBitsPerValue()
    {
      return bits;
    }

    public String getValueName(int rowIndex)
    {
      if (rowIndex >= 0 && rowIndex < numValues) {
        return (String)data[ATTRIBUTE][rowIndex];
      }
      return "";
    }
  }
}

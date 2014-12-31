// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;
import infinity.util.io.FileWriterNI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

public final class TextBitmap extends Datatype implements Editable
{
  private final String[] ids;
  private final String[] names;
  private JTable table;
  private String text;

  public TextBitmap(byte buffer[], int offset, int length, String name, String ids[], String names[])
  {
    this(null, buffer, offset, length, name, ids, names);
  }

  public TextBitmap(StructEntry parent, byte buffer[], int offset, int length, String name,
                    String ids[], String names[])
  {
    super(parent, offset, length, name);
    read(buffer, offset);
    this.ids = ids;
    this.names = names;
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    if (table == null) {
      table = new JTable(new BitmapTableModel());
      table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setDragEnabled(false);
      table.setTableHeader(null);
    }
    for (int i = 0; i < ids.length; i++)
      if (ids[i].equalsIgnoreCase(text))
        table.getSelectionModel().setSelectionInterval(i, i);

    JScrollPane scroll = new JScrollPane(table);
    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    panel.add(scroll);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets.left = 6;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_MEDIUM);
    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  @Override
  public void select()
  {
    table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), 0, false));
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    int index = table.getSelectedRow();
    if (index == -1)
      return false;
    text = ids[index];
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    FileWriterNI.writeString(os, text, getSize());
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    text = DynamicArray.getString(buffer, offset, getSize());

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    for (int i = 0; i < ids.length; i++)
      if (ids[i].equalsIgnoreCase(text))
        return text + " - " + names[i];
    return text;
  }

  public String getIdsName()
  {
    if (text != null) {
      for (int i = 0; i < ids.length; i++) {
        if (text.equals(ids[i])) {
          if (i < names.length) {
            return names[i];
          } else {
            break;
          }
        }
      }
    }
    return "";
  }

  public String getIdsValue()
  {
    return (text != null) ? text : "";
  }

// -------------------------- INNER CLASSES --------------------------

  private final class BitmapTableModel extends AbstractTableModel
  {
    private BitmapTableModel()
    {
    }

    @Override
    public int getRowCount()
    {
      return ids.length;
    }

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      if (columnIndex == 0)
        return ids[rowIndex];
      return names[rowIndex];
    }
  }
}


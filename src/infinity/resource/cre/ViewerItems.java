// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.ToolTipTableCellRenderer;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

final class ViewerItems extends JPanel implements ActionListener, ListSelectionListener, TableModelListener
{
  private final InventoryTableModel tableModel = new InventoryTableModel();
  private final JButton bOpen;
  private final JTable table;
  private final List<StructEntry> slots = new ArrayList<StructEntry>();

  ViewerItems(CreResource cre)
  {
    super(new BorderLayout(0, 3));
    List<Item> items = new ArrayList<Item>();
    HexNumber slots_offset = (HexNumber)cre.getAttribute("Item slots offset");
    for (int i = 0; i < cre.getFieldCount(); i++) {
      StructEntry entry = cre.getField(i);
      if (entry instanceof Item)
        items.add((Item)entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals("Weapon slot selected")
               && !entry.getName().equals("Weapon ability selected"))
        slots.add(entry);
    }
    for (int i = 0; i < slots.size(); i++) {
      DecNumber slot = (DecNumber)slots.get(i);
      if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
        Item item = items.get(slot.getValue());
        ResourceRef itemRef = (ResourceRef)item.getAttribute("Item");
        tableModel.addEntry(slot.getName(), itemRef);
      }
      else
        tableModel.addEntry(slot.getName(), null);
    }
    table = new JTable(tableModel);
    table.setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.getColumnModel().getColumn(0).setMaxWidth(175);
    table.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
          ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 2);
          if (ref != null) {
            Resource res = ResourceFactory.getResource(
                    ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
            new ViewFrame(getTopLevelAncestor(), res);
          }
        }
      }
    });
    JScrollPane scroll = new JScrollPane(table);
    bOpen = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bOpen.addActionListener(this);
//    bOpen.setEnabled(tableModel.getRowCount() > 0);
    table.getSelectionModel().setSelectionInterval(0, 0);
    add(new JLabel("Items"), BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    add(bOpen, BorderLayout.SOUTH);
    cre.addTableModelListener(this);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 1);
      if (ref != null) {
        Resource res = ResourceFactory.getResource(
                ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
        new ViewFrame(getTopLevelAncestor(), res);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (table.getSelectedRow() == -1)
      bOpen.setEnabled(false);
    else
      bOpen.setEnabled(tableModel.getValueAt(table.getSelectedRow(), 1) != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent event)
  {
    if (event.getType() == TableModelEvent.UPDATE) {
      CreResource cre = (CreResource)event.getSource();
      Object changed = cre.getField(event.getFirstRow());
      if (slots.contains(changed)) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0; i < cre.getFieldCount(); i++) {
          StructEntry entry = cre.getField(i);
          if (entry instanceof Item)
            items.add((Item)entry);
        }
        table.clearSelection();
        tableModel.clear();
        for (int i = 0; i < slots.size(); i++) {
          DecNumber slot = (DecNumber)slots.get(i);
          if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
            Item item = items.get(slot.getValue());
            ResourceRef itemRef = (ResourceRef)item.getAttribute("Item");
            tableModel.addEntry(slot.getName(), itemRef);
          }
          else
            tableModel.addEntry(slot.getName(), null);
        }
        tableModel.fireTableDataChanged();
        table.getSelectionModel().setSelectionInterval(0, 0);
      }
    }
  }

// --------------------- End Interface TableModelListener ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class InventoryTableModel extends AbstractTableModel
  {
    private final List<InventoryTableEntry> list = new ArrayList<InventoryTableEntry>();

    private InventoryTableModel()
    {
    }

    private void clear()
    {
      list.clear();
    }

    private void addEntry(String slot, ResourceRef item)
    {
      list.add(new InventoryTableEntry(slot, item));
    }

    @Override
    public int getRowCount()
    {
      return list.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      InventoryTableEntry entry = list.get(rowIndex);
      if (columnIndex == 0)
        return entry.slot;
      return entry.item;
    }

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public String getColumnName(int column)
    {
      if (column == 0)
        return "Slot";
      return "Item";
    }
  }

  private static final class InventoryTableEntry
  {
    private final String slot;
    private final ResourceRef item;

    private InventoryTableEntry(String slot, ResourceRef item)
    {
      this.slot = slot;
      this.item = item;
    }
  }
}


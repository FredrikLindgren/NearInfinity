// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.gui.BrowserMenuBar;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.SortableTable;
import infinity.gui.TableItem;
import infinity.gui.ViewFrame;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.cre.CreResource;
import infinity.resource.cre.Item;
import infinity.resource.key.ResourceEntry;
import infinity.util.io.FileNI;
import infinity.util.io.FileWriterNI;
import infinity.util.io.PrintWriterNI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class CreInvChecker implements Runnable, ActionListener, ListSelectionListener
{
  private final List<StructEntry> items = new ArrayList<StructEntry>();
  private final List<StructEntry> slots = new ArrayList<StructEntry>();
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private SortableTable table;

  public CreInvChecker()
  {
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer().selectEntry(
                ((Item)table.getValueAt(row, 2)).getName());
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((AbstractStruct)resource).getViewer().selectEntry(((Item)table.getValueAt(row, 2)).getName());
      }
    } else if (event.getSource() == bsave) {
      JFileChooser fc = new JFileChooser(ResourceFactory.getRootDir());
      fc.setDialogTitle("Save search result");
      fc.setSelectedFile(new FileNI("result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        File output = fc.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try {
          PrintWriter pw = new PrintWriterNI(new BufferedWriter(new FileWriterNI(output)));
          pw.println("Result of CRE inventory check");
          pw.println("Number of hits: " + table.getRowCount());
          for (int i = 0; i < table.getRowCount(); i++)
            pw.println(table.getTableItemAt(i).toString());
          pw.close();
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(resultFrame, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    List<ResourceEntry> creFiles = ResourceFactory.getInstance().getResources("CRE");
    creFiles.addAll(ResourceFactory.getInstance().getResources("CHR"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking inventories...", null, 0, creFiles.size());

    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
    table = new SortableTable(Arrays.asList(new String[]{"File", "Name", "Item"}),
                              colClasses, Arrays.asList(new Integer[]{100, 100, 200}));

    for (int i = 0; i < creFiles.size(); i++) {
      ResourceEntry entry = creFiles.get(i);
      try {
        checkCreature(new CreResource(entry));
      } catch (Exception e) {
        e.printStackTrace();
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        blocker.setBlocked(false);
        return;
      }
    }

    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      resultFrame = new ChildFrame("Result of CRE inventory check", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
      JLabel count = new JLabel(table.getRowCount() + " hit(s) found", JLabel.CENTER);
      count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      bsave.setMnemonic('s');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      panel.add(bsave);
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row != -1) {
              ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
              ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
            }
          }
        }
      });
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      bsave.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
//    for (int i = 0; i < table.getRowCount(); i++) {
//      CreInvError error = (CreInvError)table.getTableItemAt(i);
//      System.out.println(error.resourceEntry + " (" + error.resourceEntry.getSearchString() + ") -> " + error.itemRef.getAttribute("Item"));
//    }
  }

// --------------------- End Interface Runnable ---------------------

  private void checkCreature(CreResource cre)
  {
    HexNumber slots_offset = (HexNumber)cre.getAttribute("Item slots offset");
    items.clear();
    slots.clear();
    for (int i = 0; i < cre.getFieldCount(); i++) {
      StructEntry entry = cre.getField(i);
      if (entry instanceof Item)
        items.add(entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals("Weapon slot selected")
               && !entry.getName().equals("Weapon ability selected"))
        slots.add(entry);
    }
    for (int i = 0; i < slots.size(); i++) {
      DecNumber slot = (DecNumber)slots.get(i);
      if (slot.getValue() >= 0 && slot.getValue() < items.size())
        items.set(slot.getValue(), slots_offset); // Dummy object
    }
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) != slots_offset) {
        Item item = (Item)items.get(i);
        table.addTableItem(new CreInvError(cre.getResourceEntry(), item));
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class CreInvError implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final Item itemRef;

    private CreInvError(ResourceEntry resourceEntry, Item itemRef)
    {
      this.resourceEntry = resourceEntry;
      this.itemRef = itemRef;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return resourceEntry.getSearchString();
      else
        return itemRef;
    }

    @Override
    public String toString()
    {
      return String.format("File: %1$s  Name: %2$s  %3$s",
                           resourceEntry.toString(), resourceEntry.getSearchString(), itemRef.toString());
    }
  }
}


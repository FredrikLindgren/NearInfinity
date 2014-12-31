// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.datatype.Datatype;
import infinity.datatype.DecNumber;
import infinity.datatype.Editable;
import infinity.datatype.EffectType;
import infinity.datatype.HexNumber;
import infinity.datatype.InlineEditable;
import infinity.datatype.Readable;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnknownBinary;
import infinity.datatype.UnknownDecimal;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Closeable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.Viewable;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.dlg.DlgResource;
import infinity.resource.dlg.State;
import infinity.resource.dlg.Transition;
import infinity.search.AttributeSearcher;
import infinity.search.DialogItemRefSearcher;
import infinity.search.DialogStateReferenceSearcher;
import infinity.search.ReferenceSearcher;
import infinity.util.StructClipboard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

public final class StructViewer extends JPanel implements ListSelectionListener, ActionListener,
                                                          ItemListener, ChangeListener, TableModelListener
{
  // Commonly used tab names
  public static final String TAB_EDIT           = "Edit";
  public static final String TAB_VIEW           = "View";
  public static final String TAB_RAW            = "Raw";

  // Menu commands
  public static final String CMD_COPYVALUE      = "VCopy";
  public static final String CMD_PASTEVALUE     = "VPaste";
  public static final String CMD_CUT            = "Cut";
  public static final String CMD_COPY           = "Copy";
  public static final String CMD_PASTE          = "Paste";
  public static final String CMD_TOHEX          = "ToHex";
  public static final String CMD_TOSTRING       = "ToStr";
  public static final String CMD_TOBIN          = "ToBin";
  public static final String CMD_TODEC          = "ToDec";
  public static final String CMD_TOINT          = "ToInt";
  public static final String CMD_RESET          = "ResetType";
  public static final String CMD_SHOWVIEWER     = "ShowView";
  public static final String CMD_SHOWNEWVIEWER  = "ShowNewView";
  public static final String UPDATE_VALUE       = "UpdateValue";

  // Identifiers for card layout elements
  private static final String CARD_EMPTY        = "Empty";
  private static final String CARD_EDIT         = "Edit";
  private static final String CARD_TEXT         = "Text";

  private static Class<? extends StructEntry> lastNameStruct, lastIndexStruct;
  private static String lastName;
  private static int lastIndex;
  private final AbstractStruct struct;
  private final CardLayout cards = new CardLayout();
  private final JMenuItem miCopyValue = createMenuItem(CMD_COPYVALUE, "Copy value", Icons.getIcon("Copy16.gif"), this);
  private final JMenuItem miPasteValue = createMenuItem(CMD_PASTEVALUE, "Replace value", Icons.getIcon("Paste16.gif"), this);
  private final JMenuItem miCut = createMenuItem(CMD_CUT, "Cut", Icons.getIcon("Cut16.gif"), this);
  private final JMenuItem miCopy = createMenuItem(CMD_COPY, "Copy", Icons.getIcon("Copy16.gif"), this);
  private final JMenuItem miPaste = createMenuItem(CMD_PASTE, "Paste", Icons.getIcon("Paste16.gif"), this);
  private final JMenuItem miToHex = createMenuItem(CMD_TOHEX, "Edit as hex", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miToString = createMenuItem(CMD_TOSTRING, "Edit as string", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miToBin = createMenuItem(CMD_TOBIN, "Edit as binary", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miToDec = createMenuItem(CMD_TODEC, "Edit as decimal", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miToInt = createMenuItem(CMD_TOINT, "Edit as number", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miReset = createMenuItem(CMD_RESET, "Reset field type", Icons.getIcon("Refresh16.gif"), this);
  private final JMenuItem miShowViewer = createMenuItem(CMD_SHOWVIEWER, "Show in viewer", null, this);
  private final JMenuItem miShowNewViewer = createMenuItem(CMD_COPYVALUE, "Show in new viewer", null, this);
  private final JPanel lowerpanel = new JPanel(cards);
  private final JPanel editpanel = new JPanel();
  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final JPopupMenu popupmenu = new JPopupMenu();
  private final InfinityTextArea tatext = new InfinityTextArea(true);
  private final StructTable table = new StructTable();
  private final HashMap<Integer, StructEntry> entryMap = new HashMap<Integer, StructEntry>();
  private final HashMap<Viewable, ViewFrame> viewMap = new HashMap<Viewable, ViewFrame>();
  private AddRemovable emptyTypes[];
  private JMenuItem miFindAttribute, miFindReferences, miFindStateReferences, miFindRefToItem;
  private Editable editable;
  private JTabbedPane tabbedPane;

  private static JMenuItem createMenuItem(String cmd, String text, Icon icon, ActionListener l)
  {
    JMenuItem m = new JMenuItem(text, icon);
    m.setActionCommand(cmd);
    if (l != null)
      m.addActionListener(l);
    return m;
  }


  public StructViewer(AbstractStruct struct)
  {
    this(struct, null);
  }

  public StructViewer(AbstractStruct struct, Collection<Component> extraComponents)
  {
    this.struct = struct;
    struct.addTableModelListener(this);
    table.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.setFont(BrowserMenuBar.getInstance().getScriptFont());
    table.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
          Object selected = table.getModel().getValueAt(table.getSelectedRow(), 1);
          if (selected instanceof Viewable) {
            createViewFrame(table.getTopLevelAncestor(), (Viewable)selected);
//            new ViewFrame(table.getTopLevelAncestor(), (Viewable)selected);
          }
        }
      }
    });
    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
    {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row,
                                                     int column)
      {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == 2)
          setHorizontalAlignment(JLabel.TRAILING);
        else
          setHorizontalAlignment(JLabel.LEADING);
        return this;
      }
    });

    popupmenu.add(miCopyValue);
    popupmenu.add(miPasteValue);
    popupmenu.add(miCut);
    popupmenu.add(miCopy);
    popupmenu.add(miPaste);
    popupmenu.add(miToHex);
    popupmenu.add(miToBin);
    popupmenu.add(miToDec);
    popupmenu.add(miToInt);
    popupmenu.add(miToString);
    popupmenu.add(miReset);
    if (struct instanceof DlgResource) {
      popupmenu.add(miShowViewer);
      popupmenu.add(miShowNewViewer);
    }
    table.addMouseListener(new PopupListener());
    miCopyValue.setEnabled(false);
    miPasteValue.setEnabled(false);
    miCut.setEnabled(false);
    miCopy.setEnabled(false);
    miPaste.setEnabled(
            StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    miToHex.setEnabled(false);
    miToBin.setEnabled(false);
    miToDec.setEnabled(false);
    miToInt.setEnabled(false);
    miToString.setEnabled(false);
    miReset.setEnabled(false);
    miShowViewer.setEnabled(false);
    miShowNewViewer.setEnabled(false);

    tatext.setHighlightCurrentLine(false);
    tatext.setEOLMarkersVisible(false);
    tatext.setEditable(false);
    tatext.setMargin(new Insets(3, 3, 3, 3));
    tatext.setFont(BrowserMenuBar.getInstance().getScriptFont());
    InfinityScrollPane scroll = new InfinityScrollPane(tatext, true);
    scroll.setLineNumbersEnabled(false);
    table.setModel(struct);
    table.getColumnModel().getColumn(0).setPreferredWidth(10);
    table.getColumnModel().getColumn(1).setPreferredWidth(400);
    if (table.getColumnCount() == 3)
      table.getColumnModel().getColumn(2).setPreferredWidth(6);

    lowerpanel.add(scroll, CARD_TEXT);
    lowerpanel.add(editpanel, CARD_EDIT);
    lowerpanel.add(new JPanel(), CARD_EMPTY);
    cards.show(lowerpanel, CARD_EMPTY);

    if (struct instanceof HasAddRemovable && struct.getFieldCount() > 0) {
      try {
        emptyTypes = ((HasAddRemovable)struct).getAddRemovables();
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (emptyTypes == null)
        emptyTypes = new AddRemovable[0];
      JMenuItem menuItems[] = new JMenuItem[emptyTypes.length];
      for (int i = 0; i < menuItems.length; i++) {
        menuItems[i] = new JMenuItem(emptyTypes[i].getName());
      }
      if (emptyTypes.length > 0) {
        ButtonPopupMenu bpmAdd = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.Add);
        bpmAdd.setMenuItems(menuItems);
        bpmAdd.addItemListener(this);
        JButton bRemove = (JButton)buttonPanel.addControl(ButtonPanel.Control.Remove);
        bRemove.setEnabled(false);
        bRemove.addActionListener(this);
      }
    }

    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FindMenu);
    bpmFind.addItemListener(this);
    if (struct instanceof DlgResource) {
      miFindAttribute = new JMenuItem("selected attribute");
      miFindAttribute.setEnabled(false);
      miFindReferences = new JMenuItem("references to this file");
      miFindReferences.setEnabled(struct instanceof Resource && struct.getSuperStruct() == null);
      miFindStateReferences = new JMenuItem("references to this state");
      miFindStateReferences.setEnabled(false);
      miFindRefToItem = new JMenuItem("references to selected item in this file");
      miFindRefToItem.setEnabled(false);

      bpmFind.setMenuItems(new JMenuItem[]{miFindAttribute, miFindReferences,
                                           miFindStateReferences, miFindRefToItem});
    } else {
      miFindAttribute = new JMenuItem("selected attribute");
      miFindAttribute.setEnabled(false);
      miFindReferences = new JMenuItem("references to this file");
      miFindReferences.setEnabled(struct instanceof Resource && struct.getSuperStruct() == null);
      bpmFind.setMenuItems(new JMenuItem[]{miFindAttribute, miFindReferences});
    }
    JButton bView = (JButton)buttonPanel.addControl(ButtonPanel.Control.ViewEdit);
    bView.setEnabled(false);
    bView.addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.Print)).addActionListener(this);
    if (struct instanceof Resource && struct.getFieldCount() > 0 && struct.getSuperStruct() == null) {
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.Save)).addActionListener(this);
    }
    if (extraComponents != null) {
      for (final Component c: extraComponents) {
        buttonPanel.add(c);
      }
    }

    JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());
    scrollTable.setBorder(BorderFactory.createEmptyBorder());
    JSplitPane splitv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, lowerpanel);
    splitv.setDividerLocation(2 * (NearInfinity.getInstance().getHeight() - 100) / 3);

    setLayout(new BorderLayout());
    if (struct instanceof HasViewerTabs) {
      HasViewerTabs tabs = (HasViewerTabs)struct;
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(splitv, BorderLayout.CENTER);
      panel.add(buttonPanel, BorderLayout.SOUTH);
      tabbedPane = new JTabbedPane();

      // adding custom tabs
      int editIndex = -1;
      for (int i = 0; i < tabs.getViewerTabCount(); i++) {
        if (tabs.viewerTabAddedBefore(i)) {
          // adding before "Edit"
          if (editIndex < 0) {
            tabbedPane.addTab(tabs.getViewerTabName(i), tabs.getViewerTab(i));
          } else {
            tabbedPane.insertTab(tabs.getViewerTabName(i), null, tabs.getViewerTab(i), null, editIndex);
            editIndex++;
          }
        } else {
          // adding after "Edit"
          if (editIndex < 0) {
            tabbedPane.addTab(TAB_EDIT, panel);
            editIndex = tabbedPane.getTabCount() - 1;
          }
          tabbedPane.addTab(tabs.getViewerTabName(i), tabs.getViewerTab(i));
        }
      }

      // add "Edit" tab if not yet added
      if (editIndex < 0) {
        tabbedPane.addTab(TAB_EDIT, panel);
        editIndex = tabbedPane.getTabCount() - 1;
      }

      add(tabbedPane, BorderLayout.CENTER);
      if (struct.getSuperStruct() != null && struct.getSuperStruct() instanceof HasViewerTabs) {
        StructViewer sViewer = struct.getSuperStruct().getViewer();
        if (sViewer == null) {
          sViewer = struct.getSuperStruct().getSuperStruct().getViewer();
        }
        if (sViewer != null && sViewer.tabbedPane != null) {
          tabbedPane.setSelectedIndex(sViewer.tabbedPane.getSelectedIndex());
        }
      } else if (lastIndexStruct == struct.getClass()) {
        tabbedPane.setSelectedIndex(lastIndex);
      } else if (BrowserMenuBar.getInstance().getDefaultStructView() == BrowserMenuBar.DEFAULT_EDIT) {
        tabbedPane.setSelectedIndex(getEditTabIndex());
      }
      if (isEditTabSelected()) {
        if (lastNameStruct == struct.getClass()) {
          selectEntry(lastName);
        } else {
          table.getSelectionModel().setSelectionInterval(0, 0);
        }
      }
    } else {
      add(splitv, BorderLayout.CENTER);
      add(buttonPanel, BorderLayout.SOUTH);
      if (lastNameStruct == struct.getClass()) {
        selectEntry(lastName);
      } else {
        table.getSelectionModel().setSelectionInterval(0, 0);
      }
    }

    StructClipboard.getInstance().addChangeListener(this);
    table.repaint();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() instanceof JComponent &&
        buttonPanel.getControlPosition((JComponent)event.getSource()) >= 0) {
      if (buttonPanel.getControlByType(ButtonPanel.Control.ViewEdit) == event.getSource()) {
        Viewable selected = (Viewable)table.getModel().getValueAt(table.getSelectedRow(), 1);
        createViewFrame(getTopLevelAncestor(), selected);
//        new ViewFrame(getTopLevelAncestor(), selected);
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.Remove) == event.getSource()) {
        int row = table.getSelectedRow();
        AddRemovable selected = (AddRemovable)table.getModel().getValueAt(row, 1);
        struct.removeDatatype(selected, true);
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.Save) == event.getSource()) {
        if (ResourceFactory.getInstance().saveResource((Resource)struct, getTopLevelAncestor())) {
          struct.setStructChanged(false);
        }
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
        ResourceFactory.getInstance().exportResource(struct.getResourceEntry(), getTopLevelAncestor());
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.Print) == event.getSource()) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(table);
        if (pj.printDialog()) {
          try {
            pj.print();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } else if (event.getActionCommand().equals(UPDATE_VALUE)) {
      if (editable.updateValue(struct)) {
        struct.setStructChanged(true);
        struct.fireTableRowsUpdated(struct.getIndexOf(editable), struct.getIndexOf(editable));
        if (editable instanceof EffectType) // Updates multiple lines - could be done better?
          struct.fireTableDataChanged();
      }
      else
        JOptionPane.showMessageDialog(this, "Error updating value", "Error", JOptionPane.ERROR_MESSAGE);
    } else if (event.getActionCommand().equals(CMD_COPYVALUE)) {
      StructClipboard.getInstance().copyValue(struct, table.getSelectionModel().getMinSelectionIndex(),
                                              table.getSelectionModel().getMaxSelectionIndex());
    } else if (event.getActionCommand().equals(CMD_PASTEVALUE)) {
      int changed = StructClipboard.getInstance().pasteValue(struct,
                                                             table.getSelectionModel().getMinSelectionIndex());
      if (changed == 0)
        JOptionPane.showMessageDialog(this, "Attributes doesn't match!", "Error", JOptionPane.ERROR_MESSAGE);
      else {
        struct.fireTableRowsUpdated(table.getSelectionModel().getMinSelectionIndex(),
                                    table.getSelectionModel().getMinSelectionIndex() + changed);
        struct.setStructChanged(true);
      }
    } else if (event.getActionCommand().equals(CMD_CUT)) {
      ListSelectionModel lsm = table.getSelectionModel();
      int min = lsm.getMinSelectionIndex();
      int max = lsm.getMaxSelectionIndex();
      lsm.removeIndexInterval(min, max);
      table.clearSelection();
      StructClipboard.getInstance().cut(struct, min, max);
    } else if (event.getActionCommand().equals(CMD_COPY)) {
      StructClipboard.getInstance().copy(struct, table.getSelectionModel().getMinSelectionIndex(),
                                         table.getSelectionModel().getMaxSelectionIndex());
    } else if (event.getActionCommand().equals(CMD_PASTE)) {
      table.clearSelection();
      table.scrollRectToVisible(table.getCellRect(StructClipboard.getInstance().paste(struct), 1, true));
    } else if (event.getActionCommand().equals(CMD_TOHEX)) {
      convertAttribute(table.getSelectedRow(), miToHex);
    } else if (event.getActionCommand().equals(CMD_TOBIN)) {
      convertAttribute(table.getSelectedRow(), miToBin);
    } else if (event.getActionCommand().equals(CMD_TODEC)) {
      convertAttribute(table.getSelectedRow(), miToDec);
    } else if (event.getActionCommand().equals(CMD_TOINT)) {
      convertAttribute(table.getSelectedRow(), miToInt);
    } else if (event.getActionCommand().equals(CMD_TOSTRING)) {
      convertAttribute(table.getSelectedRow(), miToString);
    } else if (event.getActionCommand().equals(CMD_RESET)) {
      convertAttribute(table.getSelectedRow(), miReset);
    } else if (event.getActionCommand().equals(CMD_SHOWVIEWER)) {
      // this should only be available for DlgResources
      DlgResource dlgRes = (DlgResource) struct;
      dlgRes.showStateWithStructEntry((StructEntry)table.getValueAt(table.getSelectedRow(), 1));
      JComponent detailViewer = dlgRes.getViewerTab(0);
      JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      parent.getModel().setSelectedIndex(parent.indexOfComponent(detailViewer));
    } else if (event.getActionCommand().equals(CMD_SHOWNEWVIEWER)) {
      // get a copy of the resource first
      DlgResource dlgRes = (DlgResource) ResourceFactory.getResource(struct.getResourceEntry());
      createViewFrame(getTopLevelAncestor(), dlgRes);
//      new ViewFrame(getTopLevelAncestor(), dlgRes);
      dlgRes.showStateWithStructEntry((StructEntry)table.getValueAt(table.getSelectedRow(), 1));
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    considerMenuEnabled();
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() instanceof ButtonPopupMenu &&
        buttonPanel.getControlPosition((JComponent)event.getSource()) >= 0) {
      if (buttonPanel.getControlByType(ButtonPanel.Control.Add) == event.getSource()) {
        ButtonPopupMenu bpmAdd = (ButtonPopupMenu)event.getSource();
        JMenuItem item = bpmAdd.getSelectedItem();
        AddRemovable toadd = null;
        for (final AddRemovable emptyType : emptyTypes) {
          if (emptyType != null && emptyType.getName().equals(item.getText())) {
            toadd = emptyType;
            break;
          }
        }
        try {
          toadd = (AddRemovable)toadd.clone();
        } catch (CloneNotSupportedException e) {
          e.printStackTrace();
          return;
        }
        int index = struct.addDatatype(toadd);
        table.getSelectionModel().setSelectionInterval(index, index);
        table.scrollRectToVisible(table.getCellRect(index, 1, true));
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.FindMenu) == event.getSource()) {
        ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
        JMenuItem item = bpmFind.getSelectedItem();
        if (item == miFindAttribute) {
          new AttributeSearcher(struct, (StructEntry)table.getValueAt(table.getSelectedRow(), 1),
                                getTopLevelAncestor());
        } else if (item == miFindReferences) {
          new ReferenceSearcher(struct.getResourceEntry(), getTopLevelAncestor());
        } else if (item == miFindStateReferences) {
          State state = (State)table.getValueAt(table.getSelectedRow(), 1);
          new DialogStateReferenceSearcher(struct.getResourceEntry(), state.getNumber(), getTopLevelAncestor());
        } else if (item == miFindRefToItem) {
          new DialogItemRefSearcher((DlgResource) struct, table.getValueAt(table.getSelectedRow(), 1),
                                    getTopLevelAncestor());
        }
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getValueIsAdjusting())
      return;
    considerMenuEnabled();
    ListSelectionModel lsm = (ListSelectionModel)event.getSource();
    if (lsm.isSelectionEmpty() || lsm.getMaxSelectionIndex() != lsm.getMinSelectionIndex()) {
      tatext.setText("");
      JButton bRemove = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.Remove);
      if (bRemove != null) {
        bRemove.setEnabled(false);
      }
      JButton bView = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.ViewEdit);
      if (bView != null) {
        bView.setEnabled(false);
      }
      cards.show(lowerpanel, CARD_EMPTY);
      miToHex.setEnabled(false);
      miToBin.setEnabled(false);
      miToDec.setEnabled(false);
      miToInt.setEnabled(false);
      miToString.setEnabled(false);
      miReset.setEnabled(false);
      miShowViewer.setEnabled(false);
      if (miShowNewViewer != null) {
        miShowNewViewer.setEnabled(false);
      }
      if (miFindAttribute != null) {
        miFindAttribute.setEnabled(false);
      }
      if (miFindStateReferences != null) {
        miFindStateReferences.setEnabled(false);
      }
      if (miFindRefToItem != null) {
        miFindRefToItem.setEnabled(false);
      }
    }
    else {
      table.scrollRectToVisible(table.getCellRect(lsm.getMinSelectionIndex(), 0, true));
      Object selected = table.getModel().getValueAt(lsm.getMinSelectionIndex(), 1);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
      JButton bRemove = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.Remove);
      if (bRemove != null) {
        bRemove.setEnabled(selected instanceof AddRemovable && ((AddRemovable)selected).canRemove());
      }
      JButton bView = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.ViewEdit);
      if (bView != null) {
        bView.setEnabled(selected instanceof Viewable);
      }
      if (miFindAttribute != null) {
        miFindAttribute.setEnabled(!(selected instanceof AbstractStruct));
      }
      if (miFindStateReferences != null) {
        miFindStateReferences.setEnabled(selected instanceof State);
      }
      boolean isDataType = (selected instanceof Datatype);
      boolean isReadable = (selected instanceof Readable);
      miToHex.setEnabled(isDataType && isReadable && !(selected instanceof HexNumber ||
                                                       selected instanceof Unknown ||
                                                       selected instanceof SectionCount ||
                                                       selected instanceof AbstractCode));
      if (selected instanceof UnknownBinary || selected instanceof UnknownDecimal) {
        miToHex.setEnabled(true);
      }
      miToBin.setEnabled(isDataType && isReadable &&
                         !(selected instanceof UnknownBinary ||
                           selected instanceof SectionCount ||
                           selected instanceof AbstractCode));
      miToDec.setEnabled(isDataType && isReadable &&
                         !(selected instanceof UnknownDecimal ||
                           selected instanceof SectionCount ||
                           selected instanceof AbstractCode));
      miToInt.setEnabled(isDataType && isReadable &&
                         (selected instanceof Datatype && ((Datatype)selected).getSize() <= 4) &&
                         !(selected instanceof DecNumber ||
                           selected instanceof SectionCount ||
                           selected instanceof AbstractCode));
      miToString.setEnabled(isDataType && isReadable &&
                            (selected instanceof Unknown || selected instanceof ResourceRef) &&
                            !(selected instanceof AbstractCode));
      miReset.setEnabled(isDataType && isReadable &&
                         isCachedStructEntry(((Datatype)selected).getOffset()) &&
                         getCachedStructEntry(((Datatype)selected).getOffset()) instanceof Readable &&
                         !(selected instanceof AbstractCode));
      boolean isSpecialDlgStruct = (selected instanceof State
                                 || selected instanceof Transition
                                 || selected instanceof AbstractCode);

      if (miFindRefToItem != null) {
        miFindRefToItem.setEnabled(isSpecialDlgStruct);
      }
      miShowViewer.setEnabled(isSpecialDlgStruct);
      miShowNewViewer.setEnabled(isSpecialDlgStruct);

      if (selected instanceof Editable) {
        editable = (Editable)selected;
        editpanel.removeAll();

        JComponent editor = editable.edit(this);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        editpanel.setLayout(gbl);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbl.setConstraints(editor, gbc);
        editpanel.add(editor);

        editpanel.revalidate();
        editpanel.repaint();
        cards.show(lowerpanel, CARD_EDIT);
        editable.select();
      }
      else if (selected instanceof InlineEditable) {
        tatext.setText("");
        cards.show(lowerpanel, CARD_EMPTY);
      }
      else {
        editable = null;
        if (selected instanceof AbstractStruct)
          tatext.setText(((AbstractStruct)selected).toMultiLineString());
        else
          tatext.setText(selected.toString());
        tatext.setCaretPosition(0);
        cards.show(lowerpanel, CARD_TEXT);
      }
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent event)
  {
    if (event.getType() == TableModelEvent.UPDATE) {
      StructEntry structEntry = struct.getField(event.getFirstRow());
      if (structEntry instanceof Editable && (editable == null || (structEntry.getOffset() == editable.getOffset() &&
                                                                   structEntry != editable))) {
        editable = (Editable)structEntry;
        editpanel.removeAll();

        JComponent editor = editable.edit(this);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        editpanel.setLayout(gbl);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbl.setConstraints(editor, gbc);
        editpanel.add(editor);

        editpanel.revalidate();
        editpanel.repaint();
        cards.show(lowerpanel, CARD_EDIT);
        editable.select();
      }
    }
  }

// --------------------- End Interface TableModelListener ---------------------

  public ButtonPanel getButtonPanel()
  {
    return buttonPanel;
  }

  public void close()
  {
    StructClipboard.getInstance().removeChangeListener(this);
    if (struct instanceof Resource) {
      if (tabbedPane != null && struct instanceof HasViewerTabs) {
        lastIndex = tabbedPane.getSelectedIndex();
        lastIndexStruct = struct.getClass();
      } else {
        lastIndexStruct = null;
      }
      if (table.getSelectionModel().getMinSelectionIndex() != -1) {
        lastName = ((StructEntry)table.getModel().getValueAt(table.getSelectionModel()
                                 .getMinSelectionIndex(), 1)).getName();
        lastNameStruct = struct.getClass();
      } else {
        lastName = null;
        lastNameStruct = null;
      }
    }

    if (tabbedPane != null) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        Component c = tabbedPane.getComponentAt(i);
        if (c instanceof Closeable) {
          try {
            ((Closeable)c).close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      tabbedPane = null;
    }
  }

  public StructEntry getSelectedEntry()
  {
    if (table.getSelectedRow() == -1)
      return null;
    return (StructEntry)table.getModel().getValueAt(table.getSelectedRow(), 1);
  }

  public int getSelectedRow()
  {
    return table.getSelectedRow();
  }

  public void selectEntry(String name)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry entry = struct.getField(i);
      if (entry.getName().equals(name)) {
        selectEntry(entry);
        return;
      }
    }
  }

  public void selectEntry(int offset)
  {
    selectEntry(offset, true);
  }

  public void selectEntry(int offset, boolean recursive)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry entry = struct.getField(i);
      if (entry instanceof AbstractStruct && recursive) {
        selectEntry((AbstractStruct)entry, offset);
      } else if (entry.getOffset() == offset) {
        selectEntry(entry);
      }
    }
  }

  /** Helper method for finding out if a "View" tab is available. */
  public boolean hasViewTab()
  {
    return (getTabIndex(TAB_VIEW) >= 0);
  }

  /** Helper method for finding out if "View" tab is selected. */
  public boolean isViewTabSelected()
  {
    if (tabbedPane != null) {
      return (getTabIndex(TAB_VIEW) == tabbedPane.getSelectedIndex());
    }
    return false;
  }

  /** Helper method for selecting "View" tab if available. */
  public void selectViewTab()
  {
    if (tabbedPane != null) {
      int idx = getTabIndex(TAB_VIEW);
      if (idx >= 0) {
        tabbedPane.setSelectedIndex(idx);
      }
    }
  }

  /** Returns whether "Edit" tab is selected. */
  public boolean isEditTabSelected()
  {
    if (tabbedPane != null) {
      return (getEditTabIndex() == tabbedPane.getSelectedIndex());
    }
    return true;
  }

  /** Selects the "Edit" tab. */
  public void selectEditTab()
  {
    if (tabbedPane != null) {
      int idx = getEditTabIndex();
      if (idx >= 0) {
        tabbedPane.setSelectedIndex(idx);
      }
    }
  }

  /** Returns tab index of "Edit" tab. */
  public int getEditTabIndex()
  {
    if (tabbedPane != null) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (TAB_EDIT.equals(tabbedPane.getTitleAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Helper method for finding out if a "Raw" tab is available. */
  public boolean hasRawTab()
  {
    return (getTabIndex(TAB_RAW) >= 0);
  }

  /** Helper method for finding out if "Raw" tab is selected. */
  public boolean isRawTabSelected()
  {
    if (tabbedPane != null) {
      return (getTabIndex(TAB_RAW) == tabbedPane.getSelectedIndex());
    }
    return false;
  }

  /** Helper method for selecting "Raw" tab if available. */
  public void selectRawTab()
  {
    if (tabbedPane != null) {
      int idx = getTabIndex(TAB_RAW);
      if (idx >= 0) {
        tabbedPane.setSelectedIndex(idx);
      }
    }
  }

  /** Returns tab index of specified tab name.  */
  public int getTabIndex(String name)
  {
    if (tabbedPane != null && name != null) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (name.equals(tabbedPane.getTitleAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Adds a ChangeListener to the TabbedPane if available. */
  public void addTabChangeListener(ChangeListener l)
  {
    if (tabbedPane != null && l != null) {
      tabbedPane.addChangeListener(l);
    }
  }

  /** Removes a ChangeListener from the TabbedPane if available. */
  public void removeTabChangeListener(ChangeListener l)
  {
    if (tabbedPane != null && l != null) {
      tabbedPane.removeChangeListener(l);
    }
  }

  /** Returns an array of ChangeListeners added to the TabbedPane if available. */
  public ChangeListener[] getTabChangeListeners()
  {
    if (tabbedPane != null) {
      return tabbedPane.getChangeListeners();
    } else {
      return new ChangeListener[0];
    }
  }

  /**
   * Returns an already existing ViewFrame of the given Viewable object if available.
   * Returns a new ViewFrame object otherwise. Assumes top level ancestor of the given view as parent.
   */
  public ViewFrame getViewFrame(Viewable view)
  {
    return getViewFrame(getTopLevelAncestor(), view);
  }

  /**
   * Returns an already existing ViewFrame of the given Viewable object if available.
   * Returns a new ViewFrame object otherwise.
   */
  public ViewFrame getViewFrame(Component parent, Viewable view)
  {
    return createViewFrame(parent, view);
  }

  private void considerMenuEnabled()
  {
    ListSelectionModel lsm = table.getSelectionModel();
    if (lsm.isSelectionEmpty()) {
      miCopyValue.setEnabled(false);
      miPasteValue.setEnabled(false);
      miCut.setEnabled(false);
      miCopy.setEnabled(false);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    }
    else if (lsm.getMaxSelectionIndex() != lsm.getMinSelectionIndex()) {
      boolean isRemovable = true;
      boolean isValue = true;
      for (int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
        Object o = table.getModel().getValueAt(i, 1);
        if (!(o instanceof AddRemovable))
          isRemovable = false;
        else
          isRemovable = ((AddRemovable)o).canRemove();
        if (o instanceof AbstractStruct)
          isValue = false;
      }
      miCopyValue.setEnabled(isValue);
      miPasteValue.setEnabled(false);
      miCut.setEnabled(isRemovable);
      miCopy.setEnabled(isRemovable);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    }
    else {
      Object selected = table.getModel().getValueAt(lsm.getMinSelectionIndex(), 1);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
      if (selected instanceof AddRemovable) {
        miCopyValue.setEnabled(false);
        miPasteValue.setEnabled(false);
        miCut.setEnabled(((AddRemovable)selected).canRemove());
        miCopy.setEnabled(((AddRemovable)selected).canRemove());
      }
      else {
        miCopyValue.setEnabled(!(selected instanceof AbstractStruct));
        miPasteValue.setEnabled(miCopyValue.isEnabled() &&
                                StructClipboard.getInstance().getContentType(struct) ==
                                StructClipboard.CLIPBOARD_VALUES);
        miCut.setEnabled(false);
        miCopy.setEnabled(false);
      }
    }
  }

  private void convertAttribute(int index, JMenuItem menuitem)
  {
    StructEntry entry = struct.getField(index);
    if (!isCachedStructEntry(entry.getOffset())) setCachedStructEntry(entry);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      entry.write(baos);
      StructEntry newentry;
      if (menuitem == miToHex) {
        newentry = new Unknown(baos.toByteArray(), 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToBin) {
        newentry = new UnknownBinary(baos.toByteArray(), 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToDec) {
        newentry = new UnknownDecimal(baos.toByteArray(), 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToInt) {
        newentry = new DecNumber(baos.toByteArray(), 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToString) {
        newentry = new TextString(baos.toByteArray(), 0, entry.getSize(), entry.getName());
      } else if (menuitem == miReset) {
        newentry = removeCachedStructEntry(entry.getOffset());
        if (newentry == null || !(newentry instanceof Readable)) {
          newentry = entry;
        } else {
          ((Readable)newentry).read(baos.toByteArray(), 0);
        }
      } else {
        throw new NullPointerException();
      }
      newentry.setOffset(entry.getOffset());
      struct.setListEntry(index, newentry);
      table.getSelectionModel().removeSelectionInterval(index, index);
      table.getSelectionModel().addSelectionInterval(index, index);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void selectEntry(StructEntry structEntry)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (structEntry == o) {
        table.getSelectionModel().setSelectionInterval(i, i);
        selectEditTab();
        return;
      }
      else if (o instanceof AbstractStruct)
        selectSubEntry((AbstractStruct)o, structEntry);
    }
  }

  private void selectEntry(AbstractStruct subStruct, int offset)
  {
    for (int i = 0; i < subStruct.getFieldCount(); i++) {
      StructEntry entry = subStruct.getField(i);
      if (entry instanceof AbstractStruct)
        selectEntry((AbstractStruct)entry, offset);
      else if (entry.getOffset() == offset)
        selectSubEntry(subStruct, entry);
    }
  }

  private void selectSubEntry(AbstractStruct subStruct, StructEntry structEntry)
  {
    for (int i = 0; i < subStruct.getFieldCount(); i++) {
      StructEntry o = subStruct.getField(i);
      if (structEntry == o) {
        createViewFrame(getTopLevelAncestor(), subStruct);
//        new ViewFrame(getTopLevelAncestor(), subStruct);
        StructViewer viewer = subStruct.getViewer();
        viewer.table.getSelectionModel().setSelectionInterval(i, i);
        table.scrollRectToVisible(table.getCellRect(i, 0, true));
        viewer.selectEditTab();
        return;
      }
      else if (o instanceof AbstractStruct)
        selectSubEntry((AbstractStruct)o, structEntry);
    }
  }

  // Caches the given StructEntry object
  private void setCachedStructEntry(StructEntry struct)
  {
    if (struct != null) {
      if (!entryMap.containsKey(Integer.valueOf(struct.getOffset()))) {
        entryMap.put(struct.getOffset(), struct);
      }
    }
  }

  private StructEntry getCachedStructEntry(int offset)
  {
    return entryMap.get(Integer.valueOf(offset));
  }

  // Removes the StructEntry object at the given offset and returns it
  private StructEntry removeCachedStructEntry(int offset)
  {
    return entryMap.remove(Integer.valueOf(offset));
  }

  // Indicates whether the given StructEntry object is equal to the cached object
  private boolean isCachedStructEntry(int offset)
  {
    return entryMap.containsKey(Integer.valueOf(offset));
  }


  // Recycles existing ViewFrame constructs if possible
  private ViewFrame createViewFrame(Component parent, Viewable view)
  {
    ViewFrame frame = null;
    if (view != null) {
      if (parent == null) {
        parent = getTopLevelAncestor();
      }
      frame = viewMap.get(view);
      if (frame == null || !frame.isVisible()) {
        frame = new ViewFrame(parent, view);
        viewMap.put(view, frame);
      } else {
        frame.toFront();
      }
    }
    return frame;
  }

// -------------------------- INNER CLASSES --------------------------

  private final class PopupListener extends MouseAdapter
  {
    @Override
    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
        popupmenu.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  private final class StructTable extends JTable implements Printable
  {
    private StructTable()
    {
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
    {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setColor(Color.black);
      int fontHeight = g2.getFontMetrics().getHeight();
      int fontDesent = g2.getFontMetrics().getDescent();

      //leave room for page number
      double pageHeight = pageFormat.getImageableHeight() - (double)fontHeight;
      double pageWidth = pageFormat.getImageableWidth();
      double tableWidth = (double)getColumnModel().getTotalColumnWidth();
      double scale = (double)1;
      if (tableWidth >= pageWidth)
        scale = pageWidth / tableWidth;

      double headerHeightOnPage = (double)getTableHeader().getHeight() * scale;
      double tableWidthOnPage = tableWidth * scale;

      double oneRowHeight = (double)getRowHeight() * scale;
      int numRowsOnAPage = (int)((pageHeight - headerHeightOnPage) / oneRowHeight);
      double pageHeightForTable = oneRowHeight * (double)numRowsOnAPage;
      int totalNumPages = (int)Math.ceil((double)getRowCount() / (double)numRowsOnAPage);
      if (pageIndex >= totalNumPages)
        return NO_SUCH_PAGE;

      g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
      //bottom center
      g2.drawString(struct.getName() + " - Page " + (pageIndex + 1) + '/' + totalNumPages,
                    ((int)pageWidth >> 1) - 35, (int)(pageHeight + (double)fontHeight - (double)fontDesent));

      g2.translate((double)0.0f, headerHeightOnPage - (double)pageIndex * pageHeightForTable);

      //If this piece of the table is smaller than the size available, clip to the appropriate bounds.
      if (pageIndex + 1 == totalNumPages) {
        int lastRowPrinted = numRowsOnAPage * pageIndex;
        int numRowsLeft = getRowCount() - lastRowPrinted;
        g2.setClip(0, (int)(pageHeightForTable * (double)pageIndex), (int)Math.ceil(tableWidthOnPage),
                   (int)Math.ceil(oneRowHeight * (double)numRowsLeft));
      }
      //else clip to the entire area available.
      else
        g2.setClip(0, (int)(pageHeightForTable * (double)pageIndex), (int)Math.ceil(tableWidthOnPage),
                   (int)Math.ceil(pageHeightForTable));

      g2.scale(scale, scale);
      paint(g2);
      g2.scale((double)1 / scale, (double)1 / scale);
      g2.translate((double)0.0f, (double)pageIndex * pageHeightForTable - headerHeightOnPage);
      g2.setClip(0, 0, (int)Math.ceil(tableWidthOnPage), (int)Math.ceil(headerHeightOnPage));
      g2.scale(scale, scale);
      getTableHeader().paint(g2);
      //paint header at top

      return Printable.PAGE_EXISTS;
    }
  }
}


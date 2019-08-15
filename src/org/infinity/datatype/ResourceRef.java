// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.HasIcon;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

/**
 * Represents reference to another resource in game. This resource can be
 * sound, item, dialog, creature, image
 */
public class ResourceRef extends Datatype
    implements Editable, IsTextual, IsReference, ActionListener, ListSelectionListener
{
  private static final Comparator<ResourceRefEntry> IGNORE_CASE_EXT_COMPARATOR = new IgnoreCaseExtComparator();

  /** Special constant that represents absense of resource in the field. */
  private static final ResourceRefEntry NONE = new ResourceRefEntry("None");
  /** Possible file extensions that can have this resource. */
  private final String[] type;
  /** Raw bytes of the resource reference, read from stream. */
  private final ByteBuffer buffer;
  private String curtype;
  /** Name of the resource, called {@code ResRef}, 8 bytes usually. */
  private String resname;
  /** Button that used to open editor of current selected element in the list. */
  private JButton bView;
  /**
   * GUI component that lists all available resources that can be set to this
   * resource reference and have edit field for ability to enter resource reference
   * manually.
   */
  private TextListPanel<ResourceRefEntry> list;

  public ResourceRef(ByteBuffer h_buffer, int offset, String name, String type)
  {
    this(null, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(StructEntry parent, ByteBuffer h_buffer, int offset, String name, String type)
  {
    this(parent, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(ByteBuffer h_buffer, int offset, int length, String name, String type)
  {
    this(null, h_buffer, offset, length, name, new String[]{type});
  }

  public ResourceRef(StructEntry parent, ByteBuffer h_buffer, int offset, int length, String name,
                     String type)
  {
    this(parent, h_buffer, offset, length, name, new String[]{type});
  }

  public ResourceRef(ByteBuffer h_buffer, int offset, String name, String[] type)
  {
    this(null, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(StructEntry parent, ByteBuffer h_buffer, int offset, String name, String[] type)
  {
    this(parent, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(ByteBuffer h_buffer, int offset, int length, String name, String[] type)
  {
    this(null, h_buffer, offset, length, name, type);
  }

  public ResourceRef(StructEntry parent, ByteBuffer h_buffer, int offset, int length, String name,
                     String[] type)
  {
    super(parent, offset, length, name);
    this.buffer = StreamUtils.getByteBuffer(length);
    if (type == null || type.length == 0) {
      this.type = new String[]{""};
    } else {
      this.type = type;
    }
    this.curtype = this.type[0];
    read(h_buffer, offset);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      final ResourceRefEntry selected = list.getSelectedValue();
      if (isEditable(selected)) {
        new ViewFrame(list.getTopLevelAncestor(), ResourceFactory.getResource(selected.entry));
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    final List<List<ResourceEntry>> resourceList = new ArrayList<>(type.length);
    int entrynum = 0;
    for (String ext : type) {
      final List<ResourceEntry> entries = ResourceFactory.getResources(ext);
      resourceList.add(entries);
      entrynum += entries.size();
    }

    final List<ResourceRefEntry> values = new ArrayList<>(1 + entrynum);
    values.add(NONE);
    for (List<ResourceEntry> resources : resourceList) {
      for (ResourceEntry entry : resources) {
        //FIXME: ResRefChecker check only that point is exist, so this must be
        // the same check or this check must be inside isLegalEntry(...)
        // There only 2 places where isLegalEntry is called: this and ResRefChecker
        if (isLegalEntry(entry) && entry.toString().lastIndexOf('.') <= 8) {
          values.add(new ResourceRefEntry(entry));
        }
      }
    }
    addExtraEntries(values);
    Collections.sort(values, IGNORE_CASE_EXT_COMPARATOR);
    list = new TextListPanel<>(values, false);
    list.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2)
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
      }
    });

    ResourceEntry entry = null;
    for (int i = 0; i < type.length && entry == null; i++) {
      entry = ResourceFactory.getResourceEntry(resname + '.' + type[i], true);
    }
    if (entry != null) {
      for (ResourceRefEntry e : values) {
        if (entry.equals(e.entry)) {
          list.setSelectedValue(e, true);
          break;
        }
      }
    } else {
      list.setSelectedValue(NONE, true);
      for (ResourceRefEntry e : values) {
        if (e.name.equals(resname)) {
          list.setSelectedValue(e, true);
          break;
        }
      }
    }

    JButton bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bView = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
    bView.addActionListener(this);
    bView.setEnabled(isEditable(list.getSelectedValue()));
    list.addListSelectionListener(this);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridheight = 2;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(3, 6, 3, 0);
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.NORTH;
    gbl.setConstraints(bView, gbc);
    panel.add(bView);

    panel.setMinimumSize(Misc.getScaledDimension(DIM_MEDIUM));
    return panel;
  }

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    final ResourceRefEntry selected = list.getSelectedValue();
    if (selected == NONE) {
      resname = NONE.name;//FIXME: use null instead of this

      // notifying listeners
      fireValueUpdated(new UpdateEvent(this, struct));

      return true;
    }

    final ResourceEntry entry = selected.entry;
    if (entry == null) {
      resname = selected.name;
    } else {
      int i = -1;
      for (String ext : type) {
        //TODO: It seems that instead of toString getExtension must be used
        i = entry.toString().indexOf('.' + ext.toUpperCase(Locale.ENGLISH));
        if (i != -1) {
          resname = entry.toString().substring(0, i);
          curtype = ext;
          break;
        }
      }
      if (i == -1) {
        return false;
      }
    }

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    bView.setEnabled(isEditable(list.getSelectedValue()));
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (resname.equalsIgnoreCase(NONE.name)) {//FIXME: use null instead of NONE.name
      buffer.position(0);
      String s = StreamUtils.readString(buffer, buffer.limit());
      buffer.position(0);
      if (s.equalsIgnoreCase(NONE.name)) {// TODO: What this check do?
        StreamUtils.writeBytes(os, buffer);
      } else {
        StreamUtils.writeBytes(os, (byte)0, buffer.limit());
      }
    } else {
      StreamUtils.writeString(os, resname, getSize());
    }
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    StreamUtils.copyBytes(buffer, offset, this.buffer, 0, getSize());
    this.buffer.position(0);
    final String s = StreamUtils.readString(this.buffer, this.buffer.limit()).toUpperCase(Locale.ENGLISH);
    resname = s.isEmpty() || s.equalsIgnoreCase(NONE.name) ? NONE.name : s;//FIXME: use null instead of NONE.name

    // determine the correct file extension
    if (!resname.equals(NONE.name)) { //FIXME: use null instead of NONE.name
      for (String ext : type) {
        if (null != ResourceFactory.getResourceEntry(resname + "." + ext, true)) {
          curtype = ext;
          break;
        }
      }
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    if (resname.equals(NONE.name))//FIXME: use null instead of NONE.name
      return resname;
    String searchName = getSearchName();
    if (searchName != null)
      return getResourceName() + " (" + searchName + ')';
    return getResourceName();
  }

//--------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText()
  {
    return resname;
  }

//--------------------- End Interface IsTextual ---------------------

//--------------------- Begin Interface IsReference ---------------------

  @Override
  public String getResourceName()
  {
    if (!resname.equals(NONE.name)) {//FIXME: use null instead of NONE.name
      return resname + '.' + curtype;
    }
    return resname;
  }

//--------------------- End Interface IsReference ---------------------

  public boolean isEmpty()
  {
    return (resname.equals(NONE.name));//FIXME: use null instead of NONE.name
  }

  public String getSearchName()
  {
    ResourceEntry entry = ResourceFactory.getResourceEntry(getResourceName(), true);
    if (entry != null)
      return entry.getSearchString();
    return null;
  }

  public String getType()
  {
    return curtype;
  }

  /**
   * Check that this object can hold reference to the specified resource.
   *
   * @param entry Pointer to resource for check. If {@code null}, method returns
   *        {@code false}
   *
   * @return {@code false} if {@code entry} can not be target for this resource
   *         reference, {@code true} otherwise
   */
  public boolean isLegalEntry(ResourceEntry entry)
  {
    return entry != null && entry.getResourceName().lastIndexOf('.') != 0;
  }

  /**
   * Appends additional resources to the list of selectable resources for this reference.
   *
   * @param entries List with selectable resources. Implementers must add additional
   *        resources to it. Never {@code null}
   */
  void addExtraEntries(List<ResourceRefEntry> entries)
  {
  }

  private boolean isEditable(ResourceRefEntry ref) {
    return ref != null && ref != NONE && ref.entry != null;
  }
// -------------------------- INNER CLASSES --------------------------
  /** Class that represents resource reference in the list of choice. */
  static final class ResourceRefEntry implements HasIcon
  {
    private final ResourceEntry entry;
    private final String name;

    private ResourceRefEntry(ResourceEntry entry)
    {
      this.entry = entry;
      String string = entry.toString();
      String search = entry.getSearchString();
      if (search == null || BrowserMenuBar.getInstance().getResRefMode() == BrowserMenuBar.RESREF_ONLY)
        name = string;
      else if (BrowserMenuBar.getInstance().getResRefMode() == BrowserMenuBar.RESREF_REF_NAME)
        name = string + " (" + search + ')';
      else
        name = search + " (" + string + ')';
    }

    ResourceRefEntry(String name)
    {
      this.name = name;
      entry = null;
    }

    @Override
    public String toString()
    {
      return name;
    }

    @Override
    public ResourceRef getIcon() {
      if (entry != null) {
        return entry.getResourceIcon();
      }
      return null;
    }
  }

  private static final class IgnoreCaseExtComparator implements Comparator<ResourceRefEntry>
  {
    @Override
    public int compare(ResourceRefEntry o1, ResourceRefEntry o2)
    {
      // null always smaller any other value
      if (o1 == null) return o2 == null ? 0 : -1;
      if (o2 == null) return 1;

      // NONE always smaller any other value except null
      if (o1 == NONE) return o2 == NONE ? 0 : -1;
      if (o2 == NONE) return 1;

      final String s1 = o1.toString();
      final String s2 = o2.toString();
      //TODO: must use special method to extract only name without extension
      final int i1 = s1.lastIndexOf('.') > 0 ? s1.lastIndexOf('.') : s1.length();
      final int i2 = s2.lastIndexOf('.') > 0 ? s2.lastIndexOf('.') : s2.length();
      return s1.substring(0, i1).compareToIgnoreCase(s2.substring(0, i2));
    }
  }
}


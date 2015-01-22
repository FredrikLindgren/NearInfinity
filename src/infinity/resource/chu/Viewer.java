// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.Bitmap;
import infinity.datatype.ColorPicker;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.gui.RenderCanvas;
import infinity.gui.StructViewer;
import infinity.gui.ViewerUtil;
import infinity.resource.ResourceFactory;
import infinity.resource.graphics.BamDecoder;
import infinity.resource.graphics.BamDecoder.FrameEntry;
import infinity.resource.graphics.BamV1Decoder.BamV1Control;
import infinity.resource.graphics.MosDecoder;
import infinity.resource.graphics.BamDecoder.BamControl;
import infinity.resource.graphics.MosV1Decoder;
import infinity.util.StringResource;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


final class Viewer extends JPanel implements ActionListener, TableModelListener, ListSelectionListener,
                                             ChangeListener, MouseListener
{
  /** Supported control types. */
  public enum ControlType { Unknown, Button, Slider, TextField, TextArea, Label, ScrollBar }

  private final ChuResource chu;

  private RenderCanvas rcMain;
  private ListPanelsModel panelsModel;
  private ListControlsModel controlsModel;
  private JList panelsList, controlsList;
  private JCheckBox cbTransparentPanel, cbOutlineControls;
  private PropertiesPanel pProperties;

  /** Converts a control id into a ControlType enum. */
  public static ControlType getControlType(int type)
  {
    switch (type) {
      case 0:  return ControlType.Button;
      case 2:  return ControlType.Slider;
      case 3:  return ControlType.TextField;
      case 5:  return ControlType.TextArea;
      case 6:  return ControlType.Label;
      case 7:  return ControlType.ScrollBar;
      default: return ControlType.Unknown;
    }
  }

  /** Converts a ControlType enum into a control id. */
  public static int getControlId(ControlType type)
  {
    switch (type) {
      case Button:      return 0;
      case Slider:      return 2;
      case TextField:   return 3;
      case TextArea:    return 5;
      case Label:       return 6;
      case ScrollBar:   return 7;
      default:          return -1;
    }
  }

  /** Returns a control object of the given type. */
  public static BaseControl createControl(Viewer viewer, Control control)
  {
    ControlType type = (control != null) ? getControlType(control.getControlType()) : ControlType.Unknown;
    switch (type) {
      case Button:      return new ButtonControl(viewer, control);
      case Slider:      return new SliderControl(viewer, control);
      case TextField:   return new TextFieldControl(viewer, control);
      case TextArea:    return new TextAreaControl(viewer, control);
      case Label:       return new LabelControl(viewer, control);
      case ScrollBar:   return new ScrollBarControl(viewer, control);
      default:          return new UnknownControl(viewer, control);
    }
  }



  public Viewer(ChuResource chu)
  {
    this.chu = chu;
    this.chu.addTableModelListener(this);
    initControls();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == cbOutlineControls) {
      // updating main display
      Panel p = getSelectedPanel();
      p.repaint();
      rcMain.repaint();
    } else if (e.getSource() == cbTransparentPanel) {
      // updating main display
      Panel p = getSelectedPanel();
      p.repaint();
      rcMain.repaint();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent e)
  {
    if (e.getSource() == getResource()) {
      // updating viewer elements
      Panel p = getSelectedPanel();
      p.reset();
      setPreview(p.getImage());
    }
  }

//--------------------- End Interface TableModelListener ---------------------

//--------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    if (e.getSource() == panelsList) {
      Panel p = getSelectedPanel();
      if (p != null) {
        // updating controls list
        controlsModel.setResource(p.getResource());
        controlsList.setSelectedIndex(0);

        // updating main window
        p.repaint();
        setPreview(p.getImage());
      }
    } else if (e.getSource() == controlsList) {
      BaseControl c = (BaseControl)controlsList.getSelectedValue();
      if (c != null) {
        // updating properties panel
        getProperties().updateProperties(c);

        // updating selected control in main window
        Panel p = getSelectedPanel();
        p.repaint();
        rcMain.repaint();
      }
    }
  }

//--------------------- End Interface ListSelectionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e)
  {
    if (e.getSource() == pProperties) {
      // updating main display to reflect changes in control properties
      Panel p = getSelectedPanel();
      BaseControl c = getSelectedControl();
      if (c != null) {
        c.updateState();
        c.updateImage();
      }
      p.repaint();
      rcMain.repaint();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface MouseListener ---------------------

  @Override
  public void mouseClicked(MouseEvent e)
  {
    if (e.getSource() == panelsList) {
      if (e.getClickCount() == 2 && !e.isConsumed()) {
        Panel p = (Panel)panelsList.getSelectedValue();
        if (p != null) {
          StructViewer v = getResource().getViewer();
          if (v != null) {
            v.getViewFrame(p.getResource());
          }
        }
      }
    } else if (e.getSource() == controlsList) {
      if (e.getClickCount() == 2 && !e.isConsumed()) {
        Panel p = (Panel)panelsList.getSelectedValue();
        if (p != null) {
          BaseControl c = (BaseControl)controlsList.getSelectedValue();
          if (c != null && !c.isEmpty()) {
            StructViewer v = getResource().getViewer();
            if (v != null) {
              v.getViewFrame(p.getResource());
              v.getViewFrame(c.getResource());
            }
          }
        }
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
  public void mouseExited(MouseEvent e)
  {
  }

//--------------------- End Interface MouseListener ---------------------

  /** Returns the associated ChuResource instance. */
  public ChuResource getResource()
  {
    return chu;
  }

  /** Returns whether to make the panel background transparent if no bg image is available. */
  boolean isTransparentBackground()
  {
    return cbTransparentPanel.isSelected();
  }

  /** Returns the currently active panel. */
  Panel getSelectedPanel()
  {
    Object o = panelsModel.getElementAt(panelsList.getSelectedIndex());
    if (o instanceof Panel) {
      return (Panel)o;
    } else {
      return null;
    }
  }

  /** Returns the currently active control. */
  BaseControl getSelectedControl()
  {
    Object o = controlsModel.getElementAt(controlsList.getSelectedIndex());
    if (o instanceof BaseControl) {
      return (BaseControl)o;
    } else {
      return null;
    }
  }

  /** Returns the current properties panel instance. */
  PropertiesPanel getProperties()
  {
    return pProperties;
  }

  /** Returns the controls model. */
  ListControlsModel getControls()
  {
    return controlsModel;
  }

  /** Returns whether the specified control is currently selected. */
  boolean isControlSelected(BaseControl control)
  {
    return (control == controlsModel.getElementAt(controlsList.getSelectedIndex()));
  }

  /** Returns whether to draw boxes around controls. */
  boolean isControlOutlined()
  {
    return cbOutlineControls.isSelected();
  }

  // Update preview image
  private void setPreview(Image image)
  {
    rcMain.setImage(image);
    rcMain.revalidate();
  }

  private void initControls()
  {
    setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();

    // creating main panel
    JPanel pMain = new JPanel(new GridBagLayout());
    JScrollPane spMain = new JScrollPane(pMain);
    spMain.getHorizontalScrollBar().setUnitIncrement(16);
    spMain.getVerticalScrollBar().setUnitIncrement(16);
    rcMain = new RenderCanvas();
    rcMain.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(rcMain, gbc);

    // creating side panel
    JPanel pSideBar = new JPanel(new GridBagLayout());

    JLabel lPanels = new JLabel("Panels:");
    panelsModel = new ListPanelsModel(this);
    panelsList = new JList(panelsModel);
    panelsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    panelsList.addListSelectionListener(this);
    panelsList.addMouseListener(this);
    JScrollPane spPanels = new JScrollPane(panelsList);
    spPanels.getVerticalScrollBar().setUnitIncrement(16);
    cbTransparentPanel = new JCheckBox("Transparent background", true);
    cbTransparentPanel.addActionListener(this);

    JLabel lControls = new JLabel("Controls:");
    controlsModel = new ListControlsModel(this, null);    // assigning resource later
    controlsList = new JList(controlsModel);
    controlsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    controlsList.addListSelectionListener(this);
    controlsList.addMouseListener(this);
    JScrollPane spControls = new JScrollPane(controlsList);
    spControls.getVerticalScrollBar().setUnitIncrement(16);
    cbOutlineControls = new JCheckBox("Outline selection", true);
    cbOutlineControls.addActionListener(this);

    pProperties = new PropertiesPanel();
    pProperties.addChangeListener(this);

    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 8), 0, 0);
    pSideBar.add(lPanels, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.75, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 8, 0, 8), 0, 0);
    pSideBar.add(spPanels, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 8), 0, 0);
    pSideBar.add(cbTransparentPanel, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(16, 8, 0, 8), 0, 0);
    pSideBar.add(lControls, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 1.0, 1.25, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 8, 0, 8), 0, 0);
    pSideBar.add(spControls, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 5, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 8), 0, 0);
    pSideBar.add(cbOutlineControls, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 6, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(16, 8, 0, 8), 0, 0);
    pSideBar.add(pProperties, gbc);

    // putting all together
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(8, 8, 8, 0), 0, 0);
    add(spMain, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
    add(pSideBar, gbc);

    // selecting first available panel
    if (panelsModel.getSize() > 0) {
      panelsList.setSelectedIndex(0);
    }
  }


//----------------------------- INNER CLASSES -----------------------------

  // Data model for the panels list
  private class ListPanelsModel extends AbstractListModel
  {
    private final List<Panel> listPanels = new ArrayList<Panel>();
    private final Viewer viewer;

    public ListPanelsModel(Viewer viewer)
    {
      super();
      this.viewer = viewer;

      for (int i = 0; i < getResource().getPanelCount(); i++) {
        listPanels.add(new Panel(getViewer(), getResource().getPanel(i)));
      }
    }

    //--------------------- Begin Interface ListModel ---------------------

    @Override
    public int getSize()
    {
      return listPanels.size();
    }

    @Override
    public Object getElementAt(int index)
    {
      if (index >= 0 && index < listPanels.size()) {
        return listPanels.get(index);
      } else {
        return null;
      }
    }

    //--------------------- End Interface ListModel ---------------------

    public Viewer getViewer()
    {
      return viewer;
    }

    public ChuResource getResource()
    {
      return getViewer().getResource();
    }
  }


  // Data model for the controls list
  private class ListControlsModel extends AbstractListModel
  {
    private final List<BaseControl> listControls = new ArrayList<BaseControl>();
    private final Viewer viewer;

    private Window panel;

    public ListControlsModel(Viewer viewer, Window panel)
    {
      super();
      this.viewer = viewer;
      setResource(panel);
    }

    //--------------------- Begin Interface ListModel ---------------------

    @Override
    public int getSize()
    {
      return listControls.size();
    }

    @Override
    public Object getElementAt(int index)
    {
      return getEntry(index);
    }

    //--------------------- End Interface ListModel ---------------------

    public Viewer getViewer()
    {
      return viewer;
    }

    /** Apply new Window object and reset model data. */
    public void setResource(Window panel)
    {
      if (panel != this.panel) {
        int oldSize = getSize();
        clearList();
        this.panel = panel;

        // populating list with available controls
        for (int i = 0; i < getResource().getControlCount(); i++) {
          listControls.add(createControl(getViewer(), getResource().getControl(i)));
        }

        // notifying changed items
        int numChanged = Math.min(oldSize, getSize());
        if (numChanged > 1) {
          fireContentsChanged(this, 1, numChanged-1);
        }

        // notifying added/removed items
        int numAdded = getSize() - oldSize;
        if (numAdded > 0) {
          fireIntervalAdded(this, numChanged, numChanged+numAdded-1);
        } else if (numAdded < 0) {
          numAdded = -numAdded;
          fireIntervalRemoved(this, numChanged, numChanged+numAdded-1);
        }
      }
    }

    /** Return currently assigned Window object. */
    public Window getResource()
    {
      return panel;
    }

    // use cache to return given control
    private BaseControl getEntry(int index)
    {
      if (index >= 0 && index < getSize()) {
        return listControls.get(index);
      } else {
        return null;
      }
    }

    private void clearList()
    {
      BaseControl empty;
      if (listControls.isEmpty()) {
        empty = new UnknownControl(getViewer(), null);
      } else {
        empty = listControls.get(0);
      }
      listControls.clear();
      listControls.add(empty);   // first entry is special
    }
  }


  // Manages the Control Properties panel
  private static class PropertiesPanel extends JPanel implements ActionListener
  {
    // Format strings used to display common properties of a control
    private static final String FMT_POSITION = "X: %1$d, Y: %2$d";
    private static final String FMT_SIZE = "W: %1$d, H: %2$d";

    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private final JRadioButton[] rbButtonState = new JRadioButton[4];

    private JLabel lPosition, lSize;
    private JPanel pControl;
    private CardLayout clControl;
    private JCheckBox cbVisible, cbSliderGrabbed, cbTextFieldCaret, cbScrollBarUpState,
                      cbScrollBarDownState;

    public PropertiesPanel()
    {
      super();
      init();
    }

  //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      fireStateChanged();
    }

  //--------------------- End Interface ActionListener ---------------------

    public void addChangeListener(ChangeListener l)
    {
      if (l != null) {
        if (listeners.indexOf(l) < 0) {
          listeners.add(l);
        }
      }
    }

//    public void removeChangeListener(ChangeListener l)
//    {
//      if (l != null) {
//        int idx = listeners.indexOf(l);
//        if (idx >= 0) {
//          listeners.remove(idx);
//        }
//      }
//    }

    /** Display the properties panel for the given control type. */
    public void showPanel(ControlType type)
    {
      switch (type) {
        case Button:
        case Slider:
        case TextField:
        case TextArea:
        case Label:
        case ScrollBar:
          cbVisible.setEnabled(true);
          clControl.show(pControl, type.name());
          break;
        default:
          cbVisible.setEnabled(false);
          clControl.show(pControl, ControlType.Unknown.name());
      }
    }

    public void updateProperties(BaseControl control)
    {
      if (control != null && control.getResource() != null) {
        // setting common fields
        lPosition.setText(String.format(FMT_POSITION, control.getPosition().x, control.getPosition().y));
        lSize.setText(String.format(FMT_SIZE, control.getDimension().width, control.getDimension().height));
        setControlVisible(control.isVisible());
        showPanel(control.getType());

        // setting specialized fields
        if (control instanceof ButtonControl) {
          ButtonControl c = (ButtonControl)control;
          if (c.isUnpressed()) {
            setButtonUnpressed();
          } else if (c.isPressed()) {
            setButtonPressed();
          } else if (c.isSelected()) {
            setButtonSelected();
          } else if (c.isDisabled()) {
            setButtonDisabled();
          }
        } else if (control instanceof SliderControl) {
          SliderControl c = (SliderControl)control;
          setSliderGrabbed(c.isGrabbed());
        } else if (control instanceof TextFieldControl) {
          TextFieldControl c = (TextFieldControl)control;
          setTextFieldCaretEnabled(c.isCaretEnabled());
        } else if (control instanceof ScrollBarControl) {
          ScrollBarControl c = (ScrollBarControl)control;
          setScrollBarUpArrowPressed(c.isUpArrowPressed());
          setScrollBarDownArrowPressed(c.isDownArrowPressed());
        }
      } else {
        lPosition.setText("");
        lSize.setText("");
        showPanel(ControlType.Unknown);
      }
    }

    public void setControlVisible(boolean b) { cbVisible.setSelected(b); }
    public boolean isControlVisible() { return cbVisible.isSelected(); }

    public void setButtonUnpressed() { rbButtonState[0].setSelected(true); }
    public void setButtonPressed() { rbButtonState[1].setSelected(true); }
    public void setButtonSelected() { rbButtonState[2].setSelected(true); }
    public void setButtonDisabled() { rbButtonState[3].setSelected(true); }
    public boolean isButtonUnpressed() { return rbButtonState[0].isSelected(); }
    public boolean isButtonPressed() { return rbButtonState[1].isSelected(); }
    public boolean isButtonSelected() { return rbButtonState[2].isSelected(); }
    public boolean isButtonDisabled() { return rbButtonState[3].isSelected(); }

    public void setSliderGrabbed(boolean b) { cbSliderGrabbed.setSelected(b); }
    public boolean isSliderGrabbed() { return cbSliderGrabbed.isSelected(); }

    public void setTextFieldCaretEnabled(boolean b) { cbTextFieldCaret.setSelected(b); }
    public boolean isTextFieldCaretEnabled() { return cbTextFieldCaret.isSelected(); }

    public void setScrollBarUpArrowPressed(boolean b) { cbScrollBarUpState.setSelected(b); }
    public void setScrollBarDownArrowPressed(boolean b) { cbScrollBarDownState.setSelected(b); }
    public boolean isScrollBarUpArrowPressed() { return cbScrollBarUpState.isSelected(); }
    public boolean isScrollBarDownArrowPressed() { return cbScrollBarDownState.isSelected(); }

    private void fireStateChanged()
    {
      ChangeEvent e = new ChangeEvent(this);
      for (int i = listeners.size() - 1; i >= 0; i--) {
        listeners.get(i).stateChanged(e);
      }
    }

    private void init()
    {
      setLayout(new GridBagLayout());
      setBorder(BorderFactory.createTitledBorder("Control Properties "));

      GridBagConstraints gbc = new GridBagConstraints();

      // constructing panel commonly used by all control types
      JPanel pCommon = new JPanel(new GridBagLayout());
      JLabel lPosTitle = new JLabel("Position:");
      JLabel lSizeTitle = new JLabel("Size:");
      lPosition = new JLabel("X: 123, Y: 456");
      lSize = new JLabel("W: 64, H: 28");
      cbVisible = new JCheckBox("Control visible", true);
      cbVisible.addActionListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pCommon.add(lPosTitle, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
      pCommon.add(lPosition, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      pCommon.add(lSizeTitle, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
      pCommon.add(lSize, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      pCommon.add(cbVisible, gbc);

      // constructing specific control panels
      clControl = new CardLayout();
      pControl = new JPanel(clControl);

      // empty panel
      pControl.add(new JPanel(), ControlType.Unknown.name());

      // button panel
      JPanel pButton = new JPanel(new GridBagLayout());
      JLabel lButtonState = new JLabel("Button state:");
      ButtonGroup bg = new ButtonGroup();
      rbButtonState[0] = new JRadioButton("Unpressed", true);
      rbButtonState[1] = new JRadioButton("Pressed", false);
      rbButtonState[2] = new JRadioButton("Selected", false);
      rbButtonState[3] = new JRadioButton("Disabled", false);
      bg.add(rbButtonState[0]);
      bg.add(rbButtonState[1]);
      bg.add(rbButtonState[2]);
      bg.add(rbButtonState[3]);
      rbButtonState[0].addActionListener(this);
      rbButtonState[1].addActionListener(this);
      rbButtonState[2].addActionListener(this);
      rbButtonState[3].addActionListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButton.add(lButtonState, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButton.add(rbButtonState[0], gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButton.add(rbButtonState[1], gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButton.add(rbButtonState[2], gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButton.add(rbButtonState[3], gbc);

      pControl.add(pButton, ControlType.Button.name());


      // slider panel
      JPanel pSlider = new JPanel(new GridBagLayout());
      cbSliderGrabbed = new JCheckBox("Slider grabbed", false);
      cbSliderGrabbed.addActionListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pSlider.add(cbSliderGrabbed, gbc);

      pControl.add(pSlider, ControlType.Slider.name());


      // text field panel
      JPanel pTextField = new JPanel(new GridBagLayout());
      cbTextFieldCaret = new JCheckBox("Show caret", false);
      cbTextFieldCaret.addActionListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pTextField.add(cbTextFieldCaret, gbc);

      pControl.add(pTextField, ControlType.TextField.name());


      // text area panel (nothing to do)
      pControl.add(new JPanel(), ControlType.TextArea.name());


      // label panel (nothing to do)
      pControl.add(new JPanel(), ControlType.Label.name());


      // scroll bar panel
      JPanel pScrollBar = new JPanel(new GridBagLayout());
      cbScrollBarUpState = new JCheckBox("Up arrow pressed", false);
      cbScrollBarUpState.addActionListener(this);
      cbScrollBarDownState = new JCheckBox("Down arrow pressed", false);
      cbScrollBarDownState.addActionListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pScrollBar.add(cbScrollBarUpState, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pScrollBar.add(cbScrollBarDownState, gbc);

      pControl.add(pScrollBar, ControlType.ScrollBar.name());


      // putting all together
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
      add(pCommon, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(16, 8, 8, 8), 0, 0);
      add(pControl, gbc);

      // showing empty control by default
      showPanel(ControlType.Unknown);
    }
  }


  // Manages a single CHU panel
  private static class Panel
  {
    private final Viewer viewer;
    private final Window panel;

    private Image image, bg;

    public Panel(Viewer viewer, Window panel)
    {
      this.viewer = viewer;
      this.panel = panel;
    }

    public Viewer getViewer()
    {
      return viewer;
    }

    public Window getResource()
    {
      return panel;
    }

    /** Returns the position of the background image for this panel. */
    public Point getPosition()
    {
      return getResource().getWindowPosition();
    }

    /** Returns width and height of the background image for this panel.  */
    public Dimension getDimension()
    {
      return getResource().getWindowDimension();
    }

    /** Returns the total width and height of the panel. */
    public Dimension getPanelDimension()
    {
      Point p = getPosition();
      Dimension dim = getDimension();
      dim.width += p.x;
      dim.height += p.y;
      return dim;
    }

    /**
     * Returns the background image for this panel. Use getPosition/getDimension to correctly
     * place it on the panel.
     */
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    /** Recreates the panel. */
    public void reset()
    {
      image = null;
      bg = null;

      // recreating controls
      int numControls = getViewer().getControls().getSize();
      for (int i = 0; i < numControls; i++) {
        BaseControl control = (BaseControl)getViewer().getControls().getElementAt(i);
        if (control != null) {
          control.updateImage();
        }
      }

      repaint();
    }

    /** Forces a repaint of the panel and its controls. */
    public void repaint()
    {
      updateImage();
    }

    @Override
    public String toString()
    {
      if (getResource() != null) {
        StringBuilder sb = new StringBuilder("ID: ");
        sb.append(getResource().getWindowId());
        sb.append(" (Background: ");
        if (getResource().hasBackgroundImage()) {
          sb.append(getResource().getBackgroundImage());
        } else {
          sb.append("none");
        }
        sb.append(")");
        return sb.toString();
      } else {
        return "(none)";
      }
    }

    private void updateImage()
    {
      if (image == null) {
        Dimension dim = getPanelDimension();
        if (dim.width == 0) dim.width = 1;
        if (dim.height == 0) dim.height = 1;
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      Graphics2D g = (Graphics2D)image.getGraphics();
      if (g != null) {
        try {
          Composite comp = g.getComposite();
          g.setComposite(AlphaComposite.Src);
          g.setColor(new Color(0, getViewer().isTransparentBackground()));
          g.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
          g.setComposite(comp);

          Point pPanel = getPosition();

          // 1. drawing background image (if available)
          if (getResource().hasBackgroundImage()) {
            if (bg == null) {
              String resName = getResource().getBackgroundImage();
              MosDecoder mos = MosDecoder.loadMos(ResourceFactory.getInstance().getResourceEntry(resName));
              if (mos != null) {
                if (mos instanceof MosV1Decoder) {
                  ((MosV1Decoder)mos).setTransparencyEnabled(true);
                }
                bg = (BufferedImage)mos.getImage();
              }
            }
            if (bg != null) {
              g.drawImage(bg, pPanel.x, pPanel.y, null);
            }
          }

          // 2. drawing control elements onto the panel
          int numControls = getViewer().getControls().getSize();
          for (int i = 0; i < numControls; i++) {
            BaseControl control = (BaseControl)getViewer().getControls().getElementAt(i);
            if (control != null) {
              Image ctrlImage = control.getImage();
              if (ctrlImage != null) {
                Point pCtrl = control.getPosition();
                g.drawImage(ctrlImage, pPanel.x + pCtrl.x, pPanel.y + pCtrl.y, null);
              }
            }
          }

          // 3. drawing outline if needed
          if (getViewer().isControlOutlined()) {
            BaseControl control = getViewer().getSelectedControl();
            if (control != null) {
              Point pCtrl = control.getPosition();
              Dimension dim = control.getDimension();
              g.setColor(control.getOutlinedColor());
              g.drawRect(pPanel.x + pCtrl.x, pPanel.y + pCtrl.y, dim.width-1, dim.height-1);
            }
          }
        } finally {
          g.dispose();
          g = null;
        }
      }
    }
  }

  // Common base for control specific classes
  private static abstract class BaseControl
  {
    private final Viewer viewer;
    private final Control control;

    private Color outlinedColor;
    private boolean visible;

    protected BaseControl(Viewer viewer, Control control, ControlType type)
    {
      if (viewer == null) {
        throw new NullPointerException("viewer is null");
      }
      if (control != null && getControlType(control.getControlType()) != type) {
        throw new IllegalArgumentException("Control type does not match.");
      }
      this.viewer = viewer;
      this.control = control;
      outlinedColor = Color.GREEN;
      visible = true;
    }

    /** Returns the viewer object. */
    public Viewer getViewer()
    {
      return viewer;
    }

    /** Returns the control object. */
    public Control getResource()
    {
      return control;
    }

    /** Returns whether a valid control is associated with this instance. */
    public boolean isEmpty()
    {
      return (control == null);
    }

    /** Returns the control type. */
    public ControlType getType()
    {
      if (!isEmpty()) {
        return getControlType(getResource().getControlType());
      } else {
        return ControlType.Unknown;
      }
    }

    /** Returns the position of the control relative to the parent panel. */
    public Point getPosition()
    {
      if (!isEmpty()) {
        return getResource().getControlPosition();
      } else {
        return new Point();
      }
    }

    /** Returns width and height of the control. */
    public Dimension getDimension()
    {
      if (!isEmpty()) {
        Dimension dim = getResource().getControlDimensions();
        if (dim.width <= 0) dim.width = 1;
        if (dim.height <= 0) dim.height = 1;
        return dim;
      } else {
        return new Dimension(1, 1);
      }
    }

    /** Set whether the control should be drawn. */
    public void setVisible(boolean set)
    {
      visible = set;
    }

    /** Returns whether the control is drawn. */
    public boolean isVisible()
    {
      return visible;
    }

    /** Returns the visual representation of the current control. */
    public abstract Image getImage();

//    /** Specify the outlined color. */
//    public void setOutlinedColor(Color color)
//    {
//      if (color != null) {
//        if (!color.equals(outlinedColor)) {
//          outlinedColor = color;
//          updateImage();
//        }
//      } else {
//        outlinedColor = Color.GREEN;
//      }
//    }

    /** Returns the outlined color. */
    public Color getOutlinedColor()
    {
      return outlinedColor;
    }

    @Override
    public String toString()
    {
      if (getResource() != null) {
        StringBuilder sb = new StringBuilder("ID: ");
        sb.append(getResource().getControlId());
        sb.append(" (Type: ");
        sb.append(getControlType(getResource().getControlType()).name());
        sb.append(")");
        return sb.toString();
      } else {
        return "(none)";
      }
    }

    /** Apply settings from properties panel. Note: Override to add more functionality. */
    public void updateState()
    {
      PropertiesPanel panel = getViewer().getProperties();
      setVisible(panel.isControlVisible());
    }

    /** Use this method to repaint the control. */
    public abstract void updateImage();

    // Convert cases of the text controlled by flags.
    // (flags: 0=first letter upper case, 1=all letters upper case, 2=all letters lower case)
    protected String convertText(String text, int flags)
    {
      if (text != null && !text.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        // ctrl: 0=no change, 1=force upper, 2=force lower
        int ctrl = (flags == 2) ? 2 : 1;
        for (int i = 0; i < text.length(); i++) {
          char ch = text.charAt(i);
          if (ctrl == 1) {
            ch = Character.toUpperCase(ch);
            if (flags == 0 && !Character.isWhitespace(ch)) ctrl = 0;
          } else if (ctrl == 2) {
            ch = Character.toLowerCase(ch);
          }
          if (flags == 0 && (ch == '.' || ch == '?' || ch == '!')) {
            ctrl = 1;
          }
          sb.append(ch);
        }
        return sb.toString();
      }
      return "";
    }

    /**
     * Returns the dimension and base line for fully rendered the text with the given font BAM.
     * @param text The text to render.
     * @param fntBam The font BAM.
     * @return Required dimension and baseline to fully render the text. Dimension is specified by
     *         width and height, vertical baseline position is specified by y.
     */
    protected static Rectangle getTextDimension(String text, BamDecoder fntBam)
    {
      Rectangle rect = new Rectangle();
      if (text != null && !text.isEmpty() && fntBam != null) {

        // 1. determine text height and baseline position
        int numFrames = fntBam.frameCount();
        for (int i = 0; i < numFrames; i++) {
          FrameEntry info = fntBam.getFrameInfo(i);
          rect.y = Math.max(rect.y, info.getCenterY());
          int lower = Math.max(0, info.getHeight() - info.getCenterY());
          rect.height = Math.max(rect.height, rect.y+lower);
        }

        // 2. determine text width
        BamControl ctrl = fntBam.createControl();
        ctrl.setMode(BamControl.Mode.Individual);
        for (int i = 0; i < text.length(); i++) {
          int ch = text.charAt(i);
          if (ch < 1 || ch > 255) ch = 32;
          int frameIdx = ctrl.cycleGetFrameIndexAbsolute(ch-1, 0);
          if (frameIdx >= 0) {
            rect.width += fntBam.getFrameInfo(frameIdx).getWidth();
          }
        }
      }
      // don't return empty dimension
      if (rect.height == 0) rect.height = 1;
      if (rect.width == 0) rect.width = 1;

      return rect;
    }

    /**
     * Renders the given text message into an Image object.
     * @param text The text to render.
     * @param fntBam The Font BAM.
     * @param fntColor Text color (can be null).
     * @param trueColor Don't postprocess text if set.
     */
    protected static Image drawText(String text, BamDecoder fntBam, Color fntColor, boolean trueColor)
    {
      BufferedImage image = null;
      if (text == null) text = "";
      if (fntBam != null) {
        BamControl ctrl = fntBam.createControl();
        ctrl.setMode(BamControl.Mode.Individual);
        Rectangle rect = getTextDimension(text, fntBam);
        BufferedImage letter = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        image = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)image.getGraphics();
        try {
          int curX = 0;
          for (int i = 0; i < text.length(); i++) {
            int ch = text.charAt(i);
            if (ch < 1 || ch > 255) ch = 32;
            ctrl.cycleSet(ch-1);
            ctrl.cycleSetFrameIndex(0);
            int frameIdx = ctrl.cycleGetFrameIndexAbsolute();
            if (frameIdx >= 0) {
              ctrl.cycleGetFrame(letter);
              FrameEntry info = fntBam.getFrameInfo(frameIdx);
              int width = info.getWidth();
              int height = info.getHeight();
              int baseline = info.getCenterY();
              int dx = curX;
              int dy = rect.y - baseline;
              g.drawImage(letter, dx, dy, dx+width, dy+height, 0, 0, width, height, null);
              curX += width;
            }
          }
        } finally {
          g.dispose();
          g = null;
        }

        // postprocessing text
        // Dirty hack: the correct information is most likely stored in the FNT resource directly
        if ((ResourceFactory.isEnhancedEdition() || trueColor == false) &&
            ctrl instanceof BamV1Control) {
          int[] palette = ((BamV1Control)ctrl).getPalette();
          // Hack: determining whether palette contains alpha values
          boolean isAlpha = (palette.length == 256);
          int intensity = palette[1] & 0xff;
          for (int i = 1; i < 255 && isAlpha; i++, intensity--) {
            int alpha = intensity | (intensity << 8) | (intensity << 16);
            isAlpha &= (palette[i] & 0xffffff) == alpha;
          }

          if (isAlpha) {
            int rgb;
            if (fntColor != null) {
              rgb = (fntColor.getBlue() << 16) | (fntColor.getGreen() << 8) | fntColor.getRed();
            } else {
              rgb = palette[1] & 0x00ffffff;
            }
            int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
            if (buffer != null) {
              for (int i = 0; i < buffer.length; i++) {
                if ((buffer[i] & 0xff000000) != 0) {
                  buffer[i] = rgb | ((255 - (buffer[i] & 255)) << 24);
                }
              }
            }
          }
        }

      }
      return image;
    }
  }

  // Manages the visual appearance of unrecognized control types
  private static class UnknownControl extends BaseControl
  {
    private BufferedImage image;

    public UnknownControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.Unknown);
      updateImage();
    }

    @Override
    public Image getImage()
    {
      return image;
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            // 2. drawing control
            // nothing to do...
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }
  }

  // Manages the visual appearance of buttons
  private static class ButtonControl extends BaseControl
  {
    private static final int UNPRESSED  = 0;
    private static final int PRESSED    = 1;
    private static final int SELECTED   = 2;
    private static final int DISABLED   = 3;

    private static final HashSet<String> ignoreResourceSet = new HashSet<String>();

    static {
      // Hack: ignore a set of known background BAMs with cycle and frame indices
      ignoreResourceSet.add("BIGBUTT.BAM:0:0");
      ignoreResourceSet.add("BIGBUTT.BAM:0:1");
      ignoreResourceSet.add("BIGBUTT.BAM:0:2");
      ignoreResourceSet.add("CIFF4INV.BAM:0:0");
      ignoreResourceSet.add("CIFF4INV.BAM:0:1");
      ignoreResourceSet.add("CIFF4INV.BAM:0:2");
      ignoreResourceSet.add("CIFF4INV.BAM:0:3");
      ignoreResourceSet.add("GUICTRL.BAM:0:0");
      ignoreResourceSet.add("GUICTRL.BAM:0:1");
      ignoreResourceSet.add("GUICTRL.BAM:0:2");
      ignoreResourceSet.add("GUICTRL.BAM:0:3");
    }

    private BufferedImage image;
    private int buttonState;

    public ButtonControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.Button);
      buttonState = UNPRESSED;
      updateImage();
    }

    // Required for properties panel
    public void setUnPressed() { buttonState = UNPRESSED; }
    public void setPressed() { buttonState = PRESSED; }
    public void setSelected() { buttonState = SELECTED; }
    public void setDisabled() { buttonState = DISABLED; }
    public boolean isUnpressed() { return (buttonState == UNPRESSED); }
    public boolean isPressed() { return (buttonState == PRESSED); }
    public boolean isSelected() { return (buttonState == SELECTED); }
    public boolean isDisabled() { return (buttonState == DISABLED); }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateState()
    {
      super.updateState();
      PropertiesPanel panel = getViewer().getProperties();
      if (panel.isButtonUnpressed()) {
        setUnPressed();
      } else if (panel.isButtonPressed()) {
        setPressed();
      } else if (panel.isButtonSelected()) {
        setSelected();
      } else if (panel.isButtonDisabled()) {
        setDisabled();
      }
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            if (isVisible()) {
              // loading BAM
              String resName = ((ResourceRef)getResource().getAttribute("Button")).getResourceName();
              // getting specified cycle index
              int cycleIdx = ((DecNumber)getResource().getAttribute("Animation number")).getValue();
              int frameIdx = 0;
              // getting specified cycle frame index
              if (isUnpressed()) {
                frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Unpressed")).getValue();
              } else if (isPressed()) {
                frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Pressed")).getValue();
              } else if (isSelected()) {
                frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Selected")).getValue();
              } else if (isDisabled()) {
                frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Disabled")).getValue();
              }
              if (!isResourceIgnored(resName, cycleIdx, frameIdx)) {
                BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
                if (bam != null) {
                  BamControl bamCtrl = bam.createControl();
                  bamCtrl.setMode(BamControl.Mode.Individual);
                  if (cycleIdx >= 0 && cycleIdx < bamCtrl.cycleCount()) {
                    bamCtrl.cycleSet(cycleIdx);
                    if (frameIdx >= 0 && frameIdx < bamCtrl.cycleFrameCount()) {
                      bamCtrl.cycleSetFrameIndex(frameIdx);

                    // drawing BAM
                    Image bamImage = bamCtrl.cycleGetFrame();
                    g.drawImage(bamImage, 0, 0, null);
                    }
                  }
                }
              }
            }
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }

    // Indicates whether to ignore the resource specified by the given filename
    private static boolean isResourceIgnored(String resName, int cycleIdx, int frameIdx)
    {
      if (resName != null && !resName.isEmpty()) {
        return ignoreResourceSet.contains(String.format("%1$s:%2$d:%3$d",
                                                        resName.toUpperCase(Locale.ENGLISH), cycleIdx, frameIdx));
      } else {
        return true;
      }
    }
  }


  // Manages the visual appearance of sliders
  private static class SliderControl extends BaseControl
  {
    private BufferedImage image;
    private boolean sliderGrabbed;

    public SliderControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.Slider);
      sliderGrabbed = false;
      updateImage();
    }

    // required for properties panel
    public void setGrabbed(boolean b) { sliderGrabbed = b; }
    public boolean isGrabbed() { return sliderGrabbed; }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateState()
    {
      super.updateState();
      PropertiesPanel panel = getViewer().getProperties();
      setGrabbed(panel.isSliderGrabbed());
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            // 2.1. drawing background image
            String resName = ((ResourceRef)getResource().getAttribute("Background image")).getResourceName();
            MosDecoder mos = MosDecoder.loadMos(ResourceFactory.getInstance().getResourceEntry(resName));
            if (mos != null) {
              if (mos instanceof MosV1Decoder) {
                ((MosV1Decoder)mos).setTransparencyEnabled(true);
              }
              mos.getImage(image);
            }

            // 2.2. drawing control elements
            if (isVisible()) {
              resName = ((ResourceRef)getResource().getAttribute("Slider knob")).getResourceName();
              BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
              if (bam != null) {
                BamControl bamCtrl = bam.createControl();
                bamCtrl.setMode(BamControl.Mode.Individual);
                // getting specified cycle index
                int cycleIdx = ((DecNumber)getResource().getAttribute("Animation number")).getValue();
                cycleIdx = Math.min(bamCtrl.cycleCount()-1, Math.max(0, cycleIdx));
                bamCtrl.cycleSet(cycleIdx);
                int frameIdx;
                // getting specified cycle frame index
                if (isGrabbed()) {
                  frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Grabbed")).getValue();
                } else {
                  frameIdx = ((DecNumber)getResource().getAttribute("Frame number: Ungrabbed")).getValue();
                }
                frameIdx = Math.min(bamCtrl.cycleFrameCount()-1, Math.max(0, frameIdx));
                bamCtrl.cycleSetFrameIndex(frameIdx);

                // drawing frame
                Image knob = bamCtrl.cycleGetFrame();
                int knobX = ((DecNumber)getResource().getAttribute("Knob position: X")).getValue();
                int knobY = ((DecNumber)getResource().getAttribute("Knob position: Y")).getValue();
                g.drawImage(knob, knobX, knobY, knob.getWidth(null), knob.getHeight(null), null);
              }
            }
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }
  }


  // Manages the visual appearance of text fields
  private static class TextFieldControl extends BaseControl
  {
    private static final HashSet<String> ignoreResourceSet = new HashSet<String>();

    static {
      // Hack: ignore a set of known background MOS resources
      ignoreResourceSet.add("XXXXGRBG.MOS");
    }

    private BufferedImage image;
    private boolean caretEnabled;

    public TextFieldControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.TextField);
      caretEnabled = false;
      updateImage();
    }

    // required for properties panel
    public void setCaretEnabled(boolean b) { caretEnabled = b; }
    public boolean isCaretEnabled() { return caretEnabled; }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateState()
    {
      super.updateState();
      PropertiesPanel panel = getViewer().getProperties();
      setCaretEnabled(panel.isTextFieldCaretEnabled());
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            // 2.1. drawing background image (is it actually used?)
            String resName = ((ResourceRef)getResource().getAttribute("Background 1")).getResourceName();
            if (!isResourceIgnored(resName)) {
              MosDecoder mos = MosDecoder.loadMos(ResourceFactory.getInstance().getResourceEntry(resName));
              if (mos != null) {
                if (mos instanceof MosV1Decoder) {
                  ((MosV1Decoder)mos).setTransparencyEnabled(true);
                }
                mos.getImage(image);
              }
            }

            // 2.2. drawing text
            if (isVisible()) {
              int caretX = ((DecNumber)getResource().getAttribute("Caret position: X")).getValue();
              int caretY = ((DecNumber)getResource().getAttribute("Caret position: Y")).getValue();

              String text = ((TextString)getResource().getAttribute("Initial text")).toString();
              if (!text.isEmpty()) {
                resName = ((ResourceRef)getResource().getAttribute("Font")).getResourceName();
                BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
                if (bam != null) {
                  int maxLen = ((DecNumber)getResource().getAttribute("Field length")).getValue();
                  if (text.length() > maxLen) text = text.substring(0, maxLen);
                  int flags = ((Bitmap)getResource().getAttribute("Allowed case")).getValue();
                  text = convertText(text, flags);
                  Image textImage = drawText(text, bam, null, false);
                  if (textImage != null) {
                    g.drawImage(textImage, caretX, caretY,
                                textImage.getWidth(null), textImage.getHeight(null), null);
                  }
                }
              }

              // 2.3. drawing caret
              if (isCaretEnabled()) {
                resName = ((ResourceRef)getResource().getAttribute("Caret")).getResourceName();
                BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
                if (bam != null) {
                  BamControl bamCtrl = bam.createControl();
                  bamCtrl.setMode(BamControl.Mode.Individual);
                  // getting specified cycle index
                  int cycleIdx = ((DecNumber)getResource().getAttribute("Animation number")).getValue();
                  cycleIdx = Math.min(bamCtrl.cycleCount()-1, Math.max(0, cycleIdx));
                  bamCtrl.cycleSet(cycleIdx);
                  // getting specified cycle frame index
                  int frameIdx = ((DecNumber)getResource().getAttribute("Frame number")).getValue();
                  frameIdx = Math.min(bamCtrl.cycleFrameCount()-1, Math.max(0, frameIdx));
                  bamCtrl.cycleSetFrameIndex(frameIdx);

                  // drawing frame
                  Image caret = bamCtrl.cycleGetFrame();
                  g.drawImage(caret, caretX, caretY, null);
                }
              }
            }
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }

    // Indicates whether to ignore the resource specified by the given filename
    private static boolean isResourceIgnored(String resName)
    {
      if (resName != null && !resName.isEmpty()) {
        return ignoreResourceSet.contains(resName.toUpperCase(Locale.ENGLISH));
      } else {
        return true;
      }
    }
  }


  // Manages the visual appearance of text areas
  private static class TextAreaControl extends BaseControl
  {
    private BufferedImage image;

    public TextAreaControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.TextArea);
      updateImage();
    }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            // nothing to do?
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }
  }


  // Manages the visual appearance of labels
  private static class LabelControl extends BaseControl
  {
    private BufferedImage image;

    public LabelControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.Label);
      updateImage();
    }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            if (isVisible()) {
              String text = StringResource.getStringRef(((StringRef)getResource().getAttribute("Initial text")).getValue());
              if (text != null) {
                String resName = ((ResourceRef)getResource().getAttribute("Font")).getResourceName();
                BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
                if (bam != null) {
                  Flag flags = (Flag)getResource().getAttribute("Text flags");
                  Color col = null;
                  if (flags.isFlagSet(0)) {
                    col = new Color(((ColorPicker)getResource().getAttribute("Color 1")).getValue());
                  }
                  Image textImage = drawText(text, bam, col, flags.isFlagSet(1));
                  if (textImage != null) {
                    // calculating text alignment
                    int srcWidth = textImage.getWidth(null);
                    int srcHeight = textImage.getHeight(null);
                    int dstWidth = image.getWidth();
                    int dstHeight = image.getHeight();
                    int x = 0, y = 0;
                    if (flags.isFlagSet(3)) { // left
                      x = 0;
                    } else if (flags.isFlagSet(4)) {  // right
                      x = dstWidth - srcWidth;
                    } else {  // hcenter
                      x = (dstWidth - srcWidth) / 2;
                    }
                    if (flags.isFlagSet(5)) { // top
                      y = 0;
                    } else if (flags.isFlagSet(7)) {  // bottom
                      y = dstHeight - srcHeight;
                    } else {  // vcenter
                      y = (dstHeight - srcHeight) / 2;
                    }
                    // drawing text
                    g.drawImage(textImage, x, y, srcWidth, srcHeight, null);
                  }
                }
              }
            }
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }
  }


  // Manages the visual appearance of scroll bars
  private static class ScrollBarControl extends BaseControl
  {
    private BufferedImage image;
    private boolean upPressed, downPressed;

    public ScrollBarControl(Viewer viewer, Control control)
    {
      super(viewer, control, ControlType.ScrollBar);
      upPressed = downPressed = false;
      updateImage();
    }

    // required for properties panel
    public void setUpArrowPressed(boolean b) { upPressed = b; }
    public boolean isUpArrowPressed() { return upPressed; }
    public void setDownArrowPressed(boolean b) { downPressed = b; }
    public boolean isDownArrowPressed() { return downPressed; }

    @Override
    public Image getImage()
    {
      if (image == null) {
        updateImage();
      }
      return image;
    }

    @Override
    public void updateState()
    {
      super.updateState();
      PropertiesPanel panel = getViewer().getProperties();
      setUpArrowPressed(panel.isScrollBarUpArrowPressed());
      setDownArrowPressed(panel.isScrollBarDownArrowPressed());
    }

    @Override
    public void updateImage()
    {
      if (image == null) {
        Dimension dim = getDimension();
        image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      }

      if (!isEmpty()) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        if (g != null) {
          try {
            // 1. clearing image
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.Src);
            g.setColor(new Color(0, true));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(comp);

            // 2. drawing control
            if (isVisible()) {
              String resName = ((ResourceRef)getResource().getAttribute("Graphics")).getResourceName();
              BamDecoder bam = BamDecoder.loadBam(ResourceFactory.getInstance().getResourceEntry(resName));
              if (bam != null) {
                int cycleIdx = ((DecNumber)getResource().getAttribute("Animation number")).getValue();
                int frameTroughIdx = ((DecNumber)getResource().getAttribute("Frame number: Trough")).getValue();
                int frameSliderIdx = ((DecNumber)getResource().getAttribute("Frame number: Slider")).getValue();
                int frameUpIdx, frameDownIdx;
                if (isUpArrowPressed()) {
                  frameUpIdx = ((DecNumber)getResource().getAttribute("Frame number: Up-arrow, pressed")).getValue();
                } else {
                  frameUpIdx = ((DecNumber)getResource().getAttribute("Frame number: Up-arrow, unpressed")).getValue();
                }
                if (isDownArrowPressed()) {
                  frameDownIdx = ((DecNumber)getResource().getAttribute("Frame number: Down-arrow, pressed")).getValue();
                } else {
                  frameDownIdx = ((DecNumber)getResource().getAttribute("Frame number: Down-arrow, unpressed")).getValue();
                }
                BamControl ctrl = bam.createControl();
                ctrl.cycleSet(cycleIdx);

                // drawing trough
                Image image = ctrl.cycleGetFrame(frameTroughIdx);
                if (image != null) {
                  int idx = ctrl.cycleGetFrameIndexAbsolute(frameUpIdx);
                  int top = bam.getFrameInfo(idx).getHeight();
                  idx = ctrl.cycleGetFrameIndexAbsolute(frameDownIdx);
                  int bottom = getDimension().height - bam.getFrameInfo(idx).getHeight();
                  int w = image.getWidth(null);
                  int h = image.getHeight(null);
                  int segments = (bottom-top) / h;
                  int lastSegmentHeight = (bottom-top) % h;
                  idx = ctrl.cycleGetFrameIndexAbsolute(frameTroughIdx);
                  int curY = top;
                  for (int i = 0; i < segments; i++) {
                    g.drawImage(image, 0, curY, null);
                    curY += h;
                  }
                  if (lastSegmentHeight > 0) {
                    g.drawImage(image, 0, curY, w, curY+lastSegmentHeight,
                                0, 0, w, lastSegmentHeight, null);
                  }
                }

                // drawing up arrow
                image = ctrl.cycleGetFrame(frameUpIdx);
                if (image != null) {
                  g.drawImage(image, 0, 0, null);
                }

                // drawing down arrow
                image = ctrl.cycleGetFrame(frameDownIdx);
                if (image != null) {
                  int curY = getDimension().height - image.getHeight(null);
                  g.drawImage(image, 0, curY, null);
                }

                // drawing slider knob
                image = ctrl.cycleGetFrame(frameSliderIdx);
                if (image != null) {
                  int curY = bam.getFrameInfo(ctrl.cycleGetFrameIndexAbsolute(frameUpIdx)).getHeight();
                  g.drawImage(image, 0, curY, null);
                }
              }
            }
          } finally {
            g.dispose();
            g = null;
          }
        }
      }
    }
  }
}

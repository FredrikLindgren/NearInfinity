// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.Flag;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JComponent;


public final class DlgResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  private static final String sNonInt[] = {"Pausing dialogue", "Turn hostile",
                                           "Escape area", "Ignore attack"};
  private SectionCount countState, countTrans, countStaTri, countTranTri, countAction;
  private SectionOffset offsetState, offsetTrans, offsetStaTri, offsetTranTri, offsetAction;
  private Viewer detailViewer;
  private TreeViewer treeViewer;

  public DlgResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new State(), new Transition(), new StateTrigger(),
                              new ResponseTrigger(), new Action()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0: return StructViewer.TAB_VIEW;
      case 1: return "Tree";
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
        if (detailViewer == null)
          detailViewer = new Viewer(this);
        return detailViewer;
      case 1:
        if (treeViewer == null)
          treeViewer = new TreeViewer(this);
        return treeViewer;
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    offsetState.setValue(0x30);
    if (getFieldCount() >= 13 && getField(12).getName().equalsIgnoreCase("Threat response"))
      offsetState.setValue(0x34);
    offsetTrans.setValue(offsetState.getValue() + 0x10 * countState.getValue());
    offsetStaTri.setValue(offsetTrans.getValue() + 0x20 * countTrans.getValue());
    offsetTranTri.setValue(offsetStaTri.getValue() + 0x8 * countStaTri.getValue());
    offsetAction.setValue(offsetTranTri.getValue() + 0x8 * countTranTri.getValue());
    int stringoff = offsetAction.getValue() + 0x8 * countAction.getValue();
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof AbstractCode) {
        stringoff += ((AbstractCode)o).updateOffset(stringoff);
      }
    }
    super.write(os);

    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof AbstractCode) {
        ((AbstractCode)o).writeString(os);
      }
    }
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    addField(version);
    if (!version.toString().equalsIgnoreCase("V1.0")) {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }

    countState = new SectionCount(buffer, offset + 8, 4, "# states",
                                  State.class);
    addField(countState);
    offsetState = new SectionOffset(buffer, offset + 12, "States offset",
                                    State.class);
    addField(offsetState);
    countTrans = new SectionCount(buffer, offset + 16, 4, "# responses",
                                  Transition.class);
    addField(countTrans);
    offsetTrans = new SectionOffset(buffer, offset + 20, "Responses offset",
                                    Transition.class);
    addField(offsetTrans);
    offsetStaTri = new SectionOffset(buffer, offset + 24, "State triggers offset",
                                     StateTrigger.class);
    addField(offsetStaTri);
    countStaTri = new SectionCount(buffer, offset + 28, 4, "# state triggers",
                                   StateTrigger.class);
    addField(countStaTri);
    offsetTranTri = new SectionOffset(buffer, offset + 32, "Response triggers offset",
                                      ResponseTrigger.class);
    addField(offsetTranTri);
    countTranTri = new SectionCount(buffer, offset + 36, 4, "# response triggers",
                                    ResponseTrigger.class);
    addField(countTranTri);
    offsetAction = new SectionOffset(buffer, offset + 40, "Actions offset",
                                     infinity.resource.dlg.Action.class);
    addField(offsetAction);
    countAction = new SectionCount(buffer, offset + 44, 4, "# actions",
                                   infinity.resource.dlg.Action.class);
    addField(countAction);

    if (offsetState.getValue() > 0x30) {
      addField(new Flag(buffer, offset + 48, 4, "Threat response", sNonInt));
    }

    offset = offsetState.getValue();
    for (int i = 0; i < countState.getValue(); i++) {
      State state = new State(this, buffer, offset, i);
      offset = state.getEndOffset();
      addField(state);
    }

    offset = offsetTrans.getValue();
    for (int i = 0; i < countTrans.getValue(); i++) {
      Transition transition = new Transition(this, buffer, offset, i);
      offset = transition.getEndOffset();
      addField(transition);
    }

    int textSize = 0;
    offset = offsetStaTri.getValue();
    for (int i = 0; i < countStaTri.getValue(); i++) {
      StateTrigger statri = new StateTrigger(buffer, offset, i);
      offset += statri.getSize();
      textSize += statri.getTextLength();
      addField(statri);
    }

    offset = offsetTranTri.getValue();
    for (int i = 0; i < countTranTri.getValue(); i++) {
      ResponseTrigger trantri = new ResponseTrigger(buffer, offset, i);
      offset += trantri.getSize();
      textSize += trantri.getTextLength();
      addField(trantri);
    }

    offset = offsetAction.getValue();
    for (int i = 0; i < countAction.getValue(); i++) {
      Action action = new Action(buffer, offset, i);
      offset += action.getSize();
      textSize += action.getTextLength();
      addField(action);
    }
    return offset + textSize;
  }

  // sorry for this (visibility)
  public void showStateWithStructEntry(StructEntry entry) {
    if (detailViewer == null) {
      getViewerTab(0);
    }
    detailViewer.showStateWithStructEntry(entry);
  }
}


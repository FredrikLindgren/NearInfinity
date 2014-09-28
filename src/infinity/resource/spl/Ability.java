// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.StructViewer;
import infinity.resource.AbstractAbility;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.ResourceFactory;

import javax.swing.JComponent;

public final class Ability extends AbstractAbility implements AddRemovable, HasAddRemovable, HasViewerTabs
{
  public static final String s_hostility[] = {"Hostile", "", "", "", "Non-hostile"};
  public static final String s_abilityuse[] = {"", "", "Spell slots", "", "Innate slots"};

  Ability() throws Exception
  {
    super(null, "Spell ability", new byte[40], 0);
  }

  Ability(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Spell ability " + number, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Effect()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewTabCount()
  {
    return 1;
  }

  @Override
  public String getViewTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    return new ViewerAbility(this);
  }

  @Override
  public boolean viewTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
    list.add(new Bitmap(buffer, offset, 1, "Type", s_type));
      list.add(new Bitmap(buffer, offset + 1, 1, "Hostility", s_hostility));
    } else
      list.add(new Bitmap(buffer, offset, 2, "Type", s_type));
    list.add(new Bitmap(buffer, offset + 2, 2, "Ability location", s_abilityuse));
    list.add(new ResourceRef(buffer, offset + 4, "Icon", "BAM"));
    list.add(new Bitmap(buffer, offset + 12, 1, "Target", s_targettype));
    list.add(new UnsignDecNumber(buffer, offset + 13, 1, "# targets"));
    list.add(new DecNumber(buffer, offset + 14, 2, "Range (feet)"));
    list.add(new DecNumber(buffer, offset + 16, 2, "Minimum level"));
    list.add(new DecNumber(buffer, offset + 18, 2, "Casting speed"));
    list.add(new DecNumber(buffer, offset + 20, 2, "Bonus to hit"));
    list.add(new DecNumber(buffer, offset + 22, 2, "Dice size"));
    list.add(new DecNumber(buffer, offset + 24, 2, "# dice thrown"));
    list.add(new DecNumber(buffer, offset + 26, 2, "Damage bonus"));
    list.add(new Bitmap(buffer, offset + 28, 2, "Damage type", s_dmgtype));
    list.add(new SectionCount(buffer, offset + 30, 2, "# effects", Effect.class));
    list.add(new DecNumber(buffer, offset + 32, 2, "First effect index"));
    list.add(new DecNumber(buffer, offset + 34, 2, "# charges"));
    list.add(new Unknown(buffer, offset + 36, 2));
    if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
      list.add(new ProRef(buffer, offset + 38, "Projectile"));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Bitmap(buffer, offset + 38, 2, "Projectile", s_proj_pst));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
      list.add(new Bitmap(buffer, offset + 38, 2, "Projectile", s_proj_iwd));
    else
      list.add(new Bitmap(buffer, offset + 38, 2, "Projectile", s_projectile));
    return offset + 40;
  }
}


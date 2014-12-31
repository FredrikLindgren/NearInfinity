// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResourceTreeFolder implements Comparable<ResourceTreeFolder>
{
  private final List<ResourceEntry> resourceEntries = new ArrayList<ResourceEntry>();
  private final List<ResourceTreeFolder> folders = new ArrayList<ResourceTreeFolder>();
  private final ResourceTreeFolder parentFolder;
  private final String folderName;

  public ResourceTreeFolder(ResourceTreeFolder parentFolder, String folderName)
  {
    this.parentFolder = parentFolder;
    this.folderName = folderName;
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ResourceTreeFolder o)
  {
    return folderName.compareToIgnoreCase(o.folderName);
  }

// --------------------- End Interface Comparable ---------------------

  @Override
  public String toString()
  {
    return folderName + " - " + getChildCount();
  }

  public String folderName()
  {
    return folderName;
  }

  public List<ResourceEntry> getResourceEntries()
  {
    return Collections.unmodifiableList(resourceEntries);
  }

  public List<ResourceEntry> getResourceEntries(String type)
  {
    List<ResourceEntry> list = new ArrayList<ResourceEntry>();
    for (int i = 0; i < resourceEntries.size(); i++) {
      ResourceEntry entry = resourceEntries.get(i);
      if (entry.getExtension().equalsIgnoreCase(type))
        list.add(entry);
    }
    for (int i = 0; i < folders.size(); i++) {
      ResourceTreeFolder folder = folders.get(i);
      list.addAll(folder.getResourceEntries(type));
    }
    return list;
  }

  public void addFolder(ResourceTreeFolder folder)
  {
    folders.add(folder);
  }

  public void addResourceEntry(ResourceEntry entry)
  {
    if (entry.isVisible()) {
      resourceEntries.add(entry);
    }
  }

  public Object getChild(int index)
  {
    if (index < folders.size())
      return folders.get(index);
    return resourceEntries.get(index - folders.size());
  }

  public int getChildCount()
  {
    return folders.size() + resourceEntries.size();
  }

  public List<ResourceTreeFolder> getFolders()
  {
    return Collections.unmodifiableList(folders);
  }

  public int getIndexOfChild(Object node)
  {
    if (node instanceof ResourceTreeFolder)
      return folders.indexOf(node);
    return folders.size() + resourceEntries.indexOf(node);
  }

  public ResourceTreeFolder getParentFolder()
  {
    return parentFolder;
  }

  public void removeFolder(ResourceTreeFolder folder)
  {
    folders.remove(folder);
  }

  public void removeResourceEntry(ResourceEntry entry)
  {
    resourceEntries.remove(entry);
  }

  public void sortChildren()
  {
    Collections.sort(resourceEntries);
    Collections.sort(folders);
    for (int i = 0; i < folders.size(); i++)
      folders.get(i).sortChildren();
  }
}


// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

public interface HasAddRemovable
{
  /**
   * Returns an array of available {@link AddRemovable} prototype objects.
   * @return An array of available {@link AddRemovable} objects.
   */
  AddRemovable[] getAddRemovables() throws Exception;

  /**
   * This method is called whenever an {@link AddRemovable} entry is about to be added
   * to the parent structure. It allows subclasses to make final modifications to the given
   * {@link AddRemovable} argument before it is added to the structure or to cancel the operation.
   * @param entry The {@link AddRemovable} entry to add.
   * @return The {@link AddRemovable} entry to add.
   *         May return {@code null} to cancel the operation.
   */
  AddRemovable confirmAddEntry(AddRemovable entry) throws Exception;

  /**
   * This method is called whenever an {@link AddRemovable} entry is about to be removed from the
   * parent structure. It allows subclasses to cancel the operation.
   * @param entry The {@link AddRemovable} entry to remove.
   * @return {@code true} to continue the remove operation. {@code false} to cancel the
   *         remove operation.
   */
  boolean confirmRemoveEntry(AddRemovable entry) throws Exception;
}


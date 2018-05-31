// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.infinity.NearInfinity;
import org.infinity.check.BCSIDSChecker;
import org.infinity.check.CreInvChecker;
import org.infinity.check.DialogChecker;
import org.infinity.check.EffectsIndexChecker;
import org.infinity.check.IDSRefChecker;
import org.infinity.check.ResRefChecker;
import org.infinity.check.ResourceUseChecker;
import org.infinity.check.ScriptChecker;
import org.infinity.check.StringUseChecker;
import org.infinity.check.StrrefIndexChecker;
import org.infinity.check.StructChecker;
import org.infinity.gui.converter.ConvertToBam;
import org.infinity.gui.converter.ConvertToBmp;
import org.infinity.gui.converter.ConvertToMos;
import org.infinity.gui.converter.ConvertToPvrz;
import org.infinity.gui.converter.ConvertToTis;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructureFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.DialogSearcher;
import org.infinity.search.SearchFrame;
import org.infinity.search.SearchResource;
import org.infinity.search.TextResourceSearcher;
import org.infinity.updater.UpdateCheck;
import org.infinity.updater.UpdateInfo;
import org.infinity.updater.Updater;
import org.infinity.updater.UpdaterSettings;
import org.infinity.util.CharsetDetector;
import org.infinity.util.MassExporter;
import org.infinity.util.Misc;
import org.infinity.util.ObjectString;
import org.infinity.util.Pair;
import org.infinity.util.StringTable;
import org.infinity.util.io.FileManager;

public final class BrowserMenuBar extends JMenuBar
{
  public static final String VERSION = "v2.1-20180531";
  public static final int OVERRIDE_IN_THREE = 0, OVERRIDE_IN_OVERRIDE = 1, OVERRIDE_SPLIT = 2;
  public static final LookAndFeelInfo DEFAULT_LOOKFEEL =
      new LookAndFeelInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel");
  public static final int RESREF_ONLY = 0, RESREF_REF_NAME = 1, RESREF_NAME_REF = 2;
  public static final int DEFAULT_VIEW = 0, DEFAULT_EDIT = 1;

  // Defines platform-specific shortcut key (e.g. Ctrl on Win/Linux, Meta on Mac)
  private static final int CTRL_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  // Name of the child node in the GUI preferences path
  private static final String PREFS_PROFILES_NODE = "Profiles";

  private static BrowserMenuBar menuBar;

  private final EditMenu editMenu;
  private final FileMenu fileMenu;
  private final GameMenu gameMenu;
  private final OptionsMenu optionsMenu;
  private final SearchMenu searchMenu;
  private final ToolsMenu toolsMenu;
  private final HelpMenu helpMenu;
  private final Preferences prefsGui, prefsProfiles;

  public static BrowserMenuBar getInstance()
  {
    return menuBar;
  }

  private static JMenuItem makeMenuItem(String name, int menuKey, Icon icon, int shortKey,
                                        ActionListener listener)
  {
    JMenuItem item = new JMenuItem(name);
    if (menuKey != -1)
      item.setMnemonic(menuKey);
    if (icon != null)
      item.setIcon(icon);
    if (shortKey != -1)
      item.setAccelerator(KeyStroke.getKeyStroke(shortKey, CTRL_MASK));
    if (listener != null)
      item.addActionListener(listener);
    return item;
  }

  // Returns the main Preferences instance
  private static Preferences getPrefs()
  {
    if (getInstance() != null) {
      return getInstance().prefsGui;
    } else {
      return null;
    }
  }

  // Returns the Preferences instance for profile-specific settings
  private static Preferences getPrefsProfiles()
  {
    if (getInstance() != null) {
      return getInstance().prefsProfiles;
    } else {
      return null;
    }
  }

  public BrowserMenuBar()
  {
    menuBar = this;
    prefsGui = Preferences.userNodeForPackage(getClass());
    prefsProfiles = prefsGui.node(PREFS_PROFILES_NODE);
    gameMenu = new GameMenu();
    fileMenu = new FileMenu();
    editMenu = new EditMenu();
    searchMenu = new SearchMenu();
    toolsMenu = new ToolsMenu();
    optionsMenu = new OptionsMenu();
    helpMenu = new HelpMenu();
    add(gameMenu);
    add(fileMenu);
    add(editMenu);
    add(searchMenu);
    add(toolsMenu);
    add(optionsMenu);
    add(helpMenu);
  }

  public boolean autocheckBCS()
  {
    return optionsMenu.optionAutocheckBCS.isSelected();
  }

  public boolean showMoreCompileWarnings()
  {
    return optionsMenu.optionMoreCompileWarnings.isSelected();
  }

  public boolean showStrrefs()
  {
    return optionsMenu.optionShowStrrefs.isSelected();
  }

  public boolean showDlgTreeIcons()
  {
    return optionsMenu.optionDlgShowIcons.isSelected();
  }

  public boolean getHexColorMapEnabled()
  {
    return optionsMenu.optionShowHexColored.isSelected();
  }

  public boolean getKeepViewOnCopy()
  {
    return optionsMenu.optionKeepViewOnCopy.isSelected();
  }

  public boolean getMonitorFileChanges()
  {
//    return optionsMenu.optionMonitorFileChanges.isSelected();
    return false;
  }

  public boolean cacheOverride()
  {
    return optionsMenu.optionCacheOverride.isSelected();
  }

  public void gameLoaded(Profile.Game oldGame, String oldFile)
  {
    gameMenu.gameLoaded(oldGame, oldFile);
    fileMenu.gameLoaded();
    editMenu.gameLoaded();
    searchMenu.gameLoaded();
    optionsMenu.gameLoaded();
  }

  /** Returns state of "Text: Show Highlight Current Line" */
  public boolean getTextHighlightCurrentLine()
  {
    return optionsMenu.optionTextHightlightCurrent.isSelected();
  }

  /** Returns state of "Text: Show Line Numbers" */
  public boolean getTextLineNumbers()
  {
    return optionsMenu.optionTextLineNumbers.isSelected();
  }

  /** Returns state of "Text: Show Whitespace and Tab" */
  public boolean getTextWhitespaceVisible()
  {
    return optionsMenu.optionTextShowWhiteSpace.isSelected();
  }

  /** Returns state of "Text: Show End of Line" */
  public boolean getTextEOLVisible()
  {
    return optionsMenu.optionTextShowEOL.isSelected();
  }

  /** Returns the selected BCS color scheme. */
  public String getBcsColorScheme()
  {
    return optionsMenu.getBcsColorScheme();
  }

  /** Returns state of "BCS: Enable Syntax Highlighting" */
  public boolean getBcsSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionBCSEnableSyntax.isSelected();
  }

  /** Returns state of "BCS: Enable Code Folding" */
  public boolean getBcsCodeFoldingEnabled()
  {
    return optionsMenu.optionBCSEnableCodeFolding.isSelected();
  }

  /** Returns state of "BCS: Enable Automatic Indentation" */
  public boolean getBcsAutoIndentEnabled()
  {
    return optionsMenu.optionBCSEnableAutoIndent.isSelected();
  }

  /** Returns the selected GLSL color scheme. */
  public String getGlslColorScheme()
  {
    return optionsMenu.getGlslColorScheme();
  }

  /** Returns the selected LUA color scheme. */
  public String getLuaColorScheme()
  {
    return optionsMenu.getLuaColorScheme();
  }

  /** Returns the selected SQL color scheme. */
  public String getSqlColorScheme()
  {
    return optionsMenu.getSqlColorScheme();
  }

  /** Returns the selected TLK color scheme. */
  public String getTlkColorScheme()
  {
    return optionsMenu.getTlkColorScheme();
  }

  /** Returns the selected WeiDU.log color scheme. */
  public String getWeiDUColorScheme()
  {
    return optionsMenu.getWeiDUColorScheme();
  }

  /** Returns state of "Enable Syntax Highlighting for GLSL" */
  public boolean getGlslSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionGLSLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for LUA" */
  public boolean getLuaSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionLUAEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for SQL" */
  public boolean getSqlSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionSQLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for TLK" */
  public boolean getTlkSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionTLKEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for WeiDU.log" */
  public boolean getWeiDUSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionWeiDUEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Code Folding for GLSL" */
  public boolean getGlslCodeFoldingEnabled()
  {
    return optionsMenu.optionGLSLEnableCodeFolding.isSelected();
  }

  /** Returns whether to emulate tabs by inserting spaces instead. */
  public boolean isTextTabEmulated()
  {
    return optionsMenu.optionTextTabEmulate.isSelected();
  }

  /** Returns the number of spaces used for (real or emulated) tabs. */
  public int getTextTabSize()
  {
    return 1 << (optionsMenu.getTextIndentIndex()+1);
  }

  /** Returns the selected BCS indent. */
  public String getBcsIndent()
  {
    return optionsMenu.getBcsIndent();
  }

  public int getDefaultStructView()
  {
    return optionsMenu.getDefaultStructView();
  }

  public LookAndFeelInfo getLookAndFeel()
  {
    return optionsMenu.getLookAndFeel();
  }

  public int getGlobalFontSize()
  {
    return optionsMenu.getGlobalFontSize();
  }

  public int getOverrideMode()
  {
    return optionsMenu.getOverrideMode();
  }

  public int getResRefMode()
  {
    return optionsMenu.getResRefMode();
  }

  public Font getScriptFont()
  {
    for (int i = 0; i < OptionsMenu.FONTS.length; i++)
      if (optionsMenu.selectFont[i].isSelected())
        return OptionsMenu.FONTS[i];
    return OptionsMenu.FONTS[0];
  }

  public String getSelectedCharset()
  {
    return optionsMenu.charsetName(optionsMenu.getSelectedButtonData(), true);
  }

  public boolean backupOnSave()
  {
    return optionsMenu.optionBackupOnSave.isSelected();
  }

  public boolean ignoreOverrides()
  {
    return optionsMenu.optionIgnoreOverride.isSelected();
  }

  public boolean ignoreReadErrors()
  {
    return optionsMenu.optionIgnoreReadErrors.isSelected();
  }

  public boolean showUnknownResourceTypes()
  {
    return optionsMenu.optionShowUnknownResources.isSelected();
  }

  public void resourceEntrySelected(ResourceEntry entry)
  {
    fileMenu.resourceEntrySelected(entry);
  }

  public void resourceShown(Resource res)
  {
    fileMenu.resourceShown(res);
  }

  public boolean showOffsets()
  {
    return optionsMenu.optionShowOffset.isSelected();
  }

  /**
   * Returns the language code of the selected game language for Enhanced Edition games.
   * Returns an empty string if autodetect is selected or game is not part of the Enhanced Edition.
   */
  public String getSelectedGameLanguage()
  {
    return optionsMenu.getSelectedGameLanguage();
  }

  /**
   * Attempts to find a matching bookmark and returns its name.
   * @param keyFile The path to the game's chitin.key.
   * @return The bookmark name of a matching game or {@code null} otherwise.
   */
  public String getBookmarkName(Path keyFile)
  {
    Bookmark bookmark = gameMenu.getBookmarkOf(keyFile);
    return (bookmark != null) ? bookmark.getName() : null;
  }

  public void storePreferences()
  {
    optionsMenu.storePreferences();
    gameMenu.storePreferences();
  }


// -------------------------- INNER CLASSES --------------------------

  ///////////////////////////////
  // Game Menu
  ///////////////////////////////
  private static final class GameMenu extends JMenu implements ActionListener
  {
    private final JMenuItem gameOpenFile, gameOpenGame, gameRefresh, gameExit, gameCloseTLK,
                            gameProperties, gameBookmarkAdd, gameBookmarkEdit, gameRecentClear;

    private final JMenu gameRecent = new JMenu("Recently opened games");
    private final List<RecentGame> recentList = new ArrayList<RecentGame>();
    private final JPopupMenu.Separator gameRecentSeparator = new JPopupMenu.Separator();

    private final JMenu gameBookmarks = new JMenu("Bookmarked games");
    private final List<Bookmark> bookmarkList = new ArrayList<Bookmark>();
    private final JPopupMenu.Separator gameBookmarkSeparator = new JPopupMenu.Separator();

    private GameMenu()
    {
      super("Game");
      setMnemonic(KeyEvent.VK_G);

      gameOpenFile = makeMenuItem("Open File...", KeyEvent.VK_F, Icons.getIcon(Icons.ICON_OPEN_16),
                                  KeyEvent.VK_I, this);
      add(gameOpenFile);
      gameOpenGame = makeMenuItem("Open Game...", KeyEvent.VK_O, Icons.getIcon(Icons.ICON_OPEN_16),
                                  KeyEvent.VK_O, NearInfinity.getInstance());
      gameOpenGame.setActionCommand("Open");
      add(gameOpenGame);
      gameRefresh = makeMenuItem("Refresh Tree", KeyEvent.VK_R, Icons.getIcon(Icons.ICON_REFRESH_16),
                                 -1, NearInfinity.getInstance());
      gameRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
      gameRefresh.setActionCommand("Refresh");
      add(gameRefresh);
      gameCloseTLK = makeMenuItem("Release Dialog.tlk Lock", KeyEvent.VK_D, Icons.getIcon(Icons.ICON_RELEASE_16),
                                  -1, this);
      add(gameCloseTLK);

      gameProperties = makeMenuItem("Game Properties...", KeyEvent.VK_P, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      add(gameProperties);

      addSeparator();

      // adding bookmarked games list
      gameBookmarks.setMnemonic('b');
      add(gameBookmarks);

      bookmarkList.clear();
      int gameCount = getPrefsProfiles().getInt(Bookmark.getEntryCountKey(), 0);
      for (int i = 0; i < gameCount; i++) {
        Profile.Game game = Profile.gameFromString(getPrefsProfiles().get(Bookmark.getGameKey(i),
                                                                          Profile.Game.Unknown.toString()));
        String gamePath = getPrefsProfiles().get(Bookmark.getPathKey(i), null);
        String gameName = getPrefsProfiles().get(Bookmark.getNameKey(i), null);
        try {
          Bookmark b = new Bookmark(gameName, game, gamePath, this);
          addBookmarkedGame(bookmarkList.size(), b);
        } catch (NullPointerException e) {
          // skipping entry
        }
      }

      gameBookmarks.add(gameBookmarkSeparator);

      gameBookmarkAdd = new JMenuItem("Add current game...");
      gameBookmarkAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, CTRL_MASK | InputEvent.ALT_DOWN_MASK));
      gameBookmarkAdd.addActionListener(this);
      gameBookmarks.add(gameBookmarkAdd);

      gameBookmarkEdit = new JMenuItem("Edit bookmarks...");
      gameBookmarkEdit.addActionListener(this);
      gameBookmarks.add(gameBookmarkEdit);

      gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
      gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());

      // adding recently opened games list
      gameRecent.setMnemonic('r');
      add(gameRecent);

      recentList.clear();
      for (int i = 0; i < RecentGame.getEntryCount(); i++) {
        Profile.Game game = Profile.gameFromString(getPrefsProfiles().get(RecentGame.getGameKey(i),
                                                                          Profile.Game.Unknown.toString()));
        String gamePath = getPrefsProfiles().get(RecentGame.getPathKey(i), null);
        try {
          RecentGame rg = new RecentGame(game, gamePath, recentList.size(), this);
          addLastGame(recentList.size(), rg);
        } catch (NullPointerException e) {
          // skipping entry
        }
      }

      gameRecent.add(gameRecentSeparator);

      gameRecentClear = new JMenuItem("Clear list of recent games");
      gameRecentClear.addActionListener(this);
      gameRecent.add(gameRecentClear);

      gameRecent.setEnabled(!recentList.isEmpty());
      gameRecentSeparator.setVisible(!recentList.isEmpty());

      addSeparator();

      gameExit = makeMenuItem("Quit", KeyEvent.VK_Q, Icons.getIcon(Icons.ICON_EXIT_16), KeyEvent.VK_Q,
                              NearInfinity.getInstance());
      gameExit.setActionCommand("Exit");
      add(gameExit);
    }

    private void gameLoaded(Profile.Game oldGame, String oldFile)
    {
      // updating "Recently opened games" list
      for (int i = 0; i < recentList.size(); i++) {
        if (ResourceFactory.getKeyfile().toString().equalsIgnoreCase(recentList.get(i).getPath())) {
          removeLastGame(i);
          i--;
        }
      }

      if (oldGame != null && oldGame != Profile.Game.Unknown) {
        for (int i = 0; i < recentList.size(); i++) {
          if (oldFile.equalsIgnoreCase(recentList.get(i).getPath())) {
            removeLastGame(i);
            i--;
          }
        }
        addLastGame(0, new RecentGame(oldGame, oldFile, 0, this));
      }

      while (recentList.size() > RecentGame.getEntryCount()) {
        removeLastGame(recentList.size() - 1);
      }
    }

    // Updates list of bookmark menu items
    private void updateBookmarkedGames()
    {
      // 1. remove old bookmark items from menu
      while (gameBookmarks.getPopupMenu().getComponentCount() > 0) {
        if (gameBookmarks.getPopupMenu().getComponent(0) != gameBookmarkSeparator) {
          gameBookmarks.getPopupMenu().remove(0);
        } else {
          break;
        }
      }

      // 2. add new bookmark items to menu
      for (int i = 0, size = bookmarkList.size(); i < size; i++) {
        gameBookmarks.insert(bookmarkList.get(i).getMenuItem(), i);
      }
      gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
      gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());

      // Updating current game if needed
      Bookmark bookmark = getBookmarkOf(Profile.getChitinKey());
      if (bookmark != null) {
        Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, bookmark.getName());
        NearInfinity.getInstance().updateWindowTitle();
      }
    }

    // Removes the bookmark specified by item index from the list and associated menu
    private void removeBookmarkedGame(int idx)
    {
      if (idx >= 0 && idx < bookmarkList.size()) {
        Bookmark b = bookmarkList.remove(idx);
        if (b != null) {
          b.setActionListener(null);
        }
        if (gameBookmarks.getPopupMenu().getComponent(idx) == b.getMenuItem()) {
          gameBookmarks.getPopupMenu().remove(idx);
        } else {
          for (int i = 0, count = gameBookmarks.getPopupMenu().getComponentCount(); i < count; i++) {
            if (gameBookmarks.getPopupMenu().getComponent(i) == b.getMenuItem()) {
              gameBookmarks.getPopupMenu().remove(i);
              break;
            }
          }
        }
        Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, null);
        NearInfinity.getInstance().updateWindowTitle();
      }
    }

    // Adds the specified bookmark to the list and associated menu
    private void addBookmarkedGame(int idx, Bookmark bookmark)
    {
      if (idx < 0) {
        idx = 0;
      } else if (idx > bookmarkList.size()) {
        idx = bookmarkList.size();
      }

      // use either separator item or menu item count as upper bounds for inserting new bookmark items
      int separatorIdx = gameBookmarks.getPopupMenu().getComponentIndex(gameBookmarkSeparator);
      if (separatorIdx < 0) {
        separatorIdx = gameBookmarks.getPopupMenu().getComponentCount();
      }

      if (bookmark != null && idx <= separatorIdx) {
        bookmarkList.add(idx, bookmark);
        gameBookmarks.insert(bookmark.getMenuItem(), idx);
        gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
        gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());
        Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, bookmark.getName());
        NearInfinity.getInstance().updateWindowTitle();
      }
    }

    // Adds or replaces the current game to the bookmark section
    private void addNewBookmark(String name)
    {
      if (name != null) {
        name = name.trim();
        if (name.isEmpty()) {
          name = Profile.getProperty(Profile.Key.GET_GAME_TITLE);
        }
        Profile.Game game = Profile.getGame();
        String path = Profile.getChitinKey().toAbsolutePath().toString();
        Bookmark b = new Bookmark(name, game, path, this);

        // check whether to replace existing bookmark
        Bookmark curBookmark = getBookmarkOf(Profile.getChitinKey());
        int idx = (curBookmark != null) ? bookmarkList.indexOf(curBookmark) : -1;
        if (idx >= 0) {
          // replace existing bookmark
          removeBookmarkedGame(idx);
          addBookmarkedGame(idx, b);
        } else {
          // add new bookmark
          addBookmarkedGame(bookmarkList.size(), b);
        }
      } else {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No name specified.",
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }

    // Adds the specified last game entry to the list
    private void addLastGame(int idx, RecentGame rg)
    {
      if (rg != null) {
        if (idx < 0 || idx > recentList.size()) {
          idx = recentList.size();
        }
        rg.setIndex(idx);
        recentList.add(idx, rg);
        gameRecent.insert(rg.getMenuItem(), idx);
        gameRecent.setEnabled(!recentList.isEmpty());
        gameRecentSeparator.setVisible(!recentList.isEmpty());

        for (int i = 0; i < recentList.size(); i++) {
          recentList.get(i).setIndex(i);
        }
      }
    }

    // Removes the specified last game entry from the list
    private void removeLastGame(int idx)
    {
      if (idx >= 0 && idx < recentList.size()) {
        recentList.get(idx).clear();
        recentList.remove(idx);
        gameRecent.setEnabled(!recentList.isEmpty());
        gameRecentSeparator.setVisible(!recentList.isEmpty());

        for (int i = 0; i < recentList.size(); i++) {
          recentList.get(i).setIndex(i);
        }
      }
    }

    private void storePreferences()
    {
      // storing bookmarks
      // 1. removing excess bookmark entries from preferences
      int oldSize = getPrefsProfiles().getInt(Bookmark.getEntryCountKey(), 0);
      if (oldSize > bookmarkList.size()) {
        for (int i = bookmarkList.size(); i < oldSize; i++) {
          getPrefsProfiles().remove(Bookmark.getNameKey(i));
          getPrefsProfiles().remove(Bookmark.getPathKey(i));
          getPrefsProfiles().remove(Bookmark.getGameKey(i));
        }
      }
      // 2. storing bookmarks in preferences
      getPrefsProfiles().putInt(Bookmark.getEntryCountKey(), bookmarkList.size());
      for (int i = 0; i < bookmarkList.size(); i++) {
        Bookmark bookmark = bookmarkList.get(i);
        getPrefsProfiles().put(Bookmark.getNameKey(i), bookmark.getName());
        getPrefsProfiles().put(Bookmark.getPathKey(i), bookmark.getPath());
        getPrefsProfiles().put(Bookmark.getGameKey(i), bookmark.getGame().toString());
      }

      // storing recently used games
      for (int i = 0; i < RecentGame.getEntryCount(); i++) {
        if (i < recentList.size()) {
          RecentGame rg = recentList.get(i);
          getPrefsProfiles().put(RecentGame.getGameKey(i), rg.getGame().toString());
          getPrefsProfiles().put(RecentGame.getPathKey(i), rg.getPath());
        } else {
          getPrefsProfiles().remove(RecentGame.getGameKey(i));
          getPrefsProfiles().remove(RecentGame.getPathKey(i));
        }
      }
    }

    /** Attempts to find a bookmarked game using specified key file path. */
    public Bookmark getBookmarkOf(Path keyFile)
    {
      if (keyFile != null) {
        String path = keyFile.toAbsolutePath().toString();
        for (Iterator<Bookmark> iter = bookmarkList.iterator(); iter.hasNext();) {
          Bookmark bookmark = iter.next();
          if (bookmark.getPath().equalsIgnoreCase(path)) {
            return bookmark;
          }
        }
      }
      return null;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == gameOpenFile) {
        OpenFileFrame openframe = (OpenFileFrame)ChildFrame.getFirstFrame(OpenFileFrame.class);
        if (openframe == null) {
          openframe = new OpenFileFrame();
        }
        openframe.setVisible(true);
      } else if (event.getActionCommand().equals(Bookmark.getCommand())) {
        // Bookmark item selected
        int selected = -1;
        for (int i = 0; i < bookmarkList.size(); i++) {
          if (event.getSource() == bookmarkList.get(i).getMenuItem()) {
            selected = i;
            break;
          }
        }
        if (selected != -1) {
          Path keyFile = FileManager.resolve(bookmarkList.get(selected).getPath());
          if (!Files.isRegularFile(keyFile)) {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          bookmarkList.get(selected).getPath() + " could not be found",
                                          "Open game failed", JOptionPane.ERROR_MESSAGE);
          } else {
            NearInfinity.getInstance().openGame(keyFile);
          }
        }
      } else if (event.getActionCommand().equals(RecentGame.getCommand())) {
        // Recently opened game item selected
        int selected = -1;
        for (int i = 0; i < recentList.size(); i++) {
          if (event.getSource() == recentList.get(i).getMenuItem()) {
            selected = i;
            break;
          }
        }
        if (selected != -1) {
          Path keyFile = FileManager.resolve(recentList.get(selected).getPath());
          if (!Files.isRegularFile(keyFile)) {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          recentList.get(selected).getPath() + " could not be found",
                                          "Open game failed", JOptionPane.ERROR_MESSAGE);
          } else {
            NearInfinity.getInstance().openGame(keyFile);
          }
        }
      } else if (event.getSource() == gameCloseTLK) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Read lock released",
                                      "Release Dialog.tlk", JOptionPane.INFORMATION_MESSAGE);
      } else if (event.getSource() == gameProperties) {
        new GameProperties(NearInfinity.getInstance());
      } else if (event.getSource() == gameBookmarkAdd) {
        Object name = null;
        Bookmark bookmark = getBookmarkOf(Profile.getChitinKey());
        if (bookmark != null) {
          int retVal = JOptionPane.showConfirmDialog(NearInfinity.getInstance(),
                                                     "The game has already been bookmarked.\nDo you want to update it?",
                                                     "Update bookmark", JOptionPane.YES_NO_OPTION,
                                                     JOptionPane.QUESTION_MESSAGE);
          if (retVal == JOptionPane.YES_OPTION) {
            name = bookmark.getName();
          } else {
            return;
          }
        }
        name = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter bookmark name:",
                                           "Add game to bookmarks", JOptionPane.QUESTION_MESSAGE,
                                           null, null, name);
        if (name != null) {
          addNewBookmark(name.toString());
        }
      } else if (event.getSource() == gameBookmarkEdit) {
        List<Bookmark> list = BookmarkEditor.editBookmarks(bookmarkList);
        if (list != null) {
          bookmarkList.clear();
          bookmarkList.addAll(list);
          updateBookmarkedGames();
        }
      } else if (event.getSource() == gameRecentClear) {
        while (!recentList.isEmpty()) {
          removeLastGame(0);
        }
      }
    }
  }

  ///////////////////////////////
  // File Menu
  ///////////////////////////////

  private static final class FileMenu extends JMenu implements ActionListener
  {
    private static final class ResInfo {
      public final String label;
      public final StructureFactory.ResType resId;
      private final EnumSet<Profile.Game> supportedGames = EnumSet.noneOf(Profile.Game.class);

      public ResInfo(StructureFactory.ResType id, String text) {
        this(id, text, new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC, Profile.Game.PST,
                                          Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM,
                                          Profile.Game.IWD2, Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                          Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                          Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET});
      }

      public ResInfo(StructureFactory.ResType id, String text, Profile.Game[] games) {
        resId = id;
        label = text;
        if (games != null)
          Collections.addAll(supportedGames, games);
      }

      public boolean gameSupported(Profile.Game game)
      {
        return supportedGames.contains(game);
      }
    }

    private static final ResInfo RESOURCE[] = {
      new ResInfo(StructureFactory.ResType.RES_2DA, "2DA"),
      new ResInfo(StructureFactory.ResType.RES_ARE, "ARE"),
      new ResInfo(StructureFactory.ResType.RES_BAF, "BAF"),
      new ResInfo(StructureFactory.ResType.RES_BCS, "BCS"),
      new ResInfo(StructureFactory.ResType.RES_BIO, "BIO",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_CHR, "CHR",
                  new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC,
                                     Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM,
                                     Profile.Game.IWD2, Profile.Game.BG1EE, Profile.Game.BG1SoD,
                                     Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_CRE, "CRE"),
      new ResInfo(StructureFactory.ResType.RES_EFF, "EFF",
                  new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC,
                                     Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_IDS, "IDS"),
      new ResInfo(StructureFactory.ResType.RES_ITM, "ITM"),
      new ResInfo(StructureFactory.ResType.RES_INI, "INI",
                  new Profile.Game[]{Profile.Game.PST, Profile.Game.IWD, Profile.Game.IWDHoW,
                                     Profile.Game.IWDHowTotLM, Profile.Game.IWD2,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_PRO, "PRO",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_RES, "RES",
                  new Profile.Game[]{Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM,
                                     Profile.Game.IWD2}),
      new ResInfo(StructureFactory.ResType.RES_SPL, "SPL"),
      new ResInfo(StructureFactory.ResType.RES_SRC, "SRC",
                  new Profile.Game[]{Profile.Game.PST, Profile.Game.IWD2, Profile.Game.PSTEE}),
      new ResInfo(StructureFactory.ResType.RES_STO, "STO"),
      new ResInfo(StructureFactory.ResType.RES_VEF, "VEF",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_VVC, "VVC",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_WED, "WED"),
      new ResInfo(StructureFactory.ResType.RES_WFX, "WFX",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET}),
      new ResInfo(StructureFactory.ResType.RES_WMAP, "WMAP"),
    };

    private final JMenu newFileMenu;
    private final JMenuItem fileOpenNew, fileExport, fileAddCopy, fileRename, fileDelete, fileRestore;

    private FileMenu()
    {
      super("File");
      setMnemonic(KeyEvent.VK_F);

      newFileMenu = new JMenu("New Resource");
      newFileMenu.setIcon(Icons.getIcon(Icons.ICON_NEW_16));
      newFileMenu.setMnemonic(KeyEvent.VK_N);
      add(newFileMenu);
      fileOpenNew = makeMenuItem("Open in New Window", KeyEvent.VK_W, Icons.getIcon(Icons.ICON_OPEN_16), -1, this);
      fileOpenNew.setEnabled(false);
      add(fileOpenNew);
      fileExport = makeMenuItem("Export...", KeyEvent.VK_E, Icons.getIcon(Icons.ICON_EXPORT_16), -1, this);
      fileExport.setEnabled(false);
      add(fileExport);
      fileAddCopy = makeMenuItem("Add Copy Of...", KeyEvent.VK_A, Icons.getIcon(Icons.ICON_ADD_16), -1, this);
      fileAddCopy.setEnabled(false);
      add(fileAddCopy);
      fileRename = makeMenuItem("Rename...", KeyEvent.VK_R, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      fileRename.setEnabled(false);
      add(fileRename);
      fileDelete = makeMenuItem("Delete", KeyEvent.VK_D, Icons.getIcon(Icons.ICON_DELETE_16), -1, this);
      fileDelete.setEnabled(false);
      add(fileDelete);
      fileRestore = makeMenuItem("Restore backup", KeyEvent.VK_B, Icons.getIcon(Icons.ICON_UNDO_16), -1, this);
      fileRestore.setEnabled(false);
      add(fileRestore);
    }

    private void gameLoaded()
    {
      if (newFileMenu != null) {
        newFileMenu.removeAll();

        for (final ResInfo res : RESOURCE) {
          if (res.gameSupported(Profile.getGame())) {
            JMenuItem newFile = new JMenuItem(res.label);
            newFile.addActionListener(this);
            newFile.setActionCommand(res.label);
            newFile.setEnabled(true);
            newFileMenu.add(newFile);
          }
        }
        newFileMenu.setEnabled(newFileMenu.getItemCount() > 0);
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == fileOpenNew) {
        Resource res = ResourceFactory.getResource(
                NearInfinity.getInstance().getResourceTree().getSelected());
        if (res != null)
          new ViewFrame(NearInfinity.getInstance(), res);
      } else if (event.getSource() == fileExport) {
        ResourceFactory.exportResource(NearInfinity.getInstance().getResourceTree().getSelected(),
                                       NearInfinity.getInstance());
      } else if (event.getSource() == fileAddCopy) {
        ResourceFactory.saveCopyOfResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else if (event.getSource() == fileRename) {
        if (NearInfinity.getInstance().getResourceTree().getSelected() instanceof FileResourceEntry) {
          ResourceTree.renameResource((FileResourceEntry)NearInfinity.getInstance().getResourceTree().getSelected());
        }
      } else if (event.getSource() == fileDelete) {
        ResourceTree.deleteResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else if (event.getSource() == fileRestore) {
        ResourceTree.restoreResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else {
        for (final ResInfo res : RESOURCE) {
          if (event.getActionCommand().equals(res.label)) {
            StructureFactory.getInstance().newResource(res.resId, NearInfinity.getInstance());
          }
        }
      }
    }

    private void resourceEntrySelected(ResourceEntry entry)
    {
      fileOpenNew.setEnabled(entry != null);
      fileExport.setEnabled(entry != null);
      fileAddCopy.setEnabled(entry != null);
      fileRename.setEnabled(entry instanceof FileResourceEntry);
      fileDelete.setEnabled((entry != null && entry.hasOverride()) || entry instanceof FileResourceEntry);
      fileRestore.setEnabled(ResourceTree.isBackupAvailable(entry));
    }

    private void resourceShown(Resource res)
    {
      // not used anymore
    }
  }

  ///////////////////////////////
  // Edit Menu
  ///////////////////////////////

  private static final class EditMenu extends JMenu implements ActionListener
  {
    private final JMenuItem editString, editBIFF, editVarVar, editIni, editWeiDU, editWeiDUBGEE;

    private EditMenu()
    {
      super("Edit");
      setMnemonic(KeyEvent.VK_E);

      editString =
          makeMenuItem("String table", KeyEvent.VK_S, Icons.getIcon(Icons.ICON_EDIT_16), KeyEvent.VK_S, this);
      add(editString);
      editIni = makeMenuItem("baldur.ini", KeyEvent.VK_I, Icons.getIcon(Icons.ICON_EDIT_16), -1, NearInfinity.getInstance());
      editIni.setActionCommand("GameIni");
      add(editIni);
      editWeiDU = makeMenuItem("WeiDU.log", KeyEvent.VK_W, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      add(editWeiDU);
      editWeiDUBGEE = makeMenuItem("WeiDU-BGEE.log", -1, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      editWeiDUBGEE.setVisible(false);
      add(editWeiDUBGEE);
      editVarVar = makeMenuItem("Var.var", KeyEvent.VK_V, Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16), -1, this);
      add(editVarVar);
      // TODO: reactive when fixed
      editBIFF = makeMenuItem("BIFF", KeyEvent.VK_B, Icons.getIcon(Icons.ICON_EDIT_16), KeyEvent.VK_E, this);
      editBIFF.setToolTipText("Temporarily disabled");
      editBIFF.setEnabled(false);
      add(editBIFF);
    }

    private void gameLoaded()
    {
      Path iniFile = Profile.getProperty(Profile.Key.GET_GAME_INI_FILE);
      if (iniFile != null && Files.isRegularFile(iniFile)) {
        editIni.setText(iniFile.getFileName().toString());
        editIni.setEnabled(true);
        editIni.setToolTipText("Edit " + iniFile.toString());
      } else {
        editIni.setText("baldur.ini");
        editIni.setEnabled(false);
        editIni.setToolTipText("Ini file not available");
      }

      Path weiduFile = FileManager.query(Profile.getRootFolders(), "WeiDU.log");
      editWeiDU.setEnabled(weiduFile != null && Files.isRegularFile(weiduFile));
      editWeiDUBGEE.setVisible(Profile.getGame() == Profile.Game.EET);
      if (editWeiDUBGEE.isVisible()) {
        weiduFile = FileManager.query(Profile.getRootFolders(), "WeiDU-BGEE.log");
        editWeiDUBGEE.setEnabled(weiduFile != null && Files.isRegularFile(weiduFile));
      }

      Path varFile = FileManager.query(Profile.getRootFolders(), "VAR.VAR");
      editVarVar.setEnabled(varFile != null && Files.isRegularFile(varFile));
      if (editVarVar.isEnabled()) {
        editVarVar.setToolTipText("");
      } else {
        editVarVar.setToolTipText("Only available for Planescape: Torment");
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == editString) {
        StringEditor editor = null;
        List<ChildFrame> frames = ChildFrame.getFrames(StringEditor.class);
        for (int i = 0; i < frames.size(); i++) {
          editor = (StringEditor)frames.get(i);
        }
        if (editor == null) {
          new StringEditor();
        } else {
          editor.setVisible(true);
        }
      }
      else if (event.getSource() == editWeiDU) {
        showTextFile(NearInfinity.getInstance(), "WeiDU.log");
      }
      else if (event.getSource() == editWeiDUBGEE) {
        showTextFile(NearInfinity.getInstance(), "WeiDU-BGEE.log");
      }
      else if (event.getSource() == editVarVar) {
        new ViewFrame(NearInfinity.getInstance(),
                      ResourceFactory.getResource(
                              new FileResourceEntry(
                                  FileManager.queryExisting(Profile.getRootFolders(), "VAR.VAR"))));
      }
      else if (event.getSource() == editBIFF) {
//        new BIFFEditor();
      }
    }

    // Opens given file in text editor if file is found in the game's root folder
    private void showTextFile(Component parent, String fileName)
    {
      Path logFile = FileManager.query(Profile.getRootFolders(), fileName);
      try {
        if (logFile != null && Files.isRegularFile(logFile)) {
          new ViewFrame(parent, new PlainTextResource(new FileResourceEntry(logFile)));
        } else {
          throw new Exception();
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(parent, "Cannot open " + fileName + ".",
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  ///////////////////////////////
  // Search Menu
  ///////////////////////////////

  private static final class SearchMenu extends JMenu implements ActionListener
  {
    private final String TEXTSEARCH[] = {"2DA", "BCS", "DLG", "IDS", "INI", "LUA"};
    private final JMenuItem searchString, searchFile, searchResource;
    private final JMenu textSearchMenu;

    private SearchMenu()
    {
      super("Search");
      setMnemonic(KeyEvent.VK_S);

      searchString =
          makeMenuItem("StringRef...", KeyEvent.VK_S, Icons.getIcon(Icons.ICON_FIND_16), KeyEvent.VK_L, this);
      add(searchString);
      searchFile =
          makeMenuItem("CRE/ITM/SPL/STO...", KeyEvent.VK_C, Icons.getIcon(Icons.ICON_FIND_16), KeyEvent.VK_F, this);
      add(searchFile);
      searchResource =
          makeMenuItem("Extended search...", KeyEvent.VK_X, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      searchResource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
          Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
      add(searchResource);

      textSearchMenu = new JMenu("Text Search");
      textSearchMenu.setIcon(Icons.getIcon(Icons.ICON_EDIT_16));
      for (final String type : TEXTSEARCH) {
        JMenuItem textSearch = new JMenuItem(type);
        textSearch.addActionListener(this);
        textSearch.setActionCommand(type);
        textSearchMenu.add(textSearch);
      }
      add(textSearchMenu);
    }

    private void gameLoaded()
    {
      // Enable INI search only if the game is supporting it
      for (int i = 0, count = textSearchMenu.getMenuComponentCount(); i < count; i++) {
        if (textSearchMenu.getMenuComponent(i) instanceof JMenuItem) {
          JMenuItem mi = (JMenuItem)textSearchMenu.getMenuComponent(i);
          if ("INI".equals(mi.getText())) {
            mi.setEnabled((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_INI));
          } else if ("LUA".equals(mi.getText())) {
            mi.setEnabled((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_LUA));
          }
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == searchString) {
        StringLookup lookup = (StringLookup)ChildFrame.getFirstFrame(StringLookup.class);
        if (lookup == null)
          lookup = new StringLookup();
        lookup.setVisible(true);
      }
      else if (event.getSource() == searchFile) {
        SearchFrame search = (SearchFrame)ChildFrame.getFirstFrame(SearchFrame.class);
        if (search == null)
          search = new SearchFrame();
        search.setVisible(true);
      }
      else if (event.getSource() == searchResource) {
        SearchResource resource = (SearchResource)ChildFrame.getFirstFrame(SearchResource.class);
        if (resource == null) {
          resource = new SearchResource();
        }
        resource.setVisible(true);
      }
      else {
        for (final String type : TEXTSEARCH) {
          if (event.getActionCommand().equals(type)) {
            if (event.getActionCommand().equals("DLG")) {
              new DialogSearcher(ResourceFactory.getResources(type),
                                 getTopLevelAncestor());
            } else {
              new TextResourceSearcher(ResourceFactory.getResources(type),
                                       getTopLevelAncestor());
            }
            return;
          }
        }
      }
    }
  }

  ///////////////////////////////
  // Tools Menu
  ///////////////////////////////

  private static final class ToolsMenu extends JMenu implements ActionListener
  {
    private final JMenuItem toolInfinityAmp, toolCleanKeyfile, toolCheckAllDialog, toolCheckOverrideDialog;
    private final JMenuItem toolCheckResRef, toolIDSBrowser, toolDropZone, toolCheckCREInv;
    private final JMenuItem toolCheckIDSRef, toolCheckIDSBCSRef, toolCheckScripts, toolCheckStructs;
    private final JMenuItem toolCheckStringUse, toolCheckStringIndex, toolCheckFileUse, toolMassExport;
    private final JMenuItem toolCheckEffectsIndex;
    private final JMenuItem toolConvImageToBam, toolConvImageToBmp, toolConvImageToMos, toolConvImageToTis,
                            toolConvImageToPvrz;
    private final JCheckBoxMenuItem toolConsole, toolClipBoard;

    private ToolsMenu()
    {
      super("Tools");
      setMnemonic(KeyEvent.VK_T);

      toolInfinityAmp = makeMenuItem("InfinityAmp", KeyEvent.VK_I, Icons.getIcon(Icons.ICON_VOLUME_16), -1, this);
      add(toolInfinityAmp);

      addSeparator();

      // TODO: reactivate when fixed
      toolCleanKeyfile =
          makeMenuItem("Keyfile Cleanup", KeyEvent.VK_K, Icons.getIcon(Icons.ICON_REFRESH_16), -1, this);
      toolCleanKeyfile.setToolTipText("Temporarily disabled");
      toolCleanKeyfile.setEnabled(false);
      add(toolCleanKeyfile);

      addSeparator();

      // *** Begin Check submenu ***
      JMenu checkMenu = new JMenu("Check");
      checkMenu.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
      checkMenu.setMnemonic('c');
      add(checkMenu);

      JMenu checkSubMenu = new JMenu("Triggers & Actions For");
      checkSubMenu.setIcon(Icons.getIcon(Icons.ICON_REFRESH_16));
      toolCheckAllDialog = new JMenuItem("All Dialogues");
      toolCheckAllDialog.addActionListener(this);
      checkSubMenu.add(toolCheckAllDialog);
      toolCheckOverrideDialog = new JMenuItem("Override Dialogues Only");
      toolCheckOverrideDialog.addActionListener(this);
      checkSubMenu.add(toolCheckOverrideDialog);
      checkMenu.add(checkSubMenu);

      toolCheckScripts =
          makeMenuItem("Scripts", KeyEvent.VK_S, Icons.getIcon(Icons.ICON_REFRESH_16), -1, this);
      checkMenu.add(toolCheckScripts);

      toolCheckCREInv =
          makeMenuItem("For CRE Items Not in Inventory", KeyEvent.VK_C, Icons.getIcon(Icons.ICON_REFRESH_16),
                       -1, this);
      toolCheckCREInv.setToolTipText("Reports items present in the file but not in the inventory");
      checkMenu.add(toolCheckCREInv);

      toolCheckResRef =
          makeMenuItem("For Illegal ResourceRefs...", KeyEvent.VK_R, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      toolCheckResRef.setToolTipText("Reports resource references pointing to nonexistent files");
      checkMenu.add(toolCheckResRef);

      JMenu findMenu = new JMenu("For Unknown IDS References In");
      findMenu.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
      toolCheckIDSBCSRef = new JMenuItem("BCS & BS Files");
      toolCheckIDSBCSRef.addActionListener(this);
      findMenu.add(toolCheckIDSBCSRef);
      toolCheckIDSRef = new JMenuItem("Other Files...");
      toolCheckIDSRef.addActionListener(this);
      findMenu.add(toolCheckIDSRef);
      checkMenu.add(findMenu);
      findMenu.setToolTipText("Reports IDS references to unknown IDS values");
      toolCheckIDSBCSRef.setToolTipText("Note: GTimes, Time, Scroll, ShoutIDs, and Specific are ignored");
      toolCheckIDSRef.setToolTipText("Note: \"0\" references are ignored");

      toolCheckStructs =
          makeMenuItem("For Corrupted Files...", KeyEvent.VK_F, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      toolCheckStructs.setToolTipText("Reports structured files with partially overlapping subsections or resource-specific corruptions");
      checkMenu.add(toolCheckStructs);

      toolCheckStringUse =
          makeMenuItem("For Unused Strings", KeyEvent.VK_U, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      checkMenu.add(toolCheckStringUse);

      toolCheckStringIndex =
          makeMenuItem("For Illegal Strrefs...", KeyEvent.VK_S, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      toolCheckStringIndex.setToolTipText("Reports resources with out-of-range string references");
      checkMenu.add(toolCheckStringIndex);

      toolCheckFileUse = makeMenuItem("For Unused Files...", -1, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      checkMenu.add(toolCheckFileUse);

      toolCheckEffectsIndex =
          makeMenuItem("For Mis-indexed Effects", -1, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      checkMenu.add(toolCheckEffectsIndex);
      // *** End Check submenu ***

      // *** Begin Convert submenu ***
      JMenu convertMenu = new JMenu("Convert");
      convertMenu.setIcon(Icons.getIcon(Icons.ICON_APPLICATION_16));
      convertMenu.setMnemonic('v');
      add(convertMenu);

      toolConvImageToBam =
          makeMenuItem("BAM Converter...", KeyEvent.VK_B, Icons.getIcon(Icons.ICON_APPLICATION_16), -1, this);
      convertMenu.add(toolConvImageToBam);

      toolConvImageToBmp =
          makeMenuItem("Image to BMP...", KeyEvent.VK_I, Icons.getIcon(Icons.ICON_APPLICATION_16), -1, this);
      convertMenu.add(toolConvImageToBmp);

      toolConvImageToMos =
          makeMenuItem("Image to MOS...", KeyEvent.VK_M, Icons.getIcon(Icons.ICON_APPLICATION_16), -1, this);
      convertMenu.add(toolConvImageToMos);

      toolConvImageToPvrz =
          makeMenuItem("Image to PVRZ...", KeyEvent.VK_P, Icons.getIcon(Icons.ICON_APPLICATION_16), -1, this);
      convertMenu.add(toolConvImageToPvrz);

      toolConvImageToTis =
          makeMenuItem("Image to TIS...", KeyEvent.VK_T, Icons.getIcon(Icons.ICON_APPLICATION_16), -1, this);
      convertMenu.add(toolConvImageToTis);
      // *** End Convert submenu ***

      addSeparator();

      toolIDSBrowser =
          makeMenuItem("IDS Browser", KeyEvent.VK_B, Icons.getIcon(Icons.ICON_HISTORY_16), KeyEvent.VK_B, this);
      add(toolIDSBrowser);
      toolDropZone =
          makeMenuItem("Script Drop Zone", KeyEvent.VK_Z, Icons.getIcon(Icons.ICON_HISTORY_16), KeyEvent.VK_Z, this);
      add(toolDropZone);

      addSeparator();

      toolMassExport =
          makeMenuItem("Mass Export...", KeyEvent.VK_M, Icons.getIcon(Icons.ICON_EXPORT_16), -1, this);
      add(toolMassExport);

      addSeparator();

      toolClipBoard = new JCheckBoxMenuItem("Show Clipboard", Icons.getIcon(Icons.ICON_PASTE_16));
      toolClipBoard.addActionListener(this);
      add(toolClipBoard);
      toolConsole = new JCheckBoxMenuItem("Show Debug Console", Icons.getIcon(Icons.ICON_PROPERTIES_16));
      toolConsole.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, CTRL_MASK));
      toolConsole.addActionListener(this);
      add(toolConsole);
    }

//    private static void cleanKeyfile()
//    {
//      JLabel infolabel = new JLabel("<html><center>This will delete empty BIFFs and remove<br>" +
//                                    "references to nonexistent BIFFs.<br><br>" +
//                                    "Warning: Your existing " + ResourceFactory.getKeyfile() +
//                                    " will be overwritten!<br><br>Continue?</center></html>");
//      String options[] = {"Continue", "Cancel"};
//      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), infolabel,
//                                       "Keyfile cleanup", JOptionPane.YES_NO_OPTION,
//                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
//        return;
//      boolean updated = ResourceFactory.getKeyfile().cleanUp();
//      if (!updated)
//        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No cleanup necessary", "Cleanup completed",
//                                      JOptionPane.INFORMATION_MESSAGE);
//      else {
//        try {
//          ResourceFactory.getKeyfile().write();
//          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation completed successfully", "Cleanup completed",
//                                        JOptionPane.INFORMATION_MESSAGE);
//        } catch (IOException e) {
//          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error writing keyfile", "Error",
//                                        JOptionPane.ERROR_MESSAGE);
//          e.printStackTrace();
//        }
//      }
//    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == toolInfinityAmp) {
        InfinityAmp infAmp = (InfinityAmp)ChildFrame.getFirstFrame(InfinityAmp.class);
        if (infAmp == null)
          infAmp = new InfinityAmp();
        infAmp.setVisible(true);
      }
      else if (event.getSource() == toolIDSBrowser) {
        IdsBrowser browser = (IdsBrowser)ChildFrame.getFirstFrame(IdsBrowser.class);
        if (browser == null)
          browser = new IdsBrowser();
        browser.setVisible(true);
      }
      else if (event.getSource() == toolClipBoard) {
        ClipboardViewer viewer = (ClipboardViewer)ChildFrame.getFirstFrame(
                ClipboardViewer.class);
        if (viewer == null) {
          viewer = new ClipboardViewer();
          viewer.addWindowListener(new WindowAdapter()
          {
            @Override
            public void windowClosing(WindowEvent e)
            {
              toolClipBoard.setSelected(false);
            }
          });
        }
        viewer.setVisible(toolClipBoard.isSelected());
      }
      else if (event.getSource() == toolConsole) {
        DebugConsole console = (DebugConsole)ChildFrame.getFirstFrame(DebugConsole.class);
        if (console == null) {
          console = new DebugConsole();
          console.addWindowListener(new WindowAdapter()
          {
            @Override
            public void windowClosing(WindowEvent e)
            {
              toolConsole.setSelected(false);
            }
          });
        }
        console.setVisible(toolConsole.isSelected());
      }
      else if (event.getSource() == toolCleanKeyfile)
//        cleanKeyfile();
        ;
      else if (event.getSource() == toolDropZone) {
        BcsDropFrame bcsframe = (BcsDropFrame)ChildFrame.getFirstFrame(BcsDropFrame.class);
        if (bcsframe == null)
          bcsframe = new BcsDropFrame();
        bcsframe.setVisible(true);
      }
      else if (event.getSource() == toolCheckAllDialog)
        new DialogChecker(false);
      else if (event.getSource() == toolCheckOverrideDialog)
        new DialogChecker(true);
      else if (event.getSource() == toolCheckResRef)
        new ResRefChecker();
      else if (event.getSource() == toolCheckCREInv)
        new CreInvChecker();
      else if (event.getSource() == toolCheckIDSRef)
        new IDSRefChecker();
      else if (event.getSource() == toolCheckIDSBCSRef)
        new BCSIDSChecker();
      else if (event.getSource() == toolCheckScripts)
        new ScriptChecker();
      else if (event.getSource() == toolCheckStructs)
        new StructChecker();
      else if (event.getSource() == toolCheckStringUse)
        new StringUseChecker();
      else if (event.getSource() == toolCheckStringIndex)
        new StrrefIndexChecker();
      else if (event.getSource() == toolCheckFileUse)
        new ResourceUseChecker(NearInfinity.getInstance());
      else if (event.getSource() == toolMassExport)
        new MassExporter();
      else if (event.getSource() == toolCheckEffectsIndex)
        new EffectsIndexChecker();
      else if (event.getSource() == toolConvImageToPvrz) {
        ConvertToPvrz dlg = (ConvertToPvrz)ChildFrame.getFirstFrame(ConvertToPvrz.class);
        if (dlg == null) {
          dlg = new ConvertToPvrz();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToTis) {
        ConvertToTis dlg = (ConvertToTis)ChildFrame.getFirstFrame(ConvertToTis.class);
        if (dlg == null) {
          dlg = new ConvertToTis();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToMos) {
        ConvertToMos dlg = (ConvertToMos)ChildFrame.getFirstFrame(ConvertToMos.class);
        if (dlg == null) {
          dlg = new ConvertToMos();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToBmp) {
        ConvertToBmp dlg = (ConvertToBmp)ChildFrame.getFirstFrame(ConvertToBmp.class);
        if (dlg == null) {
          dlg = new ConvertToBmp();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToBam) {
        ConvertToBam dlg = (ConvertToBam)ChildFrame.getFirstFrame(ConvertToBam.class);
        if (dlg == null) {
          dlg = new ConvertToBam();
        }
        dlg.setVisible(true);
      }
    }
  }

  ///////////////////////////////
  // Options Menu
  ///////////////////////////////

  private static final class OptionsMenu extends JMenu implements ActionListener, ItemListener
  {
    private static final int[] FONTSIZES = {50, 75, 100, 125, 150, 175, 200, 250, 300, 400, -1};
    private static final Font[] FONTS = {
      new Font(Font.MONOSPACED, Font.PLAIN, 12), new Font(Font.SERIF, Font.PLAIN, 12),
      new Font(Font.SANS_SERIF, Font.PLAIN, 12), new Font(Font.DIALOG, Font.PLAIN, 12), null};
    private static final String DefaultCharset = "Auto";
    private static final List<String[]> CharsetsUsed = new ArrayList<String[]>();
    // BCS indentations to use when decompiling (indent, title)
    private static final String[][] BCSINDENT = { {"  ", "2 Spaces"},
                                                  {"    ", "4 Spaces"},
                                                  {"\t", "Tab"} };
    // Available color schemes for highlighted BCS format (scheme, title, description)
    private static final String[][] BCSCOLORSCHEME = {
      {InfinityTextArea.SchemeDefault, "Default", "A general-purpose default color scheme"},
      {InfinityTextArea.SchemeDark, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme"},
      {InfinityTextArea.SchemeEclipse, "Eclipse", "Mimics Eclipse's default color scheme"},
      {InfinityTextArea.SchemeIdea, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme"},
      {InfinityTextArea.SchemeMonokai, "Monokai", "A dark color scheme inspired by \"Monokai\""},
      {InfinityTextArea.SchemeVs, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme"},
      {InfinityTextArea.SchemeBCS, "BCS Light", "A color scheme which is loosely based on the WeiDU Syntax Highlighter for Notepad++"},
    };
    // Available color schemes for remaining highlighted formats (scheme, title, description)
    private static final String[][] COLORSCHEME = {
      {InfinityTextArea.SchemeDefault, "Default", "A general-purpose default color scheme"},
      {InfinityTextArea.SchemeDark, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme"},
      {InfinityTextArea.SchemeEclipse, "Eclipse", "Mimics Eclipse's default color scheme"},
      {InfinityTextArea.SchemeIdea, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme"},
      {InfinityTextArea.SchemeMonokai, "Monokai", "A dark color scheme inspired by \"Monokai\""},
      {InfinityTextArea.SchemeVs, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme"},
    };

    static {
      // Order: Display name, Canonical charset name, Tooltip
      CharsetsUsed.add(new String[]{"UTF-8", "UTF-8", "The character set of choice for the Enhanced Editions of the Baldur's Gate games."});
      CharsetsUsed.add(new String[]{"Windows-1252", "windows-1252", "Character set used in english and other latin-based languages, such as french, german, italian or spanish."});
      CharsetsUsed.add(new String[]{"Windows-1251", "windows-1251", "Character set used in russian and other cyrillic-based languages."});
      CharsetsUsed.add(new String[]{"Windows-1250", "windows-1250", "Character set used in central european and eastern european languages, such as polish or czech."});
      CharsetsUsed.add(new String[]{"Windows-31J", "windows-31j", "Character set used in japanese localizations."});
      CharsetsUsed.add(new String[]{"GBK", "GBK", "Character set for Simplified Chinese text."});
      CharsetsUsed.add(new String[]{"Big5-HKSCS", "Big5-HKSCS", "Character set for Traditional Chinese text (may not be fully compatible)."});
      CharsetsUsed.add(new String[]{"IBM-949", "x-IBM949", "Character set used in korean localizations."});
    }

    private static final String OPTION_SHOWOFFSETS              = "ShowOffsets";
    private static final String OPTION_BACKUPONSAVE             = "BackupOnSave";
    private static final String OPTION_IGNOREOVERRIDE           = "IgnoreOverride";
    private static final String OPTION_IGNOREREADERRORS         = "IgnoreReadErrors";
    private static final String OPTION_SHOWUNKNOWNRESOURCES     = "ShowUnknownResources";
    private static final String OPTION_AUTOCHECK_BCS            = "AutocheckBCS";
    private static final String OPTION_CACHEOVERRIDE            = "CacheOverride";
    private static final String OPTION_MORECOMPILERWARNINGS     = "MoreCompilerWarnings";
    private static final String OPTION_SHOWSTRREFS              = "ShowStrrefs";
    private static final String OPTION_DLG_SHOWICONS            = "DlgShowIcons";
    private static final String OPTION_SHOWHEXCOLORED           = "ShowHexColored";
    private static final String OPTION_KEEPVIEWONCOPY           = "UpdateTreeOnCopy";
//    private static final String OPTION_MONITORFILECHANGES       = "MonitorFileChanges";
    private static final String OPTION_SHOWOVERRIDES            = "ShowOverridesIn";
    private static final String OPTION_SHOWRESREF               = "ShowResRef";
    private static final String OPTION_LOOKANDFEELCLASS         = "LookAndFeelClass";
    private static final String OPTION_VIEWOREDITSHOWN          = "ViewOrEditShown";
    private static final String OPTION_FONT                     = "Font";
    private static final String OPTION_FONT_NAME                = "FontName";
    private static final String OPTION_FONT_STYLE               = "FontStyle";
    private static final String OPTION_FONT_SIZE                = "FontSize";
    private static final String OPTION_TLKCHARSET               = "TLKCharsetType";
    private static final String OPTION_LANGUAGE_GAMES           = "GameLanguages";
    private static final String OPTION_TEXT_SHOWCURRENTLINE     = "TextShowCurrentLine";
    private static final String OPTION_TEXT_SHOWLINENUMBERS     = "TextShowLineNumbers";
    private static final String OPTION_TEXT_SYMBOLWHITESPACE    = "TextShowWhiteSpace";
    private static final String OPTION_TEXT_SYMBOLEOL           = "TextShowEOL";
    private static final String OPTION_TEXT_TABSEMULATED        = "TextTabsEmulated";
    private static final String OPTION_TEXT_TABSIZE             = "TextTabSize";
    private static final String OPTION_BCS_SYNTAXHIGHLIGHTING   = "BcsSyntaxHighlighting";
    private static final String OPTION_BCS_COLORSCHEME          = "BcsColorScheme";
    private static final String OPTION_BCS_CODEFOLDING          = "BcsCodeFolding";
    private static final String OPTION_BCS_AUTO_INDENT          = "BcsAutoIndent";
    private static final String OPTION_BCS_INDENT               = "BcsIndent";
    private static final String OPTION_GLSL_SYNTAXHIGHLIGHTING  = "GlslSyntaxHighlighting";
    private static final String OPTION_GLSL_COLORSCHEME         = "GlslColorScheme";
    private static final String OPTION_GLSL_CODEFOLDING         = "GlslCodeFolding";
    private static final String OPTION_LUA_SYNTAXHIGHLIGHTING   = "LuaSyntaxHighlighting";
    private static final String OPTION_LUA_COLORSCHEME          = "LuaColorScheme";
    private static final String OPTION_SQL_SYNTAXHIGHLIGHTING   = "SqlSyntaxHighlighting";
    private static final String OPTION_SQL_COLORSCHEME          = "SqlColorScheme";
    private static final String OPTION_TLK_SYNTAXHIGHLIGHTING   = "TlkSyntaxHighlighting";
    private static final String OPTION_TLK_COLORSCHEME          = "TlkColorScheme";
    private static final String OPTION_WEIDU_SYNTAXHIGHLIGHTING = "WeiDUSyntaxHighlighting";
    private static final String OPTION_WEIDU_COLORSCHEME        = "WeiDUColorScheme";
    // this preferences key can be used internally to reset incorrectly set default values after a public release
    private static final String OPTION_OPTION_FIXED             = "OptionFixedInternal";

    // Mask used for one-time resets of options (kept track of in OPTION_OPTION_FIXED)
    private static final int MASK_OPTION_FIXED_AUTO_INDENT      = 0x00000001;

    // Identifier for autodetected game language
    private static final String LANGUAGE_AUTODETECT             = "Auto";

    private final List<DataRadioButtonMenuItem> lookAndFeel = new ArrayList<DataRadioButtonMenuItem>();

    private final JRadioButtonMenuItem[] showOverrides = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem[] showResRef = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem[] viewOrEditShown = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem[] selectFont = new JRadioButtonMenuItem[FONTS.length];
    private final JRadioButtonMenuItem[] selectTextTabSize = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem[] selectBcsIndent = new JRadioButtonMenuItem[BCSINDENT.length];
    private final JRadioButtonMenuItem[] selectBcsColorScheme = new JRadioButtonMenuItem[BCSCOLORSCHEME.length];
    private final JRadioButtonMenuItem[] selectGlslColorScheme = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final JRadioButtonMenuItem[] selectLuaColorScheme = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final JRadioButtonMenuItem[] selectSqlColorScheme = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final JRadioButtonMenuItem[] selectTlkColorScheme = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final JRadioButtonMenuItem[] selectWeiDUColorScheme = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final DataRadioButtonMenuItem[] globalFontSize = new DataRadioButtonMenuItem[FONTSIZES.length];

    private JCheckBoxMenuItem optionTextHightlightCurrent, optionTextLineNumbers,
                              optionTextShowWhiteSpace, optionTextShowEOL, optionTextTabEmulate,
                              optionBCSEnableSyntax, optionBCSEnableCodeFolding,
                              optionBCSEnableAutoIndent, optionGLSLEnableSyntax, optionLUAEnableSyntax,
                              optionSQLEnableSyntax, optionTLKEnableSyntax, optionWeiDUEnableSyntax,
                              optionGLSLEnableCodeFolding;

    private JCheckBoxMenuItem optionAutocheckBCS, optionMoreCompileWarnings;

    private JCheckBoxMenuItem optionBackupOnSave, optionShowOffset, optionIgnoreOverride,
                              optionIgnoreReadErrors, optionCacheOverride, optionShowStrrefs,
                              optionDlgShowIcons, optionShowHexColored, optionShowUnknownResources,
                              optionKeepViewOnCopy;
//                              optionMonitorFileChanges;
    private final JMenu mCharsetMenu, mLanguageMenu;
    private ButtonGroup bgCharsetButtons;
    private String languageDefinition;
    private int optionFixedInternal;

    // Stores available languages in BG(2)EE
    private final HashMap<JRadioButtonMenuItem, String> gameLanguage = new HashMap<JRadioButtonMenuItem, String>();

    private OptionsMenu()
    {
      super("Options");
      setMnemonic(KeyEvent.VK_O);

      optionFixedInternal = getPrefs().getInt(OPTION_OPTION_FIXED, 0);

      // Options
      optionBackupOnSave =
          new JCheckBoxMenuItem("Backup on save", getPrefs().getBoolean(OPTION_BACKUPONSAVE, false));
      optionBackupOnSave.setToolTipText("Enable this option to automatically create a backup " +
                                        "of the resource you want to save.");
      add(optionBackupOnSave);
      optionIgnoreOverride =
          new JCheckBoxMenuItem("Ignore Overrides", getPrefs().getBoolean(OPTION_IGNOREOVERRIDE, false));
      add(optionIgnoreOverride);
      optionIgnoreReadErrors =
          new JCheckBoxMenuItem("Ignore Read Errors", getPrefs().getBoolean(OPTION_IGNOREREADERRORS, false));
      add(optionIgnoreReadErrors);
      optionShowUnknownResources =
          new JCheckBoxMenuItem("Show Unknown Resource Types", getPrefs().getBoolean(OPTION_SHOWUNKNOWNRESOURCES, true));
      optionShowUnknownResources.setActionCommand("Refresh");
      optionShowUnknownResources.addActionListener(NearInfinity.getInstance());
      optionShowUnknownResources.setToolTipText("Uncheck this option to hide unknown or unsupported resource types and invalid filenames.");
      add(optionShowUnknownResources);
      optionShowOffset =
          new JCheckBoxMenuItem("Show Hex Offsets", getPrefs().getBoolean(OPTION_SHOWOFFSETS, false));
      add(optionShowOffset);
//      optionMonitorFileChanges =
//          new JCheckBoxMenuItem("Autoupdate resource tree", getPrefs().getBoolean(OPTION_MONITORFILECHANGES, true));
//      optionMonitorFileChanges.addActionListener(this);
//      optionMonitorFileChanges.setToolTipText("Automatically updates the resource tree whenever a file change occurs in any supported override folders.");
//      add(optionMonitorFileChanges);
      optionCacheOverride =
          new JCheckBoxMenuItem("Autocheck for Overrides", getPrefs().getBoolean(OPTION_CACHEOVERRIDE, false));
      optionCacheOverride.setToolTipText("Without this option selected, Refresh Tree is required " +
                                         "to discover new override files added while NI is open");
      add(optionCacheOverride);
      optionKeepViewOnCopy =
          new JCheckBoxMenuItem("Keep view after copy operations", getPrefs().getBoolean(OPTION_KEEPVIEWONCOPY, false));
      optionKeepViewOnCopy.setToolTipText("With this option enabled the resource tree will not switch to the new resource created by an \"Add Copy Of\" operation.");
      add(optionKeepViewOnCopy);
      optionShowStrrefs =
          new JCheckBoxMenuItem("Show Strrefs in View tabs", getPrefs().getBoolean(OPTION_SHOWSTRREFS, false));
      add(optionShowStrrefs);
      optionDlgShowIcons =
          new JCheckBoxMenuItem("Show icons in DLG tree viewer", getPrefs().getBoolean(OPTION_DLG_SHOWICONS, true));
      add(optionDlgShowIcons);
      optionShowHexColored =
          new JCheckBoxMenuItem("Show colored blocks in Raw tabs", getPrefs().getBoolean(OPTION_SHOWHEXCOLORED, true));
      add(optionShowHexColored);

      addSeparator();

      // Options->Script Compiler
      JMenu compilerMenu = new JMenu("Script Compiler");
      add(compilerMenu);
      optionAutocheckBCS =
          new JCheckBoxMenuItem("Autocheck BCS", getPrefs().getBoolean(OPTION_AUTOCHECK_BCS, true));
      optionAutocheckBCS.setToolTipText("Automatically scans scripts for compile error with this option enabled.");
      compilerMenu.add(optionAutocheckBCS);
      optionMoreCompileWarnings =
          new JCheckBoxMenuItem("Show more compiler warnings", getPrefs().getBoolean(OPTION_MORECOMPILERWARNINGS, false));
      optionMoreCompileWarnings.setToolTipText("Script compiler will generate an additional set of less severe " +
                                               "warning messages with this option enabled.");
      compilerMenu.add(optionMoreCompileWarnings);

      // Options->Text Editor
      JMenu textMenu = new JMenu("Text Editor");
      add(textMenu);
      // Options->Text Viewer/Editor->Tab Settings
      JMenu textTabs = new JMenu("Tab Settings");
      textMenu.add(textTabs);
      optionTextTabEmulate =
          new JCheckBoxMenuItem("Emulate Tabs with Spaces", getPrefs().getBoolean(OPTION_TEXT_TABSEMULATED, false));
      textTabs.add(optionTextTabEmulate);
      textTabs.addSeparator();
      ButtonGroup bg = new ButtonGroup();
      int selectedTextTabSize = getPrefs().getInt(OPTION_TEXT_TABSIZE, 1);
      selectTextTabSize[0] = new JRadioButtonMenuItem("Expand by 2 Spaces", selectedTextTabSize == 0);
      selectTextTabSize[1] = new JRadioButtonMenuItem("Expand by 4 Spaces", selectedTextTabSize == 1);
      selectTextTabSize[2] = new JRadioButtonMenuItem("Expand by 8 Spaces", selectedTextTabSize == 2);
      for (int i = 0; i < selectTextTabSize.length; i++) {
        int cnt = 1 << (i + 1);
        selectTextTabSize[i].setToolTipText(String.format("Each (real or emulated) tab will occupy %d spaces.", cnt));
        textTabs.add(selectTextTabSize[i]);
        bg.add(selectTextTabSize[i]);
      }

      // Options->Text Editor->BCS and BAF
      JMenu textBCS = new JMenu("BCS and BAF");
      textMenu.add(textBCS);
      JMenu textBCSIndent = new JMenu("BCS Indent");
      textBCS.add(textBCSIndent);
      bg = new ButtonGroup();
      int selectedBCSIndent = getPrefs().getInt(OPTION_BCS_INDENT, 2);
      if (selectedBCSIndent < 0 || selectedBCSIndent >= BCSINDENT.length) {
        selectedBCSIndent = 2;
      }
      for (int i = 0; i < BCSINDENT.length; i++) {
        selectBcsIndent[i] = new JRadioButtonMenuItem(BCSINDENT[i][1], selectedBCSIndent == i);
        textBCSIndent.add(selectBcsIndent[i]);
        bg.add(selectBcsIndent[i]);
      }
      JMenu textBCSColors = new JMenu("Color Scheme");
      textBCS.add(textBCSColors);
      bg = new ButtonGroup();
      int selectedBCSScheme = getPrefs().getInt(OPTION_BCS_COLORSCHEME, 5);
      if (selectedBCSScheme < 0 || selectedBCSScheme >= BCSCOLORSCHEME.length) {
        selectedBCSScheme = 5;
      }
      for (int i = 0; i < BCSCOLORSCHEME.length; i++) {
        selectBcsColorScheme[i] = new JRadioButtonMenuItem(BCSCOLORSCHEME[i][1], selectedBCSScheme == i);
        selectBcsColorScheme[i].setToolTipText(BCSCOLORSCHEME[i][2]);
        textBCSColors.add(selectBcsColorScheme[i]);
        bg.add(selectBcsColorScheme[i]);
      }
      optionBCSEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting",
                                                    getPrefs().getBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, true));
      textBCS.add(optionBCSEnableSyntax);
      optionBCSEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding",
                                                         getPrefs().getBoolean(OPTION_BCS_CODEFOLDING, false));
      textBCS.add(optionBCSEnableCodeFolding);
      // XXX: Work-around to fix a previously incorrectly defined option
      optionBCSEnableAutoIndent =
          new JCheckBoxMenuItem("Enable Automatic Indentation",
                                fixOption(MASK_OPTION_FIXED_AUTO_INDENT, true,
                                          getPrefs().getBoolean(OPTION_BCS_AUTO_INDENT, false)));
//        optionBCSEnableAutoIndent = new JCheckBoxMenuItem("Enable Automatic Indentation",
//                                                          getPrefs().getBoolean(OPTION_BCS_AUTO_INDENT, false));
      textBCS.add(optionBCSEnableAutoIndent);

      // Options->Text Viewer/Editor->Misc. Resource Types
      JMenu textMisc = new JMenu("Misc. Resource Types");
      textMenu.add(textMisc);
      JMenu textGLSLColors = new JMenu("Color Scheme for GLSL");
      textMisc.add(textGLSLColors);
      bg = new ButtonGroup();
      int selectedGLSLScheme = getPrefs().getInt(OPTION_GLSL_COLORSCHEME, 0);
      if (selectedGLSLScheme < 0 || selectedGLSLScheme >= COLORSCHEME.length) {
        selectedGLSLScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectGlslColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedGLSLScheme == i);
        selectGlslColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textGLSLColors.add(selectGlslColorScheme[i]);
        bg.add(selectGlslColorScheme[i]);
      }

      JMenu textLUAColors = new JMenu("Color Scheme for LUA");
      textMisc.add(textLUAColors);
      bg = new ButtonGroup();
      int selectedLUAScheme = getPrefs().getInt(OPTION_LUA_COLORSCHEME, 0);
      if (selectedLUAScheme < 0 || selectedLUAScheme >= COLORSCHEME.length) {
        selectedLUAScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectLuaColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedLUAScheme == i);
        selectLuaColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textLUAColors.add(selectLuaColorScheme[i]);
        bg.add(selectLuaColorScheme[i]);
      }

      JMenu textSQLColors = new JMenu("Color Scheme for SQL");
      textMisc.add(textSQLColors);
      bg = new ButtonGroup();
      int selectedSQLScheme = getPrefs().getInt(OPTION_SQL_COLORSCHEME, 0);
      if (selectedSQLScheme < 0 || selectedSQLScheme >= COLORSCHEME.length) {
        selectedSQLScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectSqlColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedSQLScheme == i);
        selectSqlColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textSQLColors.add(selectSqlColorScheme[i]);
        bg.add(selectSqlColorScheme[i]);
      }

      JMenu textTLKColors = new JMenu("Color Scheme for text strings");
      textMisc.add(textTLKColors);
      bg = new ButtonGroup();
      int selectedTLKScheme = getPrefs().getInt(OPTION_TLK_COLORSCHEME, 0);
      if (selectedTLKScheme < 0 || selectedTLKScheme >= COLORSCHEME.length) {
        selectedTLKScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectTlkColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedTLKScheme == i);
        selectTlkColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textTLKColors.add(selectTlkColorScheme[i]);
        bg.add(selectTlkColorScheme[i]);
      }

      JMenu textWeiDUColors = new JMenu("Color Scheme for WeiDU.log");
      textMisc.add(textWeiDUColors);
      bg = new ButtonGroup();
      int selectedWeiDUScheme = getPrefs().getInt(OPTION_WEIDU_COLORSCHEME, 0);
      if (selectedWeiDUScheme < 0 || selectedWeiDUScheme >= COLORSCHEME.length) {
        selectedWeiDUScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectWeiDUColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedWeiDUScheme == i);
        selectWeiDUColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textWeiDUColors.add(selectWeiDUColorScheme[i]);
        bg.add(selectWeiDUColorScheme[i]);
      }

      optionGLSLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for GLSL",
                                                     getPrefs().getBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionGLSLEnableSyntax);
      optionLUAEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for LUA",
                                                    getPrefs().getBoolean(OPTION_LUA_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionLUAEnableSyntax);
      optionSQLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for SQL",
                                                    getPrefs().getBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionSQLEnableSyntax);
      optionTLKEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for text strings",
                                                    getPrefs().getBoolean(OPTION_TLK_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionTLKEnableSyntax);
      optionWeiDUEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for WeiDU.log",
                                                      getPrefs().getBoolean(OPTION_WEIDU_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionWeiDUEnableSyntax);
      optionGLSLEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding for GLSL",
                                                          getPrefs().getBoolean(OPTION_GLSL_CODEFOLDING, false));
      textMisc.add(optionGLSLEnableCodeFolding);

      // Options->Text Editor (continued)
      optionTextShowWhiteSpace =
          new JCheckBoxMenuItem("Show Spaces and Tabs", getPrefs().getBoolean(OPTION_TEXT_SYMBOLWHITESPACE, false));
      textMenu.add(optionTextShowWhiteSpace);
      optionTextShowEOL =
          new JCheckBoxMenuItem("Show End of Line", getPrefs().getBoolean(OPTION_TEXT_SYMBOLEOL, false));
      textMenu.add(optionTextShowEOL);
      optionTextHightlightCurrent = new JCheckBoxMenuItem("Show Highlighted Current Line",
                                                          getPrefs().getBoolean(OPTION_TEXT_SHOWCURRENTLINE, true));
      textMenu.add(optionTextHightlightCurrent);
      optionTextLineNumbers = new JCheckBoxMenuItem("Show Line Numbers",
                                                    getPrefs().getBoolean(OPTION_TEXT_SHOWLINENUMBERS, true));
      textMenu.add(optionTextLineNumbers);

      // Options->Show ResourceRefs As
      JMenu showresrefmenu = new JMenu("Show ResourceRefs As");
      add(showresrefmenu);
      int selectedresref = getPrefs().getInt(OPTION_SHOWRESREF, RESREF_REF_NAME);
      showResRef[RESREF_ONLY] = new JRadioButtonMenuItem("Filename", selectedresref == RESREF_ONLY);
      showResRef[RESREF_ONLY].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, CTRL_MASK));
      showResRef[RESREF_REF_NAME] =
      new JRadioButtonMenuItem("Filename (Name)", selectedresref == RESREF_REF_NAME);
      showResRef[RESREF_REF_NAME].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, CTRL_MASK));
      showResRef[RESREF_NAME_REF] =
      new JRadioButtonMenuItem("Name (Filename)", selectedresref == RESREF_NAME_REF);
      showResRef[RESREF_NAME_REF].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, CTRL_MASK));
      bg = new ButtonGroup();
      for (int i = RESREF_ONLY; i <= RESREF_NAME_REF; i++) {
        showresrefmenu.add(showResRef[i]);
        bg.add(showResRef[i]);
      }

      // Options->Show Override Files
      JMenu overridesubmenu = new JMenu("Show Override Files");
      add(overridesubmenu);
      int selectedmode = getPrefs().getInt(OPTION_SHOWOVERRIDES, OVERRIDE_SPLIT);
      showOverrides[OVERRIDE_IN_THREE] =
      new JRadioButtonMenuItem("In ??? Folders (CRE, SPL, ...)", selectedmode == OVERRIDE_IN_THREE);
      showOverrides[OVERRIDE_IN_OVERRIDE] =
      new JRadioButtonMenuItem("In Override Folder", selectedmode == OVERRIDE_IN_OVERRIDE);
      showOverrides[OVERRIDE_SPLIT] =
      new JRadioButtonMenuItem("Split Between ??? and Override Folders", selectedmode == OVERRIDE_SPLIT);
      showOverrides[OVERRIDE_SPLIT].setToolTipText(
              "Indexed by Chitin.key => ??? folders; Not indexed => Override folder");
      bg = new ButtonGroup();
      for (int i = OVERRIDE_IN_THREE; i <= OVERRIDE_SPLIT; i++) {
        overridesubmenu.add(showOverrides[i]);
        bg.add(showOverrides[i]);
        showOverrides[i].setActionCommand("Refresh");
        showOverrides[i].addActionListener(NearInfinity.getInstance());
      }

      // Options->Default Structure Display
      JMenu vieworeditmenu = new JMenu("Default Structure Display");
      add(vieworeditmenu);
      int selectedview = getPrefs().getInt(OPTION_VIEWOREDITSHOWN, DEFAULT_VIEW);
      viewOrEditShown[DEFAULT_VIEW] =
      new JRadioButtonMenuItem("View", selectedview == DEFAULT_VIEW);
      viewOrEditShown[DEFAULT_EDIT] =
      new JRadioButtonMenuItem("Edit", selectedview == DEFAULT_EDIT);
      bg = new ButtonGroup();
      bg.add(viewOrEditShown[DEFAULT_VIEW]);
      bg.add(viewOrEditShown[DEFAULT_EDIT]);
      vieworeditmenu.add(viewOrEditShown[DEFAULT_VIEW]);
      vieworeditmenu.add(viewOrEditShown[DEFAULT_EDIT]);

      // Options->Global Font Size
      JMenu fontSizeMenu = new JMenu("Change Global Font Size");
      add(fontSizeMenu);
      bg = new ButtonGroup();
      fontSizeMenu.addItemListener(this);
      int selectedSize = NearInfinity.getInstance().getGlobalFontSize();
      selectedSize = Math.min(Math.max(selectedSize, 50), 400);
      boolean isCustom = true;
      for (int i = 0; i < FONTSIZES.length; i++) {
        int size = FONTSIZES[i];
        if (size > 0) {
          String msg = FONTSIZES[i] + " %" + (size == 100 ? " (Default)" : "");
          globalFontSize[i] = new DataRadioButtonMenuItem(msg,
                                                          FONTSIZES[i] == selectedSize,
                                                          Integer.valueOf(FONTSIZES[i]));
          if (FONTSIZES[i] == selectedSize) {
            isCustom = false;
          }
        } else {
          String msg = isCustom ? "Custom (" + selectedSize + " %)..." : "Custom...";
          globalFontSize[i] = new DataRadioButtonMenuItem(msg, isCustom, isCustom ? selectedSize : size);
        }
        globalFontSize[i].setActionCommand("ChangeFontSize");
        globalFontSize[i].addActionListener(this);
        fontSizeMenu.add(globalFontSize[i]);
        bg.add(globalFontSize[i]);
      }

      // Options->Look and Feel
      JMenu lookandfeelmenu = new JMenu("Look and Feel");
      add(lookandfeelmenu);
      final String selectedLF = getPrefs().get(OPTION_LOOKANDFEELCLASS, DEFAULT_LOOKFEEL.getClassName());
      LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
      bg = new ButtonGroup();
      if (info != null && info.length > 0) {
        // dynamically create a list of supported look&feel themes
        DataRadioButtonMenuItem dbmi;
        for (int i = 0; i < info.length; i++) {
          dbmi = new DataRadioButtonMenuItem(info[i].getName(),
                                             selectedLF.equalsIgnoreCase(info[i].getClassName()),
                                             info[i]);
          lookAndFeel.add(dbmi);
          bg.add(dbmi);
        }
      } else {
        // fallback solution: adding default look&feel theme
        DataRadioButtonMenuItem dbmi;
        dbmi = new DataRadioButtonMenuItem(DEFAULT_LOOKFEEL.getName(), true, DEFAULT_LOOKFEEL);
        lookAndFeel.add(dbmi);
        bg.add(dbmi);
      }
      if (bg.getSelection() == null) {
        lookAndFeel.get(0).setSelected(true);
      }
      for (final JRadioButtonMenuItem lf : lookAndFeel) {
        if (lf != null) {
          lookandfeelmenu.add(lf);
          lf.setActionCommand("ChangeLook");
          lf.addActionListener(NearInfinity.getInstance());
        }
      }

      // Options->Text Font
      JMenu scriptmenu = new JMenu("Text Font");
      add(scriptmenu);
      bg = new ButtonGroup();
      int selectedFont = getPrefs().getInt(OPTION_FONT, 0);
      selectedFont = Math.min(Math.max(selectedFont, 0), FONTS.length - 1);
      for (int i = 0; i < FONTS.length; i++) {
        if (FONTS[i] != null) {
          selectFont[i] = new JRadioButtonMenuItem(FONTS[i].getName() + ' ' + FONTS[i].getSize(),
                                                   i == selectedFont);
          selectFont[i].setFont(Misc.getScaledFont(FONTS[i]));
        } else {
          Font font = null;
          String fontName = getPrefs().get(OPTION_FONT_NAME, "");
          if (!fontName.isEmpty()) {
            font = new Font(fontName,
                            getPrefs().getInt(OPTION_FONT_STYLE, Font.PLAIN),
                            getPrefs().getInt(OPTION_FONT_SIZE, 12));
          }
          selectFont[i] = new JRadioButtonMenuItem("Select font...", i == selectedFont);
          selectFont[i].setActionCommand("TextFont");
          selectFont[i].addActionListener(this);
          applyCustomFont(font);
        }
        scriptmenu.add(selectFont[i]);
        bg.add(selectFont[i]);
      }

      // Options->TLK Charset
      String charset = getPrefs().get(OPTION_TLKCHARSET, DefaultCharset);
      if (!charsetAvailable(charset)) {
        System.err.println(String.format("Charset \"%s\" not available.", charset));
        charset = DefaultCharset;
      }
      if (!charsetName(charset, false).equals(StringTable.getCharset().name())) {
        StringTable.setCharset(charsetName(charset, false));
      }
      mCharsetMenu = initCharsetMenu(charset);
      add(mCharsetMenu);

      // Options->TLK Language
      mLanguageMenu = new JMenu("TLK Language (EE only)");
      add(mLanguageMenu);
      languageDefinition = getPrefs().get(OPTION_LANGUAGE_GAMES, "");
    }

    // (Re-)creates a list of available TLK languages
    private void resetGameLanguage()
    {
      // removing old list of available game languages
      for (JRadioButtonMenuItem r: gameLanguage.keySet()) {
        r.removeActionListener(this);
      }
      mLanguageMenu.removeAll();
      gameLanguage.clear();

      // initializing new list of available game languages
      String selectedCode = getGameLanguage(languageDefinition, Profile.getGame());

      ButtonGroup bg = new ButtonGroup();
      JRadioButtonMenuItem rbmi;

      // adding "Autodetect" for all available game types
      rbmi = createLanguageMenuItem(LANGUAGE_AUTODETECT, "Autodetect",
                                    "Autodetect language from baldur.ini. " +
                                        "Defaults to english if not available.", bg, true);
      mLanguageMenu.add(rbmi);

      if (Profile.isEnhancedEdition()) {
        List<String> languages = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAMES_AVAILABLE);
        for (final String lang: languages) {
          String langName = getDisplayLanguage(lang);
          if (!langName.equalsIgnoreCase(lang)) {
            rbmi = createLanguageMenuItem(lang, String.format("%s (%s)", langName, lang),
                                          null, bg, selectedCode.equalsIgnoreCase(lang));
            mLanguageMenu.add(rbmi);
          } else {
            rbmi = createLanguageMenuItem(lang, lang, null, bg, selectedCode.equalsIgnoreCase(lang));
            mLanguageMenu.add(rbmi);
          }
        }
      } else {
        rbmi.setEnabled(false);
        rbmi.setToolTipText(null);
      }
    }

    // Returns the name of the language specified by the given language code
    private String getDisplayLanguage(String langCode)
    {
      String retVal = langCode;
      String[] lang = langCode.split("_");
      if (lang.length >= 2) {
        retVal = (new Locale(lang[0], lang[1])).getDisplayLanguage();
        if (retVal == null || retVal.isEmpty()) {
          retVal = langCode;
        }
      }
      return retVal;
    }

    // Initializes and returns a radio button menuitem
    private JRadioButtonMenuItem createLanguageMenuItem(String code, String name, String tooltip,
                                                        ButtonGroup bg, boolean selected)
    {
      JRadioButtonMenuItem rbmi = null;
      if (code == null) {
        code = "";
      }
      if (name != null && !name.isEmpty()) {
        rbmi = new JRadioButtonMenuItem(name);
        if (tooltip != null && !tooltip.isEmpty()) {
          rbmi.setToolTipText(tooltip);
        }
        if (bg != null) {
          bg.add(rbmi);
        }
        rbmi.setSelected(selected);
        rbmi.addItemListener(this);
        gameLanguage.put(rbmi, code);
      }
      return rbmi;
    }

    private JMenu initCharsetMenu(String charset)
    {
      bgCharsetButtons = new ButtonGroup();
      JMenu menu = new JMenu("TLK Charset");
      DataRadioButtonMenuItem dmi =
          new DataRadioButtonMenuItem("Autodetect Charset", false, DefaultCharset);
      dmi.setToolTipText("Attempts to determine the correct character encoding automatically. " +
                         "May not work reliably for all game languages.");
      dmi.addActionListener(this);
      bgCharsetButtons.add(dmi);
      menu.add(dmi);

      // creating primary list of charsets
      for (int i = 0; i < CharsetsUsed.size(); i++) {
        String[] info = CharsetsUsed.get(i);
        if (info != null && info.length > 2) {
          dmi = new DataRadioButtonMenuItem(info[0], false, info[1]);
          StringBuilder sb = new StringBuilder();
          sb.append(info[2]);
          Charset cs = Charset.forName(info[1]);
          if (cs != null && !cs.aliases().isEmpty()) {
            sb.append(" Charset aliases: ");
            Iterator<String> iter = cs.aliases().iterator();
            while (iter.hasNext()) {
              sb.append(iter.next());
              if (iter.hasNext())
                sb.append(", ");
            }
          }
          dmi.setToolTipText(sb.toString());
          dmi.setActionCommand("Charset");
          dmi.addActionListener(this);
          bgCharsetButtons.add(dmi);
          menu.add(dmi);
        }
      }

      int count = 0;
      JMenu menu2 = new JMenu("More character sets");
      menu.add(menu2);

      // creating secondary list(s) of charsets
      Iterator<String> iter = Charset.availableCharsets().keySet().iterator();
      if (iter != null) {
        while (iter.hasNext()) {
          String name= iter.next();

          // check whether charset has already been added
          boolean match = false;
          for (int i = 0; i < CharsetsUsed.size(); i++) {
            String[] info = CharsetsUsed.get(i);
            if (info != null && info.length > 2) {
              if (name.equalsIgnoreCase(info[1])) {
                match = true;
                break;
              }
            }
          }
          if (match) {
            continue;
          }

          boolean official = !(name.startsWith("x-") || name.startsWith("X-"));
          String desc = official ? name : String.format("%s (unofficial)", name.substring(2));
          dmi = new DataRadioButtonMenuItem(desc, false, name);
          Charset cs = Charset.forName(name);
          if (cs != null && !cs.aliases().isEmpty()) {
            StringBuilder sb = new StringBuilder("Charset aliases: ");
            Iterator<String> csIter = cs.aliases().iterator();
            while (csIter.hasNext()) {
              sb.append(csIter.next());
              if (csIter.hasNext())
                sb.append(", ");
            }
            dmi.setToolTipText(sb.toString());
          }
          dmi.addActionListener(this);
          bgCharsetButtons.add(dmi);
          menu2.add(dmi);

          count++;

          // splitting list of charsets into manageable segments
          if (count % 30 == 0) {
            JMenu tmpMenu = new JMenu("More character sets");
            menu2.add(tmpMenu);
            menu2 = tmpMenu;
          }
        }
      }

      // Selecting specified menu item
      dmi = findCharsetButton(charset);
      if (dmi == null) {
        dmi = findCharsetButton(DefaultCharset);
      }
      if (dmi != null) {
        dmi.setSelected(true);
      }

      return menu;
    }

    // Returns the menuitem that is associated with the specified string
    private DataRadioButtonMenuItem findCharsetButton(String charset)
    {
      if (bgCharsetButtons != null && charset != null && !charset.isEmpty()) {
        Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
        while (buttonSet.hasMoreElements()) {
          AbstractButton b = buttonSet.nextElement();
          if (b instanceof DataRadioButtonMenuItem) {
            Object data = ((DataRadioButtonMenuItem)b).getData();
            if (data instanceof String) {
              if (charset.equalsIgnoreCase((String)data)) {
                return (DataRadioButtonMenuItem)b;
              }
            }
          }
        }
      }
      return null;
    }

    // Returns the charset string associated with the currently selected charset menuitem
    private String getSelectedButtonData()
    {
      Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
      if (buttonSet != null) {
        while (buttonSet.hasMoreElements()) {
          AbstractButton b = buttonSet.nextElement();
          if (b instanceof DataRadioButtonMenuItem) {
            DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem)b;
            if (dmi.isSelected()) {
              return (dmi.getData() != null) ? (String)dmi.getData() : DefaultCharset;
            }
          }
        }
      }
      return DefaultCharset;
    }

    // Attempts to determine the correct charset for the current game
    private String charsetName(String charset, boolean detect)
    {
      if (DefaultCharset.equalsIgnoreCase(charset)) {
        charset = CharsetDetector.guessCharset(detect);
      } else {
        charset = CharsetDetector.setCharset(charset);
      }
      return charset;
    }

    private boolean charsetAvailable(String charset)
    {
      if (charset != null && !charset.isEmpty()) {
        if (DefaultCharset.equalsIgnoreCase(charset)) {
          return true;
        }
        try {
          return (Charset.forName(charset) != null);
        } catch (Throwable t) {
          return false;
        }
      }
      return false;
    }

    private void gameLoaded()
    {
      // update charset selection
      StringTable.setCharset(charsetName(getSelectedButtonData(), true));
      // update language selection
      resetGameLanguage();
    }

    private void storePreferences()
    {
      getPrefs().putBoolean(OPTION_SHOWOFFSETS, optionShowOffset.isSelected());
      getPrefs().putBoolean(OPTION_BACKUPONSAVE, optionBackupOnSave.isSelected());
      getPrefs().putBoolean(OPTION_IGNOREOVERRIDE, optionIgnoreOverride.isSelected());
      getPrefs().putBoolean(OPTION_IGNOREREADERRORS, optionIgnoreReadErrors.isSelected());
      getPrefs().putBoolean(OPTION_SHOWUNKNOWNRESOURCES, optionShowUnknownResources.isSelected());
      getPrefs().putBoolean(OPTION_AUTOCHECK_BCS, optionAutocheckBCS.isSelected());
      getPrefs().putBoolean(OPTION_CACHEOVERRIDE, optionCacheOverride.isSelected());
      getPrefs().putBoolean(OPTION_MORECOMPILERWARNINGS, optionMoreCompileWarnings.isSelected());
      getPrefs().putBoolean(OPTION_SHOWSTRREFS, optionShowStrrefs.isSelected());
      getPrefs().putBoolean(OPTION_DLG_SHOWICONS, optionDlgShowIcons.isSelected());
      getPrefs().putBoolean(OPTION_SHOWHEXCOLORED, optionShowHexColored.isSelected());
      getPrefs().putBoolean(OPTION_KEEPVIEWONCOPY, optionKeepViewOnCopy.isSelected());
//      getPrefs().putBoolean(OPTION_MONITORFILECHANGES, optionMonitorFileChanges.isSelected());
      getPrefs().putInt(OPTION_SHOWRESREF, getResRefMode());
      getPrefs().putInt(OPTION_SHOWOVERRIDES, getOverrideMode());
      getPrefs().put(OPTION_LOOKANDFEELCLASS, getLookAndFeel().getClassName());
      getPrefs().putInt(OPTION_VIEWOREDITSHOWN, getDefaultStructView());
      int selectedFont = getSelectedButtonIndex(selectFont, 0);
      getPrefs().putInt(OPTION_FONT, selectedFont);
      Font font = FONTS[FONTS.length - 1];
      if (font != null) {
        getPrefs().put(OPTION_FONT_NAME, font.getName());
        getPrefs().putInt(OPTION_FONT_STYLE, font.getStyle());
        getPrefs().putInt(OPTION_FONT_SIZE, font.getSize());
      }
      int selectedIndent = getSelectedButtonIndex(selectBcsIndent, 0);
      getPrefs().putInt(OPTION_BCS_INDENT, selectedIndent);
      getPrefs().putBoolean(OPTION_TEXT_SHOWCURRENTLINE, optionTextHightlightCurrent.isSelected());
      getPrefs().putBoolean(OPTION_TEXT_SHOWLINENUMBERS, optionTextLineNumbers.isSelected());
      getPrefs().putBoolean(OPTION_TEXT_SYMBOLWHITESPACE, optionTextShowWhiteSpace.isSelected());
      getPrefs().putBoolean(OPTION_TEXT_SYMBOLEOL, optionTextShowEOL.isSelected());
      getPrefs().putBoolean(OPTION_TEXT_TABSEMULATED, optionTextTabEmulate.isSelected());
      int selectTabSize = getSelectedButtonIndex(selectTextTabSize, 1);
      getPrefs().putInt(OPTION_TEXT_TABSIZE, selectTabSize);
      int selectColorScheme = getSelectedButtonIndex(selectBcsColorScheme, 5);
      getPrefs().putInt(OPTION_BCS_COLORSCHEME, selectColorScheme);
      getPrefs().putBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, optionBCSEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_BCS_CODEFOLDING, optionBCSEnableCodeFolding.isSelected());
      getPrefs().putBoolean(OPTION_BCS_AUTO_INDENT, optionBCSEnableAutoIndent.isSelected());
      selectColorScheme = getSelectedButtonIndex(selectGlslColorScheme, 0);
      getPrefs().putInt(OPTION_GLSL_COLORSCHEME, selectColorScheme);
      selectColorScheme = getSelectedButtonIndex(selectLuaColorScheme, 0);
      getPrefs().putInt(OPTION_LUA_COLORSCHEME, selectColorScheme);
      selectColorScheme = getSelectedButtonIndex(selectSqlColorScheme, 0);
      getPrefs().putInt(OPTION_SQL_COLORSCHEME, selectColorScheme);
      selectColorScheme = getSelectedButtonIndex(selectTlkColorScheme, 0);
      getPrefs().putInt(OPTION_TLK_COLORSCHEME, selectColorScheme);
      selectColorScheme = getSelectedButtonIndex(selectWeiDUColorScheme, 0);
      getPrefs().putInt(OPTION_WEIDU_COLORSCHEME, selectColorScheme);
      getPrefs().putBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, optionGLSLEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_LUA_SYNTAXHIGHLIGHTING, optionLUAEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, optionSQLEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_TLK_SYNTAXHIGHLIGHTING, optionTLKEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_WEIDU_SYNTAXHIGHLIGHTING, optionWeiDUEnableSyntax.isSelected());
      getPrefs().putBoolean(OPTION_GLSL_CODEFOLDING, optionGLSLEnableCodeFolding.isSelected());
      getPrefs().putInt(OPTION_OPTION_FIXED, optionFixedInternal);

      String charset = getSelectedButtonData();
      getPrefs().put(OPTION_TLKCHARSET, charset);

      getPrefs().put(OPTION_LANGUAGE_GAMES, languageDefinition);
    }

    // Returns the (first) index of the selected AbstractButton array
    private int getSelectedButtonIndex(AbstractButton[] items, int defaultIndex)
    {
      int retVal = defaultIndex;
      if (items != null && items.length > 0) {
        for (int i = 0; i < items.length; i++) {
          if (items[i] != null && items[i].isSelected()) {
            retVal = i;
            break;
          }
        }
      }
      return retVal;
    }

    // Extracts entries of Game/Language pairs from the given argument
    private List<Pair<String>> extractGameLanguages(String definition)
    {
      List<Pair<String>> list = new ArrayList<Pair<String>>();
      if (definition != null && !definition.isEmpty()) {
        String[] entries = definition.split(";");
        if (entries != null) {
          for (final String entry: entries) {
            String[] elements = entry.split("=");
            if (elements != null && elements.length == 2) {
              Profile.Game game = Profile.gameFromString(elements[0]);
              if (game != Profile.Game.Unknown) {
                String lang = elements[1].trim();
                Pair<String> pair = null;
                if (lang.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
                  pair = new Pair<String>();
                  pair.setFirst(game.toString());
                  pair.setSecond(LANGUAGE_AUTODETECT);
                } else if (lang.matches("[a-z]{2}_[A-Z]{2}")) {
                  pair = new Pair<String>();
                  pair.setFirst(game.toString());
                  pair.setSecond(lang);
                }

                // check if game/language pair is already in the list
                if (pair != null) {
                  for (final Pair<String> curPair: list) {
                    if (curPair.getFirst().equalsIgnoreCase(pair.getFirst())) {
                      curPair.setSecond(pair.getSecond());
                      pair = null;
                      break;
                    }
                  }
                }

                if (pair != null) {
                  list.add(pair);
                }
              }
            }
          }
        }
      }
      return list;
    }

    // Creates a formatted string out of the Game/Language pairs included in the given list
    private String createGameLanguages(List<Pair<String>> list)
    {
      StringBuilder sb = new StringBuilder();
      if (list != null) {
        for (Iterator<Pair<String>> iter = list.iterator(); iter.hasNext();) {
          Pair<String> pair = iter.next();
          sb.append(String.format("%s=%s", pair.getFirst(), pair.getSecond()));
          if (iter.hasNext()) {
            sb.append(';');
          }
        }
      }
      return sb.toString();
    }

    // Adds or updates the Game/Language pair in the formatted "definition" string
    private String updateGameLanguages(String definition, Pair<String> pair)
    {
      List<Pair<String>> list = extractGameLanguages(definition);
      if (pair != null && pair.getFirst() != null && pair.getSecond() != null) {
        // attempt to update existing entry first
        for (final Pair<String> curPair: list) {
          if (curPair.getFirst().equalsIgnoreCase(pair.getFirst())) {
            curPair.setSecond(pair.getSecond());
            pair = null;
            break;
          }
        }

        // add new entry if necessary
        if (pair != null) {
          list.add(pair);
        }

        return createGameLanguages(list);
      }
      return "";
    }

    // Returns the language definition stored in "definition" for the specified game
    private String getGameLanguage(String definition, Profile.Game game)
    {
      if (game != null && game != Profile.Game.Unknown) {
        List<Pair<String>> list = extractGameLanguages(definition);
        for (Iterator<Pair<String>> iter = list.iterator(); iter.hasNext();) {
          Pair<String> pair = iter.next();
          Profile.Game curGame = Profile.gameFromString(pair.getFirst());
          if (curGame == game) {
            return pair.getSecond();
          }
        }
      }
      return LANGUAGE_AUTODETECT;
    }

    // Returns the currently selected game language. Returns empty string on autodetect.
    private String getSelectedGameLanguage()
    {
      String lang = getGameLanguage(languageDefinition, Profile.getGame());
      return lang.equalsIgnoreCase(LANGUAGE_AUTODETECT) ? "" : lang;
    }


    // Attempts to switch the game language in Enhanced Edition games
    private void switchGameLanguage(String newLanguage)
    {
      if (newLanguage != null) {
        // switch language and refresh resources
        String oldLanguage = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAME);
        String oldLangName = getDisplayLanguage(oldLanguage);
        String newLanguageCode;
        if (newLanguage.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
          // "Autodetect" must be converted into an actual language code before proceeding
          newLanguageCode = ResourceFactory.autodetectGameLanguage(Profile.getProperty(Profile.Key.GET_GAME_INI_FILE));
        } else {
          newLanguageCode = newLanguage;
        }
        String newLangName = getDisplayLanguage(newLanguageCode);
        boolean success = false, showErrorMsg = false;
        if (JOptionPane.showConfirmDialog(NearInfinity.getInstance(),
                                          String.format("Do you want to switch from \"%s\" to \"%s\"?", oldLangName, newLangName),
                                          "Switch game language", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          if (Profile.updateGameLanguage(newLanguageCode)) {
            languageDefinition =
                updateGameLanguages(languageDefinition,
                                    new Pair<String>(Profile.getGame().toString(), newLanguage));
            NearInfinity.getInstance().refreshGame();
            success = true;
          } else {
            showErrorMsg = true;
          }
        }
        if (success == false) {
          if (showErrorMsg == true) {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          "Unable to set new language.",
                                          "Error", JOptionPane.ERROR_MESSAGE);
          }
          for (Iterator<Map.Entry<JRadioButtonMenuItem, String>> iter = gameLanguage.entrySet().iterator();
               iter.hasNext();) {
            Map.Entry<JRadioButtonMenuItem, String> entry = iter.next();
            if (oldLanguage.equalsIgnoreCase(entry.getValue())) {
              JRadioButtonMenuItem rbmi = entry.getKey();
              // don't trigger item event
              rbmi.removeItemListener(this);
              entry.getKey().setSelected(true);
              rbmi.addItemListener(this);
              Profile.updateGameLanguage(oldLanguage);
              break;
            }
          }
        }
      }
    }

    private void applyCustomFont(Font font)
    {
      int index = FONTS.length - 1;
      FONTS[index] = (font != null) ? font : UIManager.getFont("MenuItem.font").deriveFont(Font.PLAIN);
      selectFont[index].setText(String.format("Select font... (%s %d)",
                                              FONTS[index].getName(), FONTS[index].getSize()));
      selectFont[index].setFont(FONTS[index].deriveFont(Misc.getScaledValue(12.0f)));
    }

    // Returns defValue if masked bit is clear or value if masked bit is already set
    private boolean fixOption(int mask, boolean defValue, boolean value)
    {
      boolean retVal = value;
      if ((optionFixedInternal & mask) == 0) {
        retVal = defValue;
        optionFixedInternal |= mask;
      }
      return retVal;
    }

    // Returns defValue if masked bit is clear or value if masked bit is already set
//    private int fixOption(int mask, int defValue, int value)
//    {
//      int retVal = value;
//      if ((optionFixedInternal & mask) == 0) {
//        retVal = defValue;
//        optionFixedInternal |= mask;
//      }
//      return retVal;
//    }

    // Returns defValue if masked bit is clear or value if masked bit is already set
//    private String fixOption(int mask, String defValue, String value)
//    {
//      String retVal = value;
//      if ((optionFixedInternal & mask) == 0) {
//        retVal = defValue;
//        optionFixedInternal |= mask;
//      }
//      return retVal;
//    }

    public int getTextIndentIndex()
    {
      for (int i = 0; i < selectTextTabSize.length; i++) {
        if (selectTextTabSize[i].isSelected()) {
          return i;
        }
      }
      return 1;   // default
    }

    public String getBcsIndent()
    {
      int idx = getSelectedButtonIndex(selectBcsIndent, 2);
      return BCSINDENT[idx][0];
    }

    public String getBcsColorScheme()
    {
      int idx = getSelectedButtonIndex(selectBcsColorScheme, 5);
      return BCSCOLORSCHEME[idx][0];
    }

    public String getGlslColorScheme()
    {
      int idx = getSelectedButtonIndex(selectGlslColorScheme, 0);
      return COLORSCHEME[idx][0];
    }

    public String getLuaColorScheme()
    {
      int idx = getSelectedButtonIndex(selectLuaColorScheme, 0);
      return COLORSCHEME[idx][0];
    }

    public String getSqlColorScheme()
    {
      int idx = getSelectedButtonIndex(selectSqlColorScheme, 0);
      return COLORSCHEME[idx][0];
    }

    public String getTlkColorScheme()
    {
      int idx = getSelectedButtonIndex(selectTlkColorScheme, 0);
      return COLORSCHEME[idx][0];
    }

    public String getWeiDUColorScheme()
    {
      int idx = getSelectedButtonIndex(selectWeiDUColorScheme, 0);
      return COLORSCHEME[idx][0];
    }


    public int getResRefMode()
    {
      if (showResRef[RESREF_ONLY].isSelected())
        return RESREF_ONLY;
      else if (showResRef[RESREF_NAME_REF].isSelected())
        return RESREF_NAME_REF;
      return RESREF_REF_NAME;
    }

    public int getOverrideMode()
    {
      if (showOverrides[OVERRIDE_IN_THREE].isSelected())
        return OVERRIDE_IN_THREE;
      else if (showOverrides[OVERRIDE_IN_OVERRIDE].isSelected())
        return OVERRIDE_IN_OVERRIDE;
      return OVERRIDE_SPLIT;
    }

    public LookAndFeelInfo getLookAndFeel()
    {
      for (int i = 0; i < lookAndFeel.size(); i++) {
        if (lookAndFeel.get(i) != null && lookAndFeel.get(i).isSelected()) {
          return (LookAndFeelInfo)lookAndFeel.get(i).getData();
        }
      }
      return DEFAULT_LOOKFEEL;
    }

    public int getGlobalFontSize()
    {
      return ((Integer)globalFontSize[getSelectedButtonIndex(globalFontSize, 2)].getData()).intValue();
    }

    public int getDefaultStructView()
    {
      if (viewOrEditShown[DEFAULT_VIEW].isSelected())
        return DEFAULT_VIEW;
      return DEFAULT_EDIT;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
//      if (event.getSource() == optionMonitorFileChanges) {
//        if (optionMonitorFileChanges.isSelected()) {
//          FileWatcher.getInstance().start();
//        } else {
//          FileWatcher.getInstance().stop();
//        }
//      } else if (event.getSource() == selectFont[selectFont.length - 1]) {
      if (event.getActionCommand().equals("TextFont")) {
        int index = FONTS.length - 1;
        FontChooser fc = new FontChooser();
        if (FONTS[index] != null) {
          fc.setSelectedFont(FONTS[index]);
        }
        if (fc.showDialog(NearInfinity.getInstance()) == FontChooser.OK_OPTION) {
          applyCustomFont(fc.getSelectedFont());
        }
      }
      else if (event.getActionCommand().equals("ChangeFontSize")) {
        DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem)event.getSource();
        int percent = ((Integer)dmi.getData()).intValue();
        if (dmi == globalFontSize[globalFontSize.length - 1]) {
          if (percent < 0) {
            percent = NearInfinity.getInstance().getGlobalFontSize();
          }
          String ret = JOptionPane.showInputDialog(NearInfinity.getInstance(),
                                                   "Enter font size in percent (50 - 400):",
                                                   Integer.valueOf(percent));
          if (ret == null) {
            dmi.setData(Integer.valueOf(percent));
            dmi.setText("Custom (" + percent + " %)...");
            return;
          }

          int value = NearInfinity.getInstance().getGlobalFontSize();
          try {
            int radix = 10;
            if (ret.toLowerCase().startsWith("0x")) {
              ret = ret.substring(2);
              radix = 16;
            }
            value = Integer.parseInt(ret, radix);
            if (value < 50 || value > 400) {
              JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                            "Number out of range. Using current value " + percent + ".");
              value = NearInfinity.getInstance().getGlobalFontSize();
            }
          } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          "Invalid number entered. Using current value " + percent + ".");
          }
          dmi.setData(Integer.valueOf(value));
          dmi.setText("Custom (" + value + " %)...");
          if (value == NearInfinity.getInstance().getGlobalFontSize()) return;
        }
        if (percent != NearInfinity.getInstance().getGlobalFontSize()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                        "You have to restart Near Infinity\n" +
                                            "for the font size change to take effect.");
        }
      }
      else if (event.getActionCommand().equals("Charset")) {
        DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem)event.getSource();
        String csName = (String)dmi.getData();
        if (csName != null) {
          CharsetDetector.clearCache();
          StringTable.setCharset(charsetName(csName, true));
          // re-read strings
          ActionEvent refresh = new ActionEvent(dmi, 0, "Refresh");
          NearInfinity.getInstance().actionPerformed(refresh);
        }
      }
    }

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      if (event.getStateChange() == ItemEvent.SELECTED &&
          event.getSource() instanceof JRadioButtonMenuItem &&
          gameLanguage.containsKey(event.getSource())) {
        switchGameLanguage(gameLanguage.get(event.getSource()));
      }
    }
  }

  ///////////////////////////////
  // Help Menu
  ///////////////////////////////

  private static final class HelpMenu extends JMenu implements ActionListener
  {
    private static final String wikiUrl = "https://github.com/NearInfinityBrowser/NearInfinity/wiki";

    private final JMenuItem helpAbout, helpWiki, helpLicense,
                            helpJOrbisLicense, helpFifeLicense, helpJHexViewLicense,
                            helpMonteMediaLicense, helpJFontChooserLicense, helpOracleLicense,
                            helpUpdateSettings, helpUpdateCheck;

    private HelpMenu()
    {
      super("Help");
      setMnemonic(KeyEvent.VK_H);

      helpAbout = makeMenuItem("About Near Infinity", KeyEvent.VK_A, Icons.getIcon(Icons.ICON_ABOUT_16), -1, this);
      add(helpAbout);

      helpWiki = makeMenuItem("Near Infinity Wiki", KeyEvent.VK_W, Icons.getIcon(Icons.ICON_HELP_16), -1, this);
      helpWiki.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
      add(helpWiki);

      helpLicense =
          makeMenuItem("Near Infinity License", KeyEvent.VK_N, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      add(helpLicense);

      JMenu miscLicenses = new JMenu("Third-party licenses");
      miscLicenses.setMnemonic(KeyEvent.VK_T);
      add(miscLicenses);

      helpFifeLicense =
          makeMenuItem("Fifesoft License", KeyEvent.VK_F, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpFifeLicense);

      helpJFontChooserLicense =
          makeMenuItem("JFontChooser License", KeyEvent.VK_C, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpJFontChooserLicense);

      helpJHexViewLicense =
          makeMenuItem("JHexView License", KeyEvent.VK_H, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpJHexViewLicense);

      helpJOrbisLicense =
          makeMenuItem("JOrbis License", KeyEvent.VK_J, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpJOrbisLicense);

      helpMonteMediaLicense =
          makeMenuItem("Monte Media License", KeyEvent.VK_M, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpMonteMediaLicense);

      helpOracleLicense =
          makeMenuItem("Oracle License", KeyEvent.VK_O, Icons.getIcon(Icons.ICON_EDIT_16), -1, this);
      miscLicenses.add(helpOracleLicense);

      addSeparator();

      helpUpdateSettings = makeMenuItem("Update settings...", KeyEvent.VK_S, null, -1, this);
      add(helpUpdateSettings);

      helpUpdateCheck = makeMenuItem("Check for updates", KeyEvent.VK_U, Icons.getIcon(Icons.ICON_FIND_16), -1, this);
      add(helpUpdateCheck);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == helpAbout) {
        displayAbout();
      } else if (event.getSource() == helpWiki) {
        UrlBrowser.openUrl(wikiUrl);
      } else if (event.getSource() == helpLicense) {
        displayLicense("org/infinity/License.txt", "LGPL License");
      } else if (event.getSource() == helpJOrbisLicense) {
          displayLicense("org/infinity/JOrbis.License.txt", "LGPL License");
      } else if (event.getSource() == helpFifeLicense) {
        displayLicense("org/infinity/RSyntaxTextArea.License.txt", "BSD License");
      } else if (event.getSource() == helpJHexViewLicense) {
        displayLicense("org/infinity/JHexView.License.txt", "GPL License");
      } else if (event.getSource() == helpMonteMediaLicense) {
        displayLicense("org/infinity/MonteMedia.License.txt", "Creative Commons / LGPL License");
      } else if (event.getSource() == helpJFontChooserLicense) {
        displayLicense("org/infinity/JFontChooser.License.txt", "MIT License");
      } else if (event.getSource() == helpOracleLicense) {
        displayLicense("org/infinity/Oracle.License.txt", "BSD License");
      } else if (event.getSource() == helpUpdateSettings) {
        UpdaterSettings.showDialog(NearInfinity.getInstance());
      } else if (event.getSource() == helpUpdateCheck) {
        UpdateInfo info = null;
        try {
          WindowBlocker.blockWindow(NearInfinity.getInstance(), true);
          info = Updater.getInstance().loadUpdateInfo();
          if (info == null) {
            final String msg = "Unable to find update information.\n" +
                               "Please make sure that your Update Settings have been configured correctly.";
            JOptionPane.showMessageDialog(NearInfinity.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
          }
          if (!Updater.isNewRelease(info.getRelease(), false)) {
            info = null;
          }
        } finally {
          WindowBlocker.blockWindow(NearInfinity.getInstance(), false);
        }
        UpdateCheck.showDialog(NearInfinity.getInstance(), info);
      }
    }

    private void displayAbout()
    {
      // title string
      final String versionString = "Near Infinity " + VERSION;
      // list of current links
      final ObjectString[] currentLinks = {
          new ObjectString("Active branch", "https://github.com/Argent77/NearInfinity/"),
          new ObjectString("Main branch", "https://github.com/NearInfinityBrowser/NearInfinity/"),
          new ObjectString("Wiki page", wikiUrl),
      };
      // original author
      final String originalVersion = "From Near Infinity 1.32.1 beta 24";
      final String originalCopyright = "Copyright (\u00A9) 2001-2005 - Jon Olav Hauglid";
      final ObjectString originalLink = new ObjectString("Website", "http://www.idi.ntnu.no/~joh/ni/");
      // List of contributors (ordered chronologically)
      final String[] contributors = {
          "devSin",
          "FredSRichardson",
          "Taimon",
          "Valerio Bigiani (aka The Bigg)",
          "Fredrik Lindgren (aka Wisp)",
          "Argent77",
      };
      // More contributors, in separate block
      final String[] contributorsMisc = {
          "Near Infinity logo/icon by Cuv and Troodon80",
      };
      // copyright message
      final String[] copyNearInfinityText = {
          "This program is free and may be distributed according to the terms of ",
          "the GNU Lesser General Public License."
      };
      // Third-party copyright messages
      final String[] copyThirdPartyText = {
          "Most icons (\u00A9) eclipse.org - Common Public License.",
          "RSyntaxTextArea (\u00A9) Fifesoft - Berkeley Software Distribution License.",
          "Monte Media Library by Werner Randelshofer - GNU Lesser General Public License.",
          "JOrbis (\u00A9) JCraft Inc. - GNU Lesser General Public License.",
          "JHexView by Sebastian Porst - GNU General Public License.",
      };

      // Fixed elements
      final Font defaultfont = UIManager.getFont("Label.font");
      final Font font = defaultfont.deriveFont(Misc.getScaledValue(13.0f));
      final Font bigFont = defaultfont.deriveFont(Font.BOLD, Misc.getScaledValue(20.0f));
      final Font smallFont = defaultfont.deriveFont(Misc.getScaledValue(11.0f));

      GridBagConstraints gbc = new GridBagConstraints();

      // version
      JLabel lVersion = new JLabel(versionString);
      lVersion.setFont(bigFont);

      JPanel pLinks = new JPanel(new GridBagLayout());
      {
        int row = 0;
        // current links
        for (int i = 0; i < currentLinks.length; i++, row++) {
          int top = (i > 0) ? 4 : 0;
          JLabel lTitle = new JLabel(currentLinks[i].getString() + ":");
          lTitle.setFont(font);
          String link = currentLinks[i].getObject();
          JLabel lLink = ViewerUtil.createUrlLabel(link);
          lLink.setFont(font);
          gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                  GridBagConstraints.HORIZONTAL, new Insets(top, 0, 0, 0), 0, 0);
          pLinks.add(lTitle, gbc);
          gbc = ViewerUtil.setGBC(gbc, 1, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                  GridBagConstraints.HORIZONTAL, new Insets(top, 4, 0, 0), 0, 0);
          pLinks.add(lLink, gbc);
        }

        // original author block
        JLabel label = new JLabel(originalVersion);
        label.setFont(font);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
        pLinks.add(label, gbc);
        row++;
        label = new JLabel(originalCopyright);
        label.setFont(font);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        pLinks.add(label, gbc);
        row++;
        label = new JLabel(originalLink.getString() + ":");
        label.setFont(font);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
        pLinks.add(label, gbc);
        String link = originalLink.getObject();
        label = ViewerUtil.createUrlLabel(link);
        label.setFont(font);
        gbc = ViewerUtil.setGBC(gbc, 1, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
        pLinks.add(label, gbc);
        row++;
      }

      // contributors
      JPanel pContrib = new JPanel(new GridBagLayout());
      {
        // trying to limit line width to a certain maximum
        FontMetrics fm = getFontMetrics(font);
        double maxWidth = 0.0;
        for (int i = 0; i < currentLinks.length; i++) {
          String s = currentLinks[i].getString() + ": " + currentLinks[i].getObject();
          maxWidth = Math.max(maxWidth, fm.getStringBounds(s, getGraphics()).getWidth());
        }

        // adding title
        int row = 0;
        JLabel label = new JLabel("Additional Contributors (in chronological order):");
        label.setFont(smallFont.deriveFont(Misc.getScaledValue(12.0f)));
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0);
        pContrib.add(label, gbc);
        row++;

        // adding names
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contributors.length; i++) {
          if (i > 0) {
            if (i+1 == contributors.length) {
              sb.append(" and ");
            } else {
              sb.append(", ");
            }
          }
          String s = sb.toString() + contributors[i];
          if (fm.getStringBounds(s, getGraphics()).getWidth() > maxWidth) {
            label = new JLabel(sb.toString());
            label.setFont(smallFont);
            gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
            pContrib.add(label, gbc);
            row++;
            sb = new StringBuilder();
          }
          sb.append(contributors[i]);
        }
        label = new JLabel(sb.toString());
        label.setFont(smallFont);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        pContrib.add(label, gbc);
        row++;

        // Adding misc. contributors
        for (int i = 0; i < contributorsMisc.length; i++) {
          label = new JLabel(contributorsMisc[i]);
          label.setFont(smallFont);
          gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                  GridBagConstraints.HORIZONTAL, new Insets(i == 0 ? 4 : 0, 0, 0, 0), 0, 0);
          pContrib.add(label, gbc);
          row++;
        }
      }

      // Near Infinity license
      JPanel pLicense = new JPanel(new GridBagLayout());
      {
        int row = 0;
        JLabel label = new JLabel("Near Infinity license:");
        label.setFont(smallFont.deriveFont(Misc.getScaledValue(12.0f)));
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0);
        pLicense.add(label, gbc);
        row++;

        for (int i = 0; i < copyNearInfinityText.length; i++) {
          label = new JLabel(copyNearInfinityText[i]);
          label.setFont(smallFont);
          gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                  GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
          pLicense.add(label, gbc);
          row++;
        }
      }

      // Additional licenses
      JPanel pMiscLicenses = new JPanel(new GridBagLayout());
      {
        int row = 0;
        JLabel label = new JLabel("Additional licenses:");
        label.setFont(smallFont.deriveFont(Misc.getScaledValue(12.0f)));
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0);
        pMiscLicenses.add(label, gbc);
        row++;

        for (int i = 0; i < copyThirdPartyText.length; i++) {
          label = new JLabel(copyThirdPartyText[i]);
          label.setFont(smallFont);
          gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                  GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
          pMiscLicenses.add(label, gbc);
          row++;
        }
      }

      // putting all together
      JPanel panel = new JPanel(new GridBagLayout());
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      panel.add(lVersion, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
      panel.add(pLinks, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
      panel.add(pContrib, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
      panel.add(pLicense, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
      panel.add(pMiscLicenses, gbc);

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, "About Near Infinity",
                                    JOptionPane.INFORMATION_MESSAGE, Icons.getIcon(Icons.ICON_APP_128));
    }

    private void displayLicense(String classPath, String title)
    {
      JPanel panel = new JPanel(new BorderLayout());
      JTextPane tphelp = new JTextPane();
      tphelp.setFont(new Font("Monospaced", Font.PLAIN, Misc.getScaledValue(12)));
      tphelp.setEditable(false);
      tphelp.setMargin(new Insets(3, 3, 3, 3));
      panel.add(new JScrollPane(tphelp), BorderLayout.CENTER);
      panel.setPreferredSize(Misc.getScaledDimension(new Dimension(640, 480)));

      try {
        tphelp.setPage(ClassLoader.getSystemResource(classPath));
      } catch (IOException e) {
        e.printStackTrace();
      }

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, title,
                                    JOptionPane.PLAIN_MESSAGE);
    }
  }

  // Manages bookmarked game entries
  static final class Bookmark implements Cloneable
  {
    // "Bookmarks" preferences entries (numbers are 1-based)
    private static final String BOOKMARK_NUM_ENTRIES  = "BookmarkEntries";
    private static final String FMT_BOOKMARK_NAME     = "BookmarkName%d";
    private static final String FMT_BOOKMARK_ID       = "BookmarkID%d";
    private static final String FMT_BOOKMARK_PATH     = "BookmarkPath%d";

    private static final String MENUITEM_COMMAND      = "OpenBookmark";

    private final Profile.Game game;
    private final String path;

    private String name;
    private ActionListener listener;
    private JMenuItem item;

    public Bookmark(String name, Profile.Game game, String path, ActionListener listener)
    {
      if (game == null || path == null) {
        throw new NullPointerException();
      }
      if (name == null || name.trim().isEmpty()) {
        name = Profile.getProperty(Profile.Key.GET_GLOBAL_GAME_TITLE, game);
      }
      this.name = name;
      this.game = game;
      this.path = path;
      this.listener = listener;
      updateMenuItem();
    }

    @Override
    public String toString()
    {
      return getName();
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
      return new Bookmark(getName(), getGame(), getPath(), listener);
    }


    /** Returns user-defined game name. */
    public String getName() { return name; }

    /** Sets a new name and returns the previous name (if available). */
    public String setName(String newName)
    {
      String retVal = getName();
      if (newName != null && !newName.trim().isEmpty()) {
        this.name = newName;
        updateMenuItem();
      }
      return retVal;
    }

    /** Returns game type. */
    public Profile.Game getGame() { return game; }

    /** Returns game path (i.e. full path to the chitin.key). */
    public String getPath() { return path; }

    /** Returns associated menu item. */
    public JMenuItem getMenuItem() { return item; }

    /** Returns whether the bookmark points to an existing game installation. */
    public boolean isEnabled() { return (Files.isRegularFile(FileManager.resolve(path))); }

    /** Returns ActionListener used by the associated menu item. */
    public ActionListener getActionListener() { return listener; }

    /** Assigns a new ActionListener object to the associated menu item. */
    public void setActionListener(ActionListener listener)
    {
      if (item != null) {
        item.removeActionListener(this.listener);
      }
      this.listener = listener;
      if (listener != null && item != null) {
        item.addActionListener(this.listener);
      }
    }

    // Creates or updates associated menu item
    private void updateMenuItem()
    {
      if (item == null) {
        item = new JMenuItem(getName());
        item.setToolTipText(path);
        item.setActionCommand(MENUITEM_COMMAND);
        if (listener != null) {
          item.addActionListener(listener);
        }
      } else {
        item.setText(getName());
      }
      item.setEnabled(isEnabled());
    }

    /** Returns the command string used for all menu items. */
    public static String getCommand()
    {
      return MENUITEM_COMMAND;
    }

    /** Returns the Preferences key for the number of available Bookmark entries. */
    public static String getEntryCountKey()
    {
      return BOOKMARK_NUM_ENTRIES;
    }

    /** Returns the Preferences key for a specific BookmarkID. */
    public static String getGameKey(int idx)
    {
      if (idx >= 0) {
        return String.format(FMT_BOOKMARK_ID, idx+1);
      } else {
        return null;
      }
    }

    /** Returns the Preferences key for a specific BookmarkPath. */
    public static String getPathKey(int idx)
    {
      if (idx >= 0) {
        return String.format(FMT_BOOKMARK_PATH, idx+1);
      } else {
        return null;
      }
    }

    /** Returns the Preferences key for a specific BookmarkName. */
    public static String getNameKey(int idx)
    {
      if (idx >= 0) {
        return String.format(FMT_BOOKMARK_NAME, idx+1);
      } else {
        return null;
      }
    }
  }

  // Manages individual "Recently used games" entries
  static final class RecentGame implements Cloneable
  {
    // "Recently opened games" preferences entries (numbers are 1-based)
    private static final int MAX_LASTGAME_ENTRIES = 10;
    private static final String FMT_LASTGAME_IDS  = "LastGameID%d";
    private static final String FMT_LASTGAME_PATH = "LastGamePath%d";

    private static final String MENUITEM_COMMAND  = "OpenOldGame";

    private final Profile.Game game;
    private final String path;

    private JMenuItem item;
    private ActionListener listener;
    private int index;

    public RecentGame(Profile.Game game, String path, int index, ActionListener listener)
    {
      if (game == null || game == Profile.Game.Unknown ||
          path == null || !Files.isRegularFile(FileManager.resolve(path))) {
        throw new NullPointerException();
      }
      this.game = game;
      this.path = path;
      this.index = -1;
      this.listener = listener;
      setIndex(index);
    }

    @Override
    public String toString()
    {
      if (index >= 0) {
        return String.format("%d  %s", index+1,
                             Profile.getProperty(Profile.Key.GET_GLOBAL_GAME_TITLE, game));
      } else {
        return Profile.getProperty(Profile.Key.GET_GLOBAL_GAME_TITLE, game);
      }
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
      return new RecentGame(getGame(), getPath(), getIndex(), getActionListener());
    }

    /** Returns game type. */
    public Profile.Game getGame() { return game; }

    /** Returns game path (i.e. full path to the chitin.key). */
    public String getPath() { return path; }

    /** Returns associated menu item. */
    public JMenuItem getMenuItem() { return item; }

    /** Returns current entry index. */
    public int getIndex() { return index; }

    /** Updates existing menu item or creates a new one, based on the given index. */
    public void setIndex(int index)
    {
      if (index >= 0 && index < getEntryCount() && index != this.index) {
        this.index = index;
        if (item == null) {
          item = new JMenuItem(toString());
          item.setToolTipText(path);
          item.setActionCommand(MENUITEM_COMMAND);
          if (listener != null) {
            item.addActionListener(listener);
          }
        } else {
          item.setText(toString());
        }
      }
    }

    /** Returns ActionListener used by the associated menu item. */
    public ActionListener getActionListener() { return listener; }

    /** Assigns a new ActionListener object to the associated menu item. */
    public void setActionListener(ActionListener listener)
    {
      if (item != null) {
        item.removeActionListener(this.listener);
      }
      this.listener = listener;
      if (listener != null && item != null) {
        item.addActionListener(this.listener);
      }
    }

    /** Removes the currently associated menu item. */
    public void clear()
    {
      if (item != null) {
        if (listener != null) {
          item.removeActionListener(listener);
          item.setEnabled(false);
          if (item.getParent() != null) {
            item.getParent().remove(item);
          }
          item = null;
        }
      }
    }

    /** Returns the command string used for all menu items. */
    public static String getCommand()
    {
      return MENUITEM_COMMAND;
    }

    /** Returns the max. number of supported last game entries. */
    public static int getEntryCount()
    {
      return MAX_LASTGAME_ENTRIES;
    }

    /** Returns the Preferences key for a specific LastGameID. */
    public static String getGameKey(int index)
    {
      if (index >= 0 && index < getEntryCount()) {
        return String.format(FMT_LASTGAME_IDS, index+1);
      } else {
        return null;
      }
    }

    /** Returns the Preferences key for a specific LastGamePath. */
    public static String getPathKey(int index)
    {
      if (index >= 0 && index < getEntryCount()) {
        return String.format(FMT_LASTGAME_PATH, index+1);
      } else {
        return null;
      }
    }
  }
}

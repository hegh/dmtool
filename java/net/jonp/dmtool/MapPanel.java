
package net.jonp.dmtool;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MapPanel
  extends Canvas {
  private static final int SCROLL_DIST = 25;
  private static final int LEFT = 1;
  private static final int RIGHT = 2;
  private static final int UP = 3;
  private static final int DOWN = 4;

  private static final Color PLAYER_MASK_COLOR = new Color(0, 0, 0, 255);
  private static final Color DM_EMPTY_MASK_COLOR = new Color(0, 0, 0, 128);
  private static final Color DM_HIDDEN_MASK_COLOR = new Color(0, 0, 128, 128);

  private static final int HANDLE_SIZE = 6;
  private static final Color HANDLE_COLOR = Color.red;
  private static final Color LOCKED_HANDLE_COLOR = Color.blue;

  private static final Color DEAD_AVATAR_COLOR = new Color(92, 92, 92, 192);

  private static final int OUT_OF_REGION = 0;
  private static final int NW_CORNER = 1;
  private static final int N_EDGE = 2;
  private static final int NE_CORNER = 3;
  private static final int E_EDGE = 4;
  private static final int SE_CORNER = 5;
  private static final int S_EDGE = 6;
  private static final int SW_CORNER = 7;
  private static final int W_EDGE = 8;
  private static final int IN_REGION = 9;
  private static final int NEW_REGION = 10;

  private static final Map<Integer, Integer> cursorMap;
  static {
    cursorMap = new HashMap<>();
    cursorMap.put(OUT_OF_REGION, Cursor.DEFAULT_CURSOR);
    cursorMap.put(NW_CORNER, Cursor.NW_RESIZE_CURSOR);
    cursorMap.put(N_EDGE, Cursor.N_RESIZE_CURSOR);
    cursorMap.put(NE_CORNER, Cursor.NE_RESIZE_CURSOR);
    cursorMap.put(E_EDGE, Cursor.E_RESIZE_CURSOR);
    cursorMap.put(SE_CORNER, Cursor.SE_RESIZE_CURSOR);
    cursorMap.put(S_EDGE, Cursor.S_RESIZE_CURSOR);
    cursorMap.put(SW_CORNER, Cursor.SW_RESIZE_CURSOR);
    cursorMap.put(W_EDGE, Cursor.W_RESIZE_CURSOR);
    cursorMap.put(IN_REGION, Cursor.MOVE_CURSOR);
    cursorMap.put(NEW_REGION, Cursor.CROSSHAIR_CURSOR);
  }

  private class Corners {
    final int left, right, top, bottom;
    final int width, height;
    final int midX, midY;

    final int unscaledLeft, unscaledRight, unscaledTop, unscaledBottom;
    final int unscaledWidth, unscaledHeight;

    public Corners(final Region r) {
      final double scale = dmtool.getScale(isPlayer);
      final double invScale = 1.0 / scale;
      double bx = 0;
      double by = 0;
      double bw = 0;
      double bh = 0;
      if (dragging && r == activeRegion) {
        // Mouse coordinates are in a scaled image, so reverse the scale first.
        final int dx = mx - sx;
        final int dy = my - sy;
        bx = invScale * xm * dx;
        bw = invScale * wm * dx;
        by = invScale * ym * dy;
        bh = invScale * hm * dy;
      }

      // Compute raw, unscaled corners of the region, and normalize to positive
      // width/height.
      final double rWidth = Math.abs(r.getW() + bw);
      final double rHeight = Math.abs(r.getH() + bh);
      final double rLeft = Math.min(r.getX() + bx, r.getX() + bx + r.getW() + bw);
      final double rRight = Math.max(r.getX() + bx, r.getX() + bx + r.getW() + bw);
      final double rTop = Math.min(r.getY() + by, r.getY() + by + r.getH() + bh);
      final double rBottom = Math.max(r.getY() + by, r.getY() + by + r.getH() + bh);

      unscaledLeft = (int)rLeft;
      unscaledRight = (int)rRight;
      unscaledTop = (int)rTop;
      unscaledBottom = (int)rBottom;
      unscaledWidth = (int)rWidth;
      unscaledHeight = (int)rHeight;

      final Point off = dmtool.getOffset(isPlayer);
      left = (int)(off.x + scale * rLeft);
      right = (int)(off.x + scale * rRight);
      top = (int)(off.y + scale * rTop);
      bottom = (int)(off.y + scale * rBottom);
      width = right - left;
      height = bottom - top;
      midX = (left + right) / 2;
      midY = (top + bottom) / 2;
    }

    public boolean contains(final int x, final int y) {
      return left <= x && x <= right && top <= y && y <= bottom;
    }
  }

  final DMTool dmtool;
  final Window parentWindow;
  final boolean isPlayer;

  final Color emptyMaskColor; // When there is no region.
  final Color hiddenMaskColor; // When the region is hidden.

  // Updated when the map changes.
  int imgWidth = 1;
  int imgHeight = 1;

  int mx, my; // Last known mouse position.
  int mouseStatus = OUT_OF_REGION;

  // If true, activeRegion is being created.
  // If activeRegion is null, it will be created on mouse-down.
  boolean newRegion = false;
  RegionGroup newRegionParent = null;

  Region activeRegion; // If in a region.
  int sx, sy; // Starting mouse coordinates for a resize or drag operation.
  int xm, ym, wm, hm; // Multipliers for delta x/y/w/h of region when dragging.
  int lw, lh; // Last avatar size. Used when creating a new avatar.
  boolean dragging = false;

  void setMultipliers(final int xm, final int wm, final int ym, final int hm) {
    this.xm = xm;
    this.wm = wm;
    this.ym = ym;
    this.hm = hm;
  }

  public MapPanel(final DMTool dmtool, final Window parentWindow, final boolean isPlayer) {
    this.dmtool = dmtool;
    this.parentWindow = parentWindow;
    this.isPlayer = isPlayer;

    if (isPlayer) {
      emptyMaskColor = PLAYER_MASK_COLOR;
      hiddenMaskColor = PLAYER_MASK_COLOR;
    }
    else {
      emptyMaskColor = DM_EMPTY_MASK_COLOR;
      hiddenMaskColor = DM_HIDDEN_MASK_COLOR;
    }

    SwingUtilities.invokeLater(() -> {
      createBufferStrategy(2);
      dmtool.addNewMapListener(() -> {
        rescale();
        repaint();
      });

      if (isPlayer) {
        dmtool.addResumeListener(() -> {
          rescale();
          repaint();
        });
        return;
      }

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
          if (!dragging) {
            return;
          }

          // Use Ceil so if the user saw even a small change while dragging, it
          // will appear as a change instead of as an ignored drag.
          final double invScale = 1.0 / dmtool.getScale(isPlayer);
          final double dx = Math.ceil(invScale * (mx - sx));
          final double dy = Math.ceil(invScale * (my - sy));
          dragging = false;
          activeRegion.adjustDims((int)(xm * dx), (int)(ym * dy), (int)(wm * dx), (int)(hm * dy));
          if (activeRegion.isAvatar) {
            lw = activeRegion.w;
            lh = activeRegion.h;
          }
          activeRegion.fontSize = null;
          if (newRegion) {
            int parentID = 0;
            if (newRegionParent != null) {
              parentID = newRegionParent.id;
            }
            activeRegion = dmtool.getRegions(isPlayer)
              .addRegion(parentID, activeRegion.x, activeRegion.y, activeRegion.w, activeRegion.h);
            newRegion = false;
          }
          dmtool.repaint();
        }

        @Override
        public void mousePressed(final MouseEvent e) {
          System.err.println("Mouse pressed, button " + e.getButton() + " modifiers " +
                             InputEvent.getModifiersExText(e.getModifiersEx()));
          if (e.getButton() == 4) { // Wheel tilt left.
            scroll(LEFT, 1);
            return;
          }
          if (e.getButton() == 5) { // Wheel tilt right.
            scroll(RIGHT, 1);
            return;
          }
          if (dragging && e.getButton() == 3) {
            dragging = false;
            newRegion = false;
            detectMouseOverRegion();
            repaint();
            return;
          }
          if (activeRegion == null && newRegion && e.getButton() == 1) {
            // Create a new region.
            final Point mouse = windowToImageCoords(mx, my);
            activeRegion = new Region(newRegionParent, mouse.x, mouse.y, 0, 0);
            sx = mx;
            sy = my;
            dragging = true;
            mouseStatus = SE_CORNER;
            setCursor(Cursor.getPredefinedCursor(cursorMap.get(mouseStatus)));
            setMultipliers(0, 1, 0, 1);
            return;
          }
          if (activeRegion != null && e.getButton() == 1) {
            sx = mx;
            sy = my;
            dragging = true;

            // Multipliers set how (mx - sx) and (my - sy) are applied to the x,
            // y, w, and h of the active region.
            switch (mouseStatus) {
              case NW_CORNER:
                setMultipliers(1, -1, 1, -1);
                break;
              case N_EDGE:
                setMultipliers(0, 0, 1, -1);
                break;
              case NE_CORNER:
                setMultipliers(0, 1, 1, -1);
                break;
              case E_EDGE:
                setMultipliers(0, 1, 0, 0);
                break;
              case SE_CORNER:
                setMultipliers(0, 1, 0, 1);
                break;
              case S_EDGE:
                setMultipliers(0, 0, 0, 1);
                break;
              case SW_CORNER:
                setMultipliers(1, -1, 0, 1);
                break;
              case W_EDGE:
                setMultipliers(1, -1, 0, 0);
                break;
              case IN_REGION:
                setMultipliers(1, 0, 1, 0);
                break;
              default:
                System.err.println("Should not get to MousePressed when out-of-region.");
                System.exit(1);
            }
          }
        }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(final MouseEvent e) {
          if (dragging) {
            mx = e.getX();
            my = e.getY();
            activeRegion.fontSize = null;
            repaint();
          }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
          mx = e.getX();
          my = e.getY();
          detectMouseOverRegion();
        }
      });

      addMouseWheelListener((final MouseWheelEvent e) -> {
        System.err.println("Mouse wheel " + e.getWheelRotation() + " with modifiers " +
                           InputEvent.getModifiersExText(e.getModifiersEx()) +
                           " with param string \"" + e.paramString() + "\"");
        if (e.getModifiersEx() == 0) {
          scroll(DOWN, e.getWheelRotation());
        }
        else if (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
          // Zoom in such that the same point remains under the mouse cursor
          // before/after zooming.
          final Point off = dmtool.getOffset(isPlayer);
          double scale = dmtool.getScale(isPlayer);
          final double invScale = 1.0 / scale;

          // Calculate mouse point in image coordinates.
          final double ix = (mx - off.x) * invScale;
          final double iy = (my - off.y) * invScale;

          scale -= scale * 0.05 * e.getWheelRotation();
          final int nox = (int)(mx - ix * scale);
          final int noy = (int)(my - iy * scale);
          dmtool.setScale(scale);
          dmtool.setOffset(new Point(nox, noy));
        }
        else if (e.getModifiersEx() == InputEvent.SHIFT_DOWN_MASK) {
          scroll(RIGHT, e.getWheelRotation());
        }
        else {
          return;
        }
        dmtool.repaint();
      });

      addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
          System.err.println("Key pressed " + e.getKeyCode() + " (" + e.getKeyChar() +
                             ") modifiers " + InputEvent.getModifiersExText(e.getModifiersEx()));
          if (e.getModifiersEx() == 0) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_R:
                newRegionCommand(/* isSibling = */ false);
                break;
              case KeyEvent.VK_A:
                newAvatarCommand();
                break;
              case KeyEvent.VK_D:
                duplicateRegionCommand(/* isSibling = */ false);
                break;
              case KeyEvent.VK_SPACE:
                toggleRegionStateCommand();
                break;
              case KeyEvent.VK_DELETE:
              case KeyEvent.VK_BACK_SPACE:
                deleteRegionCommand();
                break;
              case KeyEvent.VK_ESCAPE:
                cancelNewRegionCommand();
                break;
              case KeyEvent.VK_Q:
                togglePauseCommand();
                break;
              case KeyEvent.VK_LEFT:
                scroll(LEFT, 1);
                break;
              case KeyEvent.VK_RIGHT:
                scroll(RIGHT, 1);
                break;
              case KeyEvent.VK_UP:
                scroll(UP, 1);
                break;
              case KeyEvent.VK_DOWN:
                scroll(DOWN, 1);
                break;
            }
          }
          else if (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_Q:
                quitCommand();
                break;
              case KeyEvent.VK_N:
                newMapCommand();
                break;
              case KeyEvent.VK_S:
                saveCommand();
                break;
              case KeyEvent.VK_O:
                openCommand();
                break;
            }
          }
          else if (e.getModifiersEx() == InputEvent.SHIFT_DOWN_MASK) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_R:
                newRegionCommand(/* isSibling = */ true);
                break;
              case KeyEvent.VK_D:
                duplicateRegionCommand(/* isSibling = */ true);
                break;
            }
          }
          else if (e.getModifiersEx() == (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_S:
                saveAsCommand();
                break;
            }
          }
        }
      });
    });
  }

  private void togglePauseCommand() {
    dmtool.togglePause();
    repaint();
  }

  private void newMapCommand() {
    final JFileChooser chooser = new JFileChooser(dmtool.getDirectory());
    final FileNameExtensionFilter filter =
      new FileNameExtensionFilter("Supported Images", ImageIO.getReaderFileSuffixes());
    chooser.setFileFilter(filter);
    final int result = chooser.showOpenDialog(parentWindow);
    dmtool.setDirectory(chooser.getCurrentDirectory());
    if (result != JFileChooser.APPROVE_OPTION) {
      System.err.println("Chose dis-approval option " + result);
      return;
    }
    try {
      dmtool.newMap(chooser.getSelectedFile());
      System.err.println("Loaded image \"" + chooser.getSelectedFile() + "\"");
    }
    catch (final IOException e) {
      JOptionPane.showMessageDialog(parentWindow, "Failed to load image: " + e.getMessage(),
                                    "Load Error", JOptionPane.ERROR_MESSAGE);
      System.err.println("Failed to load image from \"" + chooser.getSelectedFile() + "\"");
      e.printStackTrace();
      return;
    }
  }

  private void saveCommand() {
    if (dmtool.getActiveSave() == null) {
      saveAsCommand();
      return;
    }
    try {
      dmtool.save(dmtool.getActiveSave());
      System.err.println("Saved to \"" + dmtool.getActiveSave() + "\"");
    }
    catch (final IOException e) {
      JOptionPane.showMessageDialog(parentWindow, "Failed to save file: " + e.getMessage(),
                                    "Save Error", JOptionPane.ERROR_MESSAGE);
      System.err.println("Failed to write save file \"" + dmtool.getActiveSave() + "\"");
      e.printStackTrace();
    }
  }

  private void saveAsCommand() {
    final JFileChooser chooser = new JFileChooser(dmtool.getDirectory());
    final FileNameExtensionFilter filter =
      new FileNameExtensionFilter("Saved Maps", DMTool.SAVE_FILE_EXTENSION);
    chooser.setFileFilter(filter);
    chooser.setSelectedFile(dmtool.getActiveSave());
    final int result = chooser.showSaveDialog(parentWindow);
    dmtool.setDirectory(chooser.getCurrentDirectory());
    if (result != JFileChooser.APPROVE_OPTION) {
      System.err.println("Chose dis-approval option " + result);
      return;
    }

    File file = chooser.getSelectedFile();
    if (!file.getName().endsWith(".dmap")) {
      file = new File(file.getPath() + ".dmap");
    }
    dmtool.setActiveSave(file);
    saveCommand();
  }

  private void openCommand() {
    final JFileChooser chooser = new JFileChooser(dmtool.getDirectory());
    final FileNameExtensionFilter filter =
      new FileNameExtensionFilter("Saved Maps", DMTool.SAVE_FILE_EXTENSION);
    chooser.setFileFilter(filter);
    final int result = chooser.showOpenDialog(parentWindow);
    dmtool.setDirectory(chooser.getCurrentDirectory());
    if (result != JFileChooser.APPROVE_OPTION) {
      System.err.println("Chose dis-approval option " + result);
      return;
    }
    try {
      dmtool.open(chooser.getSelectedFile());
      System.err.println("Loaded \"" + chooser.getSelectedFile() + "\"");
    }
    catch (final IOException e) {
      JOptionPane.showMessageDialog(parentWindow, "Failed to load file: " + e.getMessage(),
                                    "Open Error", JOptionPane.ERROR_MESSAGE);
      System.err.println("Failed to read save file \"" + chooser.getSelectedFile() + "\"");
      e.printStackTrace();
    }
  }

  private void quitCommand() {
    dmtool.quit();
  }

  private void newRegionCommand(final boolean isSibling) {
    if (isSibling && activeRegion != null && !activeRegion.isAvatar) {
      newRegionParent = activeRegion.parent;
    }
    else {
      newRegionParent = null;
    }
    activeRegion = null;
    newRegion = true;
    mouseStatus = NEW_REGION;
    setCursor(Cursor.getPredefinedCursor(cursorMap.get(mouseStatus)));
  }

  private void cancelNewRegionCommand() {
    dragging = false;
    newRegion = false;
    detectMouseOverRegion();
    repaint();
  }

  private void newAvatarCommand() {
    final NewAvatarDialog.AvatarSelectionResult result = NewAvatarDialog.showDialog(parentWindow);
    if (result != null) {
      final Point mouse = windowToImageCoords(mx, my);
      // Disallow creation size < 5x5 to avoid something too small to see.
      if (lw <= 5) {
        lw = 40;
      }
      if (lh <= 5) {
        lh = 40;
      }
      final Region r = dmtool.getRegions(isPlayer).addRegion(0, mouse.x, mouse.y, lw, lh);
      r.isAvatar = true;
      r.symbol = result.symbol;
      r.color = result.color;
    }
    dmtool.repaint();
  }

  private void deleteRegionCommand() {
    if (activeRegion != null) {
      dmtool.getRegions(isPlayer).removeRegion(activeRegion);
      activeRegion = null;
      dmtool.repaint();
    }
  }

  private void duplicateRegionCommand(final boolean isSibling) {
    if (activeRegion == null) {
      return;
    }

    final Region r = dmtool.getRegions(isPlayer).duplicate(activeRegion);
    if (!isSibling || r.isAvatar) { // Disallow avatar siblings.
      dmtool.getRegions(isPlayer).deparent(r);
    }
    detectMouseOverRegion();
    dmtool.repaint();
  }

  private void toggleRegionStateCommand() {
    if (activeRegion != null) {
      activeRegion.toggleState();
      dmtool.repaint();
    }
  }

  private void detectMouseOverRegion() {
    if (newRegion && activeRegion == null) {
      mouseStatus = NEW_REGION;
      return;
    }

    final Region r = regionAt(mx, my);
    if (r != null) {
      activeRegion = r;
      mouseStatus = determineMouseStatus(activeRegion);
    }
    else {
      mouseStatus = OUT_OF_REGION;
      activeRegion = null;
    }
    setCursor(Cursor.getPredefinedCursor(cursorMap.get(mouseStatus)));
    repaint();
  }

  private Region regionAt(final int x, final int y) {
    // Pick the most recently created region, preferring avatars over
    // non-avatars.
    Region avatar = null;
    Region nonAvatar = null;
    final double scale = dmtool.getScale(isPlayer);
    for (final RegionGroup group : dmtool.getRegions(isPlayer).getGroups()) {
      for (final Region r : group.getChildren()) {
        if (new Corners(r).contains(mx, my)) {
          if (r.isAvatar) {
            avatar = r;
          }
          else {
            nonAvatar = r;
          }
        }
      }
    }
    if (avatar != null) {
      return avatar;
    }
    return nonAvatar;
  }

  Point windowToImageCoords(int x, int y) {
    final Point off = dmtool.getOffset(isPlayer);
    x -= off.x;
    y -= off.y;

    final double invScale = 1.0 / dmtool.getScale(isPlayer);
    x = (int)(x * invScale);
    y = (int)(y * invScale);
    return new Point(x, y);
  }

  void scroll(final int direction, final int value) {
    int xm, ym;
    switch (direction) {
      case LEFT:
        xm = 1;
        ym = 0;
        break;
      case RIGHT:
        xm = -1;
        ym = 0;
        break;
      case UP:
        xm = 0;
        ym = 1;
        break;
      case DOWN:
        xm = 0;
        ym = -1;
        break;
      default:
        throw new IllegalArgumentException("Unknown scroll direction: " + direction);
    }
    final Point noff = new Point(dmtool.getOffset(isPlayer));
    noff.x += SCROLL_DIST * xm * value;
    noff.y += SCROLL_DIST * ym * value;
    dmtool.setOffset(noff);
    dmtool.repaint();
  }

  private void rescale() {
    if (dmtool.getImage(isPlayer) == null) {
      imgWidth = 1;
      imgHeight = 1;
      if (!isPlayer) {
        dmtool.setScale(1.0);
      }
      return;
    }

    imgWidth = dmtool.getImage(isPlayer).getWidth(this);
    imgHeight = dmtool.getImage(isPlayer).getHeight(this);

    if (isPlayer) {
      return;
    }

    final int w = getWidth();
    final int h = getHeight();
    double scale = 1.0;
    if (imgWidth > w || imgHeight > h) {
      // Too large, scale down.
      if (imgWidth > w) {
        scale = (double)w / (double)imgWidth;
      }
      if (scale * imgHeight > h) {
        // Still too big, scale down more.
        scale = (double)h / (double)imgHeight;
      }
    }
    else if (imgWidth < w && imgHeight < h) {
      // Too small, scale up until it matches at least one dimension.
      scale = (double)w / (double)imgWidth;
      if (scale * imgHeight > h) {
        // Scaled up too much, use the other dimension.
        scale = (double)h / (double)imgHeight;
      }
    }
    dmtool.setScale(scale);
  }

  public void drawAvatar(final Graphics2D g, final Region r) {
    final Corners c = new Corners(r);
    final String symbol = Character.toString(r.symbol);
    Color color = r.color;
    if (r.isDead) {
      color = DEAD_AVATAR_COLOR;
    }

    // Thick border.
    g.setColor(color);
    g.drawRect(c.left, c.top, c.width - 1, c.height - 1);
    g.drawRect(c.left + 1, c.top + 1, c.width - 3, c.height - 3);

    // Calculate & cache the font when necessary.
    int trySize = (Math.min(c.width, c.height));
    int lastChange = 0;
    if (r.lastZoomLevel != dmtool.getScale(isPlayer)) {
      r.fontSize = null;
      r.lastZoomLevel = dmtool.getScale(isPlayer);
    }
    while (r.fontSize == null && trySize > 1) {
      g.setFont(new Font(null, 0, trySize));
      final FontMetrics fontMetrics = g.getFontMetrics();
      final Rectangle2D bounds = fontMetrics.getStringBounds(symbol, g);
      if (bounds.getWidth() > c.width || bounds.getHeight() > c.height) {
        // Too big, try the next size down.
        trySize--;
        lastChange = -1;
        continue;
      }
      if (bounds.getWidth() < c.width && bounds.getHeight() < c.height) {
        // Too small. Unless we just shrunk to this size because it was too
        // big, try the next size up.
        if (lastChange == -1) {
          r.fontSize = trySize;
          break;
        }
        trySize++;
        lastChange = 1;
        continue;
      }

      // One dimension must be equal, so don't change any more.
      r.fontSize = trySize;
      break;
    }
    if (r.fontSize == null) {
      // If unable to choose a size, just use 1.
      r.fontSize = 1;
    }

    // Center the symbol in the region, adjusting for descent.
    g.setFont(new Font(null, 0, r.fontSize));
    final FontMetrics fontMetrics = g.getFontMetrics();
    final LineMetrics lineMetrics = fontMetrics.getLineMetrics(symbol, g);
    final Rectangle2D bounds = fontMetrics.getStringBounds(symbol, g);
    final double x = c.left + (c.width - bounds.getWidth()) / 2;
    final double y = c.bottom - lineMetrics.getDescent() - (c.height - bounds.getHeight()) / 2;
    g.drawString(Character.toString(r.symbol), (int)x, (int)y);

    if (r.isDead) {
      // Draw an X. Tried drawing a skull glyph, but it disappears below
      // some size threshold on MacOS.
      g.drawLine(c.left, c.top, c.right, c.bottom);
      g.drawLine(c.left, c.bottom, c.right, c.top);
    }
  }

  public void drawAvatars(final Graphics2D g) {
    // Draw dead avatars, then live.
    final Collection<Region> deadAvatars = new ArrayList<>();
    final Collection<Region> liveAvatars = new ArrayList<>();
    for (final RegionGroup group : dmtool.getRegions(isPlayer).getGroups()) {
      for (final Region r : group.getChildren()) {
        if (!r.isAvatar) {
          continue;
        }
        if (r.isDead) {
          deadAvatars.add(r);
        }
        else {
          liveAvatars.add(r);
        }
      }
    }

    for (final Region r : deadAvatars) {
      drawAvatar(g, r);
    }
    for (final Region r : liveAvatars) {
      drawAvatar(g, r);
    }
  }

  // Hides/shades areas of the screen that are not being shared.
  public Image drawMask(final Rectangle bounds) {
    // Need to use a new image so we can black the entire thing, then make
    // transparent windows in it to see through.
    final BufferedImage img =
      new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();

    // Black out the entire image.
    g.setComposite(AlphaComposite.Src);
    g.setColor(emptyMaskColor);
    g.fillRect(0, 0, bounds.width, bounds.height);

    final ArrayList<RegionGroup> drawOrder =
      new ArrayList<>(dmtool.getRegions(isPlayer).getGroups().size());
    for (final RegionGroup group : dmtool.getRegions(isPlayer).getGroups()) {
      if (!group.isVisible() && isPlayer) {
        // Don't make hidden regions into player windows.
        continue;
      }
      drawOrder.add(group);
    }
    if (!isPlayer) {
      // Player sees everything the same; DM needs transparent windows drawn
      // after translucent masks.
      drawOrder.sort((final RegionGroup a, final RegionGroup b) -> {
        // Draw the hidden regions first.
        if (!a.isVisible() && b.isVisible()) {
          return -1;
        }
        if (a.isVisible() && !b.isVisible()) {
          return 1;
        }
        return 0;
      });
    }
    for (final RegionGroup group : drawOrder) {
      for (final Region r : group.getChildren()) {
        if (r.isAvatar) {
          // These are drawn elsewhere.
          continue;
        }
        if (r.isVisible()) {
          // Make visible regions transparent.
          g.setColor(new Color(0, 0, 0, 0));
        }
        else {
          g.setColor(hiddenMaskColor);
        }

        final Corners c = new Corners(r);
        g.fillRect(c.left, c.top, c.width, c.height);
      }
    }
    if (newRegion && activeRegion != null) {
      g.setColor(hiddenMaskColor);
      final Corners c = new Corners(activeRegion);
      g.fillRect(c.left, c.top, c.width, c.height);
    }
    g.dispose();
    return img;
  }

  @Override
  public void paint(final Graphics og) {
    final Image img = dmtool.getImage(isPlayer);
    final Rectangle b = og.getClipBounds();
    if (img == null) {
      og.setColor(Color.black);
      og.fillRect(b.x, b.y, b.width, b.height);
      return;
    }

    final Point off = dmtool.getOffset(isPlayer);
    final double scale = dmtool.getScale(isPlayer);
    final AffineTransform transform = new AffineTransform();
    transform.translate(off.x, off.y);
    transform.scale(scale, scale);
    final Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      g.setColor(Color.black); // Black mat in case the image is small.
      g.fillRect(0, 0, b.width, b.height);
      g.drawImage(img, transform, this);

      g.setComposite(AlphaComposite.SrcOver);
      drawAvatars(g);
      g.drawImage(drawMask(b), null, this);

      if (isPlayer) {
        return;
      }

      if (activeRegion != null) {
        if (activeRegion.parent != null) {
          for (final Region r : activeRegion.parent.getChildren()) {
            if (r == activeRegion) {
              continue;
            }
            drawCorners(g, LOCKED_HANDLE_COLOR, r);
          }
        }
        drawCorners(g, HANDLE_COLOR, activeRegion);
        drawControls(g, activeRegion);
      }

      if (dmtool.isPaused()) {
        // Rotate slowly between red, white, red, black, ...
        final int t = (int)(System.currentTimeMillis() / 3 % 1024);
        int red;
        int gb;
        if (t < 256) { // 0 - 255
          // Move from black to red.
          red = t; // 0-255
          gb = 0;
        }
        else if (t < 512) { // 256 - 511
          // Move from red to white.
          red = 255;
          gb = t - 256; // 0 - 255
        }
        else if (t < 768) { // 512 - 768
          // Move from white to red.
          red = 255;
          gb = 255 - (t - 512); // 255 - (0 - 255) = 0 - 255
        }
        else { // 768 - 1023
          // Move from red to black.
          red = 255 - (t - 768); // 255 - (0 - 255) = 0 - 255
          gb = 0;
        }

        g.setColor(new Color(red, gb, gb, 128));
        g.setFont(new Font(null, 0, 50));
        g.drawString("PAUSED", 25, 50);
      }
    }
    finally {
      g.dispose();
      getBufferStrategy().show();
    }
  }

  @Override
  public void update(final Graphics g) {
    paint(g);
  }

  private void drawCorners(final Graphics2D g, final Color color, final Region r) {
    final Corners c = new Corners(r);
    final int hs = HANDLE_SIZE;
    final int hhs = HANDLE_SIZE / 2;
    drawHandle(g, color, c.left, c.top); // Upper-left.
    drawHandle(g, color, c.right - hs, c.top); // Upper-right.
    drawHandle(g, color, c.left, c.bottom - hs); // Lower-left.
    drawHandle(g, color, c.right - hs, c.bottom - hs); // Lower-right.
    drawHandle(g, color, c.midX - hhs, c.top); // Top.
    drawHandle(g, color, c.midX - hhs, c.bottom - hs); // Bottom.
    drawHandle(g, color, c.left, c.midY - hhs); // Left.
    drawHandle(g, color, c.right - hs, c.midY - hhs); // Right.
  }

  private int determineMouseStatus(final Region r) {
    final Corners c = new Corners(r);
    final double min = HANDLE_SIZE * 2;
    if (mouseDist(c.left, c.top) <= min) { // Upper-left.
      return NW_CORNER;
    }
    if (mouseDist(c.right, c.top) <= min) { // Upper-right.
      return NE_CORNER;
    }
    if (mouseDist(c.left, c.bottom) <= min) { // Lower-left.
      return SW_CORNER;
    }
    if (mouseDist(c.right, c.bottom) <= min) { // Lower-right.
      return SE_CORNER;
    }
    if (mouseDist(c.left + c.width / 2, c.top) <= min) { // Top.
      return N_EDGE;
    }
    if (mouseDist(c.left + c.width / 2, c.bottom) <= min) { // Bottom.
      return S_EDGE;
    }
    if (mouseDist(c.left, c.top + c.height / 2) <= min) { // Left.
      return W_EDGE;
    }
    if (mouseDist(c.right, c.top + c.height / 2) <= min) { // Right.
      return E_EDGE;
    }
    return IN_REGION;
  }

  private double mouseDist(final int x, final int y) {
    final double dx = x - mx;
    final double dy = y - my;
    return Math.sqrt(dx * dx + dy * dy);
  }

  private void drawHandle(final Graphics2D g, final Color color, final int x, final int y) {
    g.setColor(color);
    g.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
  }

  private void drawControls(final Graphics2D g, final Region r) {
    // TODO: Draw controls in the region.
  }
}

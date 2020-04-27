
package dmtool;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MapPanel
  extends Canvas {
  private static final String SKULL = "\u2620";

  private static final int SCROLL_DIST = 25;
  private static final int LEFT = 1;
  private static final int RIGHT = 2;
  private static final int UP = 3;
  private static final int DOWN = 4;

  private static final Color PLAYER_MASK_COLOR = new Color(0, 0, 0, 255);
  private static final Color DM_EMPTY_MASK_COLOR = new Color(0, 0, 0, 128);
  private static final Color DM_HIDDEN_MASK_COLOR = new Color(0, 0, 128, 128);

  private static final int HANDLE_SIZE = 6;

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
      // TODO: If moving, move all regions with the same parent.
      // Shortcuts.
      final double scale = parent.getScale(isPlayer);
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
      final Point off = parent.getOffset(isPlayer);
      left = (int)(scale * (r.getX() + bx) + off.x);
      right = (int)(scale * (r.getX() + r.w + bx + bw) + off.x);
      top = (int)(scale * (r.getY() + by) + off.y);
      bottom = (int)(scale * (r.getY() + r.h + by + bh) + off.y);
      width = right - left;
      height = bottom - top;
      midX = (int)(scale * (r.getX() + bx + (r.w + bw) / 2) + off.x);
      midY = (int)(scale * (r.getY() + by + (r.h + bh) / 2) + off.y);

      unscaledLeft = r.getX() + (int)bx;
      unscaledRight = r.getX() + (int)bx + (int)(width * invScale);
      unscaledTop = r.getY() + (int)by;
      unscaledBottom = r.getY() + (int)by + (int)(height * invScale);
      unscaledWidth = (int)(width * invScale);
      unscaledHeight = (int)(height * invScale);
    }

    public boolean contains(final int x, final int y) {
      return left <= x && x <= right && top <= y && y <= bottom;
    }
  }

  final DMTool parent;
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

  Region activeRegion; // If in a region.
  int sx, sy; // Starting mouse coordinates for a resize or drag operation.
  int xm, ym, wm, hm; // Multipliers for delta x/y/w/h of region when dragging.
  boolean dragging = false;

  void setMultipliers(final int xm, final int wm, final int ym, final int hm) {
    this.xm = xm;
    this.wm = wm;
    this.ym = ym;
    this.hm = hm;
  }

  public MapPanel(final DMTool parent, final Window parentWindow, final boolean isPlayer) {
    this.parent = parent;
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
    });
    parent.addNewMapListener(() -> {
      rescale();
      repaint();
    });

    if (isPlayer) {
      parent.addResumeListener(() -> {
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

        // TODO: If moving, move parent instead of region.
        final int dx = mx - sx;
        final int dy = my - sy;
        final double invScale = 1.0 / parent.getScale(isPlayer);
        dragging = false;
        activeRegion.x += (int)(invScale * xm * dx);
        activeRegion.w += (int)(invScale * wm * dx);
        activeRegion.y += (int)(invScale * ym * dy);
        activeRegion.h += (int)(invScale * hm * dy);
        if (newRegion) {
          activeRegion = parent.getRegions(isPlayer).addRegion(0, activeRegion.x, activeRegion.y,
                                                               activeRegion.w, activeRegion.h);
          newRegion = false;
        }
        parent.repaint();
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
          activeRegion = new Region(null, mouse.x, mouse.y, 0, 0);
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
      // TODO: Test whether scrolling/zooming while dragging works.
      if (e.getModifiersEx() == 0) {
        scroll(DOWN, e.getWheelRotation());
      }
      else if (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
        // Zoom in such that the same point remains under the mouse cursor
        // before/after zooming.
        final Point off = parent.getOffset(isPlayer);
        double scale = parent.getScale(isPlayer);
        final double invScale = 1.0 / scale;

        // Calculate mouse point in image coordinates.
        final double ix = (mx - off.x) * invScale;
        final double iy = (my - off.y) * invScale;

        scale -= scale * 0.05 * e.getWheelRotation();
        final int nox = (int)(mx - ix * scale);
        final int noy = (int)(my - iy * scale);
        parent.setScale(scale);
        parent.setOffset(new Point(nox, noy));
      }
      else if (e.getModifiersEx() == InputEvent.SHIFT_DOWN_MASK) {
        scroll(RIGHT, e.getWheelRotation());
      }
      else {
        return;
      }
      parent.repaint();
    });

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        System.err.println("Key pressed " + e.getKeyCode() + " (" + e.getKeyChar() +
                           ") modifiers " + InputEvent.getModifiersExText(e.getModifiersEx()));
        if (e.getModifiersEx() == 0) {
          switch (e.getKeyCode()) {
            case KeyEvent.VK_R:
              activeRegion = null;
              newRegion = true;
              mouseStatus = NEW_REGION;
              setCursor(Cursor.getPredefinedCursor(cursorMap.get(mouseStatus)));
              break;
            case KeyEvent.VK_A:
              final NewAvatarDialog.AvatarSelectionResult result =
                NewAvatarDialog.showDialog(parentWindow);
              if (result != null) {
                final Point mouse = windowToImageCoords(mx, my);
                final Region r = parent.getRegions(isPlayer).addRegion(0, mouse.x, mouse.y, 40, 40);
                r.isAvatar = true;
                r.symbol = result.symbol;
                r.color = result.color;
              }
              parent.repaint();
              break;
            case KeyEvent.VK_SPACE:
              if (activeRegion != null) {
                activeRegion.toggleState();
                parent.repaint();
              }
              break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
              if (activeRegion != null) {
                parent.getRegions(isPlayer).removeRegion(activeRegion.id);
                activeRegion = null;
                parent.repaint();
              }
              break;
            case KeyEvent.VK_ESCAPE:
              dragging = false;
              newRegion = false;
              detectMouseOverRegion();
              repaint();
              break;
            case KeyEvent.VK_Q:
              parent.togglePause();
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
        else if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK) {
          switch (e.getKeyCode()) {
            case KeyEvent.VK_Q:
              parent.quit();
              break;
            case KeyEvent.VK_O: {
              final JFileChooser chooser = new JFileChooser();
              final FileNameExtensionFilter filter =
                new FileNameExtensionFilter("Supported Images", ImageIO.getReaderFileSuffixes());
              chooser.setFileFilter(filter);
              final int result = chooser.showOpenDialog(parentWindow);
              if (result == JFileChooser.APPROVE_OPTION) {
                parent.newMap(chooser.getSelectedFile());
              }
              break;
            }
          }
        }
      }
    });
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
    final double scale = parent.getScale(isPlayer);
    for (final Region r : parent.getRegions(isPlayer).getRegions()) {
      if (new Corners(r).contains(mx, my)) {
        if (r.isAvatar) {
          avatar = r;
        }
        else {
          nonAvatar = r;
        }
      }
    }
    if (avatar != null) {
      return avatar;
    }
    return nonAvatar;
  }

  Point windowToImageCoords(int x, int y) {
    final Point off = parent.getOffset(isPlayer);
    x -= off.x;
    y -= off.y;

    final double invScale = 1.0 / parent.getScale(isPlayer);
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
    final Point off = parent.getOffset(isPlayer);
    off.x += SCROLL_DIST * xm * value;
    off.y += SCROLL_DIST * ym * value;
    parent.repaint();
  }

  private void rescale() {
    if (parent.getImage(isPlayer) == null) {
      imgWidth = 1;
      imgHeight = 1;
      if (!isPlayer) {
        parent.setScale(1.0);
      }
      return;
    }

    imgWidth = parent.getImage(isPlayer).getWidth(this);
    imgHeight = parent.getImage(isPlayer).getHeight(this);

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
    parent.setScale(scale);
  }

  public Image buildAvatars() {
    final BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();

    // Mark every pixel as transparent.
    g.setComposite(AlphaComposite.Src);
    g.setColor(new Color(0, 0, 0, 0));
    g.fillRect(0, 0, imgWidth, imgHeight);

    for (final Region r : parent.getRegions(isPlayer).getRegions()) {
      if (!r.isAvatar) {
        continue;
      }

      // We need to draw unscaled regions because the mask is going to be scaled
      // later.
      final Corners c = new Corners(r);
      g.setColor(r.color);
      g.setFont(new Font(null, 0, (int)(1.25 * Math.min(c.unscaledWidth, c.unscaledHeight))));
      Rectangle2D bounds = g.getFontMetrics().getStringBounds(Character.toString(r.symbol), g);
      g.drawString(Character.toString(r.symbol),
                   (int)(c.unscaledLeft + (c.unscaledWidth - bounds.getWidth()) / 2),
                   c.unscaledBottom);
      if (r.isDead) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(255, 0, 0, 64));
        bounds = g.getFontMetrics().getStringBounds(SKULL, g);
        g.drawString(SKULL, (int)(c.unscaledLeft + (c.unscaledWidth - bounds.getWidth()) / 2),
                     c.unscaledBottom);

        g.setComposite(AlphaComposite.Src);
      }
    }
    g.dispose();
    return img;
  }

  public Image buildMask() {
    final BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();

    // Black out the entire image.
    g.setComposite(AlphaComposite.Src);
    g.setColor(emptyMaskColor);
    g.fillRect(0, 0, imgWidth, imgHeight);

    for (final Region r : parent.getRegions(isPlayer).getRegions()) {
      if (r.isAvatar) {
        continue;
      }
      if (r.isVisible()) {
        g.setColor(new Color(0, 0, 0, 0)); // Make visible regions transparent.
      }
      else {
        g.setColor(hiddenMaskColor);
      }

      // We need to draw unscaled regions because the mask is going to be scaled
      // later.
      final Corners c = new Corners(r);
      g.fillRect(c.unscaledLeft, c.unscaledTop, c.unscaledWidth, c.unscaledHeight);
    }
    if (newRegion && activeRegion != null) {
      g.setColor(hiddenMaskColor);
      final Corners c = new Corners(activeRegion);
      g.fillRect(c.unscaledLeft, c.unscaledTop, c.unscaledWidth, c.unscaledHeight);
    }
    g.dispose();
    return img;
  }

  @Override
  public void paint(final Graphics og) {
    final Image img = parent.getImage(isPlayer);
    if (img == null) {
      og.setColor(Color.black);
      final Rectangle b = og.getClipBounds();
      og.fillRect(b.x, b.y, b.width, b.height);
      return;
    }

    final Point off = parent.getOffset(isPlayer);
    final double scale = parent.getScale(isPlayer);
    final AffineTransform transform = new AffineTransform();
    transform.translate(off.x, off.y);
    transform.scale(scale, scale);
    final Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      g.setColor(Color.black); // Black matte in case the image is small.
      g.fillRect(0, 0, getWidth(), getHeight());
      g.drawImage(img, transform, this);

      g.setComposite(AlphaComposite.SrcOver);
      g.drawImage(buildAvatars(), transform, this);
      g.drawImage(buildMask(), transform, this);

      if (isPlayer) {
        return;
      }

      if (activeRegion != null) {
        drawCorners(g, activeRegion);
        drawControls(g, activeRegion);
      }

      if (parent.isPaused()) {
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

  private void drawCorners(final Graphics2D g, final Region r) {
    final Corners c = new Corners(r);
    final int hs = HANDLE_SIZE;
    final int hhs = HANDLE_SIZE / 2;
    drawHandle(g, c.left, c.top); // Upper-left.
    drawHandle(g, c.right - hs, c.top); // Upper-right.
    drawHandle(g, c.left, c.bottom - hs); // Lower-left.
    drawHandle(g, c.right - hs, c.bottom - hs); // Lower-right.
    drawHandle(g, c.midX - hhs, c.top); // Top.
    drawHandle(g, c.midX - hhs, c.bottom - hs); // Bottom.
    drawHandle(g, c.left, c.midY - hhs); // Left.
    drawHandle(g, c.right - hs, c.midY - hhs); // Right.
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

  private void drawHandle(final Graphics2D g, final int x, final int y) {
    g.setColor(Color.red);
    g.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
  }

  private void drawControls(final Graphics2D g, final Region r) {
  }
}


package dmtool;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

public class MapPanel
  extends Canvas {
  private static final Color PLAYER_MASK_COLOR = new Color(0, 0, 0, 255);
  private static final Color DM_EMPTY_MASK_COLOR = new Color(0, 0, 0, 128);
  private static final Color DM_HIDDEN_MASK_COLOR = new Color(0, 0, 128, 128);

  private static final int HANDLE_SIZE = 6;

  private static final int OUT_OF_REGION = 0;
  private static final int NW_CORNER = 1;
  private static final int N_CORNER = 2;
  private static final int NE_CORNER = 3;
  private static final int E_CORNER = 4;
  private static final int SE_CORNER = 5;
  private static final int S_CORNER = 6;
  private static final int SW_CORNER = 7;
  private static final int W_CORNER = 8;
  private static final int IN_REGION = 9;

  private static final Map<Integer, Integer> cursorMap;
  static {
    cursorMap = new HashMap<>();
    cursorMap.put(OUT_OF_REGION, Cursor.DEFAULT_CURSOR);
    cursorMap.put(NW_CORNER, Cursor.NW_RESIZE_CURSOR);
    cursorMap.put(N_CORNER, Cursor.N_RESIZE_CURSOR);
    cursorMap.put(NE_CORNER, Cursor.NE_RESIZE_CURSOR);
    cursorMap.put(E_CORNER, Cursor.E_RESIZE_CURSOR);
    cursorMap.put(SE_CORNER, Cursor.SE_RESIZE_CURSOR);
    cursorMap.put(S_CORNER, Cursor.S_RESIZE_CURSOR);
    cursorMap.put(SW_CORNER, Cursor.SW_RESIZE_CURSOR);
    cursorMap.put(W_CORNER, Cursor.W_RESIZE_CURSOR);
    cursorMap.put(IN_REGION, Cursor.MOVE_CURSOR);
  }

  final DMTool parent;
  final boolean isPlayer;

  final Color emptyMaskColor; // When there is no region.
  final Color hiddenMaskColor; // When the region is hidden.

  // Updated when the map changes.
  int imgWidth = 1;
  int imgHeight = 1;

  // Last known mouse position.
  int mx, my;
  int mouseStatus = OUT_OF_REGION;

  public MapPanel(final DMTool parent, final boolean isPlayer) {
    this.parent = parent;
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
      public void mouseClicked(final MouseEvent e) {
        System.err.println("Mouse button: " + e.getButton());
      }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(final MouseEvent e) {
        mx = e.getX();
        my = e.getY();

        final double scale = parent.getScale(isPlayer);
        boolean found = false;
        for (final Region r : parent.getRegions(isPlayer).getRegions()) {
          if (r.scaledContains(scale, mx, my)) {
            found = true;
            mouseStatus = determineMouseStatus(r);
            repaint();
            break;
          }
        }
        if (!found) {
          repaint();
          mouseStatus = OUT_OF_REGION;
        }
        setCursor(Cursor.getPredefinedCursor(cursorMap.get(mouseStatus)));
      }
    });

    addMouseWheelListener((final MouseWheelEvent e) -> {
      System.err.println("Mouse scroll: " + e.getWheelRotation());
      if (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
        final double scale = parent.getScale(isPlayer) - 0.05 * e.getWheelRotation();
        parent.setScale(scale);
        System.err.println("Ctrl is down, zoomed to " + scale);
        parent.repaint();
      }
    });

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        System.err.println("Key press: \"" + e.getKeyCode() + "\"");
        if (e.getModifiersEx() == 0) {
          switch (e.getKeyCode()) {
            case KeyEvent.VK_F:
              parent.togglePause();
              break;
            case KeyEvent.VK_ESCAPE:
              parent.quit();
              break;
          }
        }
      }
    });
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

  @Override
  public void paint(final Graphics unused) {
    final Image img = parent.getImage(isPlayer);
    if (img == null) {
      return;
    }

    final double scale = parent.getScale(isPlayer);
    final AffineTransform scaleTransform = AffineTransform.getScaleInstance(scale, scale);
    final Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics();
    try {
      g.drawImage(img, scaleTransform, this);
      g.setComposite(AlphaComposite.SrcOver);
      g.drawImage(parent.getRegions(isPlayer).getMask(emptyMaskColor, hiddenMaskColor, imgWidth, imgHeight), scaleTransform, this);

      if (isPlayer) {
        return;
      }

      if (mouseStatus != OUT_OF_REGION) {
        for (final Region r : parent.getRegions(isPlayer).getRegions()) {
          if (r.scaledContains(scale, mx, my)) {
            System.err.println("Drawing controls for mouse-over region " + r.id);
            drawCorners(g, r);
            drawControls(g, r);
          }
        }
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
    final double scale = parent.getScale(isPlayer);
    drawHandle(g, scale * r.getX(), scale * r.getY()); // Upper-left.
    drawHandle(g, scale * (r.getX() + r.w) - HANDLE_SIZE + 1, scale * r.getY()); // Upper-right.
    drawHandle(g, scale * r.getX(), scale * (r.getY() + r.h) - HANDLE_SIZE + 1); // Lower-left.
    drawHandle(g, scale * (r.getX() + r.w) - HANDLE_SIZE + 1, scale * (r.getY() + r.h) - HANDLE_SIZE + 1); // Lower-right.

    drawHandle(g, scale * (r.getX() + r.w / 2), scale * r.getY()); // Top.
    drawHandle(g, scale * (r.getX() + r.w / 2), scale * (r.getY() + r.h) - HANDLE_SIZE + 1); // Bottom.
    drawHandle(g, scale * r.getX(), scale * (r.getY() + r.h / 2)); // Left.
    drawHandle(g, scale * (r.getX() + r.w) - HANDLE_SIZE + 1, scale * (r.getY() + r.h / 2)); // Right.
  }

  private int determineMouseStatus(final Region r) {
    final double min = HANDLE_SIZE * 2;
    if (scaledMouseDist(r.getX(), r.getY()) <= min) { // Upper-left.
      return NW_CORNER;
    }
    if (scaledMouseDist(r.getX() + r.w, r.getY()) <= min) { // Upper-right.
      return NE_CORNER;
    }
    if (scaledMouseDist(r.getX(), r.getY() + r.h) <= min) { // Lower-left.
      return SW_CORNER;
    }
    if (scaledMouseDist(r.getX() + r.w, r.getY() + r.h) <= min) { // Lower-right.
      return SE_CORNER;
    }
    if (scaledMouseDist(r.getX() + r.w / 2, r.getY()) <= min) { // Top.
      return N_CORNER;
    }
    if (scaledMouseDist(r.getX() + r.w / 2, r.getY() + r.h) <= min) { // Bottom.
      return S_CORNER;
    }
    if (scaledMouseDist(r.getX(), r.getY() + r.h / 2) <= min) { // Left.
      return E_CORNER;
    }
    if (scaledMouseDist(r.getX() + r.w, r.getY() + r.h / 2) <= min) {
      return W_CORNER;
    }
    return IN_REGION;
  }

  private double scaledMouseDist(final int x, final int y) {
    final double scale = parent.getScale(isPlayer);
    final double dx = scale * x - mx;
    final double dy = scale * y - my;
    return Math.sqrt(dx * dx + dy * dy);
  }

  private void drawHandle(final Graphics2D g, final double dx, final double dy) {
    final int x = (int)dx;
    final int y = (int)dy;
    g.setColor(Color.red);
    g.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
  }

  private void drawControls(final Graphics2D g, final Region r) {
  }
}

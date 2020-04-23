
package dmtool;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;

import javax.swing.SwingUtilities;

public class DMMapPanel
  extends Canvas {
  static final Color MASK_COLOR = new Color(0, 0, 0, 128);

  final DMTool parent;

  // Updated when the map changes.
  int imgWidth = 1;
  int imgHeight = 1;

  // Last known mouse position.
  int mx, my;
  boolean mouseInRegion = false;

  public DMMapPanel(final DMTool parent) {
    this.parent = parent;

    SwingUtilities.invokeLater(() -> {
      createBufferStrategy(2);
    });
    parent.addNewMapListener(() -> {
      repaint();

      // Recalculate scale to fit to window.
      rescale();
    });

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

        final double scale = parent.getDMScale();
        boolean found = false;
        for (final Region r : parent.getDMRegions().getRegions()) {
          if (r.scaledContains(scale, mx, my)) {
            found = true;
            mouseInRegion = true;
            repaint();
            break;
          }
        }
        if (!found && mouseInRegion) {
          repaint();
          mouseInRegion = false;
        }
      }
    });

    addMouseWheelListener((final MouseWheelEvent e) -> {
      System.err.println("Mouse scroll: " + e.getWheelRotation());
      if (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
        final double scale = parent.getDMScale() - 0.05 * e.getWheelRotation();
        parent.setDMScale(scale);
        System.err.println("Ctrl is down, zoomed to " + scale);
        parent.repaint();
      }
    });
  }

  private void rescale() {
    if (parent.getDMImage() == null) {
      imgWidth = 1;
      imgHeight = 1;
      parent.setDMScale(1.0);
      return;
    }

    imgWidth = parent.getDMImage().getWidth(this);
    imgHeight = parent.getDMImage().getHeight(this);
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
    parent.setDMScale(scale);
  }

  @Override
  public void paint(final Graphics unused) {
    final Image img = parent.getDMImage();
    if (img == null) {
      return;
    }

    final double scale = parent.getDMScale();
    final AffineTransform scaleTransform = AffineTransform.getScaleInstance(scale, scale);
    final Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics();
    g.drawImage(img, scaleTransform, this);
    g.setComposite(AlphaComposite.SrcOver);
    g.drawImage(parent.getDMRegions().getMask(MASK_COLOR, imgWidth, imgHeight), scaleTransform, this);

    if (mouseInRegion) {
      for (final Region r : parent.getDMRegions().getRegions()) {
        if (r.scaledContains(scale, mx, my)) {
          System.err.println("Drawing controls for mouse-over region " + r.id);
          drawCorners(g, r);
          drawControls(g, r);
        }
      }
    }

    g.dispose();
    getBufferStrategy().show();
  }

  private void drawCorners(final Graphics2D g, final Region r) {
    final double scale = parent.getDMScale();
    drawHandle(g, scale * r.getX(), scale * r.getY());
    drawHandle(g, scale * (r.getX() + r.w), scale * r.getY());
    drawHandle(g, scale * r.getX(), scale * (r.getY() + r.h));
    drawHandle(g, scale * (r.getX() + r.w), scale * (r.getY() + r.h));
  }

  private void drawHandle(final Graphics2D g, final double dx, final double dy) {
    final int x = (int)dx;
    final int y = (int)dy;
    g.setColor(Color.red);
    g.fillRect(x - 2, y - 2, 4, 4);
  }

  private void drawControls(final Graphics2D g, final Region r) {
  }
}

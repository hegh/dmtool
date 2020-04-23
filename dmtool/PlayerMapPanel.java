
package dmtool;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;

import javax.swing.SwingUtilities;

public class PlayerMapPanel
  extends Canvas {
  static final Color MASK_COLOR = new Color(0, 0, 0, 255);

  final DMTool parent;

  public PlayerMapPanel(final DMTool parent) {
    this.parent = parent;

    SwingUtilities.invokeLater(() -> {
      createBufferStrategy(2);
    });
    parent.addResumeListener(() -> {
      repaint();
    });
  }

  @Override
  public void paint(final Graphics unused) {
    final Image img = parent.getDMImage();
    if (img == null) {
      return;
    }

    if (!parent.isPaused()) {
      parent.setPlayerScale(parent.getDMScale());
    }

    final double scale = parent.getPlayerScale();
    final AffineTransform scaleTransform = AffineTransform.getScaleInstance(scale, scale);
    final Graphics2D g = (Graphics2D)getBufferStrategy().getDrawGraphics();
    g.drawImage(img, scaleTransform, this);
    g.setComposite(AlphaComposite.SrcOver);
    g.drawImage(parent.getDMRegions().getMask(MASK_COLOR, img.getWidth(this), img.getHeight(this)), scaleTransform, this);

    // FIXME: Starting unpaused for some reason.

    g.dispose();
    getBufferStrategy().show();
  }
}

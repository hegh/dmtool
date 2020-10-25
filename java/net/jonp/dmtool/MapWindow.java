
package net.jonp.dmtool;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class MapWindow
  extends JFrame {
  private static final String DM_TITLE = "Dungeon Master View - DO NOT SHARE";
  private static final String PLAYER_TITLE = "Player View";

  protected MapPanel mapPanel;

  public MapWindow(final DMTool parent, final boolean isPlayer) {
    super(isPlayer ? PLAYER_TITLE : DM_TITLE);

    SwingUtilities.invokeLater(() -> {
      mapPanel = new MapPanel(parent, this, isPlayer);

      setPreferredSize(new Dimension(1024, 768));
      setLayout(new GridLayout(1, 1));
      add(mapPanel);
      pack();

      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          parent.quit();
        }
      });

      addComponentListener(new ComponentAdapter() {
        private long debounceTime = 0;

        @Override
        public void componentResized(final ComponentEvent e) {
          // Lock the window sizes together.
          final MapWindow other = parent.getWindow(!isPlayer);
          final Dimension size = getSize();
          if (other.getSize().equals(size)) {
            return;
          }
          // On KDE, half-screen maximize bounces the window size by a few
          // pixels,
          // so debounce.
          final long now = System.currentTimeMillis();
          if (now - debounceTime < 100) {
            System.err.println("Debounce window size, isPlayer = " + isPlayer);
            return;
          }
          debounceTime = now;
          System.err
            .println("Resized to " + size.width + "x" + size.height + ", isPlayer = " + isPlayer);
          other.setSize(size);
        }
      });

      if (isPlayer) {
        return;
      }

      mapPanel.requestFocus();
    });
  }

  @Override
  public void repaint() {
    super.repaint();
    mapPanel.repaint();
  }
}

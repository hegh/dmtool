
package dmtool;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class PlayerWindow
  extends JFrame {
  private final DMTool parent;
  private final PlayerMapPanel mapPanel;

  public PlayerWindow(final DMTool parent) {
    super("Player View");
    this.parent = parent;

    mapPanel = new PlayerMapPanel(parent);

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
  }


  @Override
  public void repaint() {
    super.repaint();
    mapPanel.repaint();
  }
}

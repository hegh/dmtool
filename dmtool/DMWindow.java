
package dmtool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class DMWindow
  extends JFrame {
  private final FileList fileList;
  private final DMMapPanel mapPanel;

  public DMWindow(final DMTool parent) {
    super("Dungeon Master View - DO NOT SHARE");

    fileList = new FileList(parent);
    mapPanel = new DMMapPanel(parent);

    setPreferredSize(new Dimension(1024, 768));
    setLayout(new BorderLayout());
    add(fileList, BorderLayout.SOUTH);
    add(mapPanel, BorderLayout.CENTER);
    pack();

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        parent.quit();
      }
    });

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if (e.getModifiersEx() == 0) {
          switch (e.getKeyCode()) {
            case KeyEvent.VK_F:
              parent.togglePause();
              break;
            case KeyEvent.VK_ESCAPE:
              parent.quit();
              break;
            default:
              System.err.println("Unhandled key code: \"" + e.getKeyCode() + "\"");
          }
        }
      }
    });
  }

  @Override
  public void repaint() {
    super.repaint();
    mapPanel.repaint();
  }
}

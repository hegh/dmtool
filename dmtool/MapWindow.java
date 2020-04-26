
package dmtool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class MapWindow
  extends JFrame {
  private static final String DM_TITLE = "Dungeon Master View - DO NOT SHARE";
  private static final String PLAYER_TITLE = "Player View";

  private final FileList fileList;
  private final MapPanel mapPanel;

  public MapWindow(final DMTool parent, final boolean isPlayer) {
    super(isPlayer ? PLAYER_TITLE : DM_TITLE);

    setPreferredSize(new Dimension(1024, 768));
    setLayout(new BorderLayout());

    mapPanel = new MapPanel(parent, isPlayer);
    add(mapPanel, BorderLayout.CENTER);
    if (isPlayer) {
      fileList = null;
    }
    else {
      fileList = new FileList(parent);
      add(fileList, BorderLayout.SOUTH);
    }
    pack();

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        parent.quit();
      }
    });

    if (isPlayer) {
      return;
    }

    mapPanel.requestFocus(Cause.ACTIVATION);
  }

  @Override
  public void repaint() {
    super.repaint();
    mapPanel.repaint();
  }
}

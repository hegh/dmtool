
package net.jonp.dmtool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

public class ColorPanel
  extends JPanel {
  public ColorPanel(final Color color) {
    setBackground(color);
    setMinimumSize(new Dimension(40, 40));

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        final Color result = JColorChooser.showDialog(ColorPanel.this, "Avatar Color", getBackground());
        if (result != null) {
          setBackground(result);
        }
      }
    });
  }
}

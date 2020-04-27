
package dmtool;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent.Cause;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;

public class NewAvatarDialog
  extends Dialog {
  private static Color lastColor = Color.black;

  private char symbol;
  private Color color;
  private boolean accepted;

  public static class AvatarSelectionResult {
    public final char symbol;
    public final Color color;

    AvatarSelectionResult(final char symbol, final Color color) {
      this.symbol = symbol;
      this.color = color;
    }
  }

  public static AvatarSelectionResult showDialog(final Window parentWindow) {
    final NewAvatarDialog dlg = new NewAvatarDialog(parentWindow);
    dlg.setVisible(true);
    if (!dlg.accepted) {
      return null;
    }
    return new AvatarSelectionResult(dlg.symbol, dlg.color);
  }

  private NewAvatarDialog(final Window parentWindow) {
    super(parentWindow, "New Avatar", ModalityType.DOCUMENT_MODAL);

    final JLabel symbolLabel = new JLabel("Symbol");
    final JLimitTextField symbolText = new JLimitTextField(1);
    final JLabel colorLabel = new JLabel("Color");
    final ColorPanel colorPanel = new ColorPanel(lastColor);
    final JButton okButton = new JButton("Ok");
    final JButton cancelButton = new JButton("Cancel");

    setLayout(new GridLayout(3, 2));
    add(symbolLabel);
    add(symbolText);
    add(colorLabel);
    add(colorPanel);
    add(okButton);
    add(cancelButton);
    pack();

    symbolText.requestFocus(Cause.ACTIVATION);

    symbolLabel.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(final KeyEvent e) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_ENTER:
            okButton.doClick();
            break;
          case KeyEvent.VK_ESCAPE:
            cancelButton.doClick();
            break;
        }
      }
    });

    okButton.addActionListener((final ActionEvent e) -> {
      if (symbolText.getText().length() == 0) {
        getToolkit().beep();
        symbolText.requestFocusInWindow(Cause.ACTIVATION);
        return;
      }
      symbol = symbolText.getText().charAt(0);
      color = colorPanel.getBackground();
      lastColor = color;
      accepted = true;
      setVisible(false);
      dispose();
    });

    cancelButton.addActionListener((final ActionEvent e) -> {
      accepted = false;
      setVisible(false);
      dispose();
    });

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        accepted = false;
        setVisible(false);
        dispose();
      }
    });
  }
}

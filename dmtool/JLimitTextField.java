
package dmtool;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class JLimitTextField
  extends JTextField {
  private static class LimitDocument
    extends PlainDocument {
    final int limit;

    LimitDocument(final int limit) {
      this.limit = limit;
    }

    @Override
    public void insertString(final int offset, final String s, final AttributeSet attr)
      throws BadLocationException {
      if (s == null) {
        return;
      }
      if (getLength() + s.length() <= limit) {
        super.insertString(offset, s, attr);
      }
    }
  }

  public JLimitTextField(final int limit) {
    super(limit);
    setDocument(new LimitDocument(limit));
  }
}

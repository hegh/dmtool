
package dmtool;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

public class Main {
  public static void main(final String[] args) {
    File save = null;
    File image = null;
    for (final String arg : args) {
      final File f = new File(arg);
      if (arg.endsWith(DMTool.SAVE_FILE_EXTENSION)) {
        save = f;
        continue;
      }
      if (f.isFile()) {
        image = f;
        continue;
      }
    }

    final DMTool tool = new DMTool();
    if (save != null) {
      try {
        tool.open(save);
      }
      catch (final IOException e) {
        JOptionPane.showMessageDialog(null, "Failed to load file: " + e.getMessage(), "Load Error",
                                      JOptionPane.ERROR_MESSAGE);
        System.err.println("Failed to open \"" + save + "\"");
        e.printStackTrace();
      }
    }
    else if (image != null) {
      try {
        tool.newMap(image);
      }
      catch (final IOException e) {
        JOptionPane.showMessageDialog(null, "Failed to load image: " + e.getMessage(), "Load Error",
                                      JOptionPane.ERROR_MESSAGE);
        System.err.println("Failed to open \"" + image + "\"");
        e.printStackTrace();
      }
    }
    tool.run();
  }
}

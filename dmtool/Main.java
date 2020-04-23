
package dmtool;

import java.io.File;

public class Main {
  public static void main(final String[] args) {
    // TODO: Load previously used directory if available.
    File save = null;
    File directory = new File(System.getProperty("user.dir"));
    File image = null;
    for (final String arg : args) {
      final File f = new File(arg);
      if (arg.endsWith(".dmap")) {
        save = f;
        continue;
      }
      if (f.isDirectory()) {
        directory = f;
        continue;
      }
      if (f.isFile()) {
        image = f;
        continue;
      }
    }

    final DMTool tool = new DMTool();
    tool.setDirectory(directory);
    if (save != null) {
      tool.open(save);
    }
    else if (image != null) {
      tool.newMap(image);
    }
    tool.run();
  }
}


package dmtool;

import java.io.File;

public class Main {
  public static void main(final String[] args) {
    File save = null;
    File image = null;
    for (final String arg : args) {
      final File f = new File(arg);
      if (arg.endsWith(".dmap")) {
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
      tool.open(save);
    }
    else if (image != null) {
      tool.newMap(image);
    }
    tool.run();
  }
}

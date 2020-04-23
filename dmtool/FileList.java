
package dmtool;

import java.io.File;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

public class FileList
  extends JList<File> {
  private static class StringFile
    extends File {
    public StringFile(final String path) {
      super(path);
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  public FileList(final DMTool parent) {
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    parent.addDirectoryChangeListener(() -> {
      final File[] maps = parent.getDirectory().listFiles((final File f) -> {
        if (!f.isFile()) {
          return false;
        }
        return f.getName().toLowerCase().endsWith(".dmap");
      });

      final StringFile[] files = new StringFile[maps.length];
      for (int i = 0; i < maps.length; i++) {
        files[i] = new StringFile(maps[i].getPath());
      }
      setListData(files);
    });

    addListSelectionListener((final ListSelectionEvent e) -> {
      final File f = getSelectedValue();
      if (f == null) {
        return;
      }
      parent.open(f);
    });
  }
}

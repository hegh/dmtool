
package net.jonp.dmtool;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import net.jonp.dmtool.dmproto.DMProto;

/**
 *
 */
public class DMTool {
  public static final String SAVE_FILE_EXTENSION = "dmap";
  private static final String SAVE_FILE_FORMAT = "DMTool Map";

  // The same image when presenting; player is cloned from DM when paused.
  private BufferedImage playerImage;
  private BufferedImage dmImage;

  private MapWindow playerWindow;
  private MapWindow dmWindow;

  private double playerScale = 1.0;
  private double dmScale = 1.0;

  private Point playerOffset = new Point(0, 0);
  private Point dmOffset = new Point(0, 0);

  // The same Regions when presenting; player is cloned from DM when paused.
  private Regions playerRegions = new Regions();
  private Regions dmRegions = new Regions();

  private boolean paused = true;
  private File savePath;
  private File directory; // Where the file chooser last was.

  private final Collection<PauseListener> pauseListeners = new ArrayList<>();
  private final Collection<ResumeListener> resumeListeners = new ArrayList<>();
  private final Collection<NewMapListener> newMapListeners = new ArrayList<>();

  DMTool() {
  }

  public File getActiveSave() {
    return savePath;
  }

  public void setActiveSave(final File file) {
    savePath = file;
  }

  public File getDirectory() {
    return directory;
  }

  public void setDirectory(final File dir) {
    directory = dir;
  }

  void run() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        playerWindow = new MapWindow(DMTool.this, /* isPlayer = */ true);
        dmWindow = new MapWindow(DMTool.this, /* isPlayer = */ false);

        playerWindow.setVisible(true);
        dmWindow.setVisible(true);

        if (dmImage != null) {
          fireNewMap();
        }
      }
    });
  }

  void repaint() {
    playerWindow.repaint();
    dmWindow.repaint();
  }

  MapWindow getWindow(final boolean isPlayer) {
    if (isPlayer) {
      return playerWindow;
    }
    return dmWindow;
  }

  BufferedImage getImage(final boolean isPlayer) {
    if (isPlayer && paused) {
      return playerImage;
    }
    return dmImage;
  }

  double getScale(final boolean isPlayer) {
    if (isPlayer && paused) {
      return playerScale;
    }
    return dmScale;
  }

  void setScale(final double scale) {
    dmScale = scale;
  }

  Point getOffset(final boolean isPlayer) {
    if (isPlayer && paused) {
      return playerOffset;
    }
    return dmOffset;
  }

  void setOffset(final Point offset) {
    dmOffset = offset;
  }

  Regions getRegions(final boolean isPlayer) {
    if (isPlayer && paused) {
      return playerRegions;
    }
    return dmRegions;
  }

  boolean isPaused() {
    return paused;
  }

  void addPauseListener(final PauseListener listener) {
    pauseListeners.add(listener);
  }

  void pause() {
    if (paused) {
      return;
    }
    paused = true;
    if (dmImage != null) {
      playerImage = new BufferedImage(dmImage.getColorModel(), dmImage.copyData(null),
                                      dmImage.isAlphaPremultiplied(), null);
    }
    playerRegions = dmRegions.clone();
    playerScale = dmScale;
    playerOffset = dmOffset;
    firePaused();
  }

  private void firePaused() {
    pauseListeners.forEach((final PauseListener listener) -> {
      listener.onPause();
    });
  }

  void addResumeListener(final ResumeListener listener) {
    resumeListeners.add(listener);
  }

  void resume() {
    if (!paused) {
      return;
    }
    paused = false;
    playerImage = null;
    playerRegions = null;
    playerOffset = null;
    fireResumed();
  }

  private void fireResumed() {
    resumeListeners.forEach((final ResumeListener listener) -> {
      listener.onResume();
    });
  }

  void togglePause() {
    if (isPaused()) {
      resume();
      return;
    }
    pause();
  }

  void addNewMapListener(final NewMapListener listener) {
    newMapListeners.add(listener);
  }

  void newMap(final File f)
    throws IOException {
    final BufferedImage img = ImageIO.read(f);
    System.err.println("Loaded " + img.getWidth() + "x" + img.getHeight() + " image file: " + f);
    pause();
    savePath = null;
    dmImage = img;
    dmRegions = new Regions();
    fireNewMap();
  }

  private void fireNewMap() {
    newMapListeners.forEach((final NewMapListener listener) -> {
      listener.onNewMap();
    });
  }

  void save(final File path)
    throws IOException {
    if (dmRegions == null || dmImage == null) {
      throw new IllegalStateException("No open map");
    }

    final OutputStream out = new FileOutputStream(path);
    final ZipOutputStream zip = new ZipOutputStream(out);
    try {
      zip.setLevel(9);
      final ZipEntry version = new ZipEntry("version");
      zip.putNextEntry(version);
      zip.write(DMProto.Version.newBuilder().setFormat(SAVE_FILE_FORMAT).setVersion(1).build()
        .toByteArray());

      final ZipEntry metadata = new ZipEntry("metadata");
      zip.putNextEntry(metadata);
      zip.write(DMProto.Metadata.newBuilder().setContents("1").build().toByteArray());

      final ZipEntry one = new ZipEntry("1/");
      zip.putNextEntry(one);

      final ZipEntry pb = new ZipEntry("1/data.pb");
      zip.putNextEntry(pb);
      zip.write(dmRegions.serialize().toByteArray());

      // Don't bother compressing the map, since png is already compressed.
      zip.setLevel(0);
      final ZipEntry map = new ZipEntry("1/map.png");
      zip.putNextEntry(map);
      ImageIO.write(dmImage, "png", zip);
      zip.finish();
    }
    finally {
      zip.close();
    }
  }

  void open(final File path)
    throws IOException {
    final ZipFile zip = new ZipFile(path);
    try {
      ZipEntry entry = zip.getEntry("version");
      if (entry == null) {
        throw new IOException("Not a DMTool save file: No version entry");
      }
      final DMProto.Version version = DMProto.Version.parseFrom(zip.getInputStream(entry));
      if (!version.getFormat().equals(SAVE_FILE_FORMAT)) {
        throw new IOException("Not a DMTool saved map");
      }
      if (version.getVersion() != 1) {
        throw new IOException("Cannot parse save file: Of unsupported version " +
                              version.getVersion());
      }

      entry = zip.getEntry("metadata");
      if (entry == null) {
        throw new IOException("Bad save file: No metadata entry");
      }
      final DMProto.Metadata metadata = DMProto.Metadata.parseFrom(zip.getInputStream(entry));

      entry = zip.getEntry(metadata.getContents() + "/data.pb");
      if (entry == null) {
        throw new IOException("Bad save file: No \"data.pb\" entry for map \"" +
                              metadata.getContents() + "\"");
      }
      final DMProto.Map map = DMProto.Map.parseFrom(zip.getInputStream(entry));

      entry = zip.getEntry(metadata.getContents() + "/map.png");
      if (entry == null) {
        throw new IOException("Bad save file: No \"map.png\" entry for map \"" +
                              metadata.getContents() + "\"");
      }
      final BufferedImage img = ImageIO.read(zip.getInputStream(entry));

      // If we get here, everything worked.
      pause();
      savePath = path;
      final Regions rs = new Regions();
      rs.load(map);
      dmRegions = rs;
      dmImage = img;
      dmScale = 1;
      dmOffset = new Point(0, 0);
      fireNewMap();
    }
    finally {
      zip.close();
    }
  }

  void quit() {
    // TODO: Offer to save if necessary
    System.exit(0);
  }

  public static interface PauseListener {
    void onPause();
  }

  public static interface ResumeListener {
    void onResume();
  }

  public static interface NewMapListener {
    void onNewMap();
  }
}

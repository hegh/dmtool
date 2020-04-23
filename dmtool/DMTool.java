
package dmtool;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 *
 */
public class DMTool {
  private File directory;

  // The same image when presenting; player is cloned from DM when paused.
  private BufferedImage playerImage;
  private BufferedImage dmImage;

  private MapWindow playerWindow;
  private MapWindow dmWindow;

  private final double playerScale = 1.0;
  private double dmScale = 1.0;

  // The same Regions when presenting; player is cloned from DM when paused.
  private Regions playerRegions = new Regions();
  private Regions dmRegions = new Regions();

  private boolean paused = true;
  private File savePath;

  private final Collection<DirectoryChangeListener> directoryChangeListeners = new ArrayList<>();
  private final Collection<PauseListener> pauseListeners = new ArrayList<>();
  private final Collection<ResumeListener> resumeListeners = new ArrayList<>();
  private final Collection<NewMapListener> newMapListeners = new ArrayList<>();

  DMTool() {
  }

  public void addDirectoryChangeListener(final DirectoryChangeListener listener) {
    directoryChangeListeners.add(listener);
  }

  public void setDirectory(final File directory) {
    this.directory = directory;
    fireDirectoryChanged();
  }

  private void fireDirectoryChanged() {
    directoryChangeListeners.forEach((final DirectoryChangeListener listener) -> {
      listener.onDirectoryChange();
    });
  }

  public File getDirectory() {
    return directory;
  }

  public File getActiveSave() {
    return savePath;
  }

  void run() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        playerWindow = new MapWindow(DMTool.this, /* isPlayer = */ true);
        dmWindow = new MapWindow(DMTool.this, /* isPlayer = */ false);

        playerWindow.setVisible(true);
        dmWindow.setVisible(true);

        fireDirectoryChanged();
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

  Image getImage(final boolean isPlayer) {
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
      playerImage = new BufferedImage(dmImage.getColorModel(), dmImage.copyData(null), dmImage.isAlphaPremultiplied(), null);
    }
    playerRegions = dmRegions.clone();
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
    playerImage = dmImage;
    playerRegions = dmRegions;
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

  void newMap(final File f) {
    BufferedImage img;
    try {
      img = ImageIO.read(f);
    }
    catch (final IOException e) {
      img = null;
      // TODO: Display error message.
      System.err.println(e.getMessage());
      return;
    }

    System.err.println("Loaded " + img.getWidth() + "x" + img.getHeight() + " image file: " + f);
    pause();
    savePath = null;
    dmImage = img;
    dmRegions = new Regions();

    // TODO: Remove this, it is just for debugging:
    final Region region = dmRegions.addRegion(0, 10, 10, 50, 100);
    region.state = Region.State.VISIBLE;

    fireNewMap();
  }

  private void fireNewMap() {
    newMapListeners.forEach((final NewMapListener listener) -> {
      listener.onNewMap();
    });
  }

  void open(final File f) {
    // TODO: Open save
    // TODO: Send NewMap event
    savePath = f;
  }

  void quit() {
    // TODO: Offer to save if necessary
    System.exit(0);
  }

  public static interface DirectoryChangeListener {
    void onDirectoryChange();
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

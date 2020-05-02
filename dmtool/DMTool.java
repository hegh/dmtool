
package dmtool;

import java.awt.Image;
import java.awt.Point;
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

  private final Collection<PauseListener> pauseListeners = new ArrayList<>();
  private final Collection<ResumeListener> resumeListeners = new ArrayList<>();
  private final Collection<NewMapListener> newMapListeners = new ArrayList<>();

  DMTool() {
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

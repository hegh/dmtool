
package dmtool;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of regions and region groups that supports the clone operation,
 * to take a snapshot.
 */
public class Regions {
  private final Map<Integer, RegionGroup> groups = new HashMap<>();
  private final Map<Integer, Region> regions = new HashMap<>();

  /**
   * Returns a transparency mask representing this group of regions.
   *
   * @param emptyMask The color to use for areas not covered by any region.
   *          Probably black or something semi-transparent.
   * @param hiddenMask The color to use for areas covered by a hidden region.
   *          Probably black or something semi-transparent. Visible regions will
   *          be fully transparent.
   * @param w The width of the returned image, in pixels.
   * @param h The height of the returned image, in pixels.
   * @return An image that can be drawn over a map to mask off areas that should
   *         not be seen.
   */
  public Image getMask(final Color emptyMask, final Color hiddenMask, final int w, final int h) {
    System.err.println("Creating mask " + w + "x" + h);
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();

    // Black out the entire image.
    g.setComposite(AlphaComposite.Src);
    g.setColor(emptyMask);
    g.fillRect(0, 0, w, h);
    for (final Region r : regions.values()) {
      if (r.isVisible()) {
        g.setColor(new Color(0, 0, 0, 0)); // Make visible regions transparent.
      }
      else {
        g.setColor(hiddenMask);
      }
      g.fillRect(r.getX(), r.getY(), r.w, r.h);
    }
    g.dispose();
    return img;
  }

  public void clear() {
    groups.clear();
    regions.clear();
  }

  // Pass 0 to create a new region group.
  public Region addRegion(final int parentID, final int x, final int y, final int w, final int h) {
    RegionGroup parent;
    if (parentID == 0) {
      parent = new RegionGroup();
      groups.put(parent.id, parent);
    }
    else {
      parent = groups.get(parentID);
      if (parent == null) {
        throw new NullPointerException("no such region group: " + parentID);
      }
    }

    final Region region = new Region(parent, x, y, w, h);
    regions.put(region.id, region);
    return region;
  }

  public Collection<Region> getRegions() {
    return regions.values();
  }

  @Override
  public Regions clone() {
    final Regions n = new Regions();

    // Old ID onto copied value.
    final Map<Integer, RegionGroup> g = new HashMap<>();
    for (final RegionGroup group : groups.values()) {
      final RegionGroup copy = group.clone();
      g.put(group.id, copy);
      n.groups.put(copy.id, copy);
    }

    // Copy regions, re-parenting to copied parents.
    for (final Region region : regions.values()) {
      final RegionGroup parent = g.get(region.id);
      final Region copy = region.clone();
      copy.parent = parent;
      n.regions.put(copy.id, copy);
    }
    return n;
  }
}

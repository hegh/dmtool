
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

  private Image maskCache;
  private Color maskColor;
  private int maskWidth;
  private int maskHeight;
  private boolean dirty = true;

  /**
   * Returns a transparency mask representing this group of regions.
   *
   * @param mask The "opaque" color, for hidden regions. Probably black or
   *          something semi-transparent. Visible regions will be fully
   *          transparent.
   * @param w The width of the returned image, in pixels.
   * @param h The height of the returned image, in pixels.
   * @return An image that can be drawn over a map to mask off areas that should
   *         not be seen.
   */
  public Image getMask(final Color mask, final int w, final int h) {
    if (maskCache != null && maskColor.equals(mask) && maskWidth == w && maskHeight == h && !dirty) {
      System.err.println("Reusing cached mask");
      return maskCache;
    }

    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = img.createGraphics();

    // Mask over the entire image with inverse color, blacking out the areas
    // that should be visible.
    // We'll invert this at the end.
    g.setColor(mask);
    g.fillRect(0, 0, w, h);

    g.setComposite(AlphaComposite.Src);
    g.setColor(new Color(0, 0, 0, 0)); // Make the regions transparent.
    for (final Region r : regions.values()) {
      if (r.isVisible()) {
        g.fillRect(r.getX(), r.getY(), r.w, r.h);
      }
    }
    g.dispose();

    maskCache = img;
    maskColor = mask;
    maskWidth = w;
    maskHeight = h;
    dirty = false;
    return img;
  }

  public void clear() {
    groups.clear();
    regions.clear();
    dirty = true;
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
    dirty = true;

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

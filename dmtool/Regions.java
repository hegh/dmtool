
package dmtool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of regions and region groups that supports the clone operation,
 * to take a snapshot.
 */
public class Regions {
  private final Map<Integer, RegionGroup> groups = new HashMap<>();
  private final Collection<Region> regions = new ArrayList<>();

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
    regions.add(region);
    return region;
  }

  public Collection<Region> getRegions() {
    return regions;
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
    for (final Region region : regions) {
      final Region copy = region.clone();
      copy.parent = g.get(region.parent.id);
      n.regions.add(copy);
    }
    return n;
  }
}

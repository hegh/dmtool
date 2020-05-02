
package dmtool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of regions and region groups that supports the clone operation,
 * to take a snapshot.
 */
public class Regions {
  private final Map<Integer, RegionGroup> groups = new HashMap<>();

  public void clear() {
    groups.clear();
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
        throw new IllegalArgumentException("no such region group: " + parentID);
      }
    }
    return parent.addChild(x, y, w, h);
  }

  public Region duplicate(final Region old) {
    final Region r = old.clone();
    old.parent.addChild(r);
    switch (old.nextDupPosition) {
      case 0: // Right.
        r.adjustDims(r.getW(), 0, 0, 0);
        break;
      case 1: // Down & right.
        r.adjustDims(r.getW(), r.getH(), 0, 0);
        break;
      case 2: // Down.
        r.adjustDims(0, r.getH(), 0, 0);
        break;
      case 3: // Down & left.
        r.adjustDims(-r.getW(), r.getH(), 0, 0);
        break;
      case 4: // Left.
        r.adjustDims(-r.getW(), 0, 0, 0);
        break;
      case 5: // Up & left.
        r.adjustDims(-r.getW(), -r.getH(), 0, 0);
        break;
      case 6: // Up.
        r.adjustDims(0, -r.getH(), 0, 0);
        break;
      default: // Up & right.
        r.adjustDims(r.getW(), -r.getH(), 0, 0);
    }
    old.nextDupPosition++;
    old.nextDupPosition %= 8;
    return r;
  }

  public void deparent(final Region r) {
    removeRegion(r);
    final RegionGroup parent = new RegionGroup();
    groups.put(parent.id, parent);
    parent.addChild(r);
  }

  public void removeRegion(final Region r) {
    if (r == null) {
      return;
    }
    if (r.parent == null) {
      return;
    }
    r.parent.removeChild(r.id);
    if (r.parent.children.isEmpty()) {
      groups.remove(r.parent.id);
    }
  }

  public Collection<RegionGroup> getGroups() {
    return Collections.unmodifiableCollection(groups.values());
  }

  @Override
  public Regions clone() {
    final Regions n = new Regions();
    for (final RegionGroup group : getGroups()) {
      final RegionGroup copy = group.clone();
      n.groups.put(copy.id, copy);
    }
    return n;
  }
}

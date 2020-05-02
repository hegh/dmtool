
package dmtool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegionGroup {
  private static int nextID = 1;

  public static enum State {
    HIDDEN,
    VISIBLE,
  }

  public Map<Integer, Region> children = new HashMap<>();
  public final int id;
  public State state = State.HIDDEN;

  public RegionGroup() {
    id = nextID;
    nextID++;
  }

  public Region addChild(final int x, final int y, final int w, final int h) {
    final Region region = new Region(this, x, y, w, h);
    children.put(region.id, region);
    return region;
  }

  public void addChild(final Region region) {
    children.put(region.id, region);
    if (region.parent != this && region.parent != null) {
      region.parent.removeChild(region.id);
    }
    region.parent = this;
  }

  public void removeChild(final int id) {
    children.remove(id);
  }

  public Collection<Region> getChildren() {
    return Collections.unmodifiableCollection(children.values());
  }

  public boolean isVisible() {
    return state == State.VISIBLE;
  }

  public void toggleState() {
    if (state == State.VISIBLE) {
      state = State.HIDDEN; // TODO: Switch to Fogged once implemented.
    }
    else {
      state = State.VISIBLE;
    }
  }

  @Override
  public RegionGroup clone() {
    final RegionGroup copy = new RegionGroup();
    copy.state = state;

    for (final Region child : children.values()) {
      final Region r = child.clone();
      copy.addChild(r);
    }
    return copy;
  }
}

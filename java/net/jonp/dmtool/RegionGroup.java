
package net.jonp.dmtool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jonp.dmtool.dmproto.DMProto;

public class RegionGroup {
  private static int nextID = 1;

  public static enum State {
    HIDDEN,
    VISIBLE,
    FOGGED,
  }

  public Map<Integer, Region> children = new HashMap<>();
  public final int id;
  public State state = State.HIDDEN;

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

  public RegionGroup() {
    id = nextID;
    nextID++;
  }

  public void serializeInto(final DMProto.Map.Builder map) {
    final DMProto.Group.Builder group = DMProto.Group.newBuilder();
    switch (state) {
      case HIDDEN:
        group.setVisibility(DMProto.Group.State.HIDDEN);
        break;
      case VISIBLE:
        group.setVisibility(DMProto.Group.State.VISIBLE);
        break;
      case FOGGED:
        group.setVisibility(DMProto.Group.State.FOGGED);
        break;
      default:
        throw new IllegalStateException("Unknown visibility state: " + state);
    }
    for (final Region child : children.values()) {
      if (child.isAvatar) {
        map.addAvatar(child.serializeAsAvatar());
      }
      else {
        group.addRegion(child.serializeAsRegion());
      }
    }
    map.addRegionGroup(group.build());
  }

  public void load(final DMProto.Group group) {
    children.clear();
    switch (group.getVisibility()) {
      case HIDDEN:
        state = State.HIDDEN;
        break;
      case VISIBLE:
        state = State.VISIBLE;
        break;
      case FOGGED:
        state = State.FOGGED;
        break;
      default:
        // TODO: Support Fogged.
        state = State.HIDDEN;
    }
    for (final DMProto.Region region : group.getRegionList()) {
      final Region r = new Region();
      r.parent = this;
      r.load(region);
      children.put(r.id, r);
    }
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

  public boolean isFogged() {
    return state == State.FOGGED;
  }

  public void toggleState() {
    // Hidden -> Visible <-> Fogged
    if (state == State.VISIBLE) {
      state = State.FOGGED;
    }
    else {
      state = State.VISIBLE;
    }
  }

  public void toggleVisibility() {
    // Fogged -> Hidden <-> Visible
    if (state == State.HIDDEN) {
      state = State.VISIBLE;
    }
    else {
      state = State.HIDDEN;
    }
  }
}


package dmtool;

import java.awt.Color;

public class Region {
  private static int nextID = 1;
  public RegionGroup parent;
  public final int id;
  public int x, y;
  public int w, h;

  public boolean isAvatar;
  public boolean isDead;
  public char symbol;
  public Color color;
  public Integer fontSize; // Needs to be recalculated on resize.

  public static enum State {
    HIDDEN,
    VISIBLE,
  }

  public State state = State.HIDDEN;

  public Region(final RegionGroup parent, final int x, final int y, final int w, final int h) {
    id = nextID;
    nextID++;

    this.parent = parent;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  boolean isVisible() {
    return state == State.VISIBLE;
  }

  void toggleState() {
    if (isAvatar) {
      isDead = !isDead;
      return;
    }
    if (state == State.VISIBLE) {
      state = State.HIDDEN; // TODO: Switch to Fogged once implemented.
    }
    else {
      state = State.VISIBLE;
    }
  }

  int getX() {
    int x = this.x;
    if (parent != null) {
      x += parent.x;
    }
    return x;
  }

  int getY() {
    int y = this.y;
    if (parent != null) {
      y += parent.y;
    }
    return y;
  }

  boolean scaledContains(final double scale, final int x, final int y) {
    return scale * getX() <= x && x <= scale * (getX() + w) && scale * getY() <= y &&
           y <= scale * (getY() + h);
  }

  @Override
  public Region clone() {
    final Region copy = new Region(parent, x, y, w, h);
    copy.state = state;
    return copy;
  }
}

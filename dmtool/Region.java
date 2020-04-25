
package dmtool;

public class Region {
  private static int nextID = 1;
  public RegionGroup parent;
  public final int id;
  public int x, y;
  public int w, h;

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

  int getX() {
    return x + parent.x;
  }

  int getY() {
    return y + parent.y;
  }

  boolean scaledContains(final double scale, final int x, final int y) {
    return scale * getX() <= x && x <= scale * (getX() + w) && scale * getY() <= y && y <= scale * (getY() + h);
  }

  @Override
  public Region clone() {
    final Region copy = new Region(parent, x, y, w, h);
    copy.state = state;
    return copy;
  }
}

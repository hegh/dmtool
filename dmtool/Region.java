
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
    return parent.isVisible();
  }

  void toggleState() {
    if (isAvatar) {
      isDead = !isDead;
      return;
    }
    parent.toggleState();
  }

  void adjustDims(final int dx, final int dy, final int dw, final int dh) {
    final int ox = getX();
    final int oy = getY();
    final int ow = getW();
    final int oh = getH();
    x = ox + dx;
    y = oy + dy;
    w = ow + dw;
    h = oh + dh;
  }

  int getX() {
    int x = this.x;
    if (w < 0) {
      x += w;
    }
    return x;
  }

  int getY() {
    int y = this.y;
    if (h < 0) {
      y += h;
    }
    return y;
  }

  int getW() {
    return Math.abs(w);
  }

  int getH() {
    return Math.abs(h);
  }

  boolean scaledContains(final double scale, final int x, final int y) {
    return scale * getX() <= x && x <= scale * (getX() + w) && scale * getY() <= y &&
           y <= scale * (getY() + h);
  }

  @Override
  public Region clone() {
    final Region copy = new Region(parent, x, y, w, h);
    return copy;
  }
}

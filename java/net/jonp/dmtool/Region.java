
package net.jonp.dmtool;

import java.awt.Color;

import net.jonp.dmtool.dmproto.DMProto;

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

  // This is for convenience, to make duplications easier.
  // Do not clone this property.
  int nextDupPosition = 0;

  @Override
  public Region clone() {
    final Region copy = new Region(parent, x, y, w, h);
    copy.isAvatar = isAvatar;
    copy.isDead = isDead;
    copy.symbol = symbol;
    copy.color = color;
    copy.fontSize = fontSize;
    parent.addChild(copy);
    return copy;
  }

  public Region() {
    id = nextID;
    nextID++;
  }

  public Region(final RegionGroup parent, final int x, final int y, final int w, final int h) {
    id = nextID;
    nextID++;

    this.parent = parent;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  public DMProto.Region serializeAsRegion() {
    final DMProto.Region.Builder region = DMProto.Region.newBuilder();
    region.setRect(serializeRect());
    return region.build();
  }

  public void load(final DMProto.Region region) {
    load(region.getRect());
  }

  public DMProto.Avatar serializeAsAvatar() {
    final DMProto.Avatar.Builder avatar = DMProto.Avatar.newBuilder();
    avatar.setIsDead(isDead);
    avatar.setSymbol(Character.toString(symbol));
    avatar.setColor(serializeColor());
    avatar.setRect(serializeRect());
    return avatar.build();
  }

  public void load(final DMProto.Avatar avatar) {
    isDead = avatar.getIsDead();
    if (avatar.getSymbol().length() == 0) {
      symbol = '?';
    }
    else {
      symbol = avatar.getSymbol().charAt(0);
    }
    load(avatar.getColor());
    load(avatar.getRect());
  }

  private DMProto.RGBColor serializeColor() {
    return DMProto.RGBColor.newBuilder() //
      .setR(color.getRed()) //
      .setG(color.getGreen()) //
      .setB(color.getBlue()) //
      .build();
  }

  private void load(final DMProto.RGBColor c) {
    color = new Color(c.getR(), c.getG(), c.getB());
  }

  private DMProto.Rect serializeRect() {
    return DMProto.Rect.newBuilder() //
      .setX(getX()) //
      .setY(getY()) //
      .setW(getW()) //
      .setH(getH()) //
      .build();
  }

  private void load(final DMProto.Rect r) {
    x = r.getX();
    y = r.getY();
    w = r.getW();
    h = r.getH();
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
}
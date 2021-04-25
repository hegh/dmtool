
package net.jonp.dmtool;

import java.awt.Color;

import net.jonp.dmtool.dmproto.DMProto;

public class Region {
  public static enum Type {
    REGION,
    AVATAR,
    AREA,
  }

  public static enum Shape {
    RECTANGLE,
    ARC,
  }

  // Everything has these properties.
  private static int nextID = 1;
  public RegionGroup parent;
  public final int id;
  public int x, y;
  public int w, h;
  public Type type;

  // This is for convenience, to make duplications easier.
  // Do not clone this property.
  int nextDupPosition = 0;

  // Areas only:
  public Shape shape;
  public int rotation = 0;
  public int internalAngle = 360; // For arcs.

  // Areas and Avatars:
  public boolean isInvisible;

  // Avatars only:
  public boolean isDead;
  public char symbol;
  public int index; // To tell apart avatars with the same symbol.
  public Color color;
  public Integer fontSize; // Needs to be recalculated on resize.
  public double lastZoomLevel = 1.0; // Recalculate font on zoom change.

  @Override
  public Region clone() {
    final Region copy = new Region(parent, x, y, w, h);
    copy.type = type;
    copy.shape = shape;
    copy.isDead = isDead;
    copy.isInvisible = isInvisible;
    copy.symbol = symbol;
    copy.index = index;
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

    type = Type.REGION;
    shape = Shape.RECTANGLE;
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
    type = Type.REGION;
    load(region.getRect());
  }

  public DMProto.Avatar serializeAsAvatar() {
    final DMProto.Avatar.Builder avatar = DMProto.Avatar.newBuilder();
    avatar.setIsDead(isDead);
    avatar.setIsInvisible(isInvisible);
    avatar.setSymbol(Character.toString(symbol));
    avatar.setIndex(index);
    avatar.setColor(serializeColor());
    avatar.setRect(serializeRect());
    return avatar.build();
  }

  public void load(final DMProto.Avatar avatar) {
    type = Type.AVATAR;
    isDead = avatar.getIsDead();
    isInvisible = avatar.getIsInvisible();
    if (avatar.getSymbol().length() == 0) {
      symbol = '?';
    }
    else {
      symbol = avatar.getSymbol().charAt(0);
    }
    index = avatar.getIndex();
    load(avatar.getColor());
    load(avatar.getRect());
  }

  public DMProto.Area serializeAsArea() {
    final DMProto.Area.Builder area = DMProto.Area.newBuilder();
    area.setIsInvisible(isInvisible);
    area.setColor(serializeColor());
    area.setRect(serializeRect());
    switch (shape) {
      case RECTANGLE:
        area.setShape(DMProto.Area.Shape.RECTANGLE);
        break;
      case ARC:
        area.setShape(DMProto.Area.Shape.ARC);
        break;
    }
    area.setRotation(rotation);
    area.setInternalAngle(internalAngle);
    return area.build();
  }

  public void load(final DMProto.Area area) {
    type = Type.AREA;
    isInvisible = area.getIsInvisible();
    load(area.getColor());
    load(area.getRect());
    switch (area.getShape()) {
      case RECTANGLE:
        shape = Shape.RECTANGLE;
        break;
      case ARC:
        shape = Shape.ARC;
        break;
      default:
        shape = Shape.RECTANGLE;
    }
    rotation = area.getRotation();
    internalAngle = area.getInternalAngle();
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

  boolean isRegion() {
    return type == Type.REGION;
  }

  boolean isAvatar() {
    return type == Type.AVATAR;
  }

  boolean isArea() {
    return type == Type.AREA;
  }

  boolean isRegionVisible() {
    return parent.isVisible();
  }

  boolean isRegionFogged() {
    return parent.isFogged();
  }

  boolean isAvatarVisible() {
    return !isInvisible;
  }

  boolean isAreaVisible() {
    return !isInvisible;
  }

  void toggleState() {
    switch (type) {
      case AVATAR:
        isDead = !isDead;
        break;
      case AREA:
        isInvisible = !isInvisible;
        break;
      case REGION:
        parent.toggleState();
        break;
    }
  }

  void toggleAvatarVisibility() {
    if (!isAvatar()) {
      return;
    }
    isInvisible = !isInvisible;
  }

  void toggleAreaVisibility() {
    if (!isArea()) {
      return;
    }
    isInvisible = !isInvisible;
  }

  void toggleRegionVisibility() {
    parent.toggleVisibility();
  }

  void toggleShape() {
    if (!isArea()) {
      return;
    }
    switch (shape) {
      case RECTANGLE:
        shape = Shape.ARC;
        break;
      case ARC:
        shape = Shape.RECTANGLE;
        break;
    }
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

  public boolean intersects(final Region r) {
    if (r.x + r.w < x) {
      return false; // Too far to the left.
    }
    if (r.x > x + w) {
      return false; // Too far to the right.
    }
    if (r.y + r.h < y) {
      return false; // Too high.
    }
    if (r.y > y + h) {
      return false; // Too low.
    }
    return true;
  }
}

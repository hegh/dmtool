
package dmtool;

public class RegionGroup {
  private static int nextID = 1;

  public final int id;
  public int x, y;

  public RegionGroup() {
    id = nextID;
    nextID++;
  }

  @Override
  public RegionGroup clone() {
    final RegionGroup copy = new RegionGroup();
    copy.x = x;
    copy.y = y;
    return copy;
  }
}

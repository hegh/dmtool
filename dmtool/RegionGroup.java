
package dmtool;

public class RegionGroup {
  private static int nextID = 1;

  public final int id;
  // TODO: Add visibility.

  public RegionGroup() {
    id = nextID;
    nextID++;
  }

  @Override
  public RegionGroup clone() {
    final RegionGroup copy = new RegionGroup();
    return copy;
  }
}

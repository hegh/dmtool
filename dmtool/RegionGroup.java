
package dmtool;

public class RegionGroup {
  private static int nextID = 1;

  public static enum State {
    HIDDEN,
    VISIBLE,
  }

  public final int id;
  public State state = State.HIDDEN;

  public RegionGroup() {
    id = nextID;
    nextID++;
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
    return copy;
  }
}

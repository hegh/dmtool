# DMTool - Screensharing tool for D&D maps

A tool to selectively share maps with players over a screen-sharing service like
Google Hangouts. Includes single-letter avatars that can move around, in and out
of visibility.

## Example Use

*Torgwyn, a roguish high elf, and Gearwood, a bardic gnome, enter the inn and
see an empty common room laid out before them. The tables still have scraps of
food and half-drunk mugs of ale, and are surrounded by tipped-over chairs
indicating whomever was here last beat a hasty retreat.*

The Dungeon Master, using the window on the right side of the screen, has
prepared the view that the players will see. The dim areas are hidden from the
player's view, but the blue regions allow the DM to quickly expose new rooms as
the players move through the area.

The players are probably watching the map over a screen-sharing video conference
service like Google Hangouts or Zoom, although this would also work with an
in-person setup, either using an external monitor/TV to display the Player View
window, or by switching between windows before showing the screen to the
players.

![Inn with an empty common room](img/DMToolEnterInn.png?raw=true)

*Gearwood, always a bit of an impatient hothead and both hungry and thirsty from
the day's travels, walks past the mess without really noticing it and throws
open the door to the south. He is startled to see three large orcs waiting to
ambush him.*

The DM exposes the room and its contents to the players by hovering over the
blue region with the mouse and pressing the space bar.

![Orc ambush exposed in the room to the south](img/DMToolDiscoverOrcs.png?raw=true)

*Torgwyn rushes to Gearwood's aid and quickly dispatches the closest orc,
pushing Gearwood to the side before he can get hurt. Little do they know,
however, that three more orcs were hiding in an alcove just off of the common
room to the east. Drawn by the noise, they rush out to join the battle.*

The DM marks the foremost orc dead by hovering over its avatar and pressing the
space bar. Then, to avoid exposing to the players where they came from, the DM
pauses Player View display updates by pressing Q before dragging the other three
orcs into view. By pressing Q again, the Player View is updated to show the new
enemies.

![First orc dead, but three more approach from behind](img/DMToolOrcBattle.png?raw=true)

*Gearwood throws a Shatter spell at the back wall of the store room with the two
orcs in it, killing both of them. He and Torgwyn then move to counter the
remaining three orcs, but are now at a disadvantage because Gearwood is out of
spells for the day. Luckily, their companion Ragnar, a human fighter, entered
at that moment after he finished tying up the horses.*

The DM marks the other two orcs to the south as dead by hovering over them and
pressing space bar, and then adds Ragnar by tapping the A key and choosing a
symbol and a color for the new character.

![First three orcs dead, and Ragnar joins the battle](img/DMToolOrcsHalfDefeated.png?raw=true)

## Instructions

When you start your screen sharing, make sure to specify that it only
shares the Player View window, **not** your entire screen.

In Windows there are some issues with sharing the Player View while it is
covered by another window. What seems to work best is half-screening the
Player and DM views side-by-side.

Everything requires hotkeys, and there are no instructions built into the
tool, so you'll want to become familiar with this chart. All controls are
through the DM view; the Player view can only be resized or closed.

| Key | Description |
|-----|-------------|
| F   | Pause/Unpause updates to the Player view. |
| A   | Create a new avatar to represent a player or NPC. Opens a dialog to choose a symbol and color. |
| R   | Create a new region to control visibility. Click/drag to draw the box. |
| Shift+R | Like R, but the new region shares visibility with the region that was under the cursor. Useful for masking oddly-shaped rooms. |
| Space | Toggle player visibility of the region under the cursor, or the life/death status of the avatar under the cursor. |
| V   | Toggle visibility of an avatar. Invisible avatars can only be seen by the DM. Does nothing for regions. |
| D   | Duplicate the region or avatar under the cursor. Repeated duplications will surround the original object. |
| Shift+D | Like D, but the new region shares visibility with the region that was under the cursor. For avatars, identical to D. |
| Alt+Wheel | Adjust avatar brightness. |
| Alt+Ctrl+Wheel | Adjust avatar saturation. |
| Alt+Shift+Wheel | Adjust avatar hue. |
| Click & Drag | Move/resize region or avatar. If it is a selected avatar, move/resize all selected avatars together. |
| Right-Click & Drag | Select all avatars in the area. |
| Right-Click | Toggle selection of an avatar.
| Off-click during Drag | Cancel operation. For example, while Right-click & Dragging a selection box, a Left-click will cancel the new selection. |
| Escape | Cancel new-region creation and de-select all avatars. |
| Backspace / Delete | Delete the region or avatar under the cursor. |
| Ctrl+N | Open a new image file to start a new map. Pauses the tool before switching to the new image. Clears the active save file. |
| Ctrl+S | Save the image, regions, and avatars to a ".dmap" file. Overwrites the active save, if there is one. |
| Ctrl+Shift+S | Save-As. |
| Ctrl+O | Open a ".dmap" file saved by this tool. |
| Ctrl+Q | Quit. Does not ask for confirmation. |
| Arrow keys | Scroll around the map. |
| Mouse Wheel | Scroll up/down. |
| Shift+Wheel | Scroll left/right. |
| Ctrl+Wheel | Zoon in/out. |

## Intended Usage

### Prepare

Open a new image with Ctrl+N and resize your windows (their sizes are locked
together) to a comfortable level.

Use R, Shift+R, D, and Shift+D to place regions over the rooms/areas of your map
that you will want players to see. Verify visibility looks good by toggling
these on/off with Space and looking at the Player View.

If you want to pre-place enemies, NPCs, or your expected players, use A and D to
create the avatars, then use your mouse to resize and drag them around the map.

Once you are happy, save the map with Ctrl+S.

### Play

Open a saved map with Ctrl+O.

Zoom and scroll around to where you want your players to start. By adjusting the
view like this, your players will not be able to guess at the size of the map
based on how it looks when it is displayed.

Place your player avatars onto the map with A, if they aren't already there.

Make your players' starting location(s) visible with Space.

Unpause Player View updates with Q.

As your players move through the map, drag their avatars around and toggle
visibility. If they get into a battle, mark enemies as dead with Space or by
deleting them with Delete or Backspace. If you need to add more NPCs, use A or
D.

If you need to make larger changes, pause the Player View with Q first to avoid
exposing areas or avatars that your players should not see yet. Pausing includes
updates to scrolling and zooming, so you are free to make all kinds of
adjustments without your players seeing anything.

I recommend *not* saving the map, so it can more easily be reused next time,
unless you need to stop for now and resume later. In that case, use
Ctrl+Shift+S to Save-As rather than overwriting your original map.

### Share

If you have permission (check the license where you found your map images),
share your saved maps with other DMs so they don't need to mark out all of the
regions themselves.

## Build & Run

### Prereqs

You must have these installed:
 * Bazel
   https://bazel.build/
 * A recent Java Development Kit
   https://www.oracle.com/java/technologies/javase-jdk11-downloads.html
   or
   `apt-get install openjdk-8-jdk`

Make sure your `JAVA_HOME` is set correctly, then run this command from the
root of the repository:
```bash
bazel build java:DMTool_deploy.jar
```

The resulting jar, `bazel-bin/DMTool_deploy.jar` will contain everything you
need to run the program. You can start it with
`java -jar bazel-bin/DMTool_deploy.jar`.

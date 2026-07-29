# IOUCreative

Do you sometimes just want to build, but don't want a world with unlimited resources?

Are you tired of always having to make a creative copy of your world,
then building the same thing again block by block in survival?

**What if you could just build what you want in creative,
then pay off the blocks for it later in survival?**

> _Please note:_ This mod is a small **proof of concept** for this "hybrid" game mode,
> If it turns out to be fun, I hope it can inspire someone to make a better version at some point.

## What does it do?
There are two main features:

- Separate inventories for creative & survival
- Keep a tally of all the blocks you place / break in creative

### Separate Inventories
If you ever accidentally cleared your hard-earned gear while popping into creative mode,
this feature is for you!

Whenever you switch game modes while the mod is loaded, it will store and restore your items 
and equipment for creative and survival mode automatically. _Think of it as having a chest
labeled "CREATIVE" for your creative inventory and one labeled "SURVIVAL" for your non-creative inventory._

### Keeping track of creative builds
While you are in creative, the mod will track all the blocks you break and place 
to keep count of how many of each item you owe.

> _Tip:_ If you quickly build something in creative to see if it works, then break it all again,
> you should (hopefully) end up with no additional debt compared to what you started with.

## How to use
This mod is fully server-side, so you only need to install it to your client if you are playing singleplayer, 
or you are the one hosting a game on LAN.

1. Once the mod is installed, your inventory (and equipment) will be stored separately for both game modes. 
_(Spectator & adventure mode technically use the "SURVIVAL" inventory too.)_

2. You can then also switch to creative mode at any time and just start building, once you switch back
to another game mode, you will see the updated list of items you owe.
You may check this list at any time using the `/iouc list` command.

3. In survival, use the `/iouc pay <owed_item> [<quantity>]` command to pay off your debt while having the
item in your inventory. If you do not enter a specific quantity, it defaults to a stack at a time, 
you may also use _"all"_ to let it grab all the items in your inventory.

> _Tip:_ You can **click on an item in the list** to automatically fill in the payout command for that item.

### Additional details
The mod stores all of its custom data within the overworld dimension's data folder (under 
`dimensions/minecraft/overworld/data/ioucreative`). It does not directly modify any vanilla inventory data, 
as such, it should be completely safe to remove the mod without the risk of corruption, but as always 
_**please back up your worlds before installing**_ it.

### Some problems & open questions
- Multi-block items (beds, doors, large plants) are only counted when the "lower" half is broken.
- Entities and other special items (boats, item-frames, etc.) are not tracked
- Dropped items and items placed in inventories are not tracked
- What to do about blocks unobtainable in survival?
- Should players be able to "withdraw" negative debt items?
  - How do you avoid it becoming a free storage system?
- Should there be some sort of system for tracking group / community projects?
- _And many more..._

Feel free to share your own ideas ;)

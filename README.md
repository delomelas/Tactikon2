
## Here be dragons
### As they say

Here's most of the source code for Tactikon 2.

It's a mess - written mostly in a couple of months about 10 years ago. I learned Java and Android APIs as I went, and actively avoided importing dependencies preferring to write shoddy versions of everything I needed myself.
But it more or less worked.
Until it reached a point where it was unmaintainable and the burdon of keeping it up to date with the latest Android led to me giving up.

So here it is. If anyone wants to try to get it building again, feel free. I'd love to keep it in this repo - I'll be happy to add you as a contributor if you want to try to bring it back to life.

## GameServer
The back-end. Needs a refactor to use a real DB, or at least improve the locking on the sqlite database. Needs to clean up and remove old games, the DB was getting a bit bogged down after 5 years and about 10Gb of data.

## Tact2/gamelib
This was supposed to be everything sharable between similar multiplayer games - generic Android layouts for profile / game listings / network handling.

## Tact2/stateengine
The core of the turn-based engine. Handles passing around state objects and applying "Events" to the current state to do an action of some sort.

## Tact2/app
Everything Tactikon-specific, Android, OpenGL rendering, input

## Tact2/TactikonState
Definitions compatible with the StateEngine; defines the units and behaviours for the game

# What needs doing...
I mean, it probably needs a re-write from the ground up. But as a minimum to get it up and running again...
1. Build against up-to-date Android SDK
2. Rip out any billing and ads stuff
3. Replace anything that's particularly bad - AI, profiles, chat are all pretty terribke.
4. Replace the artisan OpenGL routines with an engine of some sort, SDL? Unity?
   

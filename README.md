# StatThing

A Minecraft Forge mod for querying and displaying player statistics in-game, with leaderboard support.

## Features

- Adds a `/score` command for viewing player statistics.
- Displays top stats for a given type and name in a leaderboard format.
- Shows your own rank and value on the leaderboard.
- Works by reading Minecraft's standard player stats from the server's stats directory.
- Built with Forge event-driven architecture and supports configuration via Forge's config system.

## Usage

After installing the mod on your server:

1. **Start your server** with StatThing installed.
2. **Use the `/score` command** in game:
   ```
   /score <stat_type> <name>
   ```
    - **`<stat_type>`**: [The registered stat type](https://minecraft.fandom.com/wiki/Statistics#Resource_location).
    - **`<name>`**: The specific stat key you want to query.

Example:
```
/score minecraft:mined minecraft:diamond_ore
```
This will show the leaderboard for players who have mined the most diamond ore.

## Building

This mod uses Gradle for building.

```sh
./gradlew build
```

The built `.jar` will appear in `build/libs/`.

## Installation

1. Download the compiled `.jar` from the releases or build it yourself.
2. Place it in your server's `mods` folder.

## Development

- Main mod entry point: [`Statthing.java`](src/main/java/com/github/antoj2/statthing/Statthing.java)
- Stat leaderboard logic: [`StatEntries.java`](src/main/java/com/github/antoj2/statthing/StatEntries.java)
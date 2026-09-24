LifeMod

A comprehensive Fabric mod for Minecraft 1.21.11 that implements a life system with TPA, combat tracking, and worldgen modifications.
Features
Player Lives System

    Every player starts with exactly 3 lives (configurable)
    Lives are displayed in the TAB player list using custom gold heart textures
    Death removes exactly 1 life
    PvP kills do NOT transfer lives (no life stealing)
    At 0 lives, the player is eliminated and banned

TPA System

    /tpa <player> - Send a teleport request
    /tpaccept - Accept a teleport request
    /tpdeny - Deny a teleport request
    Exact 5-second countdown before teleport (configurable)
    Taking damage cancels the teleport
    Combat state cancels the teleport
    Death, disconnect, or invalid target cancels the teleport

Combat System

    40-second combat timer (configurable)
    25-block escape distance requirement (configurable)
    Combat tag ends only when BOTH conditions are met:
        At least 40 seconds have passed without qualifying PvP combat
        The player is at least 25 blocks away from the attacker
    Combat logging results in death, life loss, and head drop

Death and Player Heads

    Every death drops exactly one player head with the deceased player's actual skin/profile
    Player heads use genuine Minecraft player-head items with profile data

Revival System

    Revive Beacon item can be crafted and used to revive eliminated players
    Revival gives exactly 1 life (not 3)
    Elimination bans are distinguished from manual bans

Crafting Recipes
Life Item (grants +1 life when used):

[Nautilus Shell] [Enchanted Golden Apple] [Nautilus Shell]
[Totem of Undying] [Nether Star] [Totem of Undying]
[Nautilus Shell] [Enchanted Golden Apple] [Nautilus Shell]
text
 
  
 
 

#### Revive Beacon:
 
 

[Totem of Undying] [Dead Player's Head] [Totem of Undying]
[Netherite Ingot] [Beacon] [Netherite Ingot]
[Totem of Undying] [Netherite Ingot] [Totem of Undying]
text
 
  
 
 

### World Generation
- Ominous Vaults are disabled (normal Vaults continue working)
- Ancient Debris generation is significantly rarer

## Building

1. Ensure you have Java 21 installed
2. Clone this repository
3. Run `./gradlew build`
4. The compiled JAR will be in `build/libs/`

## Installation

1. Install Fabric Loader for Minecraft 1.21.11
2. Install Fabric API 0.141.2+1.21.11
3. Place the LifeMod JAR in your `mods` folder
4. Start the server

## Configuration

Configuration file is located at `config/lifemod.json` and contains:
- `startingLives`: Starting number of lives (default: 3)
- `maxLives`: Maximum lives (default: 10)
- `tpaDelaySeconds`: TPA countdown delay (default: 5)
- `ancientDebrisRarityMultiplier`: Ancient Debris rarity (default: 0.3, lower = rarer)
- `combatTimeoutSeconds`: Combat timeout (default: 40)
- `combatEscapeDistanceBlocks`: Combat escape distance (default: 25)
- `deathHeadDropsEnabled`: Whether death head drops are enabled (default: true)
- `livesGrantedByRevival`: Lives granted on revival (default: 1)
- `maxRevivals`: Maximum revivals per player (-1 = unlimited, default: -1)

## Commands

- `/tpa <player>` - Send a teleport request
- `/tpaccept` - Accept a teleport request
- `/tpdeny` - Deny a teleport request
- `/lives` - Check your current life count

## Technical Notes

- This mod is primarily server-side
- The custom gold heart textures in the TAB list require the mod to be installed on the client
- Player life data persists across server restarts using PersistentState
- The mod uses UUID as the primary player identity
- All important game-state changes are validated server-side
 
 
Important Notes on Implementation

    Custom Font for TAB List: The gold heart display in the TAB list uses a custom font with Unicode private use area characters (\uE001 for gold heart). The font definition references the gold_heart.png texture file that should be placed in src/main/resources/assets/lifemod/textures/gui/gold_heart.png. The texture should be a 9-pixel tall, 8-pixel wide image containing the gold heart sprites.

    Ominous Vault Disabling: The mixin approach intercepts structure pool generation. A more complete implementation would need to specifically identify Trial Chamber structures and remove Ominous Vault pieces. The approach used in the "Remove Ominous Vault" mod involves scanning Trial Chamber chunks and replacing Ominous Vaults with air.

    Ancient Debris Rarity: The mixin on OreFeature.generate intercepts ore generation and applies a rarity check. When the random value exceeds the configured multiplier, the generation is cancelled, making Ancient Debris significantly rarer.

    Persistent Data: Life data is stored using PersistentState which automatically saves to the world's data folder and survives server restarts.

    Combat System: The combat manager tracks PvP interactions with a 40-second timer and 25-block distance requirement. Both conditions must be met for the combat tag to be removed.

    Networking: Custom payloads are defined for client-server communication to keep the UI updated with life counts and combat status.
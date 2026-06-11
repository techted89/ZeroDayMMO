# IPv4 World System - ZeroDayMMO

## Overview

The IPv4 World System transforms the entire IPv4 address space (4.3 billion addresses) into a massive persistent world where each IP address represents a unique tile that players can claim, build on, and defend. This creates a thematically perfect cyberpunk hacking MMO where players control "territory" in the network.

## Architecture

### Core Components

1. **IPv4WorldManager** - Central world state manager
2. **FactionControlSystem** - Faction and territory management
3. **BaseBuildingSystem** - Structure building and upgrades
4. **ThreatSpawnSystem** - Monster/virus spawning
5. **IPv4WorldMapUI** - World visualization and interaction

### World Structure

```
IPv4 Address Space (0.0.0.0 - 255.255.255.255)
├── 6 Major Regions (/8 subnets)
│   ├── ARIN_North (0.0.0.0/8)
│   ├── RIPE_Europe (32.0.0.0/8)
│   ├── APNIC_Asia (64.0.0.0/8)
│   ├── LACNIC_LatAm (128.0.0.0/8)
│   ├── AFRINIC_Africa (192.0.0.0/8)
│   └── DarkNet (224.0.0.0/8)
├── Chunks (256x256 tiles each)
└── Individual IP Tiles (32x32 world units)
```

## IPv4 Address Mapping

### Address to World Coordinates

Each IPv4 address maps to a unique 2D world position:

```csharp
// IPv4: A.B.C.D
// World X = (C << 8) | D  (0-65535)
// World Y = (A << 8) | B  (0-65535)

IPv4Address ip = new IPv4Address(192, 168, 1, 100);
Vector2I worldPos = ip.ToWorldPosition(); // (356, 49320)
```

### Biome Mapping

IP ranges determine biome type:

| IP Range | Biome | Description |
|----------|-------|-------------|
| 0.0.0.0/5 | DeepNet | Core infrastructure, high-value targets |
| 32.0.0.0/6 | CoreNetwork | High-traffic backbone |
| 64.0.0.0/7 | DataStreams | Flowing data regions |
| 128.0.0.0/7 | Firewall | Protected zones |
| 192.0.0.0/6 | Perimeter | Edge networks |
| 224.0.0.0/3 | DarkWeb | Dangerous uncharted territory |

## Territory Control

### Claiming Tiles

Players claim IP tiles for their faction:

```csharp
// Player claims an IP tile
var success = factionControl.ClaimTerritory(playerId, new IPv4Address("10.0.0.1"));

// Tile is now owned by the player's faction
var tile = worldManager.GetTile("10.0.0.1");
Console.WriteLine($"Owner: {tile.OwnerFaction}");
```

### Region Control

Factions gain control of entire regions by owning 50%+ of tiles:

```csharp
// Check faction territory
var faction = factionControl.GetPlayerFaction(playerId);
Console.WriteLine($"Controlled Regions: {faction.ControlledRegions.Count}");
Console.WriteLine($"Controlled Tiles: {faction.ControlledTiles.Count}");
```

### Territory Warfare

Factions can attack contested territory:

```csharp
// Attack enemy territory
var success = factionControl.AttackTerritory(
    attackerFactionId, 
    defenderFactionId, 
    targetIP
);
```

## Base Building

### Structure Types

**Production:**
- Server Cluster - Generates processing power
- Data Center - Stores and processes data
- Mining Rig - Mines cryptocurrency

**Defense:**
- Firewall - Protects from attacks
- IDS System - Detects threats
- Honeypot - Traps attackers

**Utility:**
- Power Generator - Powers other structures
- Network Hub - Increases bandwidth

**Special:**
- Command Center - Faction operations hub (1 per faction)
- Research Lab - Unlocks technologies

### Building Structures

```csharp
// Build a firewall on owned tile
var result = baseBuilding.BuildStructure(
    playerId, 
    new IPv4Address("10.0.0.1"), 
    "firewall"
);

if (result.Success)
{
    Console.WriteLine($"Built {result.Structure.Type} at {result.Address}");
}
```

### Structure Upgrades

```csharp
// Upgrade existing structure
var upgraded = baseBuilding.UpgradeStructure(
    playerId,
    ip,
    structureId
);
```

## Threat System

### Threat Types

**Low-Level (1-3):**
- Basic Virus - Simple malware
- Trojan Horse - Disguised threat
- Network Worm - Spreads rapidly

**Medium-Level (4-5):**
- Ransomware - Locks down tiles
- Botnet Node - Coordinated attacks

**High-Level (8-10):**
- AI Hunter - Autonomous predator
- Zero-Day Exploit - Bypasses defenses

**Boss-Level (15):**
- APT (Advanced Persistent Threat) - State-sponsored weapon

### Spawn Mechanics

Threats spawn in uncontrolled or contested tiles:

```csharp
// Threats spawn based on:
// - Tile ownership (unowned = higher chance)
// - Biome type (DarkWeb = more threats)
// - Nearby structures (attracts threats)
// - Time (threats escalate over time)
```

### Combat

```csharp
// Attack a threat
var defeated = threatSpawn.DamageThreat(
    ip,
    threatId,
    damage
);

if (defeated)
{
    var rewards = threatSpawn.GetThreatRewards(threat);
    // rewards: { "credits": 500, "data": 100 }
}
```

## Faction System

### Faction Types

- **Collective** - Decentralized hacker groups
- **Corporation** - Corporate security forces
- **Criminal** - Underground networks
- **Research** - Ethical security researchers
- **Government** - State agencies
- **Independent** - Solo operators

### Faction Resources

Factions share resources:
- Credits - Currency
- Bandwidth - Network capacity
- Processing - Compute power
- Data - Information assets
- Hardware - Physical equipment
- Crypto - Cryptocurrency

### Faction Hierarchy

```csharp
// Join a faction
factionControl.JoinFaction(playerId, factionId);

// Leave faction
factionControl.LeaveFaction(playerId);

// Get faction info
var faction = factionControl.GetPlayerFaction(playerId);
Console.WriteLine($"Faction: {faction.Name}");
Console.WriteLine($"Members: {faction.Members.Count}");
Console.WriteLine($"Territory: {faction.ControlledTiles.Count} tiles");
```

## World Map UI

### Navigation

- **Arrow Keys** - Pan camera
- **Page Up/Down** - Zoom in/out
- **Left Click** - Select tile
- **Enter** - Claim selected tile
- **B** - Build structure
- **T** - Attack threat

### Visual Indicators

- **Gray** - Unowned tiles
- **Faction Colors** - Owned territory
- **Orange** - Contested tiles
- **Red (pulsing)** - Active threats

### Tile Information

Selected tile shows:
- IP Address
- Owner faction
- Biome type
- Active threats
- Built structures

## Performance

### Chunking System

World is divided into 256x256 tile chunks:
- Only visible chunks are loaded
- Max 100 chunks loaded simultaneously
- Oldest chunks evicted when limit reached

### Optimization

```csharp
// Configure chunk size
worldManager.ChunkSize = 256;
worldManager.MaxLoadedChunks = 100;

// Get chunk for IP
var chunk = worldManager.GetChunkForAddress(ip);
```

## Integration

### Setting Up the World

```csharp
// In your main scene
var worldManager = GetNode<IPv4WorldManager>("IPv4WorldManager");
var factionControl = GetNode<FactionControlSystem>("FactionControlSystem");
var baseBuilding = GetNode<BaseBuildingSystem>("BaseBuildingSystem");
var threatSpawn = GetNode<ThreatSpawnSystem>("ThreatSpawnSystem");
var worldMapUI = GetNode<IPv4WorldMapUI>("WorldMapUI");

// Set current player
worldMapUI.SetCurrentPlayer(playerId);
```

### Event Handling

```csharp
// Subscribe to world events
worldManager.OnTileClaimed += (tile) => {
    Console.WriteLine($"Tile {tile.Address} claimed by {tile.OwnerFaction}");
};

factionControl.OnTerritoryGained += (faction, region) => {
    Console.WriteLine($"{faction.Name} gained {region.Name}");
};

threatSpawn.OnThreatSpawned += (threat) => {
    Console.WriteLine($"{threat.Type.Name} spawned at {threat.Address}");
};
```

## Testing

Run the test scene:

```bash
# Open in Godot
godot Scenes/IPv4WorldTest.tscn

# Or run directly
godot --path . -s Scenes/IPv4WorldTest.tscn
```

## Future Enhancements

### Planned Features

1. **Dynamic Events** - Network-wide incidents
2. **IP Auctions** - Bid on valuable addresses
3. **Subnet Specialization** - Bonuses for controlling /24 blocks
4. **Cross-Region Operations** - Multi-faction raids
5. **Persistent World** - Server-side state management
6. **Real-time PvP** - Live territory battles
7. **AI Directors** - Dynamic threat scaling
8. **Seasonal Events** - Time-limited challenges

### Extension Points

```csharp
// Add custom threat types
threatSpawn.AddThreatType(new ThreatType {
    Id = "custom_threat",
    Name = "Custom Malware",
    ThreatLevel = 7,
    // ...
});

// Add custom structures
baseBuilding.AddBlueprint(new StructureBlueprint {
    Id = "custom_structure",
    Name = "Custom Building",
    // ...
});

// Add custom biomes
// Extend TileBiome enum and biome mapping logic
```

## Files

### Core Systems
- `Scripts/World/IPv4Address.cs` - IP address and tile data structures
- `Scripts/World/IPv4WorldManager.cs` - World state management
- `Scripts/World/FactionControlSystem.cs` - Faction and territory
- `Scripts/World/BaseBuildingSystem.cs` - Structure building
- `Scripts/World/ThreatSpawnSystem.cs` - Threat spawning

### UI
- `Scripts/World/IPv4WorldMapUI.cs` - World map interface
- `Scripts/World/IPv4WorldTestController.cs` - Test scene controller

### Scenes
- `Scenes/IPv4WorldTest.tscn` - Test scene with all systems

## Credits

- **Concept**: IPv4-based world system for ZeroDayMMO
- **Implementation**: C# / Godot 4.x
- **Theme**: Cyberpunk hacking MMO

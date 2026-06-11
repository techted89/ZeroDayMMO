# ZeroDayMMO - Game Launcher

Simple TUI (Text User Interface) launcher for selecting and running game scenes.

## Quick Start

```bash
cd /root/ZeroDayMMO/zeroday-client-godot
./launcher.sh
```

## Installation

### Automatic Installation

Run the launcher with the `--install` flag:

```bash
./launcher.sh --install
```

This will automatically install:
- Godot Engine 4.x
- .NET SDK 8.0 (for C# support)
- Build the C# project

### Manual Installation

#### 1. Install Godot Engine

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install godot3
```

**Fedora:**
```bash
sudo dnf install godot
```

**Arch Linux:**
```bash
sudo pacman -S godot
```

**Other Systems:**
Download from https://godotengine.org/download

#### 2. Install .NET SDK

**Ubuntu/Debian:**
```bash
wget https://packages.microsoft.com/config/ubuntu/$(lsb_release -rs)/packages-microsoft-prod.deb -O packages-microsoft-prod.deb
sudo dpkg -i packages-microsoft-prod.deb
rm packages-microsoft-prod.deb
sudo apt update
sudo apt install dotnet-sdk-8.0
```

**Fedora:**
```bash
sudo dnf install dotnet-sdk-8.0
```

**Arch Linux:**
```bash
sudo pacman -S dotnet-sdk
```

**Other Systems:**
Download from https://dotnet.microsoft.com/download

#### 3. Build the Project

```bash
cd /root/ZeroDayMMO/zeroday-client-godot
dotnet build
```

## Usage

### Interactive Menu

```bash
./launcher.sh
```

You'll see a menu with available scenes:

```
1) Hacking Terminal
   Test the hacking command-line interface

2) IPv4 World System
   Explore the IP-based world and claim territory

3) Player Movement
   Test character movement and animations

4) Full Integration
   All systems combined

0) Exit
```

### Command Line Options

```bash
# Install dependencies
./launcher.sh --install

# Check dependencies only
./launcher.sh --check

# Show help
./launcher.sh --help
```

## Available Scenes

### 1. Hacking Terminal (`HackingTest.tscn`)

Test the realistic hacking command-line interface.

**Controls:**
- Type commands in the terminal
- Press Enter to execute
- F1/F2/F3 for quick info

**Try these commands:**
```bash
$ help
$ nmap 192.168.1.100
$ searchsploit sql
$ exploit sqli_001 192.168.1.100
$ status
```

### 2. IPv4 World System (`IPv4WorldTest.tscn`)

Explore the IPv4-based world and claim territory.

**Controls:**
- Arrow keys - Pan camera
- Page Up/Down - Zoom
- Left Click - Select tile
- Enter - Claim tile
- B - Build structure
- T - Attack threat

### 3. Player Movement (`GameplayTest.tscn`)

Test character movement and sprite animations.

**Controls:**
- Arrow keys - Move character
- Space - Attack
- Tab - Toggle stealth mode
- R - Respawn (when dead)

### 4. Full Integration (`FullIntegrationTest.tscn`)

All systems working together.

**Controls:**
- Arrow keys - Move player
- Space - Attack
- Tab - Stealth mode / Toggle terminal
- F1 - Help
- Esc - Quit

## Troubleshooting

### Godot Not Found

```bash
# Check if Godot is installed
which godot

# If not found, install it
./launcher.sh --install
```

### .NET SDK Not Found

```bash
# Check if dotnet is installed
dotnet --version

# If not found, install it
./launcher.sh --install
```

### Build Errors

```bash
# Clean and rebuild
cd /root/ZeroDayMMO/zeroday-client-godot
dotnet clean
dotnet build
```

### Scene Won't Load

1. Check that all `.cs` files compile:
   ```bash
   dotnet build
   ```

2. Verify scene files exist:
   ```bash
   ls -lh Scenes/*.tscn
   ```

3. Check Godot console output for errors

### Permission Denied

```bash
chmod +x launcher.sh
```

## System Requirements

- **OS:** Linux (Ubuntu 20.04+, Fedora 35+, Arch Linux)
- **Godot:** 4.0 or higher
- **.NET SDK:** 8.0 or higher
- **RAM:** 4GB minimum
- **GPU:** OpenGL 3.3 compatible

## Project Structure

```
zeroday-client-godot/
├── launcher.sh              # This launcher script
├── Scenes/
│   ├── HackingTest.tscn
│   ├── IPv4WorldTest.tscn
│   ├── GameplayTest.tscn
│   └── FullIntegrationTest.tscn
├── Scripts/
│   ├── World/
│   │   ├── IPv4WorldManager.cs
│   │   ├── FactionControlSystem.cs
│   │   ├── ThreatSpawnSystem.cs
│   │   └── BaseBuildingSystem.cs
│   ├── Terminal/
│   │   ├── HackingTerminalSystem.cs
│   │   ├── ExploitDatabase.cs
│   │   └── NetworkScanner.cs
│   └── Player/
│       └── PlayerController.cs
└── Assets/
    ├── Sprites/
    └── Fonts/
```

## Development

### Adding New Scenes

1. Create your scene in Godot editor
2. Save it to `Scenes/YourScene.tscn`
3. Add it to the launcher script:

```bash
# Edit launcher.sh and add to SCENES array
declare -A SCENES=(
    # ... existing scenes ...
    ["5"]="YourScene.tscn|Your Scene Name|Description of your scene"
)
```

### Modifying the Launcher

The launcher is a bash script. Key sections:

- `SCENES` array - Available scenes
- `check_dependencies()` - Dependency checking
- `install_dependencies()` - Installation logic
- `launch_scene()` - Scene launching

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the main project documentation
3. Check Godot console output for errors

## License

See the main project LICENSE file.

# ZeroDayMMO - Quick Start Guide

## 🚀 Get Started in 3 Steps

### 1. Install Dependencies

```bash
cd /root/ZeroDayMMO/zeroday-client-godot
./install_dependencies.sh
```

### 2. Launch the Game

```bash
./launcher.sh
```

### 3. Select a Scene

Choose from the menu:
- **1** - Hacking Terminal (test hacking commands)
- **2** - IPv4 World (explore IP-based world)
- **3** - Player Movement (test character controls)
- **4** - Full Integration (all systems combined)

---

## 📋 What You Need

- **Godot Engine 4.0+** - Game engine
- **.NET SDK 8.0+** - For C# scripts
- **Linux** - Ubuntu, Fedora, or Arch

The installer will set these up automatically.

---

## 🎮 Available Scenes

### Hacking Terminal
Test realistic hacking commands:
```bash
$ help
$ nmap 192.168.1.100
$ exploit sqli_001 192.168.1.100
```

### IPv4 World System
Explore and claim IP addresses:
- Arrow keys to pan
- Click to select tiles
- Enter to claim territory

### Player Movement
Test character animations:
- Arrow keys to move
- Space to attack
- Tab for stealth mode

### Full Integration
Everything working together!

---

## 🔧 Commands

```bash
# Install dependencies
./install_dependencies.sh

# Launch game
./launcher.sh

# Check dependencies
./launcher.sh --check

# Show help
./launcher.sh --help
```

---

## ❓ Troubleshooting

**Godot not found?**
```bash
./launcher.sh --install
```

**Build errors?**
```bash
dotnet clean
dotnet build
```

**Scene won't load?**
Check Godot console output for errors.

---

## 📚 More Information

- `LAUNCHER_README.md` - Detailed launcher guide
- `DEPENDENCIES.txt` - Full dependency list
- `HACKING_TERMINAL_SYSTEM.md` - Hacking terminal guide
- `IPv4_WORLD_SYSTEM.md` - World system guide
- `SPRITE_INTEGRATION.md` - Sprite system guide

---

## 🎯 Quick Test

Try this sequence to test everything:

```bash
# 1. Install everything
./install_dependencies.sh

# 2. Launch game
./launcher.sh

# 3. Select "1" for Hacking Terminal

# 4. Try these commands:
help
nmap 192.168.1.100
searchsploit sql
exploit sqli_001 192.168.1.100
status

# 5. Exit and try "2" for IPv4 World
# 6. Exit and try "4" for Full Integration
```

---

## 🆘 Need Help?

1. Check `LAUNCHER_README.md` for detailed instructions
2. Review troubleshooting section
3. Check Godot console output
4. Verify dependencies: `./launcher.sh --check`

---

**Ready to hack the IPv4 world? Let's go!** 🚀

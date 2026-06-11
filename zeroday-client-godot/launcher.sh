#!/bin/bash

# ZeroDayMMO Game Launcher
# Simple TUI for selecting and launching game scenes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Project paths
PROJECT_DIR="/root/ZeroDayMMO/zeroday-client-godot"
SCENES_DIR="$PROJECT_DIR/Scenes"

# Available scenes
declare -A SCENES=(
    ["1"]="HackingTest.tscn|Hacking Terminal|Test the hacking command-line interface"
    ["2"]="IPv4WorldTest.tscn|IPv4 World System|Explore the IP-based world and claim territory"
    ["3"]="GameplayTest.tscn|Player Movement|Test character movement and animations"
    ["4"]="FullIntegrationTest.tscn|Full Integration|All systems combined"
)

# Print banner
print_banner() {
    echo -e "${CYAN}"
    cat << "EOF"
 ███████╗███████╗██████╗  ██████╗ ██████╗  █████╗ ██╗   ██╗
 ╚══███╔╝██╔════╝██╔══██╗██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝
   ███╔╝ █████╗  ██║  ██║██║   ██║██║  ██║███████║ ╚████╔╝ 
  ███╔╝  ██╔══╝  ██║  ██║██║   ██║██║  ██║██╔══██║  ╚██╔╝  
 ███████╗███████╗██████╔╝╚██████╔╝██████╔╝██║  ██║   ██║   
 ╚══════╝╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   
                                                              
 ███╗   ███╗███╗   ███╗ ██████╗ 
 ████╗ ████║████╗ ████║██╔═══██╗
 ██╔████╔██║██╔████╔██║██║   ██║
 ██║╚██╔╝██║██║╚██╔╝██║██║   ██║
 ██║ ╚═╝ ██║██║ ╚═╝ ██║╚██████╔╝
 ╚═╝     ╚═╝╚═╝     ╚═╝ ╚═════╝ 
                                 
EOF
    echo -e "${NC}"
    echo -e "${GREEN}IPv4-Based Hacking MMO - Game Launcher${NC}"
    echo ""
}

# Check dependencies
check_dependencies() {
    echo -e "${BLUE}Checking dependencies...${NC}"
    echo ""
    
    local missing_deps=()
    
    # Check Godot
    if command -v godot &> /dev/null; then
        echo -e "${GREEN}✓ Godot Engine: $(godot --version 2>/dev/null || echo 'installed')${NC}"
    else
        echo -e "${RED}✗ Godot Engine: NOT FOUND${NC}"
        missing_deps+=("godot")
    fi
    
    # Check .NET SDK (for C# support)
    if command -v dotnet &> /dev/null; then
        echo -e "${GREEN}✓ .NET SDK: $(dotnet --version)${NC}"
    else
        echo -e "${YELLOW}⚠ .NET SDK: NOT FOUND (required for C# scripts)${NC}"
        missing_deps+=("dotnet")
    fi
    
    # Check project files
    if [ -f "$PROJECT_DIR/project.godot" ]; then
        echo -e "${GREEN}✓ Project file: Found${NC}"
    else
        echo -e "${RED}✗ Project file: NOT FOUND at $PROJECT_DIR${NC}"
        missing_deps+=("project")
    fi
    
    # Check scenes directory
    if [ -d "$SCENES_DIR" ]; then
        local scene_count=$(ls -1 "$SCENES_DIR"/*.tscn 2>/dev/null | wc -l)
        echo -e "${GREEN}✓ Scenes: $scene_count found${NC}"
    else
        echo -e "${RED}✗ Scenes directory: NOT FOUND${NC}"
        missing_deps+=("scenes")
    fi
    
    # Check sprites
    if [ -d "$PROJECT_DIR/Assets/Sprites" ]; then
        local sprite_count=$(ls -1 "$PROJECT_DIR/Assets/Sprites"/*.jpg 2>/dev/null | wc -l)
        echo -e "${GREEN}✓ Sprites: $sprite_count found${NC}"
    else
        echo -e "${YELLOW}⚠ Sprites directory: NOT FOUND${NC}"
    fi
    
    echo ""
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        echo -e "${YELLOW}Missing dependencies detected. Run with --install to install them.${NC}"
        echo ""
        return 1
    fi
    
    return 0
}

# Install dependencies
install_dependencies() {
    echo -e "${BLUE}Installing dependencies...${NC}"
    echo ""
    
    # Detect OS
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$NAME
    else
        OS="Unknown"
    fi
    
    echo -e "${CYAN}Detected OS: $OS${NC}"
    echo ""
    
    # Install Godot
    if ! command -v godot &> /dev/null; then
        echo -e "${YELLOW}Installing Godot Engine...${NC}"
        
        if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
            echo -e "${CYAN}Using apt package manager...${NC}"
            sudo apt update
            sudo apt install -y godot3
            
        elif [[ "$OS" == *"Fedora"* ]]; then
            echo -e "${CYAN}Using dnf package manager...${NC}"
            sudo dnf install -y godot
            
        elif [[ "$OS" == *"Arch"* ]]; then
            echo -e "${CYAN}Using pacman package manager...${NC}"
            sudo pacman -S godot
            
        else
            echo -e "${YELLOW}Automatic installation not supported for $OS${NC}"
            echo -e "${CYAN}Please install Godot manually:${NC}"
            echo "  1. Download from: https://godotengine.org/download"
            echo "  2. Extract and add to PATH"
            echo ""
            read -p "Press Enter after installing Godot..."
        fi
    fi
    
    # Install .NET SDK
    if ! command -v dotnet &> /dev/null; then
        echo -e "${YELLOW}Installing .NET SDK...${NC}"
        
        if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
            echo -e "${CYAN}Using Microsoft package repository...${NC}"
            wget https://packages.microsoft.com/config/ubuntu/$(lsb_release -rs)/packages-microsoft-prod.deb -O packages-microsoft-prod.deb
            sudo dpkg -i packages-microsoft-prod.deb
            rm packages-microsoft-prod.deb
            sudo apt update
            sudo apt install -y dotnet-sdk-8.0
            
        elif [[ "$OS" == *"Fedora"* ]]; then
            echo -e "${CYAN}Using dnf package manager...${NC}"
            sudo dnf install -y dotnet-sdk-8.0
            
        elif [[ "$OS" == *"Arch"* ]]; then
            echo -e "${CYAN}Using pacman package manager...${NC}"
            sudo pacman -S dotnet-sdk
            
        else
            echo -e "${YELLOW}Automatic installation not supported for $OS${NC}"
            echo -e "${CYAN}Please install .NET SDK manually:${NC}"
            echo "  1. Download from: https://dotnet.microsoft.com/download"
            echo "  2. Follow installation instructions"
            echo ""
            read -p "Press Enter after installing .NET SDK..."
        fi
    fi
    
    # Build C# project
    if command -v dotnet &> /dev/null; then
        echo -e "${YELLOW}Building C# project...${NC}"
        cd "$PROJECT_DIR"
        dotnet build
        echo -e "${GREEN}✓ Build complete${NC}"
    fi
    
    echo ""
    echo -e "${GREEN}✓ All dependencies installed!${NC}"
    echo ""
    read -p "Press Enter to continue..."
}

# Show scene selection menu
show_menu() {
    echo -e "${CYAN}Select a scene to launch:${NC}"
    echo ""
    
    for key in "${!SCENES[@]}"; do
        IFS='|' read -r scene name desc <<< "${SCENES[$key]}"
        echo -e "  ${GREEN}$key)${NC} $name"
        echo -e "     ${YELLOW}$desc${NC}"
        echo ""
    done
    
    echo -e "  ${GREEN}0)${NC} Exit"
    echo ""
}

# Launch scene
launch_scene() {
    local choice=$1
    
    if [ "$choice" == "0" ]; then
        echo -e "${GREEN}Goodbye!${NC}"
        exit 0
    fi
    
    if [ -z "${SCENES[$choice]}" ]; then
        echo -e "${RED}Invalid selection!${NC}"
        return 1
    fi
    
    IFS='|' read -r scene name desc <<< "${SCENES[$choice]}"
    local scene_path="$SCENES_DIR/$scene"
    
    if [ ! -f "$scene_path" ]; then
        echo -e "${RED}Scene file not found: $scene_path${NC}"
        return 1
    fi
    
    echo ""
    echo -e "${CYAN}Launching: $name${NC}"
    echo -e "${YELLOW}Scene: $scene${NC}"
    echo ""
    echo -e "${GREEN}Starting Godot...${NC}"
    echo ""
    
    # Launch Godot with the scene
    cd "$PROJECT_DIR"
    godot "$scene_path"
}

# Main menu loop
main_menu() {
    while true; do
        clear
        print_banner
        
        if ! check_dependencies; then
            echo ""
            read -p "Continue anyway? (y/n): " continue_anyway
            if [ "$continue_anyway" != "y" ]; then
                exit 1
            fi
        fi
        
        show_menu
        
        read -p "Enter choice [0-4]: " choice
        
        if launch_scene "$choice"; then
            break
        fi
        
        echo ""
        read -p "Press Enter to try again..."
    done
}

# Handle command line arguments
case "${1:-}" in
    --install|-i)
        clear
        print_banner
        install_dependencies
        ;;
    --check|-c)
        clear
        print_banner
        check_dependencies
        ;;
    --help|-h)
        echo "ZeroDayMMO Game Launcher"
        echo ""
        echo "Usage: $0 [OPTION]"
        echo ""
        echo "Options:"
        echo "  --install, -i    Install dependencies"
        echo "  --check, -c      Check dependencies only"
        echo "  --help, -h       Show this help message"
        echo ""
        echo "Without options, launches the scene selection menu."
        ;;
    *)
        main_menu
        ;;
esac

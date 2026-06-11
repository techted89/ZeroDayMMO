#!/bin/bash

# ZeroDayMMO - Quick Dependency Installer
# Run this script to install all required dependencies

set -e

echo "========================================="
echo "ZeroDayMMO - Dependency Installer"
echo "========================================="
echo ""

# Detect OS
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$NAME
    VERSION=$VERSION_ID
else
    echo "Error: Cannot detect OS"
    exit 1
fi

echo "Detected: $OS $VERSION"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# Install Godot
install_godot() {
    echo "Installing Godot Engine..."
    
    if command_exists godot; then
        echo "✓ Godot already installed"
        return 0
    fi
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt update
        sudo apt install -y godot3
        
    elif [[ "$OS" == *"Fedora"* ]]; then
        sudo dnf install -y godot
        
    elif [[ "$OS" == *"Arch"* ]]; then
        sudo pacman -S --noconfirm godot
        
    else
        echo "⚠ Automatic installation not supported for $OS"
        echo "Please install Godot manually from: https://godotengine.org/download"
        return 1
    fi
    
    echo "✓ Godot installed successfully"
}

# Install .NET SDK
install_dotnet() {
    echo "Installing .NET SDK..."
    
    if command_exists dotnet; then
        echo "✓ .NET SDK already installed"
        return 0
    fi
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        # Add Microsoft package repository
        wget -q https://packages.microsoft.com/config/ubuntu/$(lsb_release -rs)/packages-microsoft-prod.deb -O /tmp/packages-microsoft-prod.deb
        sudo dpkg -i /tmp/packages-microsoft-prod.deb
        rm /tmp/packages-microsoft-prod.deb
        
        sudo apt update
        sudo apt install -y dotnet-sdk-8.0
        
    elif [[ "$OS" == *"Fedora"* ]]; then
        sudo dnf install -y dotnet-sdk-8.0
        
    elif [[ "$OS" == *"Arch"* ]]; then
        sudo pacman -S --noconfirm dotnet-sdk
        
    else
        echo "⚠ Automatic installation not supported for $OS"
        echo "Please install .NET SDK manually from: https://dotnet.microsoft.com/download"
        return 1
    fi
    
    echo "✓ .NET SDK installed successfully"
}

# Build project
build_project() {
    echo "Building C# project..."
    
    if ! command_exists dotnet; then
        echo "✗ .NET SDK not found, cannot build"
        return 1
    fi
    
    PROJECT_DIR="/root/ZeroDayMMO/zeroday-client-godot"
    
    if [ ! -f "$PROJECT_DIR/project.godot" ]; then
        echo "✗ Project file not found at $PROJECT_DIR"
        return 1
    fi
    
    cd "$PROJECT_DIR"
    dotnet build
    
    echo "✓ Project built successfully"
}

# Main installation
echo "Step 1/3: Installing Godot Engine"
echo "-----------------------------------"
install_godot
echo ""

echo "Step 2/3: Installing .NET SDK"
echo "-----------------------------------"
install_dotnet
echo ""

echo "Step 3/3: Building Project"
echo "-----------------------------------"
build_project
echo ""

echo "========================================="
echo "✓ Installation Complete!"
echo "========================================="
echo ""
echo "You can now run the game launcher:"
echo "  ./launcher.sh"
echo ""
echo "Or check dependencies:"
echo "  ./launcher.sh --check"
echo ""

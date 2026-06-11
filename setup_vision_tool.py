#!/usr/bin/env python3
"""
vision-tool setup helper for OpenCode
Run this to add your API key to vision-tool config
"""

import json
import os
import sys
import getpass

CONFIG_PATH = "/root/ZeroDayMMO/vision-tool/config.json"
OPENCODE_CONFIG_PATH = os.path.expanduser("~/.config/opencode/opencode.jsonc")

def add_api_key():
    print("=" * 60)
    print("vision-tool API Key Setup")
    print("=" * 60)
    print("\nSupported providers (free tiers available):")
    print("  1. Gemini (Recommended - aistudio.google.com/apikey)")
    print("  2. Groq (console.groq.com/keys)")
    print("  3. Mistral (console.mistral.ai/api-keys)")
    print("  4. OpenRouter (openrouter.ai/keys)")
    print("  5. Cloudflare")
    print("  6. Azure AI")
    print("  7. Fireworks AI")
    print("  8. HuggingFace")
    print("  9. Zhipu AI (Z.AI)")
    print("\nEnter your API key (input will be hidden):")
    
    key = getpass.getpass("API Key: ").strip()
    
    if not key:
        print("No key entered. Exiting.")
        return False
    
    # Detect provider from key format
    provider = "GEMINI_API_KEY"  # Default
    if key.startswith("sk-or-"):
        provider = "OPENROUTER_API_KEY"
    elif key.startswith("sk-") and len(key) > 40:
        provider = "OPENAI_API_KEY"
    elif key.startswith("gsk_"):
        provider = "GROQ_API_KEY"
    
    # Load config
    with open(CONFIG_PATH, 'r') as f:
        config = json.load(f)
    
    config[provider] = key
    
    # Save config
    with open(CONFIG_PATH, 'w') as f:
        json.dump(config, f, indent=2)
    
    os.chmod(CONFIG_PATH, 0o600)
    
    print(f"\n✅ API key saved for provider: {provider}")
    print(f"   Config location: {CONFIG_PATH}")
    return True

def configure_opencode_mcp():
    """Add vision-tool MCP server to OpenCode config"""
    print("\n" + "=" * 60)
    print("OpenCode MCP Configuration")
    print("=" * 60)
    
    mcp_config = {
        "mcp": {
            "vision-tool": {
                "type": "local",
                "command": ["python3", "/root/ZeroDayMMO/vision-tool/vision_mcp_server.py"],
                "enabled": True
            }
        }
    }
    
    print("\nAdd this to your ~/.config/opencode/opencode.jsonc:")
    print(json.dumps(mcp_config, indent=2))
    print("\nOr run: opencode /config to edit settings")
    
    # Try to auto-configure if opencode config exists
    if os.path.exists(OPENCODE_CONFIG_PATH):
        print(f"\nFound existing config at: {OPENCODE_CONFIG_PATH}")
        response = input("Auto-merge MCP config? (y/n): ").strip().lower()
        if response == 'y':
            try:
                with open(OPENCODE_CONFIG_PATH, 'r') as f:
                    content = f.read()
                
                # Simple append approach
                if '"mcp"' not in content:
                    # Find the last closing brace and insert before it
                    # This is a simple approach - may need manual adjustment
                    print("Please manually add the MCP config to your opencode.jsonc")
                    print("The MCP server command is:")
                    print(f'  python3 {"/root/ZeroDayMMO/vision-tool/vision_mcp_server.py"}')
            except Exception as e:
                print(f"Could not auto-configure: {e}")
    
    return True

def test_vision_tool():
    """Test vision-tool with a sample image"""
    print("\n" + "=" * 60)
    print("Testing vision-tool")
    print("=" * 60)
    
    test_image = "/root/ZeroDayMMO/generated_sprites/spritsheet (1).jpg"
    if os.path.exists(test_image):
        print(f"\nTesting with: {test_image}")
        print("Running: python3 vision_proxy.py analyze_image")
        
        import subprocess
        result = subprocess.run(
            ["python3", "/root/ZeroDayMMO/vision-tool/vision_proxy.py", test_image],
            capture_output=True,
            text=True,
            timeout=30
        )
        
        if result.returncode == 0:
            print("\n✅ vision-tool is working!")
            print("Output preview:")
            print(result.stdout[:500])
        else:
            print("\n❌ vision-tool test failed")
            print("Error:", result.stderr[:500])
    else:
        print(f"Test image not found: {test_image}")

if __name__ == '__main__':
    print("vision-tool Setup Helper for OpenCode")
    print("=" * 60)
    
    if len(sys.argv) > 1 and sys.argv[1] == '--test':
        test_vision_tool()
    else:
        if add_api_key():
            configure_opencode_mcp()
            print("\n" + "=" * 60)
            print("Setup complete!")
            print("=" * 60)
            print("\nNext steps:")
            print("1. Add the MCP config to your opencode.jsonc (shown above)")
            print("2. Restart OpenCode")
            print("3. Test with: python3 setup_vision_tool.py --test")

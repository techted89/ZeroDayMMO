#!/usr/bin/env python3
"""
Generate Godot SpriteFrames (.tres) resources from JSONC maps
"""

import os
import json
from pathlib import Path

INPUT_DIR = "/tmp/spritesheet_final_analysis"
OUTPUT_DIR = "/tmp/spritesheet_final_analysis/godot"

def generate_godot_tres(jsonc_file, output_file):
    """Convert JSONC map to Godot SpriteFrames .tres format"""
    
    with open(jsonc_file, 'r') as f:
        data = json.load(f)
    
    metadata = data.get('metadata', {})
    frames = data.get('frames', [])
    animations = data.get('animations', {})
    godot_config = data.get('godot_config', {})
    
    filename = metadata.get('filename', 'unknown')
    texture_path = godot_config.get('texture_path', f"res://sprites/{filename}")
    
    # Build .tres content
    tres_lines = [
        "[gd_resource type=\"SpriteFrames\" load_steps=2 format=3]",
        "",
        f"[ext_resource type=\"Texture2D\" path=\"{texture_path}\" id=\"1\"]",
        "",
        "[resource]",
        ""
    ]
    
    # Add animations
    for anim_name, anim_data in animations.items():
        frame_indices = anim_data.get('frame_indices', [])
        fps = anim_data.get('fps', 6)
        loop = "true" if anim_data.get('loop', True) else "false"
        
        tres_lines.append(f'[sub_resource type=\"AtlasTexture\" id="{anim_name}_atlas"]')
        tres_lines.append(f'atlas = ExtResource("1")')
        tres_lines.append("")
        
        tres_lines.append(f"animation = \"{anim_name}\"")
        tres_lines.append(f"fps = {fps}")
        tres_lines.append(f"loop = {loop}")
        
        for idx in frame_indices:
            if idx < len(frames):
                frame = frames[idx]
                tres_lines.append(f"frame = SubResource(\"{anim_name}_atlas\")")
                tres_lines.append(f"region = Rect2({frame['x1']}, {frame['y1']}, {frame['width']}, {frame['height']})")
        
        tres_lines.append("")
    
    # Write file
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        f.write('\n'.join(tres_lines))
    
    print(f"Generated: {output_file}")

def generate_all_godot_resources():
    """Process all JSONC files and generate .tres resources"""
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    jsonc_files = sorted(Path(INPUT_DIR).glob("*.jsonc"))
    
    print(f"Found {len(jsonc_files)} JSONC maps to convert")
    print("=" * 80)
    
    for jsonc_file in jsonc_files:
        if jsonc_file.name == "all_sprite_maps.jsonc":
            continue
            
        output_file = os.path.join(OUTPUT_DIR, f"{jsonc_file.stem}.tres")
        generate_godot_tres(str(jsonc_file), output_file)
    
    print("=" * 80)
    print(f"All Godot resources generated in: {OUTPUT_DIR}")

if __name__ == '__main__':
    generate_all_godot_resources()

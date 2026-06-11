#!/usr/bin/env python3
"""
Comprehensive SpriteSheet Vision Analyzer
Loops through all spritesheets, calls vision-tool API for each,
and generates detailed JSONC maps with frame boundaries and animation loops.
"""

import os
import json
import sys
import glob
from pathlib import Path

sys.path.insert(0, '/root/ZeroDayMMO/vision-tool')
from vision_proxy import analyze

PROJECT_DIR = "/root/ZeroDayMMO/generated_sprites"
OUTPUT_DIR = "/tmp/spritesheet_final_analysis"

def analyze_all_spritesheets():
    """Analyze all spritesheets using vision-tool API"""
    
    # Get all image files
    patterns = ['*.jpg', '*.jpeg', '*.png']
    all_files = set()
    for p in patterns:
        all_files.update(glob.glob(os.path.join(PROJECT_DIR, p)))
    
    # Filter out annotated/analysis files
    spritesheets = sorted([f for f in all_files 
                          if '_annotated' not in f 
                          and '_analyzed' not in f
                          and '_verification' not in f])
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    results = {}
    
    print(f"Found {len(spritesheets)} spritesheets to analyze")
    print("=" * 80)
    
    for i, sheet_path in enumerate(spritesheets, 1):
        filename = os.path.basename(sheet_path)
        print(f"\n[{i}/{len(spritesheets)}] Analyzing: {filename}")
        print("-" * 80)
        
        try:
            # Call vision-tool API
            print("  Calling vision API...")
            vision_result = analyze(sheet_path)
            
            print("  ✓ Vision analysis complete")
            print(f"  Description preview: {vision_result[:200]}...")
            
            # Store result
            results[filename] = {
                'path': sheet_path,
                'vision_description': vision_result,
                'filename': filename
            }
            
            # Save individual result
            individual_file = os.path.join(OUTPUT_DIR, f"{Path(filename).stem}_vision.json")
            with open(individual_file, 'w') as f:
                json.dump(results[filename], f, indent=2)
            
        except Exception as e:
            print(f"  ✗ Error: {e}")
            results[filename] = {
                'path': sheet_path,
                'error': str(e),
                'filename': filename
            }
    
    # Save complete report
    report_path = os.path.join(OUTPUT_DIR, "all_vision_analysis.json")
    with open(report_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    print("\n" + "=" * 80)
    print(f"ANALYSIS COMPLETE")
    print(f"Individual results: {OUTPUT_DIR}/*_vision.json")
    print(f"Full report: {report_path}")
    print("=" * 80)
    
    return results

def generate_jsonc_maps(vision_results):
    """Generate JSONC maps from vision analysis results"""
    
    print("\n" + "=" * 80)
    print("GENERATING JSONC MAPS")
    print("=" * 80)
    
    jsonc_maps = {}
    
    for filename, data in vision_results.items():
        if 'error' in data:
            print(f"\nSkipping {filename} (analysis failed)")
            continue
        
        print(f"\nProcessing: {filename}")
        
        # Extract information from vision description
        description = data.get('vision_description', '')
        
        # Try to detect grid pattern from description
        grid_info = detect_grid_from_description(description, filename)
        
        # Try to detect character/actions from description
        character_info = detect_character_info(description)
        
        # Try to detect animations from description
        animations = detect_animations_from_description(description, grid_info)
        
        # Generate frame map
        frame_map = generate_frame_map(filename, grid_info, character_info, animations)
        
        jsonc_maps[filename] = frame_map
        
        # Save individual JSONC
        jsonc_file = os.path.join(OUTPUT_DIR, f"{Path(filename).stem}.jsonc")
        with open(jsonc_file, 'w') as f:
            json.dump(frame_map, f, indent=2)
        
        print(f"  ✓ Generated: {jsonc_file}")
        print(f"  Grid: {grid_info.get('rows', '?')} rows × {grid_info.get('columns', '?')} cols")
        print(f"  Animations: {list(animations.keys()) if animations else 'None detected'}")
    
    # Save combined maps
    combined_path = os.path.join(OUTPUT_DIR, "all_sprite_maps.jsonc")
    with open(combined_path, 'w') as f:
        json.dump(jsonc_maps, f, indent=2)
    
    print(f"\nCombined maps: {combined_path}")
    
    return jsonc_maps

def detect_grid_from_description(description, filename):
    """Detect grid layout from vision description"""
    import re
    
    grid_info = {
        'rows': 0,
        'columns': 0,
        'total_frames': 0,
        'frame_width': 0,
        'frame_height': 0,
        'layout_type': 'unknown'
    }
    
    # Look for grid mentions
    grid_patterns = [
        r'(\d+)\s*rows?\s*(?:and|×|x)\s*(\d+)\s*columns?',
        r'(\d+)\s*columns?\s*(?:and|×|x)\s*(\d+)\s*rows?',
        r'grid\s*of\s*(\d+)\s*(?:rows?\s*(?:and|×|x)\s*)?(\d+)\s*columns?',
        r'(\d+)\s*by\s*(\d+)\s*grid',
        r'arranged\s*(?:in|into)\s*(?:a\s*)?grid\s*(?:of\s*)?(\d+)\s*(?:×|x)\s*(\d+)',
    ]
    
    for pattern in grid_patterns:
        match = re.search(pattern, description.lower())
        if match:
            num1 = int(match.group(1))
            num2 = int(match.group(2))
            grid_info['rows'] = num1
            grid_info['columns'] = num2
            grid_info['total_frames'] = num1 * num2
            grid_info['layout_type'] = 'grid'
            break
    
    # If no grid found, try to infer from dimensions
    if grid_info['rows'] == 0:
        # Get image dimensions
        from PIL import Image
        img_path = os.path.join(PROJECT_DIR, filename)
        if os.path.exists(img_path):
            img = Image.open(img_path)
            w, h = img.size
            
            # Common sprite sheet layouts
            if w == 1024 and h == 640:
                grid_info['rows'] = 4
                grid_info['columns'] = 5
                grid_info['total_frames'] = 20
                grid_info['frame_width'] = 204
                grid_info['frame_height'] = 160
                grid_info['layout_type'] = 'inferred_1024x640'
            elif w == 1024 and h == 559:
                grid_info['rows'] = 3
                grid_info['columns'] = 4
                grid_info['total_frames'] = 12
                grid_info['frame_width'] = 256
                grid_info['frame_height'] = 186
                grid_info['layout_type'] = 'inferred_1024x559'
    
    return grid_info

def detect_character_info(description):
    """Extract character details from vision description"""
    info = {
        'character_type': 'unknown',
        'colors': [],
        'equipment': [],
        'style': 'unknown'
    }
    
    # Detect colors
    color_keywords = ['purple', 'green', 'black', 'gray', 'blue', 'red', 'yellow', 'orange', 'white', 'brown']
    for color in color_keywords:
        if color in description.lower():
            info['colors'].append(color)
    
    # Detect equipment/items
    equipment_keywords = ['helmet', 'visor', 'jacket', 'pants', 'shoes', 'backpack', 'armor', 'weapon', 'sword', 'gun']
    for item in equipment_keywords:
        if item in description.lower():
            info['equipment'].append(item)
    
    # Detect style
    if 'cartoon' in description.lower() or 'cartoonish' in description.lower():
        info['style'] = 'cartoon'
    elif 'pixel' in description.lower():
        info['style'] = 'pixel_art'
    elif 'realistic' in description.lower():
        info['style'] = 'realistic'
    
    return info

def detect_animations_from_description(description, grid_info):
    """Try to identify animation sequences from description"""
    animations = {}
    
    # Common animation keywords
    anim_keywords = {
        'idle': ['idle', 'standing', 'still', 'static', 'neutral'],
        'walk': ['walk', 'walking', 'stride', 'step', 'moving'],
        'run': ['run', 'running', 'sprint'],
        'attack': ['attack', 'attacking', 'strike', 'hit', 'punch', 'kick', 'swing'],
        'damage': ['damage', 'hurt', 'hit', 'injured', 'pain', 'wince'],
        'die': ['die', 'death', 'dead', 'falling', 'collapse', 'defeated'],
        'stealth': ['stealth', 'sneak', 'crouch', 'hide', 'camouflage'],
        'jump': ['jump', 'jumping', 'leap'],
        'cast': ['cast', 'casting', 'magic', 'spell', 'channel'],
    }
    
    # Try to infer animations from row descriptions
    # If description mentions poses or actions per row
    import re
    
    # Look for row-specific actions
    row_patterns = [
        r'row\s*(?:1|one|first).*?(?:shows?|depicts?|has|contains?)\s*(.+?)(?=row|\n|$)',
        r'row\s*(?:2|two|second).*?(?:shows?|depicts?|has|contains?)\s*(.+?)(?=row|\n|$)',
        r'row\s*(?:3|three|third).*?(?:shows?|depicts?|has|contains?)\s*(.+?)(?=row|\n|$)',
        r'row\s*(?:4|four|fourth).*?(?:shows?|depicts?|has|contains?)\s*(.+?)(?=row|\n|$)',
    ]
    
    # If we have grid info, create generic animations per row
    if grid_info['rows'] > 0 and grid_info['columns'] > 0:
        total_frames = grid_info['rows'] * grid_info['columns']
        frames_per_row = grid_info['columns']
        
        # Create default animation assignments per row
        default_animations = ['idle', 'walk', 'attack', 'damage', 'die', 'stealth', 'run', 'jump']
        
        for row in range(grid_info['rows']):
            anim_name = default_animations[row] if row < len(default_animations) else f'action_{row}'
            start_frame = row * frames_per_row
            end_frame = start_frame + frames_per_row - 1
            
            animations[anim_name] = {
                'row': row,
                'frame_indices': list(range(start_frame, end_frame + 1)),
                'frame_count': frames_per_row,
                'description': f'Row {row + 1} animation'
            }
    
    return animations

def generate_frame_map(filename, grid_info, character_info, animations):
    """Generate complete frame map for a spritesheet"""
    
    # Get image dimensions
    from PIL import Image
    img_path = os.path.join(PROJECT_DIR, filename)
    img = Image.open(img_path)
    w, h = img.size
    
    # Calculate frame dimensions
    if grid_info['columns'] > 0 and grid_info['rows'] > 0:
        frame_width = w // grid_info['columns']
        frame_height = h // grid_info['rows']
    else:
        frame_width = w
        frame_height = h
    
    # Generate frame coordinates
    frames = []
    for row in range(grid_info['rows']):
        for col in range(grid_info['columns']):
            x1 = col * frame_width
            y1 = row * frame_height
            x2 = x1 + frame_width
            y2 = y1 + frame_height
            
            frames.append({
                'index': len(frames),
                'row': row,
                'col': col,
                'x1': x1,
                'y1': y1,
                'x2': x2,
                'y2': y2,
                'width': frame_width,
                'height': frame_height
            })
    
    # Build the complete map
    frame_map = {
        'metadata': {
            'filename': filename,
            'image_dimensions': {'width': w, 'height': h},
            'grid': {
                'rows': grid_info['rows'],
                'columns': grid_info['columns'],
                'total_frames': grid_info['total_frames'],
                'frame_width': frame_width,
                'frame_height': frame_height
            },
            'character': character_info,
            'analysis_timestamp': str(__import__('datetime').datetime.now())
        },
        'frames': frames,
        'animations': animations,
        'godot_config': {
            'spriteframes_resource': f"res://sprites/{Path(filename).stem}.tres",
            'texture_path': f"res://sprites/{filename}",
            'animation_fps': 6,
            'loop_animations': True
        }
    }
    
    return frame_map

if __name__ == '__main__':
    print("=" * 80)
    print("COMPREHENSIVE SPRITESHEET VISION ANALYZER")
    print("=" * 80)
    
    # Step 1: Analyze all spritesheets with vision API
    vision_results = analyze_all_spritesheets()
    
    # Step 2: Generate JSONC maps
    jsonc_maps = generate_jsonc_maps(vision_results)
    
    print("\n" + "=" * 80)
    print("ALL TASKS COMPLETE")
    print("=" * 80)
    print(f"Vision analysis: {OUTPUT_DIR}/*_vision.json")
    print(f"JSONC maps: {OUTPUT_DIR}/*.jsonc")
    print(f"Combined report: {OUTPUT_DIR}/all_sprite_maps.jsonc")

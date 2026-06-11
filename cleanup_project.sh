#!/bin/bash

# ZeroDayMMO - Cleanup Script
# Removes old Perchance workflow files and unnecessary artifacts

set -e

echo "========================================="
echo "ZeroDayMMO - Cleanup Script"
echo "========================================="
echo ""
echo "This will remove:"
echo "  - Perchance image generation workflow"
echo "  - Old analyzer attempts"
echo "  - Temporary verification files"
echo "  - Generated stealth images"
echo ""
echo "Files to be REMOVED:"
echo ""

# Perchance workflow files
echo "Perchance Workflow:"
echo "  - perchance_mapper.py"
echo "  - stealth_perchance_automator.py"
echo "  - generate_character_sprites.py"
echo "  - generate_base_character.py"
echo "  - verify_character_sprites.py"
echo "  - perchance-fields.txt"
echo "  - gameplay_elements_db.py"
echo "  - agent_orchestrator.py"
echo "  - main_workflow.py"
echo "  - cleanup_old_files.py"
echo "  - output_stealth/ (directory)"
echo "  - config/imagegen/ (directory)"
echo ""

# Old analyzer files
echo "Old Analyzers:"
echo "  - spritesheet_analyzer.py"
echo "  - spritesheet_analyzer_headless.py"
echo "  - spritesheet_cli_analyzer.py"
echo "  - ultimate_analyzer.py"
echo "  - comprehensive_spritesheet_analyzer.py"
echo ""

# Temporary files
echo "Temporary Files:"
echo "  - generated_sprites/spritsheet (1)_annotated.jpg"
echo "  - generated_sprites/spritsheet (1)_frames.json"
echo "  - generated_sprites/verification_report.json"
echo "  - generated_sprites/verify_sprites.py"
echo "  - generated_sprites/verify_sprites_simple.py"
echo "  - generated_sprites/character_sprite_specs.json"
echo ""

echo "Files to be KEPT:"
echo "  ✓ All JPG spritesheets in generated_sprites/"
echo "  ✓ analyze_all_sprites.py (working vision analyzer)"
echo "  ✓ generate_godot_resources.py"
echo "  ✓ setup_vision_tool.py"
echo "  ✓ vision-tool/ directory"
echo "  ✓ zeroday-client-godot/ directory"
echo "  ✓ zeroday-server/ directory"
echo "  ✓ All .md documentation files"
echo "  ✓ config/ (except imagegen subdirectory)"
echo ""

read -p "Continue with cleanup? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo "Starting cleanup..."
echo ""

cd /root/ZeroDayMMO

# Remove Perchance workflow files
echo "Removing Perchance workflow files..."
rm -f perchance_mapper.py
rm -f stealth_perchance_automator.py
rm -f generate_character_sprites.py
rm -f generate_base_character.py
rm -f verify_character_sprites.py
rm -f perchance-fields.txt
rm -f gameplay_elements_db.py
rm -f agent_orchestrator.py
rm -f main_workflow.py
rm -f cleanup_old_files.py
rm -rf output_stealth/
rm -rf config/imagegen/
echo "✓ Perchance files removed"

# Remove old analyzer files
echo "Removing old analyzer files..."
rm -f spritesheet_analyzer.py
rm -f spritesheet_analyzer_headless.py
rm -f spritesheet_cli_analyzer.py
rm -f ultimate_analyzer.py
rm -f comprehensive_spritesheet_analyzer.py
echo "✓ Old analyzers removed"

# Remove temporary files from generated_sprites
echo "Removing temporary files..."
rm -f "generated_sprites/spritsheet (1)_annotated.jpg"
rm -f "generated_sprites/spritsheet (1)_frames.json"
rm -f generated_sprites/verification_report.json
rm -f generated_sprites/verify_sprites.py
rm -f generated_sprites/verify_sprites_simple.py
rm -f generated_sprites/character_sprite_specs.json
echo "✓ Temporary files removed"

# Clean up Python cache
echo "Cleaning Python cache..."
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find . -type f -name "*.pyc" -delete 2>/dev/null || true
echo "✓ Python cache cleaned"

echo ""
echo "========================================="
echo "✓ Cleanup Complete!"
echo "========================================="
echo ""
echo "Remaining files:"
ls -1 *.py *.md 2>/dev/null | grep -v ".pyc" || echo "No Python/Markdown files in root"
echo ""
echo "Generated sprites:"
ls -1 generated_sprites/*.jpg 2>/dev/null | wc -l
echo "spritesheet images"
echo ""
echo "You can now run:"
echo "  ./zeroday-client-godot/launcher.sh"
echo ""

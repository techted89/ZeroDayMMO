# Refactored Godot Image Asset Architecture

A consolidated, modular system for generating game graphics with AI image models. This document groups related concepts, provides reusable templates, and defines a swap‑able characteristic system for characters, equipment, maps, and VFX.

---

## 1. Core Prompt Blueprint (4‑Part Structure)

Every generation prompt must follow this exact order to force mathematical grid alignment and isolate the output:

```
[STRUCTURAL GRID & PROJECTION] -> [CORE SUBJECT ANCHOR] -> [FRAME‑BY‑FRAME TIMELINE] -> [ISOLATION ENGINE CONTROLS]
```

### 1.1 Structural Grid & Projection
- **Perspective:** 2.5D Isometric (2:1 dimetric).  
- **Grid Layout:** `{ROWS} rows × {COLUMNS} columns`.  
- **Orientation:** Row 1 starts at `0°` (front‑south) and increments exactly `{DEGREE}°` clockwise per row (commonly `22.5°`).  

### 1.2 Core Subject Anchor
- **Token:** `[CH_<ID>]` – binds immutable facial/body features, base build, and style.  
- **Example:** `[CH_NETRUNNER_M01]` – “sharp features, glowing neon‑cyan cyber‑eye, undercut hair, pale skin”.

### 1.3 Frame‑by‑Frame Timeline
- **Columns:** Sequential action frames (e.g., `Idle → Contact → Peak → Snap‑Back`).  
- **Rows:** Each row repeats the same column sequence for a different orientation.  

### 1.4 Isolation Engine Controls
- **Background:** Pure `#FF00FF` magenta chroma‑key or `#000000` black for transparent assets.  
- **Lighting:** Flat global directional shading; no depth‑of‑field, gradients, or lens blur.  
- **Palette Constraint:** Explicit hex list; deny gradients/anti‑aliasing.  

---

## 2. Asset Metadata Template

```json
{
  "asset_metadata": {
    "id": "<unique_string>",
    "category": "<character|equipment|map|vfx>",
    "target_node_type": "<Sprite2D|AnimatedSprite2D|TileMapLayer|Shader>",
    "linked_entity_id": "<optional_reference>"
  },
  "characteristics": {
    "base_character": "<CH_<ID>>",
    "tier": "<Tier 1|2|3|...>",
    "pose": "<Idle|Run|Attack|Hack|...>",
    "palette": ["#HEX1", "#HEX2", ...],
    "dimensions": "<WIDTH>x<HEIGHT>",
    "grid": {
      "rows": <NUM>,
      "columns": <NUM>,
      "orientation_start_deg": <DEG>,
      "rotation_step_deg": <DEG>
    },
    "negative_prompt": "<comma_separated_negative_terms>"
  },
  "generation_prompt": "<structured_prompt_string>"
}
```

### 2.1 Swappable Characteristic Blocks

| Block | Placeholder | Description |
|-------|-------------|-------------|
| **Base Character** | `<CH_<ID>>` | Persistent token for face, hair, skin, and core cyber‑mods. |
| **Tier** | `<TIER>` | `"Tier 1: Street Novice"`, `"Tier 2: Grid Runner"`, `"Tier 3: Ascended Net‑Ghost"` – drives gear complexity and glow intensity. |
| **Pose** | `<POSE>` | `"running"`, `"idle"`, `"hacking"` – determines frame‑by‑frame timeline. |
| **Palette** | `<PALETTE_LIST>` | List of allowed hex colors, e.g., `["#00F0FF","#FF0055","#121214"]`. |
| **Grid** | `<ROWS>×<COLUMNS>` | Typical `16×4` for 16 directional views, `4` frames per orientation. |
| **Negative Prompt** | `<NEGATIVE>` | `"blurry,low quality,text,watermark,cropped,multiple characters,deformed"` |

These blocks can be swapped independently to generate many variants from a single template.

---

## 3. Consolidated Prompt Instruction Template

> **Inject this exact block into every AI‑generation request.** Replace placeholders with the appropriate values from the characteristic system.

```
A structured, mathematically aligned asset sheet grid, 2.5D isometric projection, 2:1 dimetric perspective, sharp pixel art style.

[GRID LAYOUT]: {ROWS} rows by {COLUMNS} columns grid layout matrix. Row 1 to Row {ROWS} represent {DIRECTION_COUNT}-directional rotational tracking viewpoints, starting from 0 degrees (directly facing front‑south) and rotating exactly {DEGREE} degrees clockwise per row.

[SUBJECT ANCHOR]: [CH_<ID>], <BASE_DESCRIPTION>. Attire: <TIER> <EQUIP_TYPE> description, <PALETTE_DETAILS>. 

[FRAME ANIMATION TIMELINE]: Every row contains an identical, seamless looping <POSE> cycle spanning {COLUMNS} sequential frame columns from left to right.
- Column 1: Start/Idle stance
- Column 2: Initial contact/Movement progression
- Column 3: Passing position/Peak action height
- Column 4: Return/Settle phase
Frames must maintain perfect spatial height, uniform scale, and matching alignment across all cells.

[RENDER CONTROLS]: Flat global directional shading from top‑right, highly consistent lighting across all frames. No camera lens blur, no depth of field, no background gradients. All frames flat‑projected and isolated on a solid, clean, uniform #FF00FF magenta chroma‑key backdrop with sharp borders.
```

**Example Filled Prompt (Tier 2 Netrunner Trenchcoat, Running):**

```
A structured, mathematically aligned asset sheet grid, 2.5D isometric projection, 2:1 dimetric perspective, sharp pixel art style.

[GRID LAYOUT]: 16 rows by 4 columns grid layout matrix. Row 1 to Row 16 represent 16-directional rotational tracking viewpoints, starting from 0 degrees (directly facing front‑south) and rotating exactly 22.5 degrees clockwise per row.

[SUBJECT ANCHOR]: [CH_NETRUNNER_M01], dark cyberpunk aesthetic. Male netrunner, pale skin, sharp jawline, glowing neon‑cyan cybernetic optical eye lens on left side, short undercut black hair. Attire: Tier 2 grid runner gear consisting of a sleek carbon‑fiber trench coat with glowing cyan trim, embedded fiber‑optic neural ports visible on collar, and a thigh‑mounted high‑tech cyberdeck console.

[FRAME ANIMATION TIMELINE]: Every row contains an identical, seamless looping running cycle spanning 4 sequential frame columns from left to right.
- Column 1: Forward‑leaning starting stride
- Column 2: Full leg extension and floor contact
- Column 3: Passing stride with coat physics trailing
- Column 4: Recovery step into loop reset
Frames must maintain perfect spatial height, uniform scale, and matching alignment across all cells.

[RENDER CONTROLS]: Flat global directional shading from top‑right, highly consistent lighting across all frames. No camera lens blur, no depth of field, no background gradients. All frames flat‑projected and isolated on a solid, clean, uniform #FF00FF magenta chroma‑key backdrop with sharp borders.
```

---

## 4. Negative Prompt Matrix

```json
{
  "negative_prompt": "blurry,low quality,text,watermark,cropped,multiple characters,deformed,anti-aliasing,gradient,soft airbrush,perspective distortion,depth of field,lens flare,vignette,floating stray pixels,asymmetrical grids,overlapping frames"
}
```

- **Always append** this string to the generation request.  
- It removes the majority of unwanted artifacts and forces strict pixel‑art compliance.

---

## 5. Model Selection Guidance

| Model | Cost / img | Speed | Best Use |
|-------|------------|-------|----------|
| `openrouter/black-forest-labs/flux-schnell` | free | 1‑2 s | Bulk drafts, rapid iteration |
| `openrouter/black-forest-labs/flux-1.1-pro` | $0.04 | 4‑8 s | Final hero art, key VFX |
| `openrouter/google/gemini-2.5-flash-image-preview` | $0.02 | 3‑5 s | Multi‑turn editing, I2I |
| `openrouter/xai/grok-2-image-1212` | $0.02 | 2‑4 s | Realistic characters |
| `openrouter/stability-ai/stable-diffusion-3.5-large` | $0.03 | 3‑6 s | Stylized art |
| `openrouter/openai/gpt-image-1` | $0.04‑0.20 | 5‑15 s | UI mockups, high‑fidelity sprites |

*Set a monthly budget cap (`MAX_MONTHLY_SPEND`) to avoid surprise charges.*

---

## 6. Production Workflow (CLI‑Friendly)

1. **Define Characteristics** – Fill a JSON object with the placeholders above.  
2. **Generate Variants** – Use `text_to_image.py` with `--variants 8` (or `--variants 4` for quick review).  
3. **Quality Review** – Pipe each output through `vision-tool` (or the `vision_proxy.py` script) to score 1‑10 for style compliance.  
4. **Select Best** – Pick the highest‑scored variant; note its seed for reproducibility.  
5. **Pack into Atlas** – Use `free-tex-packer-cli` → `SpriteFrames .tres` → re‑import in Godot.  
6. **Version Control** – Store only the JSON manifest, the prompt template, and the seed; regenerate assets on demand.

---

## 7. Example JSON Manifest (Post‑Generation)

```json
{
  "prompt": "A structured, mathematically aligned asset sheet grid... (full prompt string)",
  "model": "openrouter/black-forest-labs/flux-schnell",
  "size": "512x512",
  "seed": 42,
  "variants": [
    { "file": "netrunner_t2_trenchcoat_run_16way/00.png", "seed": 43 },
    { "file": "netrunner_t2_trenchcoat_run_16way/01.png", "seed": 44 },
    { "file": "netrunner_t2_trenchcoat_run_16way/02.png", "seed": 45 },
    { "file": "netrunner_t2_trenchcoat_run_16way/03.png", "seed": 46 }
    // … up to 8 variants
  ],
  "created_at": "2026-06-18T14:30:00Z",
  "quality_scores": {
    "00.png": "8.5 – clean edges, correct palette",
    "01.png": "9.0 – best snap‑back frame",
    "02.png": "7.8 – minor color bleed",
    "03.png": "8.2 – good alignment"
  }
}
```

---

## 8. Template‑Driven Asset Generation (Automation)

A small Python helper can read a CSV/JSON of characteristic swaps and output a ready‑to‑run CLI command for each row:

```python
import csv, subprocess, json, os

with open('characteristics.csv') as f:
    for row in csv.DictReader(f):
        cmd = [
            'python3', 'text_to_image.py',
            '--prompt', row['generation_prompt'],
            '--negative', row['negative_prompt'],
            '--model', row['model'],
            '--size', row['dimensions'],
            '--variants', row['variants'],
            '--out', f"assets/{row['category']}/{row['id']}/",
            '--manifest', f"assets/{row['category']}/{row['id']}_manifest.json",
            '--seed', row['seed']
        ]
        subprocess.run(cmd, check=True)
```

- **CSV Columns:** `id,tier,pose,palette,dimensions,model,variants,seed,category`
- **Result:** One folder per asset containing the full sprite sheet, manifest, and seed log.

---

## 9. Quick Reference Cheat‑Sheet

| Symbol | Meaning |
|--------|---------|
| `<CH_<ID>>` | Persistent character token (face, hair, skin) |
| `<TIER>` | Gear complexity level (1‑3) |
| `<POSE>` | Animation state (idle, run, attack, hack) |
| `<PALETTE_LIST>` | Allowed hex colors, e.g., `["#00F0FF","#FF0055"]` |
| `<ROWS>` / `<COLUMNS>` | Grid dimensions (commonly `16`×`4`) |
| `<DEGREE>` | Rotation step (usually `22.5`) |
| `#FF00FF` | Magenta chroma‑key background |
| `#000000` | Black background for transparent assets |
| **Negative Prompt** | `"blurry,low quality,text,watermark,cropped,multiple characters,deformed"` |

---

### Final Note
This refactored architecture replaces scattered instructions with a single, reusable blueprint. By swapping the characteristic blocks you can instantly generate:

- **Characters** (different tiers, poses, palettes)  
- **Equipment Overlays** (armor, weapons, accessories)  
- **Maps/Tiles** (terrain blends, megamap nodes)  
- **VFX** (code bursts, ICE breaks, elemental impacts)

All outputs remain mathematically aligned, palette‑constrained, and ready for Godot’s `AnimatedSprite2D` or `TileMapLayer` pipelines. Use the provided JSON schema and CLI helpers to automate bulk generation while preserving full reproducibility. 

</content>
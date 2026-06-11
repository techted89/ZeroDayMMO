shader_type canvas_item;

uniform float scan_line_intensity : hint_range(0.0, 0.5) = 0.15;
uniform float crt_curvature : hint_range(0.0, 0.1) = 0.02;
uniform float crt_distortion : hint_range(0.0, 0.05) = 0.01;
uniform vec4 color_tint : source_color = vec4(0.0, 1.0, 0.2, 1.0);
uniform float glow_intensity : hint_range(0.0, 1.0) = 0.2;
uniform float noise_amount : hint_range(0.0, 0.1) = 0.02;

float random(vec2 st) {
	return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void fragment() {
	vec2 uv = UV;
	vec2 screen_uv = uv;
	
	// CRT curvature
	float curvature = crt_curvature;
	vec2 curved_uv = uv * 2.0 - 1.0;
	curved_uv *= 1.0 + curvature * dot(curved_uv, curved_uv);
	curved_uv = curved_uv * 0.5 + 0.5;
	
	if (curved_uv.x < 0.0 || curved_uv.x > 1.0 || curved_uv.y < 0.0 || curved_uv.y > 1.0) {
		COLOR = vec4(0.0, 0.0, 0.0, 1.0);
		return;
	}
	
	vec4 base = texture(TEXTURE, curved_uv);
	
	// Color tinting (green monitor effect)
	base.rgb *= color_tint.rgb;
	
	// Scan lines
	float scan_line = sin(screen_uv.y * TEXTURE_PIXEL_SIZE.y * 2.0 * 3.14159 * 500.0);
	scan_line = clamp(scan_line * scan_line_intensity + (1.0 - scan_line_intensity), 0.0, 1.0);
	base.rgb *= scan_line;
	
	// Screen edge darkening
	float edge_dist = distance(screen_uv, vec2(0.5, 0.5)) * 1.2;
	base.rgb *= 1.0 - edge_dist * crt_distortion * 10.0;
	
	// Subtle glow
	float glow = length(base.rgb) * glow_intensity;
	base.rgb += vec3(0.0, glow * 0.3, 0.0);
	
	// Noise
	float n = random(floor(UV * vec2(320.0, 240.0)) + TIME * 100.0);
	base.rgb += (n - 0.5) * noise_amount;
	
	COLOR = base;
}

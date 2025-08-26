#ifdef GL_ES
precision mediump float;
#endif

// --- Uniforms ---
uniform sampler2D u_texture;
uniform vec2 u_noiseOffset;
uniform vec2 u_noiseScale;

// Halo uniforms
uniform vec3  u_halo_color;
uniform float u_halo_radius;
uniform float u_halo_falloff;
uniform float u_halo_strength;
// Normalization uniforms
uniform vec2 u_texCoord_min;
uniform vec2 u_texCoord_max;
// Sunset tint uniforms
uniform float u_time;
uniform float u_sunsetCenter;
uniform float u_halfWidth;
uniform float u_tintStrength;
uniform vec3  u_sunsetTint;
// MODIFIED: Added heat shimmer uniforms
uniform sampler2D u_noiseTexture;
uniform float u_continuousTime;
uniform float u_shimmer_strength;
uniform float u_shimmer_speed;
uniform float u_shimmer_scale;


varying vec2 v_texCoord;

// --- Helper function for sunset tint ---
float bell(float x, float center, float halfWidth) {
    float d = abs(x - center);
    float n = clamp(1.0 - d / halfWidth, 0.0, 1.0);
    return 0.5 * (1.0 + cos(3.14159265 * (1.0 - n)));
}

void main() {
    // 1. Create clean [0,1] coordinates for procedural effects
    vec2 range = u_texCoord_max - u_texCoord_min;
    vec2 normalizedTexCoord = (v_texCoord - u_texCoord_min) / range;
    vec2 proceduralCoord = normalizedTexCoord;
    proceduralCoord.y = 1.0 - proceduralCoord.y;

    vec2 noiseUV01 = fract(proceduralCoord * u_shimmer_scale + vec2(0.0, u_continuousTime * u_shimmer_speed));

    // 2. --- Heat shimmer effect ---
    // Create scrolling coordinates for the noise texture
    vec2 noiseCoord = u_noiseOffset + noiseUV01 * u_noiseScale;

    // Sample the noise texture and map values to [-1,1]
    vec2 distortion = (texture2D(u_noiseTexture, noiseCoord).rg * 2.0) - 1.0;

    // Apply distortion in sprite-UV (0..1) space and clamp
    vec2 uv01_distorted = clamp(proceduralCoord + distortion * u_shimmer_strength, 0.0, 1.0);

    // Map back to atlas-space for actual texture sampling
    vec2 sampleUV = u_texCoord_min + uv01_distorted * range;

    // 3. --- Sample the sun texture ---
    vec4 baseColor = texture2D(u_texture, sampleUV);

    // 4. --- Sunset tint ---
    // Apply the tint to the (now shimmering) sun color
    float t = bell(u_time, u_sunsetCenter, u_halfWidth);
    float k = t * clamp(u_tintStrength, 0.0, 1.0);
    vec3 tintedSunRgb = mix(baseColor.rgb, u_sunsetTint, k);

    // 5. --- Halo effect ---
    // Calculate the halo with the clean, undistorted procedural coordinates
    float dist = distance(proceduralCoord, vec2(0.5));
    float glow = smoothstep(u_halo_radius + u_halo_falloff, u_halo_radius, dist);
    float haloAlpha = glow * u_halo_strength;

    // 6. --- Final composition ---
    vec3 finalRgb = mix(u_halo_color, tintedSunRgb, baseColor.a);
    float finalAlpha = max(baseColor.a, haloAlpha);

    gl_FragColor = vec4(finalRgb, finalAlpha);
}

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


// --- ADDED: Bloom effect uniforms ---
uniform float u_bloom_threshold;
uniform float u_bloom_strength;
uniform vec2  u_texelSize;
uniform float u_bloom_radius;

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

    // --- Noise texture coordinates (for heat shimmer) ---
    vec2 noiseUV01 = mod(proceduralCoord * u_shimmer_scale + vec2(0.0, u_continuousTime * u_shimmer_speed), 1.0);


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
    float glow = smoothstep(u_halo_radius, u_halo_radius - u_halo_falloff, dist);
    vec3 bloomedRgb = mix(tintedSunRgb, u_halo_color, glow * u_halo_strength);

    vec4 finalColor = vec4(bloomedRgb, baseColor.a);

    // --- ADDED: Bloom effect (single-pass approximation) ---
    vec4 bloomColor = vec4(0.0);
    float brightness = dot(finalColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > u_bloom_threshold) {
        vec2 texelSize = u_texelSize;   // Provided by the app
        float totalWeight = 0.0;

        // Constant loop bounds (−2..2) to satisfy ES2 compilers
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                if (x == 0 && y == 0) continue;

                vec2 offset = vec2(float(x), float(y)) * texelSize * u_bloom_radius;
                vec4 sampleColor = texture2D(u_texture, v_texCoord + offset);

                // Simple Gaussian-ish falloff
                float r2 = float(x * x + y * y);
                float weight = exp(-r2 / 8.0);

                bloomColor += sampleColor * weight;
                totalWeight += weight;
            }
        }
        if (totalWeight > 0.0) {
            bloomColor /= totalWeight;
        }
        finalColor += bloomColor * u_bloom_strength;
    }

    gl_FragColor = finalColor;
}

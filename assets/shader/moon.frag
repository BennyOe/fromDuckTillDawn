#ifdef GL_ES
precision mediump float;
#endif

// --- Uniforms ---
uniform sampler2D u_texture;
// Halo uniforms
uniform vec3  u_halo_color;
uniform float u_halo_radius;
uniform float u_halo_falloff;
uniform float u_halo_strength;
// Normalization uniforms
uniform vec2 u_texCoord_min;
uniform vec2 u_texCoord_max;
uniform vec4 u_ambientToCounteract;


varying vec2 v_texCoord;

void main() {
    // 1. Create clean [0,1] coordinates for procedural effects
    vec2 range = u_texCoord_max - u_texCoord_min;
    vec2 normalizedTexCoord = (v_texCoord - u_texCoord_min) / range;
    vec2 proceduralCoord = normalizedTexCoord;
    proceduralCoord.y = 1.0 - proceduralCoord.y;

    // 3. --- Sample the moon texture ---
    // Sample the texture with the distorted coordinates
    vec4 baseColor = texture2D(u_texture, v_texCoord);

    // 5. --- Halo effect ---
    // Calculate the halo with the clean, undistorted procedural coordinates
    float dist = distance(proceduralCoord, vec2(0.5));
    float glow = smoothstep(u_halo_radius + u_halo_falloff, u_halo_radius, dist);
    float haloAlpha = glow * u_halo_strength;

    // 6. --- Final composition ---
    vec3 finalRgb = mix(u_halo_color, baseColor.rgb, baseColor.a);
    float finalAlpha = max(baseColor.a, haloAlpha);

    gl_FragColor = vec4(finalRgb, finalAlpha);
}

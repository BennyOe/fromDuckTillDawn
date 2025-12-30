#ifdef GL_ES
precision mediump float;
#endif

#define MAX_SHOCKWAVES 10

varying vec2 v_texCoord;

uniform sampler2D u_texture;
uniform vec2 u_resolution_px;
uniform int u_shockwave_count;
uniform vec2 u_center_uv[MAX_SHOCKWAVES];
uniform float u_times[MAX_SHOCKWAVES];
uniform float u_radius_px[MAX_SHOCKWAVES];

float getOffsetStrength(float t, float dist, float maxRad) {
    float ripplePos = t * maxRad * 0.01; // TODO tweak speed factor
    float d = dist - ripplePos;

    // Ring mask logic
    float strength = 1.0 - smoothstep(0.0, 0.035, abs(d));

    // Smooth intro and outro fades
    strength *= smoothstep(0.0, 0.3, t);
    strength *= 1.0 - smoothstep(0.5, 1.0, t);

    return strength * 0.017; // Base distortion intensity
}

void main() {
    float aspect = u_resolution_px.x / u_resolution_px.y;

    vec2 dispR = vec2(0.0);
    vec2 dispG = vec2(0.0);
    vec2 dispB = vec2(0.0);
    float totalShading = 0.0;

    for (int i = 0; i < MAX_SHOCKWAVES; i++) {
        // CHANGED: Breaking early based on uniform count
        if (i >= u_shockwave_count) break;

        float t = u_times[i];
        vec2 center = u_center_uv[i];

        vec2 dir = v_texCoord - center;

        // Correcting distance for aspect ratio
        float dist = length(dir * vec2(aspect, 1.0));
        vec2 normDir = normalize(dir * vec2(aspect, 1.0));

        // Time offsets for chromatic aberration
        float tOffset = 0.02 * sin(t * 3.14);

        float rS = getOffsetStrength(t + tOffset, dist, u_radius_px[i]);
        float gS = getOffsetStrength(t, dist, u_radius_px[i]);
        float bS = getOffsetStrength(t - tOffset, dist, u_radius_px[i]);

        // Accumulating displacement vectors per channel
        dispR += normDir * rS;
        dispG += normDir * gS;
        dispB += normDir * bS;

        totalShading += gS * 2.0;
    }

    // CHANGED: Using texture2D and gl_FragColor for WebGL 1
    float r = texture2D(u_texture, v_texCoord - dispR).r;
    float g = texture2D(u_texture, v_texCoord - dispG).g;
    float b = texture2D(u_texture, v_texCoord - dispB).b;

    gl_FragColor = vec4(r, g, b, 1.0);
    gl_FragColor.rgb += totalShading;
}

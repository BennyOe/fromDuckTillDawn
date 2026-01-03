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
    // ripplePos should reach maxRad when t is 1.0
    float ripplePos = t * maxRad;

    float d = dist - ripplePos;

    // Ring mask logic
    float strength = 1.0 - smoothstep(0.0, 0.015, abs(d));

    // centerMask so the area directly at the source stays undistorted
    float centerMask = smoothstep(0.0, 0.15, dist);
    strength *= centerMask;

    strength *= smoothstep(0.0, 0.3, t);
    strength *= 1.0 - smoothstep(0.5, 1.0, t);

    return strength;
}

void main() {
    float aspect = u_resolution_px.x / u_resolution_px.y;

    vec2 dispR = vec2(0.0);
    vec2 dispG = vec2(0.0);
    vec2 dispB = vec2(0.0);
    float totalShading = 0.0;

    for (int i = 0; i < MAX_SHOCKWAVES; i++) {
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
        float distortionIntensity = 0.027;
        dispR += normDir * rS * distortionIntensity;
        dispG += normDir * gS * distortionIntensity;
        dispB += normDir * bS * distortionIntensity;

        totalShading += gS * 0.4;
    }

    float r = texture2D(u_texture, v_texCoord - dispR).r;
    float g = texture2D(u_texture, v_texCoord - dispG).g;
    float b = texture2D(u_texture, v_texCoord - dispB).b;

    gl_FragColor = vec4(r, g, b, 1.0);

    vec3 waveColor = vec3(0.4, 0.2, 0.2);
    gl_FragColor.rgb += totalShading * waveColor;
}

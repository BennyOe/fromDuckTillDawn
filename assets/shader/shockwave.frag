#ifdef GL_ES
precision mediump float;
#endif

#define MAX_SHOCKWAVES 10

varying vec2 v_texCoord;
uniform sampler2D u_texture;
uniform vec2 u_resolution_px;

uniform int u_shockwave_count;
uniform vec2 u_center_uv[MAX_SHOCKWAVES];
uniform float u_radius_px[MAX_SHOCKWAVES];

void main() {
    vec4 scene = texture2D(u_texture, v_texCoord);
    vec2 fragPx = v_texCoord * u_resolution_px;

    float circ = 0.0;

    for (int i = 0; i < MAX_SHOCKWAVES; i++) {
        if (i >= u_shockwave_count) break;

        vec2 centerPx = u_center_uv[i] * u_resolution_px;
        float d = distance(fragPx, centerPx);
        circ = max(circ, step(d, u_radius_px[i]));
    }

    scene.rgb += circ * 0.5;
    gl_FragColor = scene;
}

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
varying vec2 v_texCoord;

uniform float u_time;

// Wasser-Parameter
uniform float u_speed;
uniform float u_speed_x;
uniform float u_speed_y;
uniform float u_emboss;
uniform float u_intensity;
uniform float u_frequency;
uniform float u_delta;
uniform float u_distortion_scale;

const int STEPS = 8;
const int ANGLE = 7;
const float PI = 3.1415926535897932;

float waveField(vec2 coord01, float t) {
    float delta_theta = 2.0 * PI / float(ANGLE);
    float acc = 0.0;
    for (int i = 0; i < STEPS; ++i) {
        float theta = delta_theta * float(i);
        vec2 c = coord01;
        c.x += cos(theta) * t * u_speed + t * u_speed_x;
        c.y -= sin(theta) * t * u_speed - t * u_speed_y;
        acc += cos((c.x * cos(theta) - c.y * sin(theta)) * u_frequency) * u_intensity;
    }
    return cos(acc);
}

void main() {
    vec2 uv01 = v_texCoord;
    float t = u_time * 1.3;

    float step01 = 1.0 / u_delta;

    float cc1 = waveField(uv01, t);

    float dx = u_emboss * (cc1 - waveField(uv01 + vec2(step01, 0.0), t)) / step01;
    float dy = u_emboss * (cc1 - waveField(uv01 + vec2(0.0, step01), t)) / step01;

    vec2 distortion = vec2(dx, dy) * u_distortion_scale;
    vec2 distortedCoord = uv01 + distortion;

    vec4 baseCol = texture2D(u_texture, fract(distortedCoord));

    gl_FragColor = baseCol;
}

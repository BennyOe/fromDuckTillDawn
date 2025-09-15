#ifdef GL_ES
precision mediump float;
#endif

// --- Textures ---
uniform sampler2D u_texture;      // water texture (unit 0)
uniform sampler2D u_fbo_texture;  // scene FBO texture (unit 1)

// --- Letterbox-aware mapping (sizes in FBO texels/pixels) ---
uniform vec2 u_fboSize;        // (texW, texH) e.g. (1280, 1024)
uniform vec2 u_contentOffset;  // (offX, offY) e.g. (0, 152)
uniform vec2 u_contentSize;    // (contentW, contentH) e.g. (1280, 720)

// --- Varyings from vertex shader ---
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec4 v_screenPos; // clip-space position (gl_Position)

// --- Distortion uniforms (unchanged) ---
uniform float u_time;
uniform float u_speed;
uniform float u_speed_x;
uniform float u_speed_y;
uniform float u_emboss;
uniform float u_intensity;
uniform float u_frequency;
uniform float u_delta;
uniform float u_distortion_scale;

// --- Waves ---
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
    // 1) Clip → NDC → [0..1] inside the CURRENT VIEWPORT (not full backbuffer)
    vec2 viewportUV = (v_screenPos.xy / v_screenPos.w) * 0.5 + 0.5;

    // 2) Map that viewport UV into FBO PIXELS of the content box (adds the letterbox offset)
    //    -> fboPx is in [off, off+size] in TEXELS of the FBO
    vec2 fboPx = u_contentOffset + viewportUV * u_contentSize;

    // Optional safety clamp against sampling the black bars when distortion pushes outside
    fboPx = clamp(fboPx, u_contentOffset, u_contentOffset + u_contentSize);

    // 3) Build a content-relative UV (0..1) only for the wave field (stable distortion scale)
    vec2 contentUV = (fboPx - u_contentOffset) / u_contentSize;

    // 4) Distortion in content space
    float t = u_time * 1.3;
    float step01 = 1.0 / u_delta;
    float cc1 = waveField(contentUV, t);
    float dx = u_emboss * (cc1 - waveField(contentUV + vec2(step01, 0.0), t)) / step01;
    float dy = u_emboss * (cc1 - waveField(contentUV + vec2(0.0, step01), t)) / step01;
    vec2 contentUVDistorted = contentUV + vec2(dx, dy) * u_distortion_scale;

    // 5) Convert distorted content-UV back to FBO PIXELS, then to FBO UVs for sampling
    vec2 fboPxDistorted = u_contentOffset + contentUVDistorted * u_contentSize;
    vec2 fboUV = fboPxDistorted / u_fboSize;

    // 6) Sample water texture (unit 0) and background (unit 1, the FBO)
    vec4 waterColor = texture2D(u_texture, v_texCoords);
    vec4 backgroundColor = texture2D(u_fbo_texture, fboUV);

    // 7) Composite (sprite alpha drives mix). Full opacity avoids "ghost" bleed-through.
    vec4 outCol = mix(backgroundColor, waterColor, v_color.a);
    outCol.a = 1.0;
    gl_FragColor = outCol;
}

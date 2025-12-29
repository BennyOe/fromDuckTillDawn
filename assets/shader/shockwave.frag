#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
uniform sampler2D u_texture;

uniform vec2 u_center_uv;
uniform vec2 u_resolution_px;
uniform float u_radius_px;

void main() {
    vec4 scene = texture2D(u_texture, v_texCoord);

    vec2 fragPx = v_texCoord * u_resolution_px;
    vec2 centerPx = u_center_uv * u_resolution_px;
    float d = distance(fragPx, centerPx);
    float circ = step(d, u_radius_px);

    scene.rgb += circ * 0.5;
    gl_FragColor = scene;
}

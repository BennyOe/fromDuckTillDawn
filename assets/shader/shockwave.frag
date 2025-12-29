#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
uniform sampler2D u_texture;

uniform vec2 u_center_px;
uniform float u_radius_px;

void main() {
    vec4 scene = texture2D(u_texture, v_texCoord);

    float d = distance(gl_FragCoord.xy, u_center_px);
    float circ = step(d, u_radius_px);

    scene.rgb += circ * 0.5;
    gl_FragColor = scene;
}

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

// --- Distortion uniforms ---
uniform float u_time;
uniform float u_flowSpeed;
uniform float u_waveSpeed;
uniform float u_waveFrequency;
uniform float u_waveHeight;
uniform float u_rippleSpeed;
uniform float u_rippleFrequency;
uniform float u_rippleHeight;

// --- Chaos controls (keep scalar for easy binding) ---
uniform float u_chaosAmount;   // 0..0.06 strength of domain warp (content-UV space)
uniform float u_chaosScale;    // 1..8  frequency of domain warp
uniform float u_flowJitter;    // 0..1  modulates flow speed over time/Y

// Hash/Noise/FBM for irregular motion
float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }
float noise(vec2 p){
    vec2 i=floor(p), f=fract(p);
    float a=hash(i);
    float b=hash(i+vec2(1.,0.));
    float c=hash(i+vec2(0.,1.));
    float d=hash(i+vec2(1.,1.));
    vec2 u=f*f*(3.-2.*f);
    return mix(a,b,u.x) + (c-a)*u.y*(1.-u.x) + (d-b)*u.x*u.y;
}
float fbm(vec2 p){
    float s=0.0, a=0.5;
    for(int i=0;i<4;i++){ s+=a*noise(p); p=p*2.03+vec2(11.7,5.2); a*=0.5; }
    return s;
}
mat2 rot(float a){ float c=cos(a), s=sin(a); return mat2(c,-s,s,c); }

// Function to generate layered noise for waves
// It combines two sine waves to create a more natural pattern
float getWave(vec2 pos, float freq, float speed, float height) {
    float angle = pos.x * freq + u_time * speed;
    float wave = sin(angle) * cos(pos.y * freq * 0.8 + u_time * speed * 0.6);
    return wave * height;
}

void main() {
    // 1) Calculate UV coordinates for sampling the background (FBO)
    vec2 viewportUV = (v_screenPos.xy / v_screenPos.w) * 0.5 + 0.5;
    vec2 fboPx = u_contentOffset + viewportUV * u_contentSize;
    vec2 contentUV = (fboPx - u_contentOffset) / u_contentSize;

    // Add a slow flow, but jitter it over time and vertically to avoid regularity
    float jitter = (fbm(vec2(u_time*0.07, v_texCoords.y*5.3))*2.0 - 1.0) * u_flowJitter;
    float flowX = u_time * u_flowSpeed * (1.0 + jitter);
    vec2 flow = vec2(flowX, 0.0);
    vec2 texCoordsWithFlow = v_texCoords + flow;

    // Domain warp the content UVs for irregular refraction sampling
    vec2 p = contentUV * max(u_chaosScale, 0.0001);
    float a1 = fbm(p + vec2( 0.21*u_time,  0.0));
    float a2 = fbm(p + vec2(-0.17*u_time,  3.1));
    vec2 warp = (vec2(a1, a2) - 0.5) * (u_chaosAmount * 2.0);
    vec2 warpedUV = contentUV + warp;

    // Two wave layers evaluated on warped UVs
    float wave   = getWave(warpedUV, u_waveFrequency,   u_waveSpeed,   u_waveHeight);
    float ripples= getWave(warpedUV, u_rippleFrequency, u_rippleSpeed, u_rippleHeight);

    // Build a 2D distortion vector and rotate it to break axis alignment
    vec2 distortion = rot(0.7) * vec2(wave + ripples, wave - 0.7*ripples);

    // Apply refraction to the background with the chaotic distortion
    vec2 fboPxDistorted = fboPx + distortion * u_contentSize;
    vec2 fboUV = fboPxDistorted / u_fboSize;
    fboUV = clamp(fboUV, u_contentOffset / u_fboSize, (u_contentOffset + u_contentSize) / u_fboSize);

    // Also warp the water albedo sampling a bit so its motion doesn’t look locked
    vec2 texWarp = texCoordsWithFlow + warp * 0.035;
    vec4 waterColor = texture2D(u_texture, texWarp);
    vec4 backgroundColor = texture2D(u_fbo_texture, fboUV);

    // Composite
    vec4 outCol = mix(backgroundColor, waterColor, v_color.a);
    outCol.a = 1.0;
    gl_FragColor = outCol;
}

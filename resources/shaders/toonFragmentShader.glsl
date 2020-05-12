#version 330 core

in vec2 UV;
in vec3 fragNormalV;
in vec3 fragPosV;

in vec3 cameraPosV;
in vec3 lightPosV;

const vec3 lightSourcePositionTest = vec3(0, 0, 10);

out vec3 color;

uniform sampler2D texSampler;

uniform int shaderMode;

// boundary conditions to create toon effect
const float diffMin = 0.01;
const float diffMed = 0.1;
const float specMin = 0.1;


// Tone mapping and display encoding combined
vec3 tonemap(vec3 linearRGB)
{
    float L_white = 0.7; // Controls the brightness of the image

    float inverseGamma = 1./2.2;
    return pow(linearRGB/L_white, vec3(inverseGamma)); // Display encoding - a gamma
}

void main(){

    float I_a = 0.2;
    float k_d = 0.4;
    float k_s = 0.4;
    vec3 linear_color = vec3(0);
    float roughness = 10;

    vec3 C_diff = vec3(texture( texSampler, UV ));
    vec3 C_spec = vec3(0.5);

    vec3 N = normalize(fragNormalV);
    vec3 L = normalize(lightPosV - fragPosV);
    vec3 V = normalize(cameraPosV - fragPosV);
    vec3 R = normalize(reflect(-L, N));

    float diffuse = k_d * max(0, dot(N, L));
    float specular = k_s * pow(max(0, dot(V, R)), roughness);

    if (shaderMode == 0) {
        // phong shading model
        linear_color = C_diff * I_a + C_diff * diffuse + C_spec * specular;

    } else if (shaderMode == 1) {
        // toon shading
        linear_color = C_diff * I_a;
        if (diffuse > diffMed) {
            linear_color += C_diff * k_d;
        } else if (diffuse > diffMin) {
            linear_color += C_diff * k_d * 0.5;
        }
        if (specular > specMin) {
            linear_color += C_spec * k_s;
        }
    }

    float test = dot(N, L);
    float threshold = 1;
    if (test > threshold) {
//        linear_color = vec3(test - threshold);
    } else {
//        linear_color = vec3(0);
    }

//    linear_color = vec3(max(0,dot(N, L)));

    color = tonemap(linear_color);
}

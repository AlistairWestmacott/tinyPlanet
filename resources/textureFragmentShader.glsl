#version 330 core

// Interpolated values from the vertex shaders
in vec2 UV;
in vec3 frag_normal;
in vec3 frag_pos;

// Ouput data
out vec3 color;

// Values that stay constant for the whole mesh.
uniform sampler2D texSampler;

// location of point light source.
const vec3 pointLightSource = vec3(0, 0, 10);
const vec3 cameraPosition = vec3(4, 3, 3);

// Tone mapping and display encoding combined
vec3 tonemap(vec3 linearRGB)
{
    float L_white = 0.7; // Controls the brightness of the image

    float inverseGamma = 1./2.2;
    return pow(linearRGB/L_white, vec3(inverseGamma)); // Display encoding - a gamma
}

void main(){

    vec3 I_a = vec3(0.5, 0.5, 0.5);
    float k_d = 0.5;
    float k_s = 0.5;
    vec3 linear_color = vec3(0, 0, 0);
    float alpha = 10; // TODO: not sure what alpha actually represents

    // Sample the texture and replace diffuse surface colour (C_diff) with texel value
    vec3 C_diff = vec3(texture( texSampler, UV ));
    C_diff = vec3(1,1,1);
    vec3 C_spec = vec3(1, 1, 1);

    float I = 1; // TODO: figure out what this needs to be, I'm not sure where to get this value from

    // Calculate colour using Phong illumination model

    vec3 n = normalize(frag_normal);

    vec3 L = normalize(pointLightSource - frag_pos);
    vec3 V = normalize(cameraPosition - frag_pos);
    vec3 R = normalize(reflect(L * -1, n));

    vec3 diffuse = C_diff * k_d * I * max(0, dot(n, L));
    vec3 specular = C_spec * k_s * I * pow(max(0, dot(R, V)), alpha);


    //linear_color = I_a * C_diff + diffuse + specular;
//    linear_color = I_a * diffuse + specular;
    color = C_diff * max(0, dot(n, L));
    color = specular + diffuse;

    // what actually is tonemapping?
//    color = tonemap(linear_color);

    // Output color = color of the texture at the specified UV
    //color = texture( texSampler, UV ).rgb;
}

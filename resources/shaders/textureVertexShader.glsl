#version 330 core

// Input vertex data, different for all executions of this shader.
layout(location = 0) in vec3 vertexPosition_m;
layout(location = 1) in vec2 vertexUV;
layout(location = 2) in vec3 vertexNormal_m;

// Output data ; will be interpolated for each fragment.
out vec2 UV;
out vec3 fragNormalV;
out vec3 fragPosV;

out vec3 cameraPosV;
out vec3 lightPosV;

uniform vec3 cameraV;
uniform vec3 lightV;

uniform mat4 MVP;
uniform mat4 MV;

uniform mat3 normalMVP;

void main(){

    // Output position of the vertex, in clip space : MVP * position
    gl_Position =  MVP * vec4(vertexPosition_m, 1);

    vec4 fragNormalHomoW = MV * vec4(vertexNormal_m, 0);
    vec4 fragPosHomoW = MV * vec4(vertexPosition_m, 1);

    fragNormalV = fragNormalHomoW.xyz;
    fragPosV = fragPosHomoW.xyz;

    // UV of the vertex. No special space for this one.
    UV = vertexUV;

    cameraPosV = cameraV;
    lightPosV = lightV;

}
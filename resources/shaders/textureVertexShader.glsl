#version 330 core

// Input vertex data, different for all executions of this shader.
layout(location = 0) in vec3 vertexPosition_modelspace;
layout(location = 1) in vec2 vertexUV;
layout(location = 2) in vec3 vertexNormal;

// Output data ; will be interpolated for each fragment.
out vec2 UV;
out vec3 frag_normal;    // fragment normal in world coordinates
out vec3 frag_pos; 		// fragment position in world coordinates

out vec3 cameraPosition;
out vec3 lightSourcePosition;

uniform vec3 camera;
uniform vec3 lightSource;

uniform mat4 MVP;

uniform mat3 normalMVP;

void main(){

    // Output position of the vertex, in clip space : MVP * position
    gl_Position =  MVP * vec4(vertexPosition_modelspace,1);
    frag_pos = vec3(gl_Position.x, gl_Position.y, gl_Position.z);

    frag_normal = normalMVP * vertexNormal;

    // UV of the vertex. No special space for this one.
    UV = vertexUV;

    cameraPosition = camera;
    lightSourcePosition = lightSource;

}
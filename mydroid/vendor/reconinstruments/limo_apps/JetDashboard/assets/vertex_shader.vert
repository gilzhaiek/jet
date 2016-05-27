precision mediump float;

attribute vec4 vPosition;
attribute vec2 vTexture;
attribute float a_MVPMatrixIndex;

uniform float u_EnableDrawingText;
uniform mat4 u_MVPMatrix[24];

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projMatrix;

varying vec2 texCoords;
varying vec3 vertexPos;

void main(){

	if(u_EnableDrawingText > 0.5){
		int mvpMatrixIndex = int(a_MVPMatrixIndex);
		texCoords = vTexture;
		vertexPos = vec3(0.0);
		gl_Position = u_MVPMatrix[mvpMatrixIndex] * vPosition;
	}
	else {
		vec4 outputVec = projMatrix * viewMatrix * modelMatrix * vPosition; 
		texCoords = vTexture;
		vertexPos = vec3(modelMatrix * vPosition); 
		gl_Position = outputVec; 
	}
}

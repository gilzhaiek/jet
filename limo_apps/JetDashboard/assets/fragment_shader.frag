precision mediump float;

uniform sampler2D texSampler;

/*Enables*/
uniform float u_EnableDrawingText;
uniform float u_EnableAlphaTest;
uniform float u_EnableRadialGradient;

/*uniforms for radial blur*/
uniform vec3 u_CenterOfRadius;
uniform float u_RadialGradientBegin;
uniform float u_RadialGradientEnd;

/*stuff for text rendering*/
uniform float u_TextSharpness;
uniform vec4 u_Color;

varying vec2 texCoords;
varying vec3 vertexPos;

float lerp(float v0, float v1, float t){
	return (1.0-t)*v0 + t*v1;
}

vec4 applyGradient(vec4 inputColor){
	if(u_EnableRadialGradient > 0.5){
		float distanceFromCenter = length(vertexPos - u_CenterOfRadius);
		if(distanceFromCenter >= u_RadialGradientBegin
				&& distanceFromCenter < u_RadialGradientEnd){
			float t = (distanceFromCenter - u_RadialGradientBegin) / (u_RadialGradientEnd - u_RadialGradientBegin);
			inputColor.rgb = inputColor.rgb * lerp(1.0, 0.0, t);
		}
		else if(distanceFromCenter >= u_RadialGradientEnd){
			discard;
		}
	}
	return inputColor;
}

void main(){
	if(u_EnableDrawingText > 0.5){
		vec4 result_color = texture2D(texSampler, texCoords).w * u_Color;
		float low_threshold = u_TextSharpness * 0.20;
		if(u_EnableAlphaTest > 0.5){
			/*any alpha value below the low threshold, discard*/
			if(result_color.a < low_threshold){
				discard;
			}
			/*between the low threshold and the upper threshold, u_TextSharpness,
			 create a black outline around each character.
			 This is so the text is clearly visible in all backgrounds.*/
			else if(result_color.a < u_TextSharpness){
				result_color.rgb = vec3(0.0);
				result_color.a = 0.75;
			}
		}
		gl_FragColor = result_color;
	} 
	else {
		gl_FragColor = applyGradient(texture2D(texSampler, texCoords.st)); 
	}
}

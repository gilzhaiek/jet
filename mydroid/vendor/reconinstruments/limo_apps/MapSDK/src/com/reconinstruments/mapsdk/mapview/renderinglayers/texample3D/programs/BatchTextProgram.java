package com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.programs;

import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.AttribVariable;



public class BatchTextProgram extends Program {

	private static final AttribVariable[] programVariables = {
		AttribVariable.A_Position, AttribVariable.A_TexCoordinate, AttribVariable.A_MVPMatrixIndex
	};
	
	private static final String vertexShaderCode =
			"uniform mat4 u_MVPMatrix[24];      \n"     // An array representing the combined 
														// model/view/projection matrices for each sprite
			
		  + "attribute float a_MVPMatrixIndex; \n"	// The index of the MVPMatrix of the particular sprite
		  + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
		  + "attribute vec2 a_TexCoordinate;\n"     // Per-vertex texture coordinate information we will pass in
		  + "varying vec2 v_TexCoordinate;  \n"   // This will be passed into the fragment shader.
		  + "void main()                    \n"     // The entry point for our vertex shader.
		  + "{                              \n"
		  + "   int mvpMatrixIndex = int(a_MVPMatrixIndex); \n"
		  + "   v_TexCoordinate = a_TexCoordinate; \n"
		  + "   gl_Position = u_MVPMatrix[mvpMatrixIndex]   \n"     // gl_Position is a special variable used to store the final position.
		  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
		  											// normalized screen coordinates.
		  + "}                              \n";    


	private static final String fragmentShaderCode =
			"precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
	        // precision in the fragment shader.
			+ "uniform sampler2D u_Texture;       \n"    // The input texture.
			+ "uniform float u_EnableAlphaTest;\n"
			+ "uniform float u_TextSharpness;"
			+ "uniform vec4 u_Color;          \n"
			+ "varying vec2 v_TexCoordinate;  \n" // Interpolated texture coordinate per fragment.
			
			+ "void main()                    \n"     // The entry point for our fragment shader.
			+ "{                              \n"
			+ "	  vec4 result_color = texture2D(u_Texture, v_TexCoordinate).w * u_Color;\n"
			+ "	  if(u_EnableAlphaTest > 0.5){\n"
			+ "	  	  if(result_color.a < u_TextSharpness){\n"
			+ "		     discard;\n"
			+ "		  }\n"
			+ "	  }\n"
			+ "   gl_FragColor = result_color;\n" // texture is grayscale so take only grayscale value from  
																					   // it when computing color output (otherwise font is always black)
			+ "}                             \n";

	@Override
	public void init() {
		super.init(vertexShaderCode, fragmentShaderCode, programVariables);
	}
	
}

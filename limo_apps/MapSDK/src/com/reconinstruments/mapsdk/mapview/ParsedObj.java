package com.reconinstruments.mapsdk.mapview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;

/**
 * Parser for OBJ model files. Upon completing <code>parse()</code>, <code>MeshGL</code> object will be initialized.
 */
public class ParsedObj {

	/*******************************************
	 * Private / Protected members
	 * *****************************************/
	
	private static final String TAG = ParsedObj.class.getSimpleName();
	
	private MeshGL mMesh;
	private String mFilename = null;

	private BufferedReader objFileVert, objFileFaces;
	private static volatile ArrayList<ArrayList<Float>> verts, normals, tex;
	private static volatile LinkedList<ArrayList<Float>> vertBuffer, normBuffer, texBuffer;
	private static volatile ArrayList<Float> vertArray, normArray, texArray;
	private String buffer, bufferFaces;
	private volatile float[] maxVerts, minVerts;
	private volatile float[] center;
	private volatile int pointsPerFace = 3, numTriangles = 0;
	private volatile boolean loaded = false, hasTextures = false, hasNormals = false, facesNegative = false;
	private float mBoundingRadius;
	
	/**
	 * This list keeps track of how many points have been parsed since
	 * the last occurrence of 'f' or the faces element in the file to parse.
	 * Used only when dealing with negative vertex reference members<br><br>
	 * <b>e.g.</b> <br><br>
	 * v 0.000000 2.000000 2.000000<br>
	 * v 0.000000 0.000000 2.000000<br>
	 * v 2.000000 0.000000 2.000000<br>
	 * v 2.000000 2.000000 2.000000<br>
	 * f -4 -3 -2 -1<br>
	 * <i>coordsSinceLastFace[0] = 4</i> <br><br>
	 * v 2.000000 2.000000 0.000000<br>
	 * v 2.000000 0.000000 0.000000<br>
	 * v 0.000000 0.000000 0.000000<br>
	 * v 0.000000 2.000000 0.000000<br>
	 * f -4 -3 -2 -1<br>
	 * <i>coordsSinceLastFace[1] = 4 </i><br>
	 */
	private volatile ConcurrentLinkedQueue<Integer> numVertSinceLastFace, numTexSinceLastFace, numNormSinceLastFace;
	
	/**
	 * using separate counters for verts, tex, and normals recorded because 
	 * ConcurrentLinkedQueue.size() is not constant-time
	 */
	private volatile int numVerts = 0, numTex = 0, numNorms = 0;
	private FaceParser faceParserThread;

	/*******************************************
	 * Public members
	 * *****************************************/
	public ArrayList<MeshGL> meshList;
	private volatile int progress = 0, verts_progress = 0, tex_progress = 0, norms_progress = 0, faces_progress = 0;

	enum ElementType {
		MESHES, VERTICES, TEXTURES, NORMALS, FACES
	};

	ElementType element;
	
	/**
	 * <b>Constructor</b> Parses an OBJ file given a path and filename
	 * 
	 * @param <b><i>filename</i></b> name of full path to file
	 */
	public ParsedObj(String filename, MeshGL mesh) {
		mFilename = filename;
		mMesh = mesh;
		try {
			objFileVert = new BufferedReader(new FileReader(new File(filename)));
			objFileFaces = new BufferedReader(new FileReader(new File(filename)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}

		verts = new ArrayList<ArrayList<Float>>();
		normals = new ArrayList<ArrayList<Float>>();
		tex = new ArrayList<ArrayList<Float>>();
		vertArray = new ArrayList<Float>();
		normArray = new ArrayList<Float>();
		texArray = new ArrayList<Float>();
		meshList = new ArrayList<MeshGL>();
		vertBuffer = new LinkedList<ArrayList<Float>>();
		normBuffer = new LinkedList<ArrayList<Float>>();
		texBuffer = new LinkedList<ArrayList<Float>>();

		maxVerts = new float[3];
		minVerts = new float[3];
		center = new float[3];

		numVertSinceLastFace = new ConcurrentLinkedQueue<Integer>();
		numTexSinceLastFace = new ConcurrentLinkedQueue<Integer>();
		numNormSinceLastFace = new ConcurrentLinkedQueue<Integer>();
		
		faceParserThread = new FaceParser();
	}

	public boolean parse() {
		Log.d(TAG,"Parsing " + mFilename + "... ");
		faceParserThread.start();
		try {

			while ((buffer = advanceLine(objFileVert)) != null) {

				if (buffer.length() == 0 || buffer.charAt(0) == '#'
						|| buffer.startsWith("\r\n") || buffer.equals("")) {
					continue;
				}

				// meshes
				else if (buffer.charAt(0) == 'o') {
				}

				// vertex, normal, and texture coordinates
				else if (buffer.charAt(0) == 'v') {
					String split[] = buffer.split(" +");

					// textures
					if (buffer.charAt(1) == 't') {
						ArrayList<Float> vt = new ArrayList<Float>();
						for (int j = 1; j < 3; j++)
							vt.add(Float.valueOf(split[j]));
						tex.add(vt);
						numTex++;
					}
					// normals
					else if (buffer.charAt(1) == 'n') {
						ArrayList<Float> vn = new ArrayList<Float>();
						for (int i = 1; i < 4; i++)
							vn.add(Float.valueOf(split[i]));
						normals.add(vn);
						numNorms++;
					}

					// verts
					else {
						ArrayList<Float> v = new ArrayList<Float>();
						for (int k = 1; k < 4; k++){
							float value = Float.valueOf(split[k]);
							v.add(value);
							minVerts[k-1] = (minVerts[k-1] > value) ? value : minVerts[k-1];
							maxVerts[k-1] = (maxVerts[k-1] < value) ? value : maxVerts[k-1];
						}
						verts.add(v);
						numVerts++;
					}
				}
				
				else if (buffer.charAt(0) == 'f') {
					numVertSinceLastFace.add(numVerts);
					numTexSinceLastFace.add(numTex);
					numNormSinceLastFace.add(numNorms);
				}
				synchronized(faceParserThread){
					faceParserThread.notify();
				}
			}// end of while loop

			faceParserThread.join();

			// determine center point of mesh(es)
			for (int t = 0; t < 3; t++) {
				center[t] = (Math.abs(maxVerts[t]) - Math.abs(minVerts[t])) / 2.0f;
			}
			
			//get bounding radius
			mBoundingRadius = (float) Math.sqrt((maxVerts[0] - center[0]) * (maxVerts[0] - center[0]) 
					+ (maxVerts[1] - center[1]) * (maxVerts[1] - center[1]) 
					+ (maxVerts[2] - center[2]) * (maxVerts[2] - center[2]));
			
			generateArrays();
			
			loaded = true;
			objFileVert.close();
			objFileFaces.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		return true;
	}

	/*******************************
	 * 
	 * Generate Arrays
	 * 
	 *******************************/
	// convert to float arrays
	private void generateArrays() {
		// add vertices
		int size = vertArray.size(), normSize = normArray.size(), texSize = texArray
				.size(), numVerts = size / 3;
		float[] ff = new float[size], nn = new float[normSize], tt = new float[texSize];

		for (int i = 0; i < numVerts; i++) {
			for (int j = 0; j < 3; j++) {
				ff[j + 3 * i] = vertArray
						.get(j + 3 * i);
				if (normSize > 0 && (j + 3 * i) < normSize)
					nn[j + 3 * i] = normArray.get(j + 3 * i);
			}
			for (int k = 0; k < 2; k++) {
				if (texSize > 0 && (k + 2 * i) < texSize) {
					tt[k + 2 * i] = texArray.get(k + 2 * i);
				} else
					break;
			}

		}
		mMesh.init(ff, tt, nn, mBoundingRadius);
	}
	
	private String advanceLine(BufferedReader bf) throws IOException {
		String line = bf.readLine();
		if(line != null && !line.isEmpty()){
			if(line.charAt(0) != 'f')
				progress++;
		}
		else
			if(line != null && line.isEmpty()) progress++;
		return line;
	}
	
	/*********************** GET methods ****************************/

	public String getFilename(){
		return mFilename;
	}
	
	public float getBoundingRadius(){
		return mBoundingRadius;
	}
	
	/**
	 * Returns the current progress value (the current line number in the
	 * parsing operation)
	 * 
	 * @returns int - progress
	 */
	public int getProgress() {
		return progress + faces_progress;
	}
	
	public int getVertsProgress(){
		return verts_progress;
	}
	public int getTexProgress(){
		return tex_progress;
	}
	public int getNormsProgress(){
		return norms_progress;
	}
	public int getFacesProgress(){
		return faces_progress;
	}

	public int getNumTriangles(){
		return numTriangles;
	}
	
	public int getNumVertices(){
		return numVerts;
	}
	
	public int getNumTextures(){
		return numTex;
	}
	
	public int getNumNormals(){
		return numNorms;
	}
	
	public MeshGL getMesh(){
		return mMesh;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	/**********************************************************
	 * 
	 * 					FaceParser Class
	 * 
	 * ********************************************************/
	
	private class FaceParser extends Thread implements Runnable {
		
		private String advanceFaceLine(BufferedReader bf) throws IOException {
			String line = bf.readLine();
			if(line != null && !line.isEmpty()){
				if(line.charAt(0) == 'f')
					faces_progress++;
			}
			return line;
		}

		@Override
		public void run() {
			synchronized (this) {
				try {
					while ((bufferFaces = advanceFaceLine(objFileFaces)) != null) {

						if (bufferFaces.length() == 0
								|| bufferFaces.charAt(0) == '#'
								|| bufferFaces.startsWith("\r\n")
								|| bufferFaces.equals("")) {
							continue;
						}

						// faces
						else if (bufferFaces.charAt(0) == 'f') {
							
							String temp[] = bufferFaces.split(" +");

							pointsPerFace = temp.length - 1;
							numTriangles = (pointsPerFace == 3) ? numTriangles+1 : numTriangles+2;

							int firstSlash, secondSlash, vertOffset = 0, texOffset = 0, normOffset = 0;
							
							//if faces element has a negative vertex reference number, 
							//
							if(temp[1].indexOf("-") != -1){
								facesNegative = true;
							}
							else
								facesNegative = false;
							
							if(facesNegative){
								while(numVertSinceLastFace.peek() == null
										|| numTexSinceLastFace.peek() == null 
										|| numNormSinceLastFace.peek() == null) wait();
								vertOffset = numVertSinceLastFace.peek() + 1;
								texOffset = numTexSinceLastFace.peek() + 1;
								normOffset = numNormSinceLastFace.peek() + 1;
							}
														
							for (int i = 1; i < pointsPerFace + 1; i++) {

								// if one '/' is found, mesh contains normals
								// and/or
								// textures
								/*****************************************************/
								/****************** FIRST SLASH **********************/
								if ((firstSlash = temp[i].indexOf("/", 0)) != -1) {

									// take firstSlash # of chars from
									// bufferFaces, and
									// convert that into a float
									int vertIndex = Integer.valueOf(temp[i]
											.substring(0, firstSlash)) - 1;
									
									//if mesh is in QUADS
									if (i != 2 && pointsPerFace == 4) {
										while (verts.size() < (vertIndex + 1)) wait();
										vertBuffer.add(verts.get(vertOffset + vertIndex));
									}

									//if mesh is in TRIANGLES
									if (i != 4) {
										while (verts.size() < (vertIndex + 1)) wait();
										for (int j = 0; j < 3; j++) {
											Float value = verts.get(vertOffset + vertIndex).get(j);
											vertArray.add(value);
										}
									}

									/******************************************************/
									/****************** SECOND SLASH **********************/
									// check if there is a second slash
									if ((secondSlash = temp[i].indexOf("/",
											firstSlash + 1)) != -1) {

										// if two '/'s in a row, there are no textures
										if ((secondSlash - firstSlash) <= 1) {
											int normIndex = Integer.valueOf(temp[i].substring(secondSlash + 1)) - 1;
											
											//QUADS
											if (i != 2 && pointsPerFace == 4) {
												while (normals.size() < (normIndex + 1)) wait();
												normBuffer.add(normals.get(normOffset + normIndex));
											}

											//TRIANGLES
											if (i != 4) {
												while (normals.size() < (normIndex + 1)) wait();
												for (int k = 0; k < 3; k++) {
													normArray.add(normals.get(normOffset + normIndex).get(k));
												}
											}
											hasNormals = true;
											hasTextures = false;
										}

										// otherwise, we have vert/tex/normal
										else {
											int texIndex = Integer.valueOf(temp[i].substring(firstSlash + 1,secondSlash)) - 1;
											int normIndex = Integer.valueOf(temp[i].substring(secondSlash + 1)) - 1;

											//QUADS
											if (i != 2 && pointsPerFace == 4) {
												while (tex.size() < (texIndex + 1) || normals.size() < (normIndex + 1))
													wait();
												texBuffer.add(tex.get(texOffset + texIndex));
												normBuffer.add(normals.get(normOffset + normIndex));
											}

											//TRIANGLES
											if (i != 4) {
												while (tex.size() < (texIndex + 1) || normals.size() < (normIndex + 1))
													wait();
												texArray.add(tex.get(texOffset + texIndex).get(0));
												texArray.add(1 - tex.get(texOffset + texIndex).get(1));

												for (int t = 0; t < 3; t++) {
													normArray.add(normals.get(normOffset + normIndex).get(t));
												}
											}

											hasNormals = hasTextures = true;
										}

									}

									// if there is no second slash vert/tex
									else {
										int texIndex = Integer.valueOf(temp[i].substring(firstSlash + 1)) - 1;

										//QUADS
										if (i != 2 && pointsPerFace == 4) {
											while (tex.size() < (texIndex + 1)) wait();
											texBuffer.add(tex.get(texOffset + texIndex));
										}

										//TRIANGLES
										if (i != 4) {
											while (tex.size() < (texIndex + 1)) wait();
											texArray.add(tex.get(texOffset + texIndex).get(0));
											texArray.add(1 - tex.get(texOffset + texIndex).get(1));
										}
										hasNormals = false;
										hasTextures = true;
									}
								}
								/**************************************************/
								/****************** NO SLASH **********************/
								// no slashes found, only verts
								else {
									int index = Integer.valueOf(temp[i]) - 1;
									
									//QUADS
									if (i != 2 && pointsPerFace == 4) {
										while (verts.size() < (index + 1)) wait();
										vertBuffer.add(verts.get(vertOffset + index));
									}

									//TRIANGLES
									if (i != 4) {
										while (verts.size() < (index + 1)) wait();
										for (int g = 0; g < 3; g++) {
											Float value = verts.get(vertOffset + index).get(g);
											vertArray.add(value);
										}
									}
									hasNormals = hasTextures = false;
								} 
							} //end of for loop
							
							if (facesNegative) {
								numVertSinceLastFace.remove();
								if (hasNormals)
									numNormSinceLastFace.remove();
								if (hasTextures)
									numTexSinceLastFace.remove();
							}
							
							// convert quad faces into triangles
							// keep track of a buffer to store the extra
							// vertices
							if (pointsPerFace == 4) {
								while (!vertBuffer.isEmpty()) {
									if (vertBuffer.peek() != null) {
										ArrayList<Float> ff = vertBuffer.poll();
										vertArray.addAll(ff);
									}
									if (normBuffer.peek() != null)
										normArray.addAll(normBuffer.poll());
									if (texBuffer.peek() != null)
										texArray.addAll(texBuffer.poll());
								}
							}

						} // end of faces
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
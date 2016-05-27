/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.routing;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * This interface define the  rendering related functions
 * that might be used by the Routing Engine for an ReEdge
 */

public interface IEdgeRenderer
{
	void drawHilite( Canvas canvas, Matrix transform );
	RectF getBBox();										//return the bounding box of the renderer
}
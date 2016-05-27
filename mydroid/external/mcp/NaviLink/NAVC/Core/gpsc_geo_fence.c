/*********************************************************************************

                                   TI GPS Confidential

*********************************************************************************/
/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName			:	gpsc_geofence.c
 *
 * Description     	:
 * This file contains the geo-fence implementation for both circular and 
 * polygonal fence
 *
 *
 * Author         	: 	Makbul Siram - makbul@ti.com
 *
 *
 ******************************************************************************
 */

#include "gpsc_geo_fence.h"

#include <math.h>
#include "gpsc_msg.h"
#define TRUE  1
#define FALSE 0
#define C_PI 3.1415926535898 
#define VEL_FACTOR 0.01526 /* 1000/65535 m/sec per bit */
#define EARTH_RADIUS 6371 // in km

/* 
 *  U8 in
 *  0: point is outside the fence
 *  1: point is inside the geofence
 */
S32 decimal_rounding(S32 num)
{  
  U8 temp;
  S32 num1;
  if(num<0)
  {
    num1 = -num;
    temp = (U8)(num1%10);
    if(temp > 5){

      num = num - 10;
      num = num/10;
    }
    else
    {
      num = num/10;
    }
  }
  else{
    temp = (U8)(num%10);
    if(temp > 5){

      num = num + 10;
      num = num/10;
    }
    else
    {
      num = num/10;
    }
  }
  return num;
}

/*===========================================================================

FUNCTION
  mapXY

DESCRIPTION
  This function Map Latittude and Longitude to X-Y coordinate system using
  Spherical Polar Coordinates System

PARAMETERS
  p_zPoint: point in X-Y system
  lat: latitude to be mapped
  lan: longitude to be mapped
===========================================================================*/

U8 mapToXY(S32 *p_DestnX , S32 *p_DestnY, S32 latitude, S32 longitude) 
{
    DBL x,y;

    x = latitude*(DBL)(22/(pow(2,32)*7));
    y = longitude*(DBL)(22/(pow(2,31)*7));
    
    STATUSMSG("Geofence: Fix Latitude %lf  Longitude %lf",x,y);
	if(x > (C_PI/2) || x < -(C_PI/2) || y > C_PI || y < -C_PI)
	{
		return 0;
		
	}
 
    *p_DestnX = (S32)decimal_rounding((S32)(x*pow(10,8)));  
    *p_DestnY = (S32)decimal_rounding((S32)(y*pow(10,8))); 
    return 1;
    
}
/*===========================================================================

FUNCTION
  geofence_boundary_check

DESCRIPTION
  checks boundary condition upto 5 decimal places. point on the line is considered
  as inside fence.
  Points are expected to be stored in after decimal rounding, so no need to do
  this step again here.

PARAMETERS
  
===========================================================================*/

U8 geofence_boundary_check(U8 n,T_GPSC_geo_fence_vertices *p_zVertices,
                  T_GPSC_geo_fence_vertices *p_zPoint)
{

  S32 x, y, x1, y1, x2, y2;
  U8 i, j, in=0;

  x = p_zPoint->geo_fence_latitude;
  y = p_zPoint->geo_fence_longitude;
  
  for (i=0,j = (U8)(n-1); i < n; j = i++) 
  {
      x1 = p_zVertices[j].geo_fence_latitude;
      y1 = p_zVertices[j].geo_fence_longitude;
      x2 = p_zVertices[i].geo_fence_latitude;
      y2 = p_zVertices[i].geo_fence_longitude;
	  
      if( (y*(x2-x1))==( x*(y2-y1) + y1*x2 - y2*x1) )
      {
        in = 1; 
		break;
      }
      else
      {
        in = 0;
      }
   
   } 
   
   return in;
}

/*===========================================================================

FUNCTION
  geofence_polygon

DESCRIPTION
  This function checks if the point is in or out of the polygonal fence using
  Ray Casting Algorithm

PARAMETERS
  n: Number of vertices in polygon
  p_zVertices: vertices of polygon
  p_zPoint: point
===========================================================================*/
U8 geofence_polygon(U8 n, T_GPSC_geo_fence_vertices *p_zVertices,
                    T_GPSC_geo_fence_vertices *p_zPoint)
{
      U8 i, j, in = 0;
	  S32 DeltaX;
	  S32 DeltaY;
        
	  
      for (i=0, j = (U8)(n-1); i < n; j = i++) /* for each edge in polygon, starting from the edge(vn-vo) */
      {  
		  /* check if one vertice ies in upper plane and another lies in lower plane of
             the positive ray 
           */
		  if( (p_zVertices[i].geo_fence_longitude > p_zPoint->geo_fence_longitude) != (p_zVertices[j].geo_fence_longitude >= p_zPoint->geo_fence_longitude ))
		  {
			  
			 DeltaX = (p_zVertices[i].geo_fence_latitude - p_zVertices[j].geo_fence_latitude);
             DeltaY = (p_zVertices[i].geo_fence_longitude- p_zVertices[j].geo_fence_longitude);
           
			/* checking if the edge is in right side of the point */ 
		     if( (p_zPoint->geo_fence_latitude) < ( (p_zPoint->geo_fence_longitude - p_zVertices[j].geo_fence_longitude)*DeltaX / DeltaY  +  p_zVertices[j].geo_fence_latitude ) )
			 {
		        in = (U8)!in;
			 }
		  } //end of if
	  } //end of for

	  if(in)
	  {
		STATUSMSG("Geofence: Fix inside the Polygonal Fence\n");
	  }
	  else
	  {
		 STATUSMSG("Geofence: Fix outside the Polygonal Fence\n");
	  }
       return in;
}
/*===========================================================================

FUNCTION
  geofence_circle

DESCRIPTION
  This function checks if the fix(point) is in or out of circular fence

PARAMETERS
  p_zCenter: pointer to the center of crcle
  radius: radius of the circle
  p_zPoint: point to be tested
===========================================================================*/
U8 geofence_circle(T_GPSC_geo_fence_vertices *p_zCenter, U16 radius, 
                   T_GPSC_geo_fence_vertices *p_zPoint)
{
    U32 dist=0;
    U8 in;
	DBL CenterX, CenterY, FixX, FixY, DeltaX, DeltaY;
	DBL a, c;
	
	/* We have divided by 1000000, as we have multiplied by 100000 earlier for decimal rounding*/
	CenterX = (DBL)(p_zCenter->geo_fence_latitude )/pow(10,7);
	CenterY = (DBL)(p_zCenter->geo_fence_longitude)/pow(10,7);
	FixX = (DBL)(p_zPoint->geo_fence_latitude)/pow(10,7);
	FixY = (DBL)(p_zPoint->geo_fence_longitude)/pow(10,7);
	
	DeltaX = FixX - CenterX;
	DeltaY = FixY - CenterY;

	/* Using Haversine formula for calculating the distanve between two points on earth*/
	a = pow(sin(DeltaX/2),2) + cos(CenterX)*cos(FixX)*pow(sin(DeltaY/2),2);
	c = 2*atan2(sqrt(a),sqrt(1-a));
	dist = (U32)(EARTH_RADIUS*1000*c); /* in meters*/
    if(dist <= radius)
    {
      in = TRUE;
      STATUSMSG("Geofence: Fix inside the Circular Fence\n");
    }
    else
    {
      in = FALSE;
	  STATUSMSG("Geofence: Fix outside the Circular Fence\n");
    }
    return in;
}


/*===========================================================================

FUNCTION
  gpsc_geofence

DESCRIPTION
  This function selects the type of geo-fence: no-fence, circle and polygon

PARAMETERS
  p_zGeoFenceConfig: pointer to geo-fence config
  lat: latitude
  lan: longitude
  alt: altitude
  velocity: pointer to velocity
===========================================================================*/

U8 gpsc_geofence(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig,T_GPSC_geo_fence_vertices *p_zPoint)
{

  U8 in = 2;
  switch(p_zGeoFenceConfig->vertices_number)
  {
    case 0: /* invalid geo-fence */
      /*Check if the Configuration requested for first position to be the center of the circle*/
      if(!(p_zGeoFenceConfig->geo_fence_control  & GEOFENCE_SPECIFIED_CENTER_MASK))
      {
        /*Set the current point as the center of the circlular geofence*/
             p_zGeoFenceConfig->vertices_number = 1;
             p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_latitude = p_zPoint->geo_fence_latitude;
             p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_longitude = p_zPoint->geo_fence_longitude;  
             in = TRUE;             
             break;             
      }
      //no break for else condition
    case 2:
	  p_zGeoFenceConfig->geo_fence_control = 0;
      STATUSMSG("Geofence not defined for vertices number = %d: fence disabled\n",p_zGeoFenceConfig->vertices_number);
      in = TRUE;
	  break;
    case 1: /* circular geo-fence */
      in = geofence_circle(p_zGeoFenceConfig->geo_fence_vertices,p_zGeoFenceConfig->radius,p_zPoint);
      break;
    default: /* n >= 3 is considered as valid polygonal geo-fence */
      if(geofence_boundary_check(p_zGeoFenceConfig->vertices_number,p_zGeoFenceConfig->geo_fence_vertices,p_zPoint))
      {
        in = TRUE;
      }
      else
      {
        in = geofence_polygon(p_zGeoFenceConfig->vertices_number,p_zGeoFenceConfig->geo_fence_vertices,p_zPoint);
      }
      break; 
  }
  
  return in;
}




/*===========================================================================

FUNCTION
  gpsc_check_geofence_features

DESCRIPTION
  This function checks the user setting on geo-fence and enable or disable
  as per user's configuration.

PARAMETERS
  p_zGeoFenceConfig: pointer to geo-fence config
  in: inside or outside the fence
  velocity: pointer to velocity
 
===========================================================================*/

U8 gpsc_check_geofence_features(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig,
                                S32 lat, S32 lan, S16 alt, S16 *p_Velocity)
{
  U8 reportFix = FALSE;
  DBL Vel_East;
  DBL Vel_North;
  DBL avg_speed;

  /* to avoid warning */
  //UNREFERENCED_PARAMETER(alt);

  /* altitude limit: TBD */

  /*1. Check Speed Fence*/  
  if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_SPEED_MASK)
  {
    Vel_East = p_Velocity[0] * VEL_FACTOR;
	Vel_North = p_Velocity[1] * VEL_FACTOR;
	
	avg_speed = sqrt(Vel_East*Vel_East + Vel_North*Vel_North);
    if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_SPEED_LIMIT_MASK)
    {      
      if(avg_speed > p_zGeoFenceConfig->speed_limit )
      {
        // speed reported is outside the limit
        STATUSMSG("Geofence: Reporting Speed outside the limit");
        reportFix = TRUE;
      }
    }
    else if(avg_speed <= p_zGeoFenceConfig->speed_limit)
    {
        // speed reported is inside the limit
        STATUSMSG("Geofence: Reporting Speed inside the limit");
        reportFix = TRUE;
    }

  }

  /*2. Check Location Fence*/  
  if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_LOCATION_MASK)
  {
    U8 in;
    T_GPSC_geo_fence_vertices p_zPoint;

    mapToXY(&p_zPoint.geo_fence_latitude,&p_zPoint.geo_fence_longitude, lat, lan);
    in = gpsc_geofence(p_zGeoFenceConfig,&p_zPoint);
    if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_LOCATION_LIMIT_MASK)
    {
    /* 
     * report fix when location mask is enabled and location(lat/lon) OR altitude is outside fence 
     */

      if(!in)
      {

        STATUSMSG("Geofence: Reporting Point lat/lon outside the fence");
		reportFix = TRUE;
		      	
      }
		
		if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_ALTITUDE_MASK)
		{
            alt = (S16) (alt/2);
	      
		
			if(alt > p_zGeoFenceConfig->altitude_limit)
			{
				STATUSMSG("Geofence: Reporting Altitude outside the limit");
				reportFix = TRUE;
			}
      }
    }
    else if(in)
    {
     /* 
      * report fix when location mask is enabled and location(lat/lon) AND altitude are inside fence
      */
      STATUSMSG("Geofence: Reporting Point lat/lon inside the fence");
	  
      if(p_zGeoFenceConfig->geo_fence_control & GEOFENCE_ALTITUDE_MASK)
	  {
            alt = (S16) (alt/2);
	      
		
			if(alt <= p_zGeoFenceConfig->altitude_limit)
			{
				STATUSMSG("Geofence: Reporting Altitude inside the limit");
				reportFix = TRUE;
			}
	  }
      
    }
  }
    
  /*
   * report fix when both location and speed mask is disabled
   */
  if ((p_zGeoFenceConfig->geo_fence_control & GEOFENCE_MOTION_MASK) == 0)
  {
    reportFix = TRUE;
  }        
  return reportFix;
}

/*===========================================================================

FUNCTION
  gpsc_geofence_check_line

DESCRIPTION
  checks if all vertices fall on a line


PARAMETERS
  
===========================================================================*/
U8 gpsc_geofence_check_line(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig)
{
#define POINT_ON_LINE 1
#define POINT_OFF_LINE 0

  U8 vert;
  S32 delta_x, delta_y;
  
  delta_y =  p_zGeoFenceConfig->geo_fence_vertices[1].geo_fence_longitude - p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_longitude;
  delta_x = p_zGeoFenceConfig->geo_fence_vertices[1].geo_fence_latitude - p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_latitude;
  for(vert=2;vert<p_zGeoFenceConfig->vertices_number; vert++)
     {
       
	   if(
		   (p_zGeoFenceConfig->geo_fence_vertices[vert].geo_fence_longitude*delta_x) != ( delta_y*p_zGeoFenceConfig->geo_fence_vertices[vert].geo_fence_latitude
		   + (p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_longitude)*(p_zGeoFenceConfig->geo_fence_vertices[1].geo_fence_latitude)
		   - (p_zGeoFenceConfig->geo_fence_vertices[1].geo_fence_longitude)*(p_zGeoFenceConfig->geo_fence_vertices[0].geo_fence_latitude))
		 )
           return POINT_OFF_LINE;

     }
   return POINT_ON_LINE;


   
}



/*===========================================================================

FUNCTION
  gpsc_geofence_sort_valid_vertices

DESCRIPTION
  Sort the database so that no adjacent vertices are same

PARAMETERS
  
===========================================================================*/
U8 gpsc_geofence_sort_valid_vertices(T_GPSC_geo_fence_config_params *p_zGeoFenceConfig)
{
/*Same point may be repeated in a polygon, but 
   Adjacent Vertices cannot be same. It should be counted as one*/
  U8 num_vertices = p_zGeoFenceConfig->vertices_number;
  U8 i=0, index=0;

  /*KlocWork Critical Issue:51 Resolved by adding boundary check*/
	if((num_vertices > 1) && (num_vertices <= 10))
	{
	  for(i=0, index=1;i<num_vertices-1;i++)
	  {
		   if((p_zGeoFenceConfig->geo_fence_vertices[i].geo_fence_latitude == p_zGeoFenceConfig->geo_fence_vertices[i+1].geo_fence_latitude) &&
		  (p_zGeoFenceConfig->geo_fence_vertices[i].geo_fence_longitude== p_zGeoFenceConfig->geo_fence_vertices[i+1].geo_fence_longitude))
		   {
				/*If next vertice is same ignore the vertice.*/
				   continue;
		   }

			/*Shift the lower vertices if they are differ*/
		 if(index != i+1)
		 {
			   /*If next vertice is different and its not already occupying the same position
			  copy the good vertice to the index.*/
			   p_zGeoFenceConfig->geo_fence_vertices[index].geo_fence_latitude = p_zGeoFenceConfig->geo_fence_vertices[i+1].geo_fence_latitude;
		  p_zGeoFenceConfig->geo_fence_vertices[index].geo_fence_longitude= p_zGeoFenceConfig->geo_fence_vertices[i+1].geo_fence_longitude;

		 }

			 index++;
       
	  }
	  /*Overwrite the new total number of vertices.*/
	   p_zGeoFenceConfig->vertices_number = index;
	}
   return TRUE;

}
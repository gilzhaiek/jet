/*
 * Copyright (C) 2015 Recon Instruments
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include<stdio.h>   
#include<time.h> 

/* The following defines are from NAVD */
#define C_PI 3.1415926535898
#define C_LSB_LAT              (C_PI/65536.0/65536.0) /* pi / (2^32) */
#define C_LSB_LON              (C_PI/32768.0/65536.0) /* pi / (2^31) */
#define C_RAD_TO_DEG           (180.0 / C_PI )   /* factor converting from radius to degree */
#define SECONDS_JAN1_1970_TO_JAN6_1980   315964800

typedef struct
{
	signed long  l_PosLat;
	signed long  l_PosLong;
	signed short  x_PosHt;
	float  f_PosUnc;
	double  d_ToaSec;  /* RTC Timestamp, in seconds */
} gpsc_nv_pos_type;  /* Non-volatile memory position struture */


int main(int argc, char** argv) {
	FILE * aidingFile = fopen("GPSCPositionFile", "wb");

	gpsc_nv_pos_type nvs_position = {0};

	float user_lat = 0.0;
	float user_lon = 0.0;
	float user_ht = 0.0;
	float user_uncertainty = 0.0;

	double currentTimeSeconds = (double) (time(0) - SECONDS_JAN1_1970_TO_JAN6_1980);

	printf("Sizeof(signed long): %d\n", sizeof(signed long));
	printf("Sizeof(signed short): %d\n", sizeof(signed short));
	printf("Sizeof(float): %d\n", sizeof(float));
	printf("Sizeof(double): %d\n", sizeof(double));
	
	printf("Current GPS Time seconds is: %d\n", (unsigned long) currentTimeSeconds);

	printf("Please enter the latitude (decimal deg):\n");
	scanf("%f", &user_lat);
	printf("Please enter the longitude (decimal deg):\n");
	scanf("%f", &user_lon);
	printf("Please enter a height (m):\n");
	scanf("%f", &user_ht);
	printf("Please enter the uncertainty (m):\n");
	scanf("%f", &user_uncertainty);

	nvs_position.l_PosLat = (signed long) ((double) user_lat / (double) C_LSB_LAT / (double) C_RAD_TO_DEG);
	nvs_position.l_PosLong = (signed long) ((double) user_lon / (double) C_LSB_LON / (double) C_RAD_TO_DEG);
	nvs_position.x_PosHt = (short) user_ht;
	nvs_position.f_PosUnc = (float) user_uncertainty;
	nvs_position.d_ToaSec = (double) currentTimeSeconds;


	printf("\nCollected data:\n");
	printf("Lat: %d\n", nvs_position.l_PosLat);
	printf("Lon: %d\n", nvs_position.l_PosLong);
	printf("Ht: %d\n", nvs_position.x_PosHt);
	printf("Unc: %f\n", nvs_position.f_PosUnc);
	printf("Time: %d\n", (unsigned int) nvs_position.d_ToaSec);

	printf("\nWriting data to aiding file...\n");

	if (aidingFile != NULL)
	{
		fwrite(&nvs_position, sizeof(gpsc_nv_pos_type), 1, aidingFile);
		fclose(aidingFile);
		printf("Done writing aiding file");
	}
	else
	{
		printf("Error opening file to write");
	}

	return 0;
}

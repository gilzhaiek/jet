#include "gpsc_types.h"
#define QNOISE_FACT1 (1.0/16.0)
#define QNOISE_FACT2 (1.0)
#define PI (3.14159265359)


typedef struct {

	DBL f_StateNoise;   //Kalman state variable - process noise
	DBL f_StateNew;     //Kalman state variable
	DBL f_StatePrev;    //previous filtere output
	U8  u_FilterStart;  //Flag to indicate if teh filter is already active
	U32 q_FCount;		//FCount corresponding to the last filtered heading
} HdgFiltStateVar;

typedef struct{
	FLT f_VelEast;
	FLT f_VelNorth;
	FLT f_Heading;
	U32 q_FCount;
} VelocityVar;



void gpsc_HdgFiltInit(void);
void gpsc_HdgFilter(VelocityVar* p_CurrentVelocityVar, VelocityVar* p_FilteredVelocityVar);


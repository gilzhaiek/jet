#ifndef GPSC_SVDIR_H
#define GPSC_SVDIR_H


#include "gpsc_types.h"
#include "gpsc_data.h"
#include "gpsc_consts.h"
#include "gpsc_cd.h"
#include "gpsc_msg.h"
#include "mcpf_services.h"

/************************************************************************/
/*					Internal Functions Definition                       */
/************************************************************************/
void gpsc_cd_DecodeEphUnits(pe_RawEphemeris	*p_In, cd_Ephemeris	*p_Out);
void gpsc_cd_kepler(McpDBL *mk, McpDBL *q_E, McpDBL *ek);
void gpsc_cd_ephxyz(McpU32 GpsTime, pe_RawEphemeris *RawEphemeris, gpsc_SV_xyz_type *svs);
void gpsc_cd_Ecef2Enu(McpDBL d_Xform[][3], const McpDBL *d_Ecef, McpDBL *d_Enu);
void gpsc_cd_Lla2Ecef(McpDBL d_lla[], McpDBL d_ecef[]);
void gpsc_cd_Llh2EnuMatrixGet(McpDBL d_Xform[][3], McpDBL d_Llh[3]);
void gpsc_cd_CalcSvDirection(gpsc_inject_pos_est_type *p_zInjectPosEst, gpsc_SV_xyz_type *z_SvXyz, 
							 McpDBL *elev, McpDBL *azi);


#endif

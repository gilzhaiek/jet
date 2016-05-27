/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Satelliteinfo.h"

static asn_TYPE_member_t asn_MBR_SatelliteInfo_1[] = {
	{ ATF_POINTER, 0, 0,		
		&asn_DEF_SatelliteInfoElement,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		""
		},
};

static asn_SET_OF_specifics_t asn_SPC_SatelliteInfo_specs_1 = {
	sizeof(struct SatelliteInfo),
	offsetof(struct SatelliteInfo, _asn_ctx),
	0,
};
static asn_per_constraints_t asn_PER_SatelliteInfo_constr_1 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 5,  5,  1,  31 }	/* (SIZE(1..31)) */
};
asn_TYPE_descriptor_t asn_DEF_SatelliteInfo = {
	"SatelliteInfo",
	SEQUENCE_OF_free,
	SEQUENCE_OF_print,
	SEQUENCE_OF_constraint,
	SEQUENCE_OF_decode_uper,
	SEQUENCE_OF_encode_uper,
	&asn_PER_SatelliteInfo_constr_1,
	asn_MBR_SatelliteInfo_1,
	1,	/* Single element */
	&asn_SPC_SatelliteInfo_specs_1	/* Additional specs */
};


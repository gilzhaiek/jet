/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/SUPLPOSINIT.h"

static asn_TYPE_member_t asn_MBR_SUPLPOSINIT_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLPOSINIT, sETCapabilities),
		
		&asn_DEF_SETCapabilities,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sETCapabilities"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLPOSINIT, requestedAssistData),
		
		&asn_DEF_RequestedAssistData,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"requestedAssistData"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLPOSINIT, locationId),
		
		&asn_DEF_LocationId,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"locationId"
		},
	{ ATF_POINTER, 3, offsetof(struct SUPLPOSINIT, position),
		
		&asn_DEF_Position,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"position"
		},
	{ ATF_POINTER, 2, offsetof(struct SUPLPOSINIT, sUPLPOS),
		
		&asn_DEF_SUPLPOS,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sUPLPOS"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLPOSINIT, ver),
		
		&asn_DEF_Ver,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"ver"
		},
};
static int asn_MAP_SUPLPOSINIT_oms_1[] = { 1, 3, 4, 5 };


static asn_SEQUENCE_specifics_t asn_SPC_SUPLPOSINIT_specs_1 = {
	sizeof(struct SUPLPOSINIT),
	offsetof(struct SUPLPOSINIT, _asn_ctx),
	
	asn_MAP_SUPLPOSINIT_oms_1,	/* Optional members */
	4, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLPOSINIT = {
	"SUPLPOSINIT",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLPOSINIT_1,
	6,	/* Elements count */
	&asn_SPC_SUPLPOSINIT_specs_1	/* Additional specs */
};


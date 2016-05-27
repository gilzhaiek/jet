/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/SUPLPOS.h"

static asn_TYPE_member_t asn_MBR_SUPLPOS_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLPOS, posPayLoad),
		
		&asn_DEF_PosPayLoad,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"posPayLoad"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLPOS, velocity),
		
		&asn_DEF_Velocity,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"velocity"
		},
};
static int asn_MAP_SUPLPOS_oms_1[] = { 1 };


static asn_SEQUENCE_specifics_t asn_SPC_SUPLPOS_specs_1 = {
	sizeof(struct SUPLPOS),
	offsetof(struct SUPLPOS, _asn_ctx),
	
	asn_MAP_SUPLPOS_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLPOS = {
	"SUPLPOS",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLPOS_1,
	2,	/* Elements count */
	&asn_SPC_SUPLPOS_specs_1	/* Additional specs */
};


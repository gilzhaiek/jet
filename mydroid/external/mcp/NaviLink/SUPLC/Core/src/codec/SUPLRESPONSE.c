/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/SUPLRESPONSE.h"

static asn_TYPE_member_t asn_MBR_SUPLRESPONSE_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLRESPONSE, posMethod),
		
		&asn_DEF_PosMethod,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"posMethod"
		},
	{ ATF_POINTER, 3, offsetof(struct SUPLRESPONSE, sLPAddress),
		
		&asn_DEF_SLPAddress,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sLPAddress"
		},
	{ ATF_POINTER, 2, offsetof(struct SUPLRESPONSE, sETAuthKey),
		
		&asn_DEF_SETAuthKey,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sETAuthKey"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLRESPONSE, keyIdentity4),
		
		&asn_DEF_KeyIdentity4,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"keyIdentity4"
		},
};
static int asn_MAP_SUPLRESPONSE_oms_1[] = { 1, 2, 3 };


static asn_SEQUENCE_specifics_t asn_SPC_SUPLRESPONSE_specs_1 = {
	sizeof(struct SUPLRESPONSE),
	offsetof(struct SUPLRESPONSE, _asn_ctx),
	
	asn_MAP_SUPLRESPONSE_oms_1,	/* Optional members */
	3, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLRESPONSE = {
	"SUPLRESPONSE",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLRESPONSE_1,
	4,	/* Elements count */
	&asn_SPC_SUPLRESPONSE_specs_1	/* Additional specs */
};


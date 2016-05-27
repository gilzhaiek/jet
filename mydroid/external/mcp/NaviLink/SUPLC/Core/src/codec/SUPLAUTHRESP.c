/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/SUPLAUTHRESP.h"

static asn_TYPE_member_t asn_MBR_SUPLAUTHRESP_1[] = {
	{ ATF_POINTER, 3, offsetof(struct SUPLAUTHRESP, sPCAuthKey),
		
		&asn_DEF_SPCAuthKey,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sPCAuthKey"
		},
	{ ATF_POINTER, 2, offsetof(struct SUPLAUTHRESP, keyIdentity3),
		
		&asn_DEF_KeyIdentity3,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"keyIdentity3"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLAUTHRESP, statusCode),
		
		&asn_DEF_StatusCode,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"statusCode"
		},
};
static int asn_MAP_SUPLAUTHRESP_oms_1[] = { 0, 1, 2 };

static asn_SEQUENCE_specifics_t asn_SPC_SUPLAUTHRESP_specs_1 = {
	sizeof(struct SUPLAUTHRESP),
	offsetof(struct SUPLAUTHRESP, _asn_ctx),
	
	asn_MAP_SUPLAUTHRESP_oms_1,	/* Optional members */
	3, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLAUTHRESP = {
	"SUPLAUTHRESP",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLAUTHRESP_1,
	3,	/* Elements count */
	&asn_SPC_SUPLAUTHRESP_specs_1	/* Additional specs */
};


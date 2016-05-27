/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Suplinit.h"

static asn_TYPE_member_t asn_MBR_SUPLINIT_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLINIT, posMethod),

		&asn_DEF_PosMethod,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"posMethod"
		},
	{ ATF_POINTER, 3, offsetof(struct SUPLINIT, notification),
		
		&asn_DEF_Notification,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"notification"
		},
	{ ATF_POINTER, 2, offsetof(struct SUPLINIT, sLPAddress),
		
		&asn_DEF_SLPAddress,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sLPAddress"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLINIT, qoP),
		
		&asn_DEF_QoP,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"qoP"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLINIT, sLPMode),
		
		&asn_DEF_SLPMode,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sLPMode"
		},
	{ ATF_POINTER, 2, offsetof(struct SUPLINIT, mAC),
		
		&asn_DEF_MAC,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"mAC"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLINIT, keyIdentity),
		
		&asn_DEF_KeyIdentity,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"keyIdentity"
		},
};
static int asn_MAP_SUPLINIT_oms_1[] = { 1, 2, 3, 5, 6 };


static asn_SEQUENCE_specifics_t asn_SPC_SUPLINIT_specs_1 = {
	sizeof(struct SUPLINIT),
	offsetof(struct SUPLINIT, _asn_ctx),
	
	asn_MAP_SUPLINIT_oms_1,	/* Optional members */
	5, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLINIT = {
	"SUPLINIT",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLINIT_1,
	7,	/* Elements count */
	&asn_SPC_SUPLINIT_specs_1	/* Additional specs */
};


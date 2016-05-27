/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Sessionid.h"

static asn_TYPE_member_t asn_MBR_SessionID_1[] = {
	{ ATF_POINTER, 2, offsetof(struct SessionID, setSessionID),
		
		&asn_DEF_SetSessionID,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"setSessionID"
		},
	{ ATF_POINTER, 1, offsetof(struct SessionID, slpSessionID),
		
		&asn_DEF_SlpSessionID,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"slpSessionID"
		},
};
static int asn_MAP_SessionID_oms_1[] = { 0, 1 };


static asn_SEQUENCE_specifics_t asn_SPC_SessionID_specs_1 = {
	sizeof(struct SessionID),
	offsetof(struct SessionID, _asn_ctx),

	asn_MAP_SessionID_oms_1,	/* Optional members */
	2, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SessionID = {
	"SessionID",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SessionID_1,
	2,	/* Elements count */
	&asn_SPC_SessionID_specs_1	/* Additional specs */
};


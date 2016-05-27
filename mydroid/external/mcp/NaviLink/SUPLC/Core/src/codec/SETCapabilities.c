/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Setcapabilities.h"

static asn_TYPE_member_t asn_MBR_SETCapabilities_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SETCapabilities, posTechnology),
		
		&asn_DEF_PosTechnology,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"posTechnology"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETCapabilities, prefMethod),
		
		&asn_DEF_PrefMethod,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"prefMethod"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETCapabilities, posProtocol),
		
		&asn_DEF_PosProtocol,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"posProtocol"
		},
};


static asn_SEQUENCE_specifics_t asn_SPC_SETCapabilities_specs_1 = {
	sizeof(struct SETCapabilities),
	offsetof(struct SETCapabilities, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	2,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SETCapabilities = {
	"SETCapabilities",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SETCapabilities_1,
	3,	/* Elements count */
	&asn_SPC_SETCapabilities_specs_1	/* Additional specs */
};


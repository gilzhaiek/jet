/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/SUPLAUTHREQ.h"

static asn_TYPE_member_t asn_MBR_SUPLAUTHREQ_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLAUTHREQ, sETNonce),
		
		&asn_DEF_SETNonce,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sETNonce"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLAUTHREQ, keyIdentity2),
		
		&asn_DEF_KeyIdentity2,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"keyIdentity2"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_SUPLAUTHREQ_specs_1 = {
	sizeof(struct SUPLAUTHREQ),
	offsetof(struct SUPLAUTHREQ, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLAUTHREQ = {
	"SUPLAUTHREQ",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLAUTHREQ_1,
	2,	/* Elements count */
	&asn_SPC_SUPLAUTHREQ_specs_1	/* Additional specs */
};


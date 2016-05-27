/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Posprotocol.h"

static asn_TYPE_member_t asn_MBR_PosProtocol_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct PosProtocol, tia801),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"tia801"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosProtocol, rrlp),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"rrlp"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosProtocol, rrc),
		
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"rrc"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_PosProtocol_specs_1 = {
	sizeof(struct PosProtocol),
	offsetof(struct PosProtocol, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_PosProtocol = {
	"PosProtocol",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_PosProtocol_1,
	3,	/* Elements count */
	&asn_SPC_PosProtocol_specs_1	/* Additional specs */
};


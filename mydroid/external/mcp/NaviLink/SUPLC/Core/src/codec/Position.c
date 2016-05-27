/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Position.h"

static asn_TYPE_member_t asn_MBR_Position_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct Position, timestamp),

		&asn_DEF_UTCTime,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"timestamp"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct Position, positionEstimate),
		
		&asn_DEF_PositionEstimate,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"positionEstimate"
		},
	{ ATF_POINTER, 1, offsetof(struct Position, velocity),
		
		&asn_DEF_Velocity,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"velocity"
		},
};
static int asn_MAP_Position_oms_1[] = { 2 };


static asn_SEQUENCE_specifics_t asn_SPC_Position_specs_1 = {
	sizeof(struct Position),
	offsetof(struct Position, _asn_ctx),
	
	asn_MAP_Position_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_Position = {
	"Position",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_Position_1,
	3,	/* Elements count */
	&asn_SPC_Position_specs_1	/* Additional specs */
};


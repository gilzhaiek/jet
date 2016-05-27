/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Locationid.h"

static asn_TYPE_member_t asn_MBR_LocationId_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct LocationId, cellInfo),
		&asn_DEF_CellInfo,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cellInfo"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct LocationId, status),
		&asn_DEF_Status,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"status"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_LocationId_specs_1 = {
	sizeof(struct LocationId),
	offsetof(struct LocationId, _asn_ctx),
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_LocationId = {
	"LocationId",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_LocationId_1,
	2,	/* Elements count */
	&asn_SPC_LocationId_specs_1	/* Additional specs */
};


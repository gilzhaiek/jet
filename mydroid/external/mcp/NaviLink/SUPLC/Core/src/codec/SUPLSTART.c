/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Suplstart.h"

static asn_TYPE_member_t asn_MBR_SUPLSTART_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLSTART, sETCapabilities),
		
		&asn_DEF_SETCapabilities,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"sETCapabilities"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SUPLSTART, locationId),
		
		&asn_DEF_LocationId,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"locationId"
		},
	{ ATF_POINTER, 1, offsetof(struct SUPLSTART, qoP),
		
		&asn_DEF_QoP,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"qoP"
		},
};
static int asn_MAP_SUPLSTART_oms_1[] = { 2 };


static asn_SEQUENCE_specifics_t asn_SPC_SUPLSTART_specs_1 = {
	sizeof(struct SUPLSTART),
	offsetof(struct SUPLSTART, _asn_ctx),
	
	asn_MAP_SUPLSTART_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SUPLSTART = {
	"SUPLSTART",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SUPLSTART_1,
	3,	/* Elements count */
	&asn_SPC_SUPLSTART_specs_1	/* Additional specs */
};


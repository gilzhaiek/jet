/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Nmr.h"

static asn_TYPE_member_t asn_MBR_NMR_1[] = {
	{ ATF_POINTER, 0, 0,
		
		&asn_DEF_NMRelement,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		""
		},
};

static asn_SET_OF_specifics_t asn_SPC_NMR_specs_1 = {
	sizeof(struct NMR),
	offsetof(struct NMR, _asn_ctx),
	0,
};
static asn_per_constraints_t asn_PER_NMR_constr_1 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 4,  4,  1,  15 }	/* (SIZE(1..15)) */
};
asn_TYPE_descriptor_t asn_DEF_NMR = {
	"NMR",
	SEQUENCE_OF_free,
	SEQUENCE_OF_print,
	SEQUENCE_OF_constraint,
	SEQUENCE_OF_decode_uper,
	SEQUENCE_OF_encode_uper,
	&asn_PER_NMR_constr_1,
	asn_MBR_NMR_1,
	1,	/* Single element */
	&asn_SPC_NMR_specs_1	/* Additional specs */
};


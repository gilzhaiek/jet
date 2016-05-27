/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Slpaddress.h"

static asn_TYPE_member_t asn_MBR_SLPAddress_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SLPAddress, choice.iPAddress),
		
		&asn_DEF_IPAddress,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"iPAddress"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SLPAddress, choice.fQDN),
		
		&asn_DEF_FQDN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"fQDN"
		},
};

static asn_CHOICE_specifics_t asn_SPC_SLPAddress_specs_1 = {
	sizeof(struct SLPAddress),
	offsetof(struct SLPAddress, _asn_ctx),
	offsetof(struct SLPAddress, present),
	sizeof(((struct SLPAddress *)0)->present),
	
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_SLPAddress_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  1,  1,  0,  1 }	/* (0..1,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_SLPAddress = {
	"SLPAddress",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_SLPAddress_constr_1,
	asn_MBR_SLPAddress_1,
	2,	/* Elements count */
	&asn_SPC_SLPAddress_specs_1	/* Additional specs */
};


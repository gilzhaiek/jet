/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Velocity.h"

static asn_TYPE_member_t asn_MBR_Velocity_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct Velocity, choice.horvel),
		
		&asn_DEF_Horvel,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"horvel"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct Velocity, choice.horandvervel),
		
		&asn_DEF_Horandvervel,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"horandvervel"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct Velocity, choice.horveluncert),
		
		&asn_DEF_Horveluncert,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"horveluncert"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct Velocity, choice.horandveruncert),
		
		&asn_DEF_Horandveruncert,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"horandveruncert"
		},
};

static asn_CHOICE_specifics_t asn_SPC_Velocity_specs_1 = {
	sizeof(struct Velocity),
	offsetof(struct Velocity, _asn_ctx),
	offsetof(struct Velocity, present),
	sizeof(((struct Velocity *)0)->present),
	
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_Velocity_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  2,  2,  0,  3 }	/* (0..3,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_Velocity = {
	"Velocity",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_Velocity_constr_1,
	asn_MBR_Velocity_1,
	4,	/* Elements count */
	&asn_SPC_Velocity_specs_1	/* Additional specs */
};


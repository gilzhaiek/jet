/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Frequencyinfo.h"

static asn_TYPE_member_t asn_MBR_modeSpecificInfo_2[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct modeSpecificInfo_t, choice.fdd),
		&asn_DEF_FrequencyInfoFDD,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"fdd"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct modeSpecificInfo_t, choice.tdd),
		&asn_DEF_FrequencyInfoTDD,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"tdd"
		},
};

static asn_CHOICE_specifics_t asn_SPC_modeSpecificInfo_specs_2 = {
	sizeof(struct modeSpecificInfo_t),
	offsetof(struct modeSpecificInfo_t, _asn_ctx),
	offsetof(struct modeSpecificInfo_t, present),
	sizeof(((struct modeSpecificInfo_t *)0)->present),
	0,
	2	/* Extensions start */
};
static asn_per_constraints_t asn_PER_modeSpecificInfo_constr_2 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  1,  1,  0,  1 }	/* (0..1,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_modeSpecificInfo_2 = {
	"modeSpecificInfo",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_modeSpecificInfo_constr_2,
	asn_MBR_modeSpecificInfo_2,
	2,	/* Elements count */
	&asn_SPC_modeSpecificInfo_specs_2	/* Additional specs */
};

static asn_per_constraints_t asn_PER_memb_modeSpecificInfo_constr_2 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  1,  1,  0,  1 }	/* (0..1,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_FrequencyInfo_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct FrequencyInfo, modeSpecificInfo),
		
		&asn_DEF_modeSpecificInfo_2,
		0,	/* Defer constraints checking to the member type */
		&asn_PER_memb_modeSpecificInfo_constr_2,
		0,
		"modeSpecificInfo"
		},
};


static asn_SEQUENCE_specifics_t asn_SPC_FrequencyInfo_specs_1 = {
	sizeof(struct FrequencyInfo),
	offsetof(struct FrequencyInfo, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_FrequencyInfo = {
	"FrequencyInfo",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_FrequencyInfo_1,
	1,	/* Elements count */
	&asn_SPC_FrequencyInfo_specs_1	/* Additional specs */
};


/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Cellinfo.h"

static asn_TYPE_member_t asn_MBR_CellInfo_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct CellInfo, choice.gsmCell),
		&asn_DEF_GsmCellInformation,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"gsmCell"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CellInfo, choice.wcdmaCell),
		&asn_DEF_WcdmaCellInformation,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"wcdmaCell"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CellInfo, choice.cdmaCell),
		&asn_DEF_CdmaCellInformation,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cdmaCell"
		},
};
static asn_CHOICE_specifics_t asn_SPC_CellInfo_specs_1 = {
	sizeof(struct CellInfo),
	offsetof(struct CellInfo, _asn_ctx),
	offsetof(struct CellInfo, present),
	sizeof(((struct CellInfo *)0)->present),
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_CellInfo_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  2,  2,  0,  2 }	/* (0..2,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_CellInfo = {
	"CellInfo",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_CellInfo_constr_1,
	asn_MBR_CellInfo_1,
	3,	/* Elements count */
	&asn_SPC_CellInfo_specs_1	/* Additional specs */
};


/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Pospayload.h"

static int
memb_tia801payload_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 8192)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static int
memb_rrcPayload_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 8192)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static int
memb_rrlpPayload_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 8192)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_tia801payload_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 13,  13,  1,  8192 }	/* (SIZE(1..8192)) */
};
static asn_per_constraints_t asn_PER_memb_rrcPayload_constr_3 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 13,  13,  1,  8192 }	/* (SIZE(1..8192)) */
};
static asn_per_constraints_t asn_PER_memb_rrlpPayload_constr_4 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 13,  13,  1,  8192 }	/* (SIZE(1..8192)) */
};
static asn_TYPE_member_t asn_MBR_PosPayLoad_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct PosPayLoad, choice.tia801payload),
		&asn_DEF_OCTET_STRING,
		memb_tia801payload_constraint_1,
		&asn_PER_memb_tia801payload_constr_2,
		0,
		"tia801payload"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosPayLoad, choice.rrcPayload),
		
		&asn_DEF_OCTET_STRING,
		memb_rrcPayload_constraint_1,
		&asn_PER_memb_rrcPayload_constr_3,
		0,
		"rrcPayload"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct PosPayLoad, choice.rrlpPayload),
		
		&asn_DEF_OCTET_STRING,
		memb_rrlpPayload_constraint_1,
		&asn_PER_memb_rrlpPayload_constr_4,
		0,
		"rrlpPayload"
		},
};

static asn_CHOICE_specifics_t asn_SPC_PosPayLoad_specs_1 = {
	sizeof(struct PosPayLoad),
	offsetof(struct PosPayLoad, _asn_ctx),
	offsetof(struct PosPayLoad, present),
	sizeof(((struct PosPayLoad *)0)->present),
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_PosPayLoad_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  2,  2,  0,  2 }	/* (0..2,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_PosPayLoad = {
	"PosPayLoad",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_PosPayLoad_constr_1,
	asn_MBR_PosPayLoad_1,
	3,	/* Elements count */
	&asn_SPC_PosPayLoad_specs_1	/* Additional specs */
};


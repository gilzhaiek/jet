/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Setauthkey.h"

static int
memb_shortKey_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	if(st->size > 0) {
		/* Size in bits */
		size = 8 * st->size - (st->bits_unused & 0x07);
	} else {
		size = 0;
	}
	
	if((size == 128)) {
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
memb_longKey_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	if(st->size > 0) {
		/* Size in bits */
		size = 8 * st->size - (st->bits_unused & 0x07);
	} else {
		size = 0;
	}
	
	if((size == 256)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_shortKey_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  128,  128 }	/* (SIZE(128..128)) */
};
static asn_per_constraints_t asn_PER_memb_longKey_constr_3 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  256,  256 }	/* (SIZE(256..256)) */
};
static asn_TYPE_member_t asn_MBR_SETAuthKey_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SETAuthKey, choice.shortKey),
		
		&asn_DEF_BIT_STRING,
		memb_shortKey_constraint_1,
		&asn_PER_memb_shortKey_constr_2,
		0,
		"shortKey"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETAuthKey, choice.longKey),
		
		&asn_DEF_BIT_STRING,
		memb_longKey_constraint_1,
		&asn_PER_memb_longKey_constr_3,
		0,
		"longKey"
		},
};

static asn_CHOICE_specifics_t asn_SPC_SETAuthKey_specs_1 = {
	sizeof(struct SETAuthKey),
	offsetof(struct SETAuthKey, _asn_ctx),
	offsetof(struct SETAuthKey, present),
	sizeof(((struct SETAuthKey *)0)->present),
	
	0,
	2	/* Extensions start */
};
static asn_per_constraints_t asn_PER_SETAuthKey_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  1,  1,  0,  1 }	/* (0..1,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_SETAuthKey = {
	"SETAuthKey",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_SETAuthKey_constr_1,
	asn_MBR_SETAuthKey_1,
	2,	/* Elements count */
	&asn_SPC_SETAuthKey_specs_1	/* Additional specs */
};


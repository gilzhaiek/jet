/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Setid.h"

static int check_permitted_alphabet_6(const void *sptr) {
	/* The underlying type is IA5String */
	const IA5String_t *st = (const IA5String_t *)sptr;
	const uint8_t *ch = st->buf;
	const uint8_t *end = ch + st->size;
	
	for(; ch < end; ch++) {
		uint8_t cv = *ch;
		if(!(cv <= 127)) return -1;
	}
	return 0;
}

static int
memb_msisdn_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 8)) {
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
memb_mdn_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 8)) {
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
memb_min_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 34)) {
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
memb_imsi_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 8)) {
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
memb_nai_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const IA5String_t *st = (const IA5String_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 1000)
		 && !check_permitted_alphabet_6(st)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_msisdn_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  8,  8 }	/* (SIZE(8..8)) */
};
static asn_per_constraints_t asn_PER_memb_mdn_constr_3 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  8,  8 }	/* (SIZE(8..8)) */
};
static asn_per_constraints_t asn_PER_memb_min_constr_4 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  34,  34 }	/* (SIZE(34..34)) */
};
static asn_per_constraints_t asn_PER_memb_imsi_constr_5 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  8,  8 }	/* (SIZE(8..8)) */
};
static asn_per_constraints_t asn_PER_memb_nai_constr_6 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 10,  10,  1,  1000 }	/* (SIZE(1..1000)) */
};
static asn_TYPE_member_t asn_MBR_SETId_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.msisdn),
		
		&asn_DEF_OCTET_STRING,
		memb_msisdn_constraint_1,
		&asn_PER_memb_msisdn_constr_2,
		0,
		"msisdn"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.mdn),
		
		&asn_DEF_OCTET_STRING,
		memb_mdn_constraint_1,
		&asn_PER_memb_mdn_constr_3,
		0,
		"mdn"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.min),
		
		&asn_DEF_BIT_STRING,
		memb_min_constraint_1,
		&asn_PER_memb_min_constr_4,
		0,
		"min"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.imsi),
		
		&asn_DEF_OCTET_STRING,
		memb_imsi_constraint_1,
		&asn_PER_memb_imsi_constr_5,
		0,
		"imsi"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.nai),
		
		&asn_DEF_IA5String,
		memb_nai_constraint_1,
		&asn_PER_memb_nai_constr_6,
		0,
		"nai"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SETId, choice.iPAddress),
		
		&asn_DEF_IPAddress,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"iPAddress"
		},
};

static asn_CHOICE_specifics_t asn_SPC_SETId_specs_1 = {
	sizeof(struct SETId),
	offsetof(struct SETId, _asn_ctx),
	offsetof(struct SETId, present),
	sizeof(((struct SETId *)0)->present),
	
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_SETId_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  3,  3,  0,  5 }	/* (0..5,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_SETId = {
	"SETId",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_SETId_constr_1,
	asn_MBR_SETId_1,
	6,	/* Elements count */
	&asn_SPC_SETId_specs_1	/* Additional specs */
};


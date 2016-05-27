/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Ipaddress.h"

static int
memb_ipv4Address_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 4)) {
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
memb_ipv6Address_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size == 16)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_ipv4Address_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  4,  4 }	/* (SIZE(4..4)) */
};
static asn_per_constraints_t asn_PER_memb_ipv6Address_constr_3 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  16,  16 }	/* (SIZE(16..16)) */
};
static asn_TYPE_member_t asn_MBR_IPAddress_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct IPAddress, choice.ipv4Address),
		
		&asn_DEF_OCTET_STRING,
		memb_ipv4Address_constraint_1,
		&asn_PER_memb_ipv4Address_constr_2,
		0,
		"ipv4Address"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct IPAddress, choice.ipv6Address),
		
		&asn_DEF_OCTET_STRING,
		memb_ipv6Address_constraint_1,
		&asn_PER_memb_ipv6Address_constr_3,
		0,
		"ipv6Address"
		},
};

static asn_CHOICE_specifics_t asn_SPC_IPAddress_specs_1 = {
	sizeof(struct IPAddress),
	offsetof(struct IPAddress, _asn_ctx),
	offsetof(struct IPAddress, present),
	sizeof(((struct IPAddress *)0)->present),
	
	0,
	-1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_IPAddress_constr_1 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_IPAddress = {
	"IPAddress",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_IPAddress_constr_1,
	asn_MBR_IPAddress_1,
	2,	/* Elements count */
	&asn_SPC_IPAddress_specs_1	/* Additional specs */
};


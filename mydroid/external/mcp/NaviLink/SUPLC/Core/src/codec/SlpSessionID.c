/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Slpsessionid.h"

static int
memb_sessionID_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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

static asn_per_constraints_t asn_PER_memb_sessionID_constr_2 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 0,  0,  4,  4 }	/* (SIZE(4..4)) */
};
static asn_TYPE_member_t asn_MBR_SlpSessionID_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SlpSessionID, sessionID),
		
		&asn_DEF_OCTET_STRING,
		memb_sessionID_constraint_1,
		&asn_PER_memb_sessionID_constr_2,
		0,
		"sessionID"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SlpSessionID, slpId),
		
		&asn_DEF_SLPAddress,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"slpId"
		},
};


static asn_SEQUENCE_specifics_t asn_SPC_SlpSessionID_specs_1 = {
	sizeof(struct SlpSessionID),
	offsetof(struct SlpSessionID, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SlpSessionID = {
	"SlpSessionID",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SlpSessionID_1,
	2,	/* Elements count */
	&asn_SPC_SlpSessionID_specs_1	/* Additional specs */
};


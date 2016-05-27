/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Setsessionid.h"

static int
memb_sessionId_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 65535)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_sessionId_constr_2 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_SetSessionID_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct SetSessionID, sessionId),
		
		&asn_DEF_NativeInteger,
		memb_sessionId_constraint_1,
		&asn_PER_memb_sessionId_constr_2,
		0,
		"sessionId"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct SetSessionID, setId),
		
		&asn_DEF_SETId,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"setId"
		},
};


static asn_SEQUENCE_specifics_t asn_SPC_SetSessionID_specs_1 = {
	sizeof(struct SetSessionID),
	offsetof(struct SetSessionID, _asn_ctx),
	
	0, 0, 0,	/* Optional elements (not needed) */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_SetSessionID = {
	"SetSessionID",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_SetSessionID_1,
	2,	/* Elements count */
	&asn_SPC_SetSessionID_specs_1	/* Additional specs */
};


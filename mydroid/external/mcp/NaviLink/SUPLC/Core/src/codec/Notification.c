/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Notification.h"

static int
memb_requestorId_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size >= 1 && size <= 50)) {
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
memb_clientName_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
	
	if((size >= 1 && size <= 50)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_requestorId_constr_4 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 8,  6,  1,  50 }	/* (SIZE(1..50)) */
};
static asn_per_constraints_t asn_PER_memb_clientName_constr_6 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 8,  6,  1,  50 }	/* (SIZE(1..50)) */
};
static asn_TYPE_member_t asn_MBR_Notification_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct Notification, notificationType),
		
		&asn_DEF_NotificationType,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"notificationType"
		},
	{ ATF_POINTER, 5, offsetof(struct Notification, encodingType),
		
		&asn_DEF_EncodingType,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"encodingType"
		},
	{ ATF_POINTER, 4, offsetof(struct Notification, requestorId),
		
		&asn_DEF_OCTET_STRING,
		memb_requestorId_constraint_1,
		&asn_PER_memb_requestorId_constr_4,
		0,
		"requestorId"
		},
	{ ATF_POINTER, 3, offsetof(struct Notification, requestorIdType),
		
		&asn_DEF_FormatIndicator,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"requestorIdType"
		},
	{ ATF_POINTER, 2, offsetof(struct Notification, clientName),
		
		&asn_DEF_OCTET_STRING,
		memb_clientName_constraint_1,
		&asn_PER_memb_clientName_constr_6,
		0,
		"clientName"
		},
	{ ATF_POINTER, 1, offsetof(struct Notification, clientNameType),
		
		&asn_DEF_FormatIndicator,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"clientNameType"
		},
};
static int asn_MAP_Notification_oms_1[] = { 1, 2, 3, 4, 5 };


static asn_SEQUENCE_specifics_t asn_SPC_Notification_specs_1 = {
	sizeof(struct Notification),
	offsetof(struct Notification, _asn_ctx),
	
	asn_MAP_Notification_oms_1,	/* Optional members */
	5, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_Notification = {
	"Notification",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_Notification_1,
	6,	/* Elements count */
	&asn_SPC_Notification_specs_1	/* Additional specs */
};


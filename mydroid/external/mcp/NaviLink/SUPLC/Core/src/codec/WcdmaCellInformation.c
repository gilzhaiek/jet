/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "codec/Wcdmacellinformation.h"

static int
memb_refMCC_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 999)) {
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
memb_refMNC_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 999)) {
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
memb_refUC_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 268435455)) {
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
memb_primaryScramblingCode_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 511)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_refMCC_constr_2 = {
	{ APC_CONSTRAINED,	 10,  10,  0,  999 }	/* (0..999) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refMNC_constr_3 = {
	{ APC_CONSTRAINED,	 10,  10,  0,  999 }	/* (0..999) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refUC_constr_4 = {
	{ APC_CONSTRAINED,	 28, -1,  0,  268435455 }	/* (0..268435455) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_primaryScramblingCode_constr_6 = {
	{ APC_CONSTRAINED,	 9,  9,  0,  511 }	/* (0..511) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_WcdmaCellInformation_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct WcdmaCellInformation, refMCC),
		
		&asn_DEF_NativeInteger,
		memb_refMCC_constraint_1,
		&asn_PER_memb_refMCC_constr_2,
		0,
		"refMCC"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct WcdmaCellInformation, refMNC),
		
		&asn_DEF_NativeInteger,
		memb_refMNC_constraint_1,
		&asn_PER_memb_refMNC_constr_3,
		0,
		"refMNC"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct WcdmaCellInformation, refUC),
		
		&asn_DEF_NativeInteger,
		memb_refUC_constraint_1,
		&asn_PER_memb_refUC_constr_4,
		0,
		"refUC"
		},
	{ ATF_POINTER, 3, offsetof(struct WcdmaCellInformation, frequencyInfo),
		
		&asn_DEF_FrequencyInfo,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"frequencyInfo"
		},
	{ ATF_POINTER, 2, offsetof(struct WcdmaCellInformation, primaryScramblingCode),
		
		&asn_DEF_NativeInteger,
		memb_primaryScramblingCode_constraint_1,
		&asn_PER_memb_primaryScramblingCode_constr_6,
		0,
		"primaryScramblingCode"
		},
	{ ATF_POINTER, 1, offsetof(struct WcdmaCellInformation, measuredResultsList),
		
		&asn_DEF_MeasuredResultsList,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"measuredResultsList"
		},
};
static int asn_MAP_WcdmaCellInformation_oms_1[] = { 3, 4, 5 };


static asn_SEQUENCE_specifics_t asn_SPC_WcdmaCellInformation_specs_1 = {
	sizeof(struct WcdmaCellInformation),
	offsetof(struct WcdmaCellInformation, _asn_ctx),
	
	asn_MAP_WcdmaCellInformation_oms_1,	/* Optional members */
	3, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_WcdmaCellInformation = {
	"WcdmaCellInformation",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_WcdmaCellInformation_1,
	6,	/* Elements count */
	&asn_SPC_WcdmaCellInformation_specs_1	/* Additional specs */
};


/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Gsmcellinformation.h"

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
memb_refLAC_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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

static int
memb_refCI_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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

static int
memb_tA_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 255)) {
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
static asn_per_constraints_t asn_PER_memb_refLAC_constr_4 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refCI_constr_5 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_tA_constr_7 = {
	{ APC_CONSTRAINED,	 8,  8,  0,  255 }	/* (0..255) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_GsmCellInformation_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct GsmCellInformation, refMCC),
		&asn_DEF_NativeInteger,
		memb_refMCC_constraint_1,
		&asn_PER_memb_refMCC_constr_2,
		0,
		"refMCC"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct GsmCellInformation, refMNC),
		&asn_DEF_NativeInteger,
		memb_refMNC_constraint_1,
		&asn_PER_memb_refMNC_constr_3,
		0,
		"refMNC"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct GsmCellInformation, refLAC),
		&asn_DEF_NativeInteger,
		memb_refLAC_constraint_1,
		&asn_PER_memb_refLAC_constr_4,
		0,
		"refLAC"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct GsmCellInformation, refCI),
		&asn_DEF_NativeInteger,
		memb_refCI_constraint_1,
		&asn_PER_memb_refCI_constr_5,
		0,
		"refCI"
		},
	{ ATF_POINTER, 2, offsetof(struct GsmCellInformation, nMR),
		&asn_DEF_NMR,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"nMR"
		},
	{ ATF_POINTER, 1, offsetof(struct GsmCellInformation, tA),
		&asn_DEF_NativeInteger,
		memb_tA_constraint_1,
		&asn_PER_memb_tA_constr_7,
		0,
		"tA"
		},
};
static int asn_MAP_GsmCellInformation_oms_1[] = { 4, 5 };

static asn_SEQUENCE_specifics_t asn_SPC_GsmCellInformation_specs_1 = {
	sizeof(struct GsmCellInformation),
	offsetof(struct GsmCellInformation, _asn_ctx),
	asn_MAP_GsmCellInformation_oms_1,	/* Optional members */
	2, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_GsmCellInformation = {
	"GsmCellInformation",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_GsmCellInformation_1,
	6,	/* Elements count */
	&asn_SPC_GsmCellInformation_specs_1	/* Additional specs */
};


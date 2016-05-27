/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Qop.h"

static int
memb_horacc_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 127)) {
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
memb_veracc_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 127)) {
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
memb_maxLocAge_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
memb_delay_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 7)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_horacc_constr_2 = {
	{ APC_CONSTRAINED,	 7,  7,  0,  127 }	/* (0..127) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_veracc_constr_3 = {
	{ APC_CONSTRAINED,	 7,  7,  0,  127 }	/* (0..127) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_maxLocAge_constr_4 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_delay_constr_5 = {
	{ APC_CONSTRAINED,	 3,  3,  0,  7 }	/* (0..7) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_QoP_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct QoP, horacc),
		
		&asn_DEF_NativeInteger,
		memb_horacc_constraint_1,
		&asn_PER_memb_horacc_constr_2,
		0,
		"horacc"
		},
	{ ATF_POINTER, 3, offsetof(struct QoP, veracc),
		
		&asn_DEF_NativeInteger,
		memb_veracc_constraint_1,
		&asn_PER_memb_veracc_constr_3,
		0,
		"veracc"
		},
	{ ATF_POINTER, 2, offsetof(struct QoP, maxLocAge),
		
		&asn_DEF_NativeInteger,
		memb_maxLocAge_constraint_1,
		&asn_PER_memb_maxLocAge_constr_4,
		0,
		"maxLocAge"
		},
	{ ATF_POINTER, 1, offsetof(struct QoP, delay),
		
		&asn_DEF_NativeInteger,
		memb_delay_constraint_1,
		&asn_PER_memb_delay_constr_5,
		0,
		"delay"
		},
};
static int asn_MAP_QoP_oms_1[] = { 1, 2, 3 };


static asn_SEQUENCE_specifics_t asn_SPC_QoP_specs_1 = {
	sizeof(struct QoP),
	offsetof(struct QoP, _asn_ctx),
	
	asn_MAP_QoP_oms_1,	/* Optional members */
	3, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_QoP = {
	"QoP",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_QoP_1,
	4,	/* Elements count */
	&asn_SPC_QoP_specs_1	/* Additional specs */
};


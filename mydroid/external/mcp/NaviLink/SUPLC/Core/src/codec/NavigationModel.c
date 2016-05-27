/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Navigationmodel.h"

static int
memb_gpsWeek_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 1023)) {
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
memb_gpsToe_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 167)) {
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
memb_nSAT_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 31)) {
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
memb_toeLimit_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 10)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_gpsWeek_constr_2 = {
	{ APC_CONSTRAINED,	 10,  10,  0,  1023 }	/* (0..1023) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_gpsToe_constr_3 = {
	{ APC_CONSTRAINED,	 8,  8,  0,  167 }	/* (0..167) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_nSAT_constr_4 = {
	{ APC_CONSTRAINED,	 5,  5,  0,  31 }	/* (0..31) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_toeLimit_constr_5 = {
	{ APC_CONSTRAINED,	 4,  4,  0,  10 }	/* (0..10) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_NavigationModel_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct NavigationModel, gpsWeek),
		&asn_DEF_NativeInteger,
		memb_gpsWeek_constraint_1,
		&asn_PER_memb_gpsWeek_constr_2,
		0,
		"gpsWeek"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct NavigationModel, gpsToe),
		
		&asn_DEF_NativeInteger,
		memb_gpsToe_constraint_1,
		&asn_PER_memb_gpsToe_constr_3,
		0,
		"gpsToe"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct NavigationModel, nSAT),
		
		&asn_DEF_NativeInteger,
		memb_nSAT_constraint_1,
		&asn_PER_memb_nSAT_constr_4,
		0,
		"nSAT"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct NavigationModel, toeLimit),
		
		&asn_DEF_NativeInteger,
		memb_toeLimit_constraint_1,
		&asn_PER_memb_toeLimit_constr_5,
		0,
		"toeLimit"
		},
	{ ATF_POINTER, 1, offsetof(struct NavigationModel, satInfo),
		
		&asn_DEF_SatelliteInfo,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"satInfo"
		},
};
static int asn_MAP_NavigationModel_oms_1[] = { 4 };


static asn_SEQUENCE_specifics_t asn_SPC_NavigationModel_specs_1 = {
	sizeof(struct NavigationModel),
	offsetof(struct NavigationModel, _asn_ctx),
	
	asn_MAP_NavigationModel_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_NavigationModel = {
	"NavigationModel",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_NavigationModel_1,
	5,	/* Elements count */
	&asn_SPC_NavigationModel_specs_1	/* Additional specs */
};


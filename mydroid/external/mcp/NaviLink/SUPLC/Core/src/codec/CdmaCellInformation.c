/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Cdmacellinformation.h"

static int
memb_refNID_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
memb_refSID_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 32767)) {
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
memb_refBASEID_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
memb_refBASELAT_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 4194303)) {
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
memb_reBASELONG_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 8388607)) {
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
memb_refREFPN_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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

static int
memb_refWeekNumber_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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
memb_refSeconds_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	long value;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	value = *(const long *)sptr;
	
	if((value >= 0 && value <= 4194303)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

static asn_per_constraints_t asn_PER_memb_refNID_constr_2 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refSID_constr_3 = {
	{ APC_CONSTRAINED,	 15,  15,  0,  32767 }	/* (0..32767) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refBASEID_constr_4 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refBASELAT_constr_5 = {
	{ APC_CONSTRAINED,	 22, -1,  0,  4194303 }	/* (0..4194303) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_reBASELONG_constr_6 = {
	{ APC_CONSTRAINED,	 23, -1,  0,  8388607 }	/* (0..8388607) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refREFPN_constr_7 = {
	{ APC_CONSTRAINED,	 9,  9,  0,  511 }	/* (0..511) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refWeekNumber_constr_8 = {
	{ APC_CONSTRAINED,	 16,  16,  0,  65535 }	/* (0..65535) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_refSeconds_constr_9 = {
	{ APC_CONSTRAINED,	 22, -1,  0,  4194303 }	/* (0..4194303) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_CdmaCellInformation_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refNID),
		&asn_DEF_NativeInteger,
		memb_refNID_constraint_1,
		&asn_PER_memb_refNID_constr_2,
		0,
		"refNID"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refSID),
		&asn_DEF_NativeInteger,
		memb_refSID_constraint_1,
		&asn_PER_memb_refSID_constr_3,
		0,
		"refSID"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refBASEID),
		&asn_DEF_NativeInteger,
		memb_refBASEID_constraint_1,
		&asn_PER_memb_refBASEID_constr_4,
		0,
		"refBASEID"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refBASELAT),
		&asn_DEF_NativeInteger,
		memb_refBASELAT_constraint_1,
		&asn_PER_memb_refBASELAT_constr_5,
		0,
		"refBASELAT"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, reBASELONG),
		&asn_DEF_NativeInteger,
		memb_reBASELONG_constraint_1,
		&asn_PER_memb_reBASELONG_constr_6,
		0,
		"reBASELONG"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refREFPN),
		&asn_DEF_NativeInteger,
		memb_refREFPN_constraint_1,
		&asn_PER_memb_refREFPN_constr_7,
		0,
		"refREFPN"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refWeekNumber),
		&asn_DEF_NativeInteger,
		memb_refWeekNumber_constraint_1,
		&asn_PER_memb_refWeekNumber_constr_8,
		0,
		"refWeekNumber"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CdmaCellInformation, refSeconds),
		&asn_DEF_NativeInteger,
		memb_refSeconds_constraint_1,
		&asn_PER_memb_refSeconds_constr_9,
		0,
		"refSeconds"
		},
};

static asn_SEQUENCE_specifics_t asn_SPC_CdmaCellInformation_specs_1 = {
	sizeof(struct CdmaCellInformation),
	offsetof(struct CdmaCellInformation, _asn_ctx),
	0, 0, 0,	/* Optional elements (not needed) */
	1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_CdmaCellInformation = {
	"CdmaCellInformation",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_CdmaCellInformation_1,
	8,	/* Elements count */
	&asn_SPC_CdmaCellInformation_specs_1	/* Additional specs */
};


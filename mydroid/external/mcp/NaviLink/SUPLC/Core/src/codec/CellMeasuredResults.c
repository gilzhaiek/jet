/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Cellmeasuredresults.h"

static int
memb_cellIdentity_constraint_1(asn_TYPE_descriptor_t *td, const void *sptr,
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

static asn_TYPE_member_t asn_MBR_fdd_4[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct fdd, primaryCPICH_Info),
		
		&asn_DEF_PrimaryCPICH_Info,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"primaryCPICH-Info"
		},
	{ ATF_POINTER, 3, offsetof(struct fdd, cpich_Ec_N0),
		
		&asn_DEF_CPICH_Ec_N0,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cpich-Ec-N0"
		},
	{ ATF_POINTER, 2, offsetof(struct fdd, cpich_RSCP),
		
		&asn_DEF_CPICH_RSCP,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cpich-RSCP"
		},
	{ ATF_POINTER, 1, offsetof(struct fdd, pathloss),
		
		&asn_DEF_Pathloss,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"pathloss"
		},
};
static int asn_MAP_fdd_oms_4[] = { 1, 2, 3 };


static asn_SEQUENCE_specifics_t asn_SPC_fdd_specs_4 = {
	sizeof(struct fdd),
	offsetof(struct fdd, _asn_ctx),
	
	asn_MAP_fdd_oms_4,	/* Optional members */
	3, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_fdd_4 = {
	"fdd",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_fdd_4,
	4,	/* Elements count */
	&asn_SPC_fdd_specs_4	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_tdd_9[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct tdd, cellParametersID),
		
		&asn_DEF_CellParametersID,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cellParametersID"
		},
	{ ATF_POINTER, 4, offsetof(struct tdd, proposedTGSN),
		
		&asn_DEF_TGSN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"proposedTGSN"
		},
	{ ATF_POINTER, 3, offsetof(struct tdd, primaryCCPCH_RSCP),
		
		&asn_DEF_PrimaryCCPCH_RSCP,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"primaryCCPCH-RSCP"
		},
	{ ATF_POINTER, 2, offsetof(struct tdd, pathloss),
		
		&asn_DEF_Pathloss,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"pathloss"
		},
	{ ATF_POINTER, 1, offsetof(struct tdd, timeslotISCP_List),
		
		&asn_DEF_TimeslotISCP_List,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"timeslotISCP-List"
		},
};
static int asn_MAP_tdd_oms_9[] = { 1, 2, 3, 4 };


static asn_SEQUENCE_specifics_t asn_SPC_tdd_specs_9 = {
	sizeof(struct tdd),
	offsetof(struct tdd, _asn_ctx),
	
	asn_MAP_tdd_oms_9,	/* Optional members */
	4, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_tdd_9 = {
	"tdd",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_tdd_9,
	5,	/* Elements count */
	&asn_SPC_tdd_specs_9	/* Additional specs */
};

static asn_TYPE_member_t asn_MBR_modeSpecificInfo_3[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct modeSpecificInfo, choice.fdd),
		
		&asn_DEF_fdd_4,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"fdd"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct modeSpecificInfo, choice.tdd),
		
		&asn_DEF_tdd_9,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"tdd"
		},
};

static asn_CHOICE_specifics_t asn_SPC_modeSpecificInfo_specs_3 = {
	sizeof(struct modeSpecificInfo),
	offsetof(struct modeSpecificInfo, _asn_ctx),
	offsetof(struct modeSpecificInfo, present),
	sizeof(((struct modeSpecificInfo *)0)->present),
	
	0,
	-1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_modeSpecificInfo_constr_3 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static /* Use -fall-defs-global to expose */
asn_TYPE_descriptor_t asn_DEF_modeSpecificInfo_3 = {
	"modeSpecificInfo",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_modeSpecificInfo_constr_3,
	asn_MBR_modeSpecificInfo_3,
	2,	/* Elements count */
	&asn_SPC_modeSpecificInfo_specs_3	/* Additional specs */
};

static asn_per_constraints_t asn_PER_memb_cellIdentity_constr_2 = {
	{ APC_CONSTRAINED,	 28, -1,  0,  268435455 }	/* (0..268435455) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_per_constraints_t asn_PER_memb_modeSpecificInfo_constr_3 = {
	{ APC_CONSTRAINED,	 1,  1,  0,  1 }	/* (0..1) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
static asn_TYPE_member_t asn_MBR_CellMeasuredResults_1[] = {
	{ ATF_POINTER, 1, offsetof(struct CellMeasuredResults, cellIdentity),
		&asn_DEF_NativeInteger,
		memb_cellIdentity_constraint_1,
		&asn_PER_memb_cellIdentity_constr_2,
		0,
		"cellIdentity"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct CellMeasuredResults, modeSpecificInfo),
		&asn_DEF_modeSpecificInfo_3,
		0,	/* Defer constraints checking to the member type */
		&asn_PER_memb_modeSpecificInfo_constr_3,
		0,
		"modeSpecificInfo"
		},
};
static int asn_MAP_CellMeasuredResults_oms_1[] = { 0 };

static asn_SEQUENCE_specifics_t asn_SPC_CellMeasuredResults_specs_1 = {
	sizeof(struct CellMeasuredResults),
	offsetof(struct CellMeasuredResults, _asn_ctx),
	asn_MAP_CellMeasuredResults_oms_1,	/* Optional members */
	1, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_CellMeasuredResults = {
	"CellMeasuredResults",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_CellMeasuredResults_1,
	2,	/* Elements count */
	&asn_SPC_CellMeasuredResults_specs_1	/* Additional specs */
};


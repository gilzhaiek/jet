/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Requestedassistdata.h"

static asn_TYPE_member_t asn_MBR_RequestedAssistData_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, almanacRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"almanacRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, utcModelRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"utcModelRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, ionosphericModelRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"ionosphericModelRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, dgpsCorrectionsRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"dgpsCorrectionsRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, referenceLocationRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"referenceLocationRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, referenceTimeRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"referenceTimeRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, acquisitionAssistanceRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"acquisitionAssistanceRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, realTimeIntegrityRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"realTimeIntegrityRequested"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct RequestedAssistData, navigationModelRequested),
        
		&asn_DEF_BOOLEAN,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"navigationModelRequested"
		},
	{ ATF_POINTER, 2, offsetof(struct RequestedAssistData, navigationModelData),
        
		&asn_DEF_NavigationModel,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"navigationModelData"
		},
	{ ATF_POINTER, 1, offsetof(struct RequestedAssistData, ver2_RequestedAssistData_extension),
		//(ASN_TAG_CLASS_CONTEXT | (10 << 2)),
		//-1,	/* IMPLICIT tag at current level */
		&asn_DEF_Ver2_RequestedAssistData_extension,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"ver2-RequestedAssistData-extension"
		},
};
static int asn_MAP_RequestedAssistData_oms_1[] = { 9, 10 };
/*static ber_tlv_tag_t asn_DEF_RequestedAssistData_tags_1[] = {
	(ASN_TAG_CLASS_UNIVERSAL | (16 << 2))
};*/
/*
static asn_TYPE_tag2member_t asn_MAP_RequestedAssistData_tag2el_1[] = {
    { (ASN_TAG_CLASS_CONTEXT | (0 << 2)), 0, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (1 << 2)), 1, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (2 << 2)), 2, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (3 << 2)), 3, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (4 << 2)), 4, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (5 << 2)), 5, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (6 << 2)), 6, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (7 << 2)), 7, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (8 << 2)), 8, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (9 << 2)), 9, 0, 0 }, 
    { (ASN_TAG_CLASS_CONTEXT | (10 << 2)), 10, 0, 0 }
};*/
static asn_SEQUENCE_specifics_t asn_SPC_RequestedAssistData_specs_1 = {
	sizeof(struct RequestedAssistData),
	offsetof(struct RequestedAssistData, _asn_ctx),
    
	asn_MAP_RequestedAssistData_oms_1,	/* Optional members */
	1, 1,	/* Root/Additions */
	9,	/* Start extensions */
	12	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_RequestedAssistData = {
	"RequestedAssistData",
	//"RequestedAssistData",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_RequestedAssistData_1,
	11,	/* Elements count */
	&asn_SPC_RequestedAssistData_specs_1	/* Additional specs */
};


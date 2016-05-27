/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Measuredresults.h"

static asn_TYPE_member_t asn_MBR_MeasuredResults_1[] = {
	{ ATF_POINTER, 3, offsetof(struct MeasuredResults, frequencyInfo),
		
		&asn_DEF_FrequencyInfo,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"frequencyInfo"
		},
	{ ATF_POINTER, 2, offsetof(struct MeasuredResults, utra_CarrierRSSI),
		
		&asn_DEF_UTRA_CarrierRSSI,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"utra-CarrierRSSI"
		},
	{ ATF_POINTER, 1, offsetof(struct MeasuredResults, cellMeasuredResultsList),
		
		&asn_DEF_CellMeasuredResultsList,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"cellMeasuredResultsList"
		},
};
static int asn_MAP_MeasuredResults_oms_1[] = { 0, 1, 2 };


static asn_SEQUENCE_specifics_t asn_SPC_MeasuredResults_specs_1 = {
	sizeof(struct MeasuredResults),
	offsetof(struct MeasuredResults, _asn_ctx),
	
	asn_MAP_MeasuredResults_oms_1,	/* Optional members */
	3, 0,	/* Root/Additions */
	-1,	/* Start extensions */
	-1	/* Stop extensions */
};
asn_TYPE_descriptor_t asn_DEF_MeasuredResults = {
	"MeasuredResults",
	SEQUENCE_free,
	SEQUENCE_print,
	SEQUENCE_constraint,
	SEQUENCE_decode_uper,
	SEQUENCE_encode_uper,
	0,	/* No PER visible constraints */
	asn_MBR_MeasuredResults_1,
	3,	/* Elements count */
	&asn_SPC_MeasuredResults_specs_1	/* Additional specs */
};


/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/Ulpmessage.h"

static asn_TYPE_member_t asn_MBR_UlpMessage_1[] = {
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLINIT),
		
		&asn_DEF_SUPLINIT,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLINIT"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLSTART),
		
		&asn_DEF_SUPLSTART,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLSTART"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLRESPONSE),
		
		&asn_DEF_SUPLRESPONSE,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLRESPONSE"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLPOSINIT),
		
		&asn_DEF_SUPLPOSINIT,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLPOSINIT"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLPOS),
		
		&asn_DEF_SUPLPOS,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLPOS"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLEND),
		
		&asn_DEF_SUPLEND,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLEND"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLAUTHREQ),
		
		&asn_DEF_SUPLAUTHREQ,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLAUTHREQ"
		},
	{ ATF_NOFLAGS, 0, offsetof(struct UlpMessage, choice.msSUPLAUTHRESP),
		
		&asn_DEF_SUPLAUTHRESP,
		0,	/* Defer constraints checking to the member type */
		0,	/* No PER visible constraints */
		0,
		"msSUPLAUTHRESP"
		},
};

static asn_CHOICE_specifics_t asn_SPC_UlpMessage_specs_1 = {
	sizeof(struct UlpMessage),
	offsetof(struct UlpMessage, _asn_ctx),
	offsetof(struct UlpMessage, present),
	sizeof(((struct UlpMessage *)0)->present),
	
	0,
	1	/* Extensions start */
};
static asn_per_constraints_t asn_PER_UlpMessage_constr_1 = {
	{ APC_CONSTRAINED | APC_EXTENSIBLE,  3,  3,  0,  7 }	/* (0..7,...) */,
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 }
};
asn_TYPE_descriptor_t asn_DEF_UlpMessage = {
	"UlpMessage",
	CHOICE_free,
	CHOICE_print,
	CHOICE_constraint,
	CHOICE_decode_uper,
	CHOICE_encode_uper,
	&asn_PER_UlpMessage_constr_1,
	asn_MBR_UlpMessage_1,
	8,	/* Elements count */
	&asn_SPC_UlpMessage_specs_1	/* Additional specs */
};


/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/UTCTime.h"
#include "per_codec/GeneralizedTime.h"

#ifdef	__CYGWIN__
#include "/usr/include/time.h"
#else
#include <time.h>
#endif	/* __CYGWIN__ */

#ifndef	__ASN_INTERNAL_TEST_MODE__


static asn_per_constraints_t asn_PER_UTCTime_constr_1 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
	{ APC_CONSTRAINED,	 7,  -1,  0, -1 }	/* (SIZE(1..255)) */
};

asn_TYPE_descriptor_t asn_DEF_UTCTime = {
	"UTCTime",
	OCTET_STRING_free,
	0,
	UTCTime_constraint,
	OCTET_STRING_decode_uper,	/* Implemented in terms of OCTET STRING */
	OCTET_STRING_encode_uper,	/* Implemented in terms of OCTET STRING */
	&asn_PER_UTCTime_constr_1,	/* PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

#endif	/* __ASN_INTERNAL_TEST_MODE__ */

/*
 * Check that the time looks like the time.
 */
int
UTCTime_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	
	const UTCTime_t *st = (const UTCTime_t *)sptr;
	if(st && st->buf) {
		uint8_t *buf = st->buf;
		uint8_t *end = buf + st->size;

		/*
		 * Check the alphabet of the VisibleString.
		 * ISO646, ISOReg#6
		 * The alphabet is a subset of ASCII between the space
		 * and "~" (tilde).
		 */
		for(; buf < end; buf++) {
			if(*buf < 0x20 || *buf > 0x7e) {
				_ASN_CTFAIL(app_key, td,
					"%s: value byte %ld (%d) "
					"not in VisibleString alphabet (%s:%d)",
					td->name,
					(long)((buf - st->buf) + 1),
					*buf,
					__FILE__, __LINE__);
				return -1;
			}
		}
	} else {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	return 0;
}

/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/NumericString.h"

asn_TYPE_descriptor_t asn_DEF_NumericString = {
	"NumericString",
	OCTET_STRING_free,
	OCTET_STRING_print_utf8,   /* ASCII subset */
	NumericString_constraint,
	0,    /* Implemented in terms of OCTET STRING */
	0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

int
NumericString_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const NumericString_t *st = (const NumericString_t *)sptr;

	if(st && st->buf) {
		uint8_t *buf = st->buf;
		uint8_t *end = buf + st->size;

		/*
		 * Check the alphabet of the NumericString.
		 * ASN.1:1984 (X.409)
		 */
		for(; buf < end; buf++) {
			switch(*buf) {
			case 0x20:
			case 0x30: case 0x31: case 0x32: case 0x33: case 0x34:
			case 0x35: case 0x36: case 0x37: case 0x38: case 0x39:
				continue;
			}
			_ASN_CTFAIL(app_key, td,
				"%s: value byte %ld (%d) "
				"not in NumericString alphabet (%s:%d)",
				td->name,
				(long)((buf - st->buf) + 1),
				*buf,
				__FILE__, __LINE__);
			return -1;
		}
	} else {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	return 0;
}

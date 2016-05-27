/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/IA5String.h"


asn_TYPE_descriptor_t asn_DEF_IA5String = {
	"IA5String",
	OCTET_STRING_free,
	OCTET_STRING_print_utf8,	/* ASCII subset */
	IA5String_constraint,       /* Constraint on the alphabet */
	OCTET_STRING_decode_uper,	/* Unaligned PER decoder */
	OCTET_STRING_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

int
IA5String_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const IA5String_t *st = (const IA5String_t *)sptr;

	if(st && st->buf) {
		uint8_t *buf = st->buf;
		uint8_t *end = buf + st->size;
		/*
		 * IA5String is generally equivalent to 7bit ASCII.
		 * ISO/ITU-T T.50, 1963.
		 */
		for(; buf < end; buf++) {
			if(*buf > 0x7F) {
				_ASN_CTFAIL(app_key, td,
					"%s: value byte %ld out of range: "
					"%d > 127 (%s:%d)",
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


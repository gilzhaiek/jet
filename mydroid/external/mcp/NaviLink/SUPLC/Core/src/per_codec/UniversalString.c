/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/UniversalString.h"
#include "per_codec/UTF8String.h"

/*
 * UniversalString basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_UniversalString = {
	"UniversalString",
	OCTET_STRING_free,
	UniversalString_print,      /* Convert into UTF8 and print */
	asn_generic_no_constraint,
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};


static ssize_t
UniversalString__dump(const UniversalString_t *st,
		asn_app_consume_bytes_f *cb, void *app_key) {
	char scratch[128];			/* Scratchpad buffer */
	char *p = scratch;
	ssize_t wrote = 0;
	uint8_t *ch;
	uint8_t *end;

	ch = st->buf;
	end = (st->buf + st->size);
	for(end -= 3; ch < end; ch += 4) {
		uint32_t wc =     (ch[0] << 24)
				| (ch[1] << 16)
				| (ch[2] << 8)
				|  ch[3];	/* 4 bytes */
		if(sizeof(scratch) - (p - scratch) < 6) {
			wrote += p - scratch;
			if(cb(scratch, p - scratch, app_key) < 0)
				return -1;
			p = scratch;
		}
		if(wc < 0x80) {
			*p++ = (char)wc;
		} else if(wc < 0x800) {
			*p++ = 0xc0 | ((wc >> 6));
			*p++ = 0x80 | ((wc & 0x3f));
		} else if(wc < 0x10000) {
			*p++ = 0xe0 | ((wc >> 12));
			*p++ = 0x80 | ((wc >> 6) & 0x3f);
			*p++ = 0x80 | ((wc & 0x3f));
		} else if(wc < 0x200000) {
			*p++ = 0xf0 | ((wc >> 18));
			*p++ = 0x80 | ((wc >> 12) & 0x3f);
			*p++ = 0x80 | ((wc >> 6) & 0x3f);
			*p++ = 0x80 | ((wc & 0x3f));
		} else if(wc < 0x4000000) {
			*p++ = 0xf8 | ((wc >> 24));
			*p++ = 0x80 | ((wc >> 18) & 0x3f);
			*p++ = 0x80 | ((wc >> 12) & 0x3f);
			*p++ = 0x80 | ((wc >> 6) & 0x3f);
			*p++ = 0x80 | ((wc & 0x3f));
		} else {
			*p++ = 0xfc | ((wc >> 30) & 0x1);
			*p++ = 0x80 | ((wc >> 24) & 0x3f);
			*p++ = 0x80 | ((wc >> 18) & 0x3f);
			*p++ = 0x80 | ((wc >> 12) & 0x3f);
			*p++ = 0x80 | ((wc >> 6) & 0x3f);
			*p++ = 0x80 | ((wc & 0x3f));
		}
	}

	wrote += p - scratch;
	if(cb(scratch, p - scratch, app_key) < 0)
		return -1;

	return wrote;
}


int
UniversalString_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	const UniversalString_t *st = (const UniversalString_t *)sptr;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(!st || !st->buf) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	if(UniversalString__dump(st, cb, app_key) < 0)
		return -1;

	return 0;
}


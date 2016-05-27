/*-
 * Copyright (c) 2003, 2004 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/BMPString.h"
#include "per_codec/UTF8String.h"

asn_TYPE_descriptor_t asn_DEF_BMPString = {
	"BMPString",
	OCTET_STRING_free,          /* Implemented in terms of OCTET STRING */
	BMPString_print,
	asn_generic_no_constraint,  /* No constraint by default */
	0, 0,
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};

/*
 * BMPString specific contents printer.
 */
static ssize_t
BMPString__dump(const BMPString_t *st,
		asn_app_consume_bytes_f *cb, void *app_key) {
	char scratch[128];			/* Scratchpad buffer */
	char *p = scratch;
	ssize_t wrote = 0;
	uint8_t *ch;
	uint8_t *end;

	ch = st->buf;
	end = (st->buf + st->size);
	for(end--; ch < end; ch += 2) {
		uint16_t wc = (ch[0] << 8) | ch[1];	/* 2 bytes */
		if(sizeof(scratch) - (p - scratch) < 3) {
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
		} else {
			*p++ = 0xe0 | ((wc >> 12));
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
BMPString_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	const BMPString_t *st = (const BMPString_t *)sptr;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(!st || !st->buf)
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	if(BMPString__dump(st, cb, app_key) < 0)
		return -1;

	return 0;
}


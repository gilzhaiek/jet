/*-
 * Copyright (c) 2003, 2004 Lev Walkin <vlm@lionet.info>. All rights reserved.
 * Redistribution and modifications are permitted subject to BSD license.
 */
#include "per_codec/asn_internal.h"
#include "per_codec/BIT_STRING.h"


static asn_OCTET_STRING_specifics_t asn_DEF_BIT_STRING_specs = {
	sizeof(BIT_STRING_t),
	offsetof(BIT_STRING_t, _asn_ctx),
	1,	/* Special indicator that this is a BIT STRING type */
};
asn_TYPE_descriptor_t asn_DEF_BIT_STRING = {
	"BIT STRING",
	OCTET_STRING_free,         /* Implemented in terms of OCTET STRING */
	BIT_STRING_print,
	BIT_STRING_constraint,
	OCTET_STRING_decode_uper,	/* Unaligned PER decoder */
	OCTET_STRING_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	&asn_DEF_BIT_STRING_specs
};

/*
 * BIT STRING generic constraint.
 */
int
BIT_STRING_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;

	if(st && st->buf) {
		if(st->size == 1 && st->bits_unused) {
			_ASN_CTFAIL(app_key, td,
				"%s: invalid padding byte (%s:%d)",
				td->name, __FILE__, __LINE__);
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

static char *_bit_pattern[16] = {
	"0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111",
	"1000", "1001", "1010", "1011", "1100", "1101", "1110", "1111"
};


/*
 * BIT STRING specific contents printer.
 */
int
BIT_STRING_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	static const char *h2c = "0123456789ABCDEF";
	char scratch[64];
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	uint8_t *buf;
	uint8_t *end;
	char *p = scratch;

	(void)td;	/* Unused argument */

	if(!st || !st->buf)
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	ilevel++;
	buf = st->buf;
	end = buf + st->size;

	/*
	 * Hexadecimal dump.
	 */
	for(; buf < end; buf++) {
		if((buf - st->buf) % 16 == 0 && (st->size > 16)
				&& buf != st->buf) {
			_i_INDENT(1);
			/* Dump the string */
			if(cb(scratch, p - scratch, app_key) < 0) return -1;
			p = scratch;
		}
		*p++ = h2c[*buf >> 4];
		*p++ = h2c[*buf & 0x0F];
		*p++ = 0x20;
	}

	if(p > scratch) {
		p--;	/* Eat the tailing space */

		if((st->size > 16)) {
			_i_INDENT(1);
		}

		/* Dump the incomplete 16-bytes row */
		if(cb(scratch, p - scratch, app_key) < 0)
			return -1;
	}

	return 0;
}


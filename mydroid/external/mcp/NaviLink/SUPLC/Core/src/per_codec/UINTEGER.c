/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/UINTEGER.h"
#include "per_codec/asn_codecs_prim.h"	/* Encoder and decoder of a primitive type */
#include <errno.h>

/*
 * INTEGER basic type description.
 */
asn_TYPE_descriptor_t asn_DEF_UINTEGER = {
	"UINTEGER",
	ASN__PRIMITIVE_TYPE_free,
	UINTEGER_print,
	asn_generic_no_constraint,
	UINTEGER_decode_uper,	/* Unaligned PER decoder */
	UINTEGER_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	0	/* No specifics */
};


static const asn_UINTEGER_enum_map_t *UINTEGER_map_enum2value(asn_UINTEGER_specifics_t *specs, const char *lstart, const char *lstop);

/*
 * INTEGER specific human-readable output.
 */
static ssize_t
UINTEGER__dump(asn_TYPE_descriptor_t *td, const UINTEGER_t *st, asn_app_consume_bytes_f *cb, void *app_key, int plainOrXER) {
	asn_UINTEGER_specifics_t *specs=(asn_UINTEGER_specifics_t *)td->specifics;
	char scratch[32];	// Enough for 64-bit integer 
	uint8_t *buf = st->buf;
	uint8_t *buf_end = st->buf + st->size;
	signed long accum;
	ssize_t wrote = 0;
	char *p;
	int ret;

	
//	  Advance buf pointer until the start of the value's body.
//	  This will make us able to process large integers using simple case,
//	  when the actual value is small
//	  (0x0000000000abcdef would yield a fine 0x00abcdef)
	 
	// Skip the insignificant leading bytes 
	for(; buf < buf_end-1; buf++) 
	{
		switch(*buf) {
		case 0x00: if((buf[1] & 0x80) == 0) continue; break;
		case 0xff: if((buf[1] & 0x80) != 0) continue; break;
		}
		break;
	}

	// Simple case: the integer size is small 
	if((size_t)(buf_end - buf) <= sizeof(accum)) 
	{
		const asn_UINTEGER_enum_map_t *el;
		size_t scrsize;
		char *scr;

		if(buf == buf_end) {
			accum = 0;
		} else {
			accum = (*buf & 0x80) ? -1 : 0;
			for(; buf < buf_end; buf++)
				accum = (accum << 8) | *buf;
		}

		el = UINTEGER_map_value2enum(specs, accum);
		if(el) {
			scrsize = el->enum_len + 32;
			scr = (char *)alloca(scrsize);
			if(plainOrXER == 0)
				ret = snprintf(scr, scrsize,
					"%ld (%s)", accum, el->enum_name);
			else
				ret = snprintf(scr, scrsize,
					"<%s/>", el->enum_name);
		} else if(plainOrXER && specs && specs->strict_enumeration) {
			ASN_DEBUG("ASN.1 forbids dealing with "
				"unknown value of ENUMERATED type");
			errno = EPERM;
			return -1;
		} else {
			scrsize = sizeof(scratch);
			scr = scratch;
			ret = snprintf(scr, scrsize, "%ld", accum);
		}
		assert(ret > 0 && (size_t)ret < scrsize);
		return (cb(scr, ret, app_key) < 0) ? -1 : ret;
	} else if(plainOrXER && specs && specs->strict_enumeration) {
		
// 		  Here and earlier, we cannot encode the ENUMERATED values
// 		  if there is no corresponding identifier.
		 
		ASN_DEBUG("ASN.1 forbids dealing with "
			"unknown value of ENUMERATED type");
		errno = EPERM;
		return -1;
	}

// 	 Output in the long xx:yy:zz... format 
// 	 TODO: replace with generic algorithm (Knuth TAOCP Vol 2, 4.3.1) 
	for(p = scratch; buf < buf_end; buf++) 
	{
		static const char *h2c = "0123456789ABCDEF";
		if((p - scratch) >= (ssize_t)(sizeof(scratch) - 4)) 
		{
			/* Flush buffer */
			if(cb(scratch, p - scratch, app_key) < 0)
				return -1;
			wrote += p - scratch;
			p = scratch;
		}
		*p++ = h2c[*buf >> 4];
		*p++ = h2c[*buf & 0x0F];
		*p++ = 0x3a;	/* ":" */
	}
	if(p != scratch)
		p--;	// Remove the last ":" 

	wrote += p - scratch;
	return (cb(scratch, p - scratch, app_key) < 0) ? -1 : wrote;
}


 // INTEGER specific human-readable output.
 
int UINTEGER_print(asn_TYPE_descriptor_t *td, 
				   const void *sptr, 
				   int ilevel,
				   asn_app_consume_bytes_f *cb, 
				   void *app_key) 
{
	const UINTEGER_t *st = (const UINTEGER_t *)sptr;
	ssize_t ret;

	(void)td;
	(void)ilevel;

	if(!st || !st->buf)
		ret = cb("<absent>", 8, app_key);
	else
		ret = UINTEGER__dump(td, st, cb, app_key, 0);

	return (ret < 0) ? -1 : 0;
}

struct e2v_key 
{
	const char *start;
	const char *stop;
	asn_UINTEGER_enum_map_t *vemap;
	unsigned int *evmap;
};

static int UINTEGER__compar_enum2value(const void *kp, const void *am) 
{
	const struct e2v_key *key = (const struct e2v_key *)kp;
	const asn_UINTEGER_enum_map_t *el = (const asn_UINTEGER_enum_map_t *)am;
	const char *ptr, *end, *name;

	// Remap the element (sort by different criterion) 
	el = key->vemap + key->evmap[el - key->vemap];

	// Compare strings 
	for(ptr = key->start, end = key->stop, name = el->enum_name;
			ptr < end; ptr++, name++) {
		if(*ptr != *name)
			return *(const unsigned char *)ptr
				- *(const unsigned char *)name;
	}
	return name[0] ? -1 : 0;
}

static const asn_UINTEGER_enum_map_t *
UINTEGER_map_enum2value(asn_UINTEGER_specifics_t *specs, const char *lstart, const char *lstop) {
	asn_UINTEGER_enum_map_t *el_found;
	int count = specs ? specs->map_count : 0;
	struct e2v_key key;
	const char *lp;

	if(!count) return NULL;

	// Guaranteed: assert(lstart < lstop); 
	// Figure out the tag name 
	for(lstart++, lp = lstart; lp < lstop; lp++) 
	{
		switch(*lp) 
		{
		case 9: case 10: case 11: case 12: case 13: case 32: // WSP 
		case 0x2f: /* '/' */ case 0x3e: /* '>' */
			break;
		default:
			continue;
		}
		break;
	}
	if(lp == lstop) return NULL;	// No tag found 
	lstop = lp;

	key.start = lstart;
	key.stop = lstop;
	key.vemap = specs->value2enum;
	key.evmap = specs->enum2value;
	el_found = (asn_UINTEGER_enum_map_t *)bsearch(&key,
												   specs->value2enum, 
												   count, 
												   sizeof(specs->value2enum[0]),
												   UINTEGER__compar_enum2value);
	if(el_found) 
	{
		/* Remap enum2value into value2enum */
		el_found = key.vemap + key.evmap[el_found - key.vemap];
	}
	return el_found;
}

static int UINTEGER__compar_value2enum(const void *kp, 
									   const void *am) 
{
	unsigned long a = *(const unsigned long *)kp;
	const asn_UINTEGER_enum_map_t *el = (const asn_UINTEGER_enum_map_t *)am;
	unsigned long b = el->nat_value;
	if(a < b) return -1;
	else if(a == b) return 0;
	else return 1;
}

const asn_UINTEGER_enum_map_t * UINTEGER_map_value2enum(asn_UINTEGER_specifics_t *specs, 
														unsigned long value)
{
	int count = specs ? specs->map_count : 0;
	if(!count) return 0;
	return (asn_UINTEGER_enum_map_t *)bsearch(&value, 
											  specs->value2enum,
											  count, 
											  sizeof(specs->value2enum[0]),
											  UINTEGER__compar_value2enum);
}

static int
UINTEGER_st_prealloc(UINTEGER_t *st, int min_size) 
{
	void *p = MALLOC(min_size + 1);
	if(p)
	{
		void *b = st->buf;
		st->size = 0;
		st->buf = p;
		FREEMEM(b);
		return 0;
	} else {
		return -1;
	}
}


asn_dec_rval_t UINTEGER_decode_uper(asn_codec_ctx_t *opt_codec_ctx, 
									asn_TYPE_descriptor_t *td,
									asn_per_constraints_t *constraints, 
									void **sptr, 
									asn_per_data_t *pd) 
{
	asn_dec_rval_t rval = { RC_OK, 0 };
	UINTEGER_t *st = (UINTEGER_t *)*sptr;
	asn_per_constraint_t *ct;
	int repeat;

	(void)opt_codec_ctx;

	if(!st) {
		st = (UINTEGER_t *)(*sptr = CALLOC(1, sizeof(*st)));
		if(!st) _ASN_DECODE_FAILED;
	}

	if(!constraints) constraints = td->per_constraints;
	ct = constraints ? &constraints->value : 0;

	if(ct && ct->flags & APC_EXTENSIBLE) 
	{
		int inext = per_get_few_bits(pd, 1);
		if(inext < 0) _ASN_DECODE_STARVED;
		if(inext) ct = 0;
	}

	FREEMEM(st->buf);
	if(ct) 
	{
		if(ct->flags & APC_SEMI_CONSTRAINED) 
		{
			st->buf = (uint8_t *)CALLOC(1, 2);
			if(!st->buf) _ASN_DECODE_FAILED;
			st->size = 1;
		} 
		else if(ct->flags & APC_CONSTRAINED && ct->range_bits >= 0) 
		{
			size_t size = (ct->range_bits + 7) >> 3;
			st->buf = (uint8_t *)MALLOC(1 + size + 1);
			if(!st->buf) _ASN_DECODE_FAILED;
			st->size = size;
		}
		else 
		{
			st->size = 0;
		}
	} 
	else 
	{
		st->size = 0;
	}

	/* X.691, #12.2.2 */
	if(ct && ct->flags != APC_UNCONSTRAINED) 
	{
		/* #10.5.6 */
		ASN_DEBUG("Integer with range %d bits", ct->range_bits);
		if(ct->range_bits >= 0) 
		{
			unsigned long value = per_get_few_bits(pd, ct->range_bits);
			if(value < 0) _ASN_DECODE_STARVED;
			ASN_DEBUG("Got value %ld + low %ld", value, ct->lower_bound);
			value += ct->lower_bound;
			if(asn_ulong2UINTEGER(st, value)) _ASN_DECODE_FAILED;
			return rval;
		}
	} 
	else 
	{
		ASN_DEBUG("Decoding unconstrained integer %s", td->name);
	}

	/* X.691, #12.2.3, #12.2.4 */
	do {
		ssize_t len;
		void *p;
		int ret;

		/* Get the PER length */
		len = uper_get_length(pd, -1, &repeat);
		if(len < 0) _ASN_DECODE_STARVED;

		p = REALLOC(st->buf, st->size + len + 1);
		if(!p) _ASN_DECODE_FAILED;
		st->buf = (uint8_t *)p;

		ret = per_get_many_bits(pd, &st->buf[st->size], 0, 8 * len);
		if(ret < 0) _ASN_DECODE_STARVED;
		st->size += len;
	} while(repeat);
	st->buf[st->size] = 0;	/* JIC */

	/* #12.2.3 */
	if(ct && ct->lower_bound) 
	{
		/*
		 * TODO: replace by in-place arithmetics.
		 */
		unsigned long value;
		if(asn_UINTEGER2ulong(st, &value))
			_ASN_DECODE_FAILED;
		if(asn_ulong2UINTEGER(st, value + (unsigned long)ct->lower_bound))
			_ASN_DECODE_FAILED;
	}

	return rval;
}

asn_enc_rval_t UINTEGER_encode_uper(asn_TYPE_descriptor_t *td, 
									asn_per_constraints_t *constraints, 
									void *sptr, 
									asn_per_outp_t *po) 
{
	asn_enc_rval_t er;
	UINTEGER_t *st = (UINTEGER_t *)sptr;
	const uint8_t *buf;
	const uint8_t *end;
	asn_per_constraint_t *ct;
	unsigned long value = 0;

	if(!st || st->size == 0) _ASN_ENCODE_FAILED;

	if(!constraints) constraints = td->per_constraints;
	ct = constraints ? &constraints->value : 0;

	er.encoded = 0;

	if(ct) {
		int inext = 0;
		if(asn_UINTEGER2ulong(st, &value))
			_ASN_ENCODE_FAILED;
		/* Check proper range */
		if(ct->flags & APC_SEMI_CONSTRAINED) {
			if(value < (unsigned long)ct->lower_bound)
				inext = 1;
		} else if(ct->range_bits >= 0) 
		{
			if(value < (unsigned long) ct->lower_bound || value > (unsigned long)ct->upper_bound)
				inext = 1;
		}
		ASN_DEBUG("Value %ld (%02x/%d) lb %ld ub %ld %s",value, 
														 st->buf[0], 
														 st->size,
														 ct->lower_bound, 
														 ct->upper_bound,
														 inext ? "ext" : "fix");
		if(ct->flags & APC_EXTENSIBLE) 
		{
			if(per_put_few_bits(po, inext, 1)) _ASN_ENCODE_FAILED;
			if(inext) ct = 0;
		} 
		else if(inext) 
		{
			_ASN_ENCODE_FAILED;
		}
	}


	/* X.691, #12.2.2 */
	if(ct && ct->range_bits >= 0) {
		/* #10.5.6 */
		ASN_DEBUG("Encoding integer with range %d bits",
			ct->range_bits);
		if(per_put_few_bits(po, value - (unsigned long)ct->lower_bound, ct->range_bits))
			_ASN_ENCODE_FAILED;
		_ASN_ENCODED_OK(er);
	}

	if(ct && (unsigned long)ct->lower_bound) 
	{
		ASN_DEBUG("Adjust lower bound to %ld", ct->lower_bound);
		/* TODO: adjust lower bound */
		_ASN_ENCODE_FAILED;
	}

	for(buf = st->buf, end = st->buf + st->size; buf < end;) 
	{
		ssize_t mayEncode = uper_put_length(po, end - buf);
		if(mayEncode < 0) _ASN_ENCODE_FAILED;

		if(per_put_many_bits(po, buf, 8 * mayEncode)) _ASN_ENCODE_FAILED;
		buf += mayEncode;
	}

	_ASN_ENCODED_OK(er);
}

int asn_UINTEGER2ulong(const UINTEGER_t *iptr, unsigned long *lptr) 
{
	uint8_t *b, *end;
	size_t size;
	unsigned long l;

	/* Sanity checking */
	if(!iptr || !iptr->buf || !lptr) {
		errno = EINVAL;
		return -1;
	}

	/* Cache the begin/end of the buffer */
	b = iptr->buf;	/* Start of the INTEGER buffer */
	size = iptr->size;
	end = b + size;	/* Where to stop */

	if(size > sizeof(unsigned long)) {
		uint8_t *end1 = end - 1;
		/*
		 * Slightly more advanced processing,
		 * able to >sizeof(long) bytes,
		 * when the actual value is small
		 * (0x0000000000abcdef would yield a fine 0x00abcdef)
		 */
		/* Skip out the insignificant leading bytes */
		for(; b < end1; b++) 
		{
			switch(*b) 
			{
				case 0x00: if((b[1] & 0x80) == 0) continue; break;
				case 0xff: if((b[1] & 0x80) != 0) continue; break;
			}
			break;
		}

		size = end - b;
		if(size > sizeof(unsigned long)) {
			/* Still cannot fit the long */
			errno = ERANGE;
			return -1;
		}
	}

	/* Shortcut processing of a corner case */
	if(end == b) {
		*lptr = 0;
		return 0;
	}

	/* Perform the sign initialization */
	/* Actually l = -(*b >> 7); gains nothing, yet unreadable! */
	if((*b >> 7)) l = -1; else l = 0;

	/* Conversion engine */
	for(; b < end; b++)
		l = (l << 8) | *b;

	*lptr = l;
	return 0;
}

int
asn_ulong2UINTEGER(UINTEGER_t *st, unsigned long value) {
	uint8_t *buf, *bp;
	uint8_t *p;
	uint8_t *pstart;
	uint8_t *pend1;
	int littleEndian = 1;	/* Run-time detection */
	int add;

	if(!st) 
	{
		errno = EINVAL;
		return -1;
	}

	buf = (uint8_t *)MALLOC(sizeof(value));
	if(!buf) return -1;

	if(*(char *)&littleEndian) 
	{
		pstart = (uint8_t *)&value + sizeof(value) - 1;
		pend1 = (uint8_t *)&value;
		add = -1;
	} else 
	{
		pstart = (uint8_t *)&value;
		pend1 = pstart + sizeof(value) - 1;
		add = 1;
	}

	/*
	 * If the contents octet consists of more than one octet,
	 * then bits of the first octet and bit 8 of the second octet:
	 * a) shall not all be ones; and
	 * b) shall not all be zero.
	 */
	for(p = pstart; p != pend1; p += add) 
	{
		switch(*p) 
		{
		case 0x00: if((*(p+add) & 0x80) == 0)
			continue;
			break;
		case 0xff: if((*(p+add) & 0x80))
			continue;
			break;
		}
		break;
	}
	/* Copy the integer body */
	for(pstart = p, bp = buf, pend1 += add; p != pend1; p += add)
		*bp++ = *p;

	if(st->buf) FREEMEM(st->buf);
	st->buf = buf;
	st->size = bp - buf;

	return 0;
}

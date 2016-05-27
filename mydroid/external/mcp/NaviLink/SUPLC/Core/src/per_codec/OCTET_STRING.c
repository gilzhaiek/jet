/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/OCTET_STRING.h"
#include "per_codec/BIT_STRING.h"	/* for .bits_unused member */

#define MAX_BUF_SIZE 8192
static asn_OCTET_STRING_specifics_t asn_DEF_OCTET_STRING_specs = {
	sizeof(OCTET_STRING_t),
	offsetof(OCTET_STRING_t, _asn_ctx),
	0
};
static asn_per_constraint_t asn_DEF_OCTET_STRING_constraint = {
	APC_SEMI_CONSTRAINED, -1, -1, 0, 0
};
asn_TYPE_descriptor_t asn_DEF_OCTET_STRING = {
	"OCTET STRING",		/* Canonical name */
	OCTET_STRING_free,
	OCTET_STRING_print,	/* non-ascii stuff, generally */
	asn_generic_no_constraint,
	OCTET_STRING_decode_uper,	/* Unaligned PER decoder */
	OCTET_STRING_encode_uper,	/* Unaligned PER encoder */
	0,	/* No PER visible constraints */
	0, 0,	/* No members */
	&asn_DEF_OCTET_STRING_specs
};

#undef	_CH_PHASE
#undef	NEXT_PHASE
#undef	PREV_PHASE
#define	_CH_PHASE(ctx, inc) do {					\
		if(ctx->phase == 0)					\
			ctx->context = 0;				\
		ctx->phase += inc;					\
	} while(0)
#define	NEXT_PHASE(ctx)	_CH_PHASE(ctx, +1)
#define	PREV_PHASE(ctx)	_CH_PHASE(ctx, -1)

#undef	ADVANCE
#define	ADVANCE(num_bytes)	do {					\
		size_t num = (num_bytes);				\
		buf_ptr = ((const char *)buf_ptr) + num;		\
		size -= num;						\
		consumed_myself += num;					\
	} while(0)

#undef	RETURN
#define	RETURN(_code)	do {						\
		asn_dec_rval_t tmprval;					\
		tmprval.code = _code;					\
		tmprval.consumed = consumed_myself;			\
		return tmprval;						\
	} while(0)

#undef	APPEND
#define	APPEND(bufptr, bufsize)	do {					\
		size_t _bs = (bufsize);		/* Append size */	\
		size_t _ns = ctx->context;	/* Allocated now */	\
		size_t _es = st->size + _bs;	/* Expected size */	\
		/* int is really a typeof(st->size): */			\
		if((int)_es < 0) RETURN(RC_FAIL);			\
		if(_ns <= _es) {					\
			void *ptr;					\
			/* Be nice and round to the memory allocator */	\
			do { _ns = _ns ? _ns << 1 : 16; }		\
			    while(_ns <= _es);				\
			/* int is really a typeof(st->size): */		\
			if((int)_ns < 0) RETURN(RC_FAIL);		\
			ptr = REALLOC(st->buf, _ns);			\
			if(ptr) {					\
				st->buf = (uint8_t *)ptr;		\
				ctx->context = _ns;			\
			} else {					\
				RETURN(RC_FAIL);			\
			}						\
			ASN_DEBUG("Reallocating into %ld", (long)_ns);	\
		}							\
		memcpy(st->buf + st->size, bufptr, _bs);		\
		/* Convenient nul-termination */			\
		st->buf[_es] = '\0';					\
		st->size = _es;						\
	} while(0)

/*
 * Internal variant of the OCTET STRING.
 */
typedef enum OS_type {
	_TT_GENERIC	= 0,	/* Just a random OCTET STRING */
	_TT_BIT_STRING	= 1,	/* BIT STRING type, a special case */
	_TT_ANY		= 2	/* ANY type, a special case too */
} OS_type_e;

/*
 * The main reason why ASN.1 is still alive is that too much time and effort
 * is necessary for learning it more or less adequately, thus creating a gut
 * necessity to demonstrate that aquired skill everywhere afterwards.
 * No, I am not going to explain what the following stuff is.
 */
struct _stack_el {
	int	cont_level;	/* Depth of subcontainment */
	int	want_nulls;	/* Want null "end of content" octets? */
	int	bits_chopped;	/* Flag in BIT STRING mode */
	struct _stack_el *prev;
	struct _stack_el *next;
};
struct _stack {
	struct _stack_el *tail;
	struct _stack_el *cur_ptr;
};

static struct _stack_el *
OS__add_stack_el(struct _stack *st) {
	struct _stack_el *nel;

	/*
	 * Reuse the old stack frame or allocate a new one.
	 */
	if(st->cur_ptr && st->cur_ptr->next) {
		nel = st->cur_ptr->next;
		nel->bits_chopped = 0;
		/* Retain the nel->cont_level, it's correct. */
	} else {
		nel = (struct _stack_el *)CALLOC(1, sizeof(struct _stack_el));
		if(nel == NULL)
			return NULL;
	
		if(st->tail) {
			/* Increase a subcontainment depth */
			nel->cont_level = st->tail->cont_level + 1;
			st->tail->next = nel;
		}
		nel->prev = st->tail;
		st->tail = nel;
	}

	st->cur_ptr = nel;

	return nel;
}

static struct _stack *
_new_stack() {
	return (struct _stack *)CALLOC(1, sizeof(struct _stack));
}

/*
 * Decode OCTET STRING type.
 */

// static int
// OCTET_STRING__handle_control_chars(void *struct_ptr, const void *chunk_buf, size_t chunk_size) {
//     /*
//      * This might be one of the escape sequences
//      * for control characters. Check it out.
//      * #11.15.5
//      */
//     int control_char = OS__check_escaped_control_char(chunk_buf,chunk_size);
//     if(control_char >= 0) {
//         OCTET_STRING_t *st = (OCTET_STRING_t *)struct_ptr;
//         void *p = REALLOC(st->buf, st->size + 2);
//         if(p) {
//             st->buf = (uint8_t *)p;
//             st->buf[st->size++] = control_char;
//             st->buf[st->size] = '\0';    /* nul-termination */
//             return 0;
//         }
//     }
//     
//     return -1;    /* No, it's not */
// }

/*
 * Convert from hexadecimal format (cstring): "AB CD EF"
 */
static ssize_t OCTET_STRING__convert_hexadecimal(void *sptr, const void *chunk_buf, size_t chunk_size, int have_more) {
	OCTET_STRING_t *st = (OCTET_STRING_t *)sptr;
	const char *chunk_stop = (const char *)chunk_buf;
	const char *p = chunk_stop;
	const char *pend = p + chunk_size;
	unsigned int clv = 0;
	int half = 0;	/* Half bit */
	uint8_t *buf;

	/* Reallocate buffer according to high cap estimation */
	ssize_t _ns = st->size + (chunk_size + 1) / 2;
	void *nptr = REALLOC(st->buf, _ns + 1);
	if(!nptr) return -1;
	st->buf = (uint8_t *)nptr;
	buf = st->buf + st->size;

	/*
	 * If something like " a b c " appears here, the " a b":3 will be
	 * converted, and the rest skipped. That is, unless buf_size is greater
	 * than chunk_size, then it'll be equivalent to "ABC0".
	 */
	for(; p < pend; p++) {
		int ch = *(const unsigned char *)p;
		switch(ch) {
		case 0x09: case 0x0a: case 0x0c: case 0x0d:
		case 0x20:
			/* Ignore whitespace */
			continue;
		case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: /*01234*/
		case 0x35: case 0x36: case 0x37: case 0x38: case 0x39: /*56789*/
			clv = (clv << 4) + (ch - 0x30);
			break;
		case 0x41: case 0x42: case 0x43:	/* ABC */
		case 0x44: case 0x45: case 0x46:	/* DEF */
			clv = (clv << 4) + (ch - 0x41 + 10);
			break;
		case 0x61: case 0x62: case 0x63:	/* abc */
		case 0x64: case 0x65: case 0x66:	/* def */
			clv = (clv << 4) + (ch - 0x61 + 10);
			break;
		default:
			*buf = 0;	/* JIC */
			return -1;
		}
		if(half++) {
			half = 0;
			*buf++ = clv;
			chunk_stop = p + 1;
		}
	}

	/*
	 * Check partial decoding.
	 */
	if(half) {
		if(have_more) {
			/*
			 * Partial specification is fine,
			 * because no more more PXER_TEXT data is available.
			 */
			*buf++ = clv << 4;
			chunk_stop = p;
		}
	} else {
		chunk_stop = p;
	}

	st->size = buf - st->buf;	/* Adjust the buffer size */
	assert(st->size <= _ns);
	st->buf[st->size] = 0;		/* Courtesy termination */

	return (chunk_stop - (const char *)chunk_buf);	/* Converted size */
}

/*
 * Convert from binary format: "00101011101"
 */
static ssize_t OCTET_STRING__convert_binary(void *sptr, const void *chunk_buf, size_t chunk_size, int have_more) {
	BIT_STRING_t *st = (BIT_STRING_t *)sptr;
	const char *p = (const char *)chunk_buf;
	const char *pend = p + chunk_size;
	int bits_unused = st->bits_unused & 0x7;
	uint8_t *buf;

	/* Reallocate buffer according to high cap estimation */
	ssize_t _ns = st->size + (chunk_size + 7) / 8;
	void *nptr = REALLOC(st->buf, _ns + 1);
	if(!nptr) return -1;
	st->buf = (uint8_t *)nptr;
	buf = st->buf + st->size;

	(void)have_more;

	if(bits_unused == 0)
		bits_unused = 8;
	else if(st->size)
		buf--;

	/*
	 * Convert series of 0 and 1 into the octet string.
	 */
	for(; p < pend; p++) {
		int ch = *(const unsigned char *)p;
		switch(ch) {
		case 0x09: case 0x0a: case 0x0c: case 0x0d:
		case 0x20:
			/* Ignore whitespace */
			break;
		case 0x30:
		case 0x31:
			if(bits_unused-- <= 0) {
				*++buf = 0;	/* Clean the cell */
				bits_unused = 7;
			}
			*buf |= (ch&1) << bits_unused;
			break;
		default:
			st->bits_unused = bits_unused;
			return -1;
		}
	}

	if(bits_unused == 8) {
		st->size = buf - st->buf;
		st->bits_unused = 0;
	} else {
		st->size = buf - st->buf + 1;
		st->bits_unused = bits_unused;
	}

	assert(st->size <= _ns);
	st->buf[st->size] = 0;		/* Courtesy termination */

	return chunk_size;	/* Converted in full */
}

/*
 * Something like strtod(), but with stricter rules.
 */
static int
OS__strtoent(int base, const char *buf, const char *end, int32_t *ret_value) {
	int32_t val = 0;
	const char *p;

	for(p = buf; p < end; p++) {
		int ch = *p;

		/* Strange huge value */
		if((val * base + base) < 0)
			return -1;

		switch(ch) {
		case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: /*01234*/
		case 0x35: case 0x36: case 0x37: case 0x38: case 0x39: /*56789*/
			val = val * base + (ch - 0x30);
			break;
		case 0x41: case 0x42: case 0x43:	/* ABC */
		case 0x44: case 0x45: case 0x46:	/* DEF */
			val = val * base + (ch - 0x41 + 10);
			break;
		case 0x61: case 0x62: case 0x63:	/* abc */
		case 0x64: case 0x65: case 0x66:	/* def */
			val = val * base + (ch - 0x61 + 10);
			break;
		case 0x3b:	/* ';' */
			*ret_value = val;
			return (p - buf) + 1;
		default:
			return -1;	/* Character set error */
		}
	}

	*ret_value = -1;
	return (p - buf);
}

/*
 * Convert from the plain UTF-8 format, expanding entity references: "2 &lt; 3"
 */
static ssize_t OCTET_STRING__convert_entrefs(void *sptr, const void *chunk_buf, size_t chunk_size, int have_more) {
	OCTET_STRING_t *st = (OCTET_STRING_t *)sptr;
	const char *p = (const char *)chunk_buf;
	const char *pend = p + chunk_size;
	uint8_t *buf;

	/* Reallocate buffer */
	ssize_t _ns = st->size + chunk_size;
	void *nptr = REALLOC(st->buf, _ns + 1);
	if(!nptr) return -1;
	st->buf = (uint8_t *)nptr;
	buf = st->buf + st->size;

	/*
	 * Convert series of 0 and 1 into the octet string.
	 */
	for(; p < pend; p++) {
		int ch = *(const unsigned char *)p;
		int len;	/* Length of the rest of the chunk */

		if(ch != 0x26 /* '&' */) {
			*buf++ = ch;
			continue;	/* That was easy... */
		}

		/*
		 * Process entity reference.
		 */
		len = chunk_size - (p - (const char *)chunk_buf);
		if(len == 1 /* "&" */) goto want_more;
		if(p[1] == 0x23 /* '#' */) {
			const char *pval;	/* Pointer to start of digits */
			int32_t val = 0;	/* Entity reference value */
			int base;

			if(len == 2 /* "&#" */) goto want_more;
			if(p[2] == 0x78 /* 'x' */)
				pval = p + 3, base = 16;
			else
				pval = p + 2, base = 10;
			len = OS__strtoent(base, pval, p + len, &val);
			if(len == -1) {
				/* Invalid charset. Just copy verbatim. */
				*buf++ = ch;
				continue;
			}
			if(!len || pval[len-1] != 0x3b) goto want_more;
			assert(val > 0);
			p += (pval - p) + len - 1; /* Advance past entref */

			if(val < 0x80) {
				*buf++ = (char)val;
			} else if(val < 0x800) {
				*buf++ = 0xc0 | ((val >> 6));
				*buf++ = 0x80 | ((val & 0x3f));
			} else if(val < 0x10000) {
				*buf++ = 0xe0 | ((val >> 12));
				*buf++ = 0x80 | ((val >> 6) & 0x3f);
				*buf++ = 0x80 | ((val & 0x3f));
			} else if(val < 0x200000) {
				*buf++ = 0xf0 | ((val >> 18));
				*buf++ = 0x80 | ((val >> 12) & 0x3f);
				*buf++ = 0x80 | ((val >> 6) & 0x3f);
				*buf++ = 0x80 | ((val & 0x3f));
			} else if(val < 0x4000000) {
				*buf++ = 0xf8 | ((val >> 24));
				*buf++ = 0x80 | ((val >> 18) & 0x3f);
				*buf++ = 0x80 | ((val >> 12) & 0x3f);
				*buf++ = 0x80 | ((val >> 6) & 0x3f);
				*buf++ = 0x80 | ((val & 0x3f));
			} else {
				*buf++ = 0xfc | ((val >> 30) & 0x1);
				*buf++ = 0x80 | ((val >> 24) & 0x3f);
				*buf++ = 0x80 | ((val >> 18) & 0x3f);
				*buf++ = 0x80 | ((val >> 12) & 0x3f);
				*buf++ = 0x80 | ((val >> 6) & 0x3f);
				*buf++ = 0x80 | ((val & 0x3f));
			}
		} else {
			/*
			 * Ugly, limited parsing of &amp; &gt; &lt;
			 */
			char *sc = (char *)memchr(p, 0x3b, len > 5 ? 5 : len);
			if(!sc) goto want_more;
			if((sc - p) == 4
				&& p[1] == 0x61	/* 'a' */
				&& p[2] == 0x6d	/* 'm' */
				&& p[3] == 0x70	/* 'p' */) {
				*buf++ = 0x26;
				p = sc;
				continue;
			}
			if((sc - p) == 3) {
				if(p[1] == 0x6c) {
					*buf = 0x3c;	/* '<' */
				} else if(p[1] == 0x67) {
					*buf = 0x3e;	/* '>' */
				} else {
					/* Unsupported entity reference */
					*buf++ = ch;
					continue;
				}
				if(p[2] != 0x74) {
					/* Unsupported entity reference */
					*buf++ = ch;
					continue;
				}
				buf++;
				p = sc;
				continue;
			}
			/* Unsupported entity reference */
			*buf++ = ch;
		}

		continue;
	want_more:
		if(have_more) {
			/*
			 * We know that no more data (of the same type)
			 * is coming. Copy the rest verbatim.
			 */
			*buf++ = ch;
			continue;
		}
		chunk_size = (p - (const char *)chunk_buf);
		/* Processing stalled: need more data */
		break;
	}

	st->size = buf - st->buf;
	assert(st->size <= _ns);
	st->buf[st->size] = 0;		/* Courtesy termination */

	return chunk_size;	/* Converted in full */
}

asn_dec_rval_t
OCTET_STRING_decode_uper(asn_codec_ctx_t *opt_codec_ctx,
	asn_TYPE_descriptor_t *td, asn_per_constraints_t *constraints,
	void **sptr, asn_per_data_t *pd) {

	asn_OCTET_STRING_specifics_t *specs = td->specifics
		? (asn_OCTET_STRING_specifics_t *)td->specifics
		: &asn_DEF_OCTET_STRING_specs;
	asn_per_constraint_t *ct = constraints ? &constraints->size
				: (td->per_constraints
					? &td->per_constraints->size
					: &asn_DEF_OCTET_STRING_constraint);
	asn_dec_rval_t rval = { RC_OK, 0 };
	BIT_STRING_t *st = (BIT_STRING_t *)*sptr;
	ssize_t consumed_myself = 0;
	int repeat;
	int value=0;
	int i=0;
	int unit_bits = (specs->subvariant != 1) * 7 + 1;

	(void)opt_codec_ctx;

	/*
	 * Allocate the string.
	 */
	if(!st) {
		st = (BIT_STRING_t *)(*sptr = CALLOC(1, specs->struct_size));
		if(!st) RETURN(RC_FAIL);
	}
	
	st->buf=NULL;

	ASN_DEBUG("PER Decoding %s %ld .. %ld bits %d",
		ct->flags & APC_EXTENSIBLE ? "extensible" : "fixed",
		ct->lower_bound, ct->upper_bound, ct->effective_bits);

	if(ct->flags & APC_EXTENSIBLE) {
		int inext = per_get_few_bits(pd, 1);
		if(inext < 0) RETURN(RC_WMORE);
		if(inext) ct = &asn_DEF_OCTET_STRING_constraint;
		consumed_myself = 0;
	}

	if(ct->effective_bits >= 0
	&& (!st->buf || st->size < ct->upper_bound)) {
		FREEMEM(st->buf);
		if(unit_bits == 1) {
			st->size = (ct->upper_bound + 7) >> 3;
		} else {
			st->size = ct->upper_bound;
		}
		st->buf = (uint8_t *)MALLOC(st->size + 1);
		if(!st->buf) { st->size = 0; RETURN(RC_FAIL); }
	}

	/* X.691, #16.5: zero-length encoding */
	/* X.691, #16.6: short fixed length encoding (up to 2 octets) */
	/* X.691, #16.7: long fixed length encoding (up to 64K octets) */
	if(ct->effective_bits == 0) {
		int ret = per_get_many_bits(pd, st->buf, 0,
					    unit_bits * ct->upper_bound);
		if(ret < 0) RETURN(RC_WMORE);
		consumed_myself += unit_bits * ct->upper_bound;
		st->buf[st->size] = 0;
		if(unit_bits == 1 && (ct->upper_bound & 0x7))
			st->bits_unused = 8 - (ct->upper_bound & 0x7);
		RETURN(RC_OK);
	}

	st->size = 0;
	do {
		ssize_t len_bytes;
		ssize_t len_bits;
		void *p;
		int ret;

		/* Get the PER length */
		len_bits = uper_get_length(pd, ct->effective_bits, &repeat);
		if(len_bits < 0) RETURN(RC_WMORE);
		len_bits += ct->lower_bound;

		ASN_DEBUG("Got PER length eb %ld, len %ld, %s (%s)",
			(long)ct->effective_bits, (long)len_bits,
			repeat ? "repeat" : "once", td->name);
		if(unit_bits == 1) {
			len_bytes = (len_bits + 7) >> 3;
			if(len_bits & 0x7)
				st->bits_unused = 8 - (len_bits & 0x7);
			/* len_bits be multiple of 16K if repeat is set */
		} else {
			len_bytes = len_bits;
			len_bits = len_bytes << 3;
			if(ct->range_bits < 8 && ct->range_bits > 0)   //Added by Ildar!!!!!
				len_bits = ct->range_bits * len_bytes;
		}
		p = REALLOC(st->buf, st->size + len_bytes + 1);
		if(!p) RETURN(RC_FAIL);
		st->buf = (uint8_t *)p;

		if(ct->range_bits < 8 && ct->range_bits > 0) 
		{
			for(i=0;i < len_bytes;i++)
			{
				value = per_get_few_bits(pd,ct->range_bits);
				st->buf[i]=value;
				if(value < 0) RETURN(RC_WMORE);
			}
		}
		else
		{
			ret = per_get_many_bits(pd, &st->buf[st->size], 0, len_bits);
			if(ret < 0) RETURN(RC_WMORE);
		}
		st->size += len_bytes;
	} while(repeat);
	st->buf[st->size] = 0;	/* nul-terminate */

	return rval;
}


asn_enc_rval_t
OCTET_STRING_encode_uper(asn_TYPE_descriptor_t *td,
        asn_per_constraints_t *constraints, void *sptr, asn_per_outp_t *po) {

	asn_OCTET_STRING_specifics_t *specs = td->specifics
		? (asn_OCTET_STRING_specifics_t *)td->specifics
		: &asn_DEF_OCTET_STRING_specs;
	asn_per_constraint_t *ct = constraints ? &constraints->size
				: (td->per_constraints
					? &td->per_constraints->size
					: &asn_DEF_OCTET_STRING_constraint);
	const BIT_STRING_t *st = (const BIT_STRING_t *)sptr;
	int unit_bits = (specs->subvariant != 1) * 7 + 1;
	asn_enc_rval_t er;
	int ct_extensible = ct->flags & APC_EXTENSIBLE;
	int inext = 0;		/* Lies not within extension root */
	int sizeinunits = st->size;
	const uint8_t *buf;
	int ret;
	int i=0;

	if(!st || !st->buf)
		_ASN_ENCODE_FAILED;

	if(unit_bits == 1) {
		ASN_DEBUG("BIT STRING of %d bytes, %d bits unused",
				sizeinunits, st->bits_unused);
		sizeinunits = sizeinunits * 8 - (st->bits_unused & 0x07);
	}

	ASN_DEBUG("Encoding %s into %d units of %d bits"
		" (%d..%d, effective %d)%s",
		td->name, sizeinunits, unit_bits,
		ct->lower_bound, ct->upper_bound,
		ct->effective_bits, ct_extensible ? " EXT" : "");

	/* Figure out wheter size lies within PER visible consrtaint */
	if(ct->range_bits < 8 && ct->range_bits > 0)             //Added by Ildar!!!!!!!!!!!!!!!!!!!!!!!
		unit_bits = ct->range_bits;

	if(ct->effective_bits >= 0) {
		if(sizeinunits < ct->lower_bound
		|| sizeinunits > ct->upper_bound) {
			if(ct_extensible) {
				ct = &asn_DEF_OCTET_STRING_constraint;
				inext = 1;
			} else
				_ASN_ENCODE_FAILED;
		}
	} else {
		inext = 0;
	}

	if(ct_extensible) {
		/* Declare whether length is [not] within extension root */
		if(per_put_few_bits(po, inext, 1))
			_ASN_ENCODE_FAILED;
	}

	/* X.691, #16.5: zero-length encoding */
	/* X.691, #16.6: short fixed length encoding (up to 2 octets) */
	/* X.691, #16.7: long fixed length encoding (up to 64K octets) */
	if(ct->effective_bits >= 0) {
		ASN_DEBUG("Encoding %d bytes (%ld), length in %d bits",
				st->size, sizeinunits - ct->lower_bound,
				ct->effective_bits);
		ret = per_put_few_bits(po, sizeinunits - ct->lower_bound,
				ct->effective_bits);
		if(ret) _ASN_ENCODE_FAILED;
        if(ct->range_bits < 8 && ct->range_bits > 0) 
        {
            for(i=0;i < sizeinunits;i++)
            {
                ret = per_put_few_bits(po, st->buf[i], unit_bits);
                if(ret) _ASN_ENCODE_FAILED;
            }
        }
        else
        {
		ret = per_put_many_bits(po, st->buf, sizeinunits * unit_bits);
		if(ret) _ASN_ENCODE_FAILED;
        }
		_ASN_ENCODED_OK(er);
	}

	ASN_DEBUG("Encoding %d bytes", st->size);

	if(sizeinunits == 0) {
		if(uper_put_length(po, 0))
			_ASN_ENCODE_FAILED;
		_ASN_ENCODED_OK(er);
	}

	buf = st->buf;
	while(sizeinunits) {
		ssize_t maySave = uper_put_length(po, sizeinunits);
		if(maySave < 0) _ASN_ENCODE_FAILED;

		ASN_DEBUG("Encoding %d of %d", maySave, sizeinunits);

		if(ct->range_bits < 8 && ct->range_bits > 0) 
		{
			for(i=0;i < sizeinunits;i++)
			{
				ret = per_put_few_bits(po, buf[i],unit_bits);
				if(ret) _ASN_ENCODE_FAILED;
			}
		}
		else
		{
			ret = per_put_many_bits(po, buf, maySave * unit_bits);
			if(ret) _ASN_ENCODE_FAILED;
		}
			if(unit_bits == 1)
				buf += maySave >> 3;
			else
				buf += maySave;
			sizeinunits -= maySave;
			assert(!(maySave & 0x07) || !sizeinunits);
	}

	_ASN_ENCODED_OK(er);
}

int
OCTET_STRING_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
	asn_app_consume_bytes_f *cb, void *app_key) {
	static const char *h2c = "0123456789ABCDEF";
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;
	char scratch[16 * 3 + 4];
	char *p = scratch;
	uint8_t *buf;
	uint8_t *end;
	size_t i;

	(void)td;	/* Unused argument */

	if(!st || !st->buf) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/*
	 * Dump the contents of the buffer in hexadecimal.
	 */
	buf = st->buf;
	end = buf + st->size;
	for(i = 0; buf < end; buf++, i++) {
		if(!(i % 16) && (i || st->size > 16)) {
			if(cb(scratch, p - scratch, app_key) < 0)
				return -1;
			_i_INDENT(1);
			p = scratch;
		}
		*p++ = h2c[(*buf >> 4) & 0x0F];
		*p++ = h2c[*buf & 0x0F];
		*p++ = 0x20;
	}

	if(p > scratch) {
		p--;	/* Remove the tail space */
		if(cb(scratch, p - scratch, app_key) < 0)
			return -1;
	}

	return 0;
}

int
OCTET_STRING_print_utf8(asn_TYPE_descriptor_t *td, const void *sptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	const OCTET_STRING_t *st = (const OCTET_STRING_t *)sptr;

	(void)td;	/* Unused argument */
	(void)ilevel;	/* Unused argument */

	if(st && st->buf) {
		return (cb(st->buf, st->size, app_key) < 0) ? -1 : 0;
	} else {
		return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;
	}
}

void
OCTET_STRING_free(asn_TYPE_descriptor_t *td, void *sptr, int contents_only) {
	OCTET_STRING_t *st = (OCTET_STRING_t *)sptr;
	asn_OCTET_STRING_specifics_t *specs = td->specifics
				? (asn_OCTET_STRING_specifics_t *)td->specifics
				: &asn_DEF_OCTET_STRING_specs;
	asn_struct_ctx_t *ctx = (asn_struct_ctx_t *)
					((char *)st + specs->ctx_offset);
	struct _stack *stck;

	if(!td || !st)
		return;

	ASN_DEBUG("Freeing %s as OCTET STRING", td->name);

	if(st->buf) {
		FREEMEM(st->buf);
	}

	/*
	 * Remove decode-time stack.
	 */
	stck = (struct _stack *)ctx->ptr;
	if(stck) {
		while(stck->tail) {
			struct _stack_el *sel = stck->tail;
			stck->tail = sel->prev;
			FREEMEM(sel);
		}
		FREEMEM(stck);
	}

	if(!contents_only) {
		FREEMEM(st);
	}
}

/*
 * Conversion routines.
 */
int
OCTET_STRING_fromBuf(OCTET_STRING_t *st, const char *str, int len) {
	void *buf;

	if(st == 0 || (str == 0 && len)) {
		errno = E_INVAL;
		return -1;
	}

	/*
	 * Clear the OCTET STRING.
	 */
	if(str == NULL) {
		FREEMEM(st->buf);
		st->buf = 0;
		st->size = 0;
		return 0;
	}

	/* Determine the original string size, if not explicitly given */
	if(len < 0)
		len = strlen(str);

	/* Allocate and fill the memory */
	buf = MALLOC(len + 1);
	if(buf == NULL)
		return -1;

	memcpy(buf, str, len);
	((uint8_t *)buf)[len] = '\0';	/* Couldn't use memcpy(len+1)! */
	FREEMEM(st->buf);
	st->buf = (uint8_t *)buf;
	st->size = len;

	return 0;
}

OCTET_STRING_t *
OCTET_STRING_new_fromBuf(asn_TYPE_descriptor_t *td, const char *str, int len) {
	asn_OCTET_STRING_specifics_t *specs = td->specifics
				? (asn_OCTET_STRING_specifics_t *)td->specifics
				: &asn_DEF_OCTET_STRING_specs;
	OCTET_STRING_t *st;

	st = (OCTET_STRING_t *)CALLOC(1, specs->struct_size);
	if(st && str && OCTET_STRING_fromBuf(st, str, len)) {
		FREEMEM(st);
		st = NULL;
	}

	return st;
}


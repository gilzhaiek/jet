/*
 * Copyright (c) 2007 Open Mobile Alliance Ltd.  All Rights Reserved. (http://www.openmobilealliance.org)
 */

#include "per_codec/asn_internal.h"

#include "codec/FQDN.h"

static int permitted_alphabet_table_1[256] = {
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	/*                  */
0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,	/*                  */
0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,	/*              -.  */
1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,	/* 0123456789       */
0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,	/*  ABCDEFGHIJKLMNO */
1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,	/* PQRSTUVWXYZ      */
0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,	/*  abcdefghijklmno */
1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,	/* pqrstuvwxyz      */
};

static char fqdn_alphabet_table[] = ".-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   
void  FQDN_convert_symbols_after_decode(FQDN_t *fqdn)
{
    uint8_t i=0;
    for(;i < fqdn->size;i++) 
    {
        uint8_t alphabet_index = (uint8_t)fqdn->buf[i];
        fqdn->buf[i] = fqdn_alphabet_table[alphabet_index];
    }
}

void  FQDN_convert_symbols_before_encode(FQDN_t *fqdn)
{
    uint8_t i=0;
    uint8_t j=0;
    for(;j < fqdn->size;j++) 
    {
        for(;i < sizeof(fqdn_alphabet_table);i++) 
        {
            if(fqdn->buf[j] == fqdn_alphabet_table[i])
            {
                fqdn->buf[j] = i;
                i=0;
                break;
            }
        }
    }
}

static int check_permitted_alphabet_1(const void *sptr) {
	int *table = permitted_alphabet_table_1;
	/* The underlying type is VisibleString */
	const VisibleString_t *st = (const VisibleString_t *)sptr;
	const uint8_t *ch = st->buf;
	const uint8_t *end = ch + st->size;
	
	for(; ch < end; ch++) {
		uint8_t cv = *ch;
		if(!table[cv]) return -1;
	}
	return 0;
}

int
FQDN_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
			asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	const VisibleString_t *st = (const VisibleString_t *)sptr;
	size_t size;
	
	if(!sptr) {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
	
	size = st->size;
	
	if((size >= 1 && size <= 255)
		 && !check_permitted_alphabet_1(st)) {
		/* Constraint check succeeded */
		return 0;
	} else {
		_ASN_CTFAIL(app_key, td, sptr,
			"%s: constraint failed (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}
}

/*
 * This type is implemented using VisibleString,
 * so here we adjust the DEF accordingly.
 */
static void
FQDN_1_inherit_TYPE_descriptor(asn_TYPE_descriptor_t *td) {
	td->free_struct    = asn_DEF_VisibleString.free_struct;
	td->print_struct   = asn_DEF_VisibleString.print_struct;
	td->uper_decoder   = asn_DEF_VisibleString.uper_decoder;
	td->uper_encoder   = asn_DEF_VisibleString.uper_encoder;
	if(!td->per_constraints)
		td->per_constraints = asn_DEF_VisibleString.per_constraints;
	td->elements       = asn_DEF_VisibleString.elements;
	td->elements_count = asn_DEF_VisibleString.elements_count;
	td->specifics      = asn_DEF_VisibleString.specifics;
}

void
FQDN_free(asn_TYPE_descriptor_t *td,
		void *struct_ptr, int contents_only) {
	FQDN_1_inherit_TYPE_descriptor(td);
	td->free_struct(td, struct_ptr, contents_only);
}

int
FQDN_print(asn_TYPE_descriptor_t *td, const void *struct_ptr,
		int ilevel, asn_app_consume_bytes_f *cb, void *app_key) {
	FQDN_1_inherit_TYPE_descriptor(td);
	return td->print_struct(td, struct_ptr, ilevel, cb, app_key);
}

asn_dec_rval_t
FQDN_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints, void **structure, asn_per_data_t *per_data) {
	FQDN_1_inherit_TYPE_descriptor(td);
	return td->uper_decoder(opt_codec_ctx, td, constraints, structure, per_data);
}

asn_enc_rval_t
FQDN_encode_uper(asn_TYPE_descriptor_t *td,
		asn_per_constraints_t *constraints,
		void *structure, asn_per_outp_t *per_out) {
	FQDN_1_inherit_TYPE_descriptor(td);
	return td->uper_encoder(td, constraints, structure, per_out);
}


static asn_per_constraints_t asn_PER_FQDN_constr_1 = {
	{ APC_UNCONSTRAINED,	-1, -1,  0,  0 },
    { APC_CONSTRAINED,     6,  8,  1,  255 }    /* (SIZE(1..255)) */
};
asn_TYPE_descriptor_t asn_DEF_FQDN = {
	"FQDN",
	FQDN_free,
	FQDN_print,
	FQDN_constraint,
	FQDN_decode_uper,
	FQDN_encode_uper,
	&asn_PER_FQDN_constr_1,
	0, 0,	/* No members */
	0	/* No specifics */
};


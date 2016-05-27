/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_internal.h"
#include "per_codec/constr_SEQUENCE.h"
#include "per_codec/per_ext_sup.h"

/*
 * Number of bytes left for this structure.
 * (ctx->left) indicates the number of bytes _transferred_ for the structure.
 * (size) contains the number of bytes in the buffer passed.
 */
#define	LEFT	((size<(size_t)ctx->left)?size:(size_t)ctx->left)

/*
 * If the subprocessor function returns with an indication that it wants
 * more data, it may well be a fatal decoding problem, because the
 * size is constrained by the <TLV>'s L, even if the buffer size allows
 * reading more data.
 * For example, consider the buffer containing the following TLVs:
 * <T:5><L:1><V> <T:6>...
 * The TLV length clearly indicates that one byte is expected in V, but
 * if the V processor returns with "want more data" even if the buffer
 * contains way more data than the V processor have seen.
 */
#define	SIZE_VIOLATION	(ctx->left >= 0 && (size_t)ctx->left <= size)

/*
 * This macro "eats" the part of the buffer which is definitely "consumed",
 * i.e. was correctly converted into local representation or rightfully skipped.
 */
#undef	ADVANCE
#define	ADVANCE(num_bytes)	do {		\
		size_t num = num_bytes;		\
		ptr = ((const char *)ptr) + num;\
		size -= num;			\
		if(ctx->left >= 0)		\
			ctx->left -= num;	\
		consumed_myself += num;		\
	} while(0)

/*
 * Switch to the next phase of parsing.
 */
#undef	NEXT_PHASE
#undef	PHASE_OUT
#define	NEXT_PHASE(ctx)	do {			\
		ctx->phase++;			\
		ctx->step = 0;			\
	} while(0)
#define	PHASE_OUT(ctx)	do { ctx->phase = 10; } while(0)

/*
 * Return a standardized complex structure.
 */
#undef	RETURN
#define	RETURN(_code)	do {			\
		rval.code = _code;		\
		rval.consumed = consumed_myself;\
		return rval;			\
	} while(0)

/*
 * Check whether we are inside the extensions group.
 */
#define	IN_EXTENSION_GROUP(specs, memb_idx)	\
	( ((memb_idx) > (specs)->ext_after)	\
	&&((memb_idx) < (specs)->ext_before))

int
SEQUENCE_print(asn_TYPE_descriptor_t *td, const void *sptr, int ilevel,
		asn_app_consume_bytes_f *cb, void *app_key) {
	int edx;
	int ret;

	if(!sptr) return (cb("<absent>", 8, app_key) < 0) ? -1 : 0;

	/* Dump preamble */
	if(cb(td->name, strlen(td->name), app_key) < 0
	|| cb(" ::= {", 6, app_key) < 0)
		return -1;

	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) {
				if(elm->optional) continue;
				/* Print <absent> line */
				/* Fall through */
			}
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		/* Indentation */
		_i_INDENT(1);

		/* Print the member's name and stuff */
		if(cb(elm->name, strlen(elm->name), app_key) < 0
		|| cb(": ", 2, app_key) < 0)
			return -1;

		/* Print the member itself */
		ret = elm->type->print_struct(elm->type, memb_ptr, ilevel + 1,
			cb, app_key);
		if(ret) return ret;
	}

	ilevel--;
	_i_INDENT(1);

	return (cb("}", 1, app_key) < 0) ? -1 : 0;
}

void
SEQUENCE_free(asn_TYPE_descriptor_t *td, void *sptr, int contents_only) {
	int edx;

	if(!td || !sptr)
		return;

	ASN_DEBUG("Freeing %s as SEQUENCE", td->name);

	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		void *memb_ptr;
		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(void **)((char *)sptr + elm->memb_offset);
			if(memb_ptr)
				ASN_STRUCT_FREE(*elm->type, memb_ptr);
		} else {
			memb_ptr = (void *)((char *)sptr + elm->memb_offset);
			ASN_STRUCT_FREE_CONTENTS_ONLY(*elm->type, memb_ptr);
		}
	}

	if(!contents_only) {
		FREEMEM(sptr);
	}
}

int
SEQUENCE_constraint(asn_TYPE_descriptor_t *td, const void *sptr,
		asn_app_constraint_failed_f *ctfailcb, void *app_key) {
	int edx;

	if(!sptr) {
		_ASN_CTFAIL(app_key, td,
			"%s: value not given (%s:%d)",
			td->name, __FILE__, __LINE__);
		return -1;
	}

	/*
	 * Iterate over structure members and check their validity.
	 */
	for(edx = 0; edx < td->elements_count; edx++) {
		asn_TYPE_member_t *elm = &td->elements[edx];
		const void *memb_ptr;

		if(elm->flags & ATF_POINTER) {
			memb_ptr = *(const void * const *)((const char *)sptr + elm->memb_offset);
			if(!memb_ptr) {
				if(elm->optional)
					continue;
				_ASN_CTFAIL(app_key, td,
				"%s: mandatory element %s absent (%s:%d)",
				td->name, elm->name, __FILE__, __LINE__);
				return -1;
			}
		} else {
			memb_ptr = (const void *)((const char *)sptr + elm->memb_offset);
		}

		if(elm->memb_constraints) {
			int ret = elm->memb_constraints(elm->type, memb_ptr,
				ctfailcb, app_key);
			if(ret) return ret;
		} else {
			int ret = elm->type->check_constraints(elm->type,
				memb_ptr, ctfailcb, app_key);
			if(ret) return ret;
			/*
			 * Cannot inherit it earlier:
			 * need to make sure we get the updated version.
			 */
			elm->memb_constraints = elm->type->check_constraints;
		}
	}

	return 0;
}

asn_dec_rval_t
SEQUENCE_decode_uper(asn_codec_ctx_t *opt_codec_ctx, asn_TYPE_descriptor_t *td,
    asn_per_constraints_t *constraints, void **sptr, asn_per_data_t *pd) {
	asn_SEQUENCE_specifics_t *specs = (asn_SEQUENCE_specifics_t *)td->specifics;
	void *st = *sptr;	/* Target structure. */
	int extpresent = 0;	/* Extension additions are present */
	uint8_t *opres;		/* Presence of optional root members */
	asn_per_data_t opmd;
	asn_dec_rval_t rv;
	int edx;
	int total_extension=0; //number of a present extension
	int repeat;

	(void)constraints;

    if(_ASN_STACK_OVERFLOW_CHECK(opt_codec_ctx))
        _ASN_DECODE_FAILED;

    if(!st) {
		st = *sptr = CALLOC(1, specs->struct_size);
		if(!st) _ASN_DECODE_FAILED;
	}

	ASN_DEBUG("Decoding %s as SEQUENCE (UPER)", td->name);

	/* Handle extensions */
	if(specs->ext_before >= 0) 
	{
		extpresent = per_get_few_bits(pd, 1);
		if(extpresent < 0) _ASN_DECODE_STARVED;
	}
	else
	{//new add from SUPL
		if(specs->ext_after >= 0) 
		{
			extpresent = per_get_few_bits(pd, 1);
			if(extpresent < 0) _ASN_DECODE_STARVED;
		}
	}

	/* Prepare a place and read-in the presence bitmap */
	//get optional presents field
	if(specs->roms_count) 
	{
		opres = (uint8_t *)MALLOC(((specs->roms_count + 7) >> 3) + 1);
		if(!opres) _ASN_DECODE_FAILED;
		/* Get the presence map */
		if(per_get_many_bits(pd, opres, 0, specs->roms_count)) 
		{
			FREEMEM(opres);
			_ASN_DECODE_STARVED;
		}
		opmd.buffer = opres;
		opmd.nboff = 0;
		opmd.nbits = specs->roms_count;
		ASN_DEBUG("Read in presence bitmap for %s of %d bits (%x..)", td->name, specs->roms_count, *opres);
	}
	else 
	{
		opres = 0;
		memset(&opmd, 0, sizeof opmd);
	}

	/*
	 * Get the sequence ROOT elements.
	 */
	//for(edx = 0; edx < ((specs->ext_before < 0) ? td->elements_count : specs->ext_before + 1); edx++) 
	for(edx = 0; edx < ((specs->ext_before < 0) ? td->elements_count : specs->ext_after + 1); edx++) 
	{
		asn_TYPE_member_t *elm = &td->elements[edx];
		void *memb_ptr;		/* Pointer to the member */
		void **memb_ptr2;	/* Pointer to that pointer */

		/* Fetch the pointer to this member */
		if(elm->flags & ATF_POINTER) 
		{
			memb_ptr2 = (void **)((char *)st + elm->memb_offset);
		} 
		else 
		{
			memb_ptr = (char *)st + elm->memb_offset;
			memb_ptr2 = &memb_ptr;
		}

		/* Deal with optionality */
		if(elm->optional) 
		{
			int present = per_get_few_bits(&opmd, 1);
			ASN_DEBUG("Member %s->%s is optional, p=%d (%d->%d)", td->name, elm->name, present, (int)opmd.nboff, (int)opmd.nbits);
			if(present == 0) {
				/* This element is not present */
				if(elm->default_value) 
				{
					/* Fill-in DEFAULT */
					if(elm->default_value(1, memb_ptr2)) 
					{
						FREEMEM(opres);
						_ASN_DECODE_FAILED;
					}
				}
				/* The member is just not present */
				continue;
			}
			/* Fall through */
		}

		/* Fetch the member from the stream */
		ASN_DEBUG("Decoding member %s in %s", elm->name, td->name);
		rv = elm->type->uper_decoder(opt_codec_ctx, elm->type, elm->per_constraints, memb_ptr2, pd);
		if(rv.code != RC_OK) 
		{
			ASN_DEBUG("Failed decode %s in %s", elm->name, td->name);
			FREEMEM(opres);
			return rv;
		}
	}

	/*
	 * Deal with extensions.
	 */
	if(!extpresent) 
	{
		for(edx = specs->roms_count; edx < specs->roms_count + specs->aoms_count; edx++) 
		{
			asn_TYPE_member_t *elm = &td->elements[edx];
			void *memb_ptr;		/* Pointer to the member */
			void **memb_ptr2;	/* Pointer to that pointer */

			if(!elm->default_value) continue;

			/* Fetch the pointer to this member */
			if(elm->flags & ATF_POINTER) 
			{
				memb_ptr2 = (void **)((char *)st + elm->memb_offset);
			} 
			else 
			{
				memb_ptr = (char *)st + elm->memb_offset;
				memb_ptr2 = &memb_ptr;
			}

			/* Set default value */
			if(elm->default_value(1, memb_ptr2)) 
			{
				FREEMEM(opres);
				_ASN_DECODE_FAILED;
			}
		}
	} 
	else
	{//process present extension
    /*    asn_TYPE_member_t *elm;
        void *memb_ptr;		// Pointer to the member 
        void **memb_ptr2;	// Pointer to that pointer 
        ssize_t ext_length=0;
        edx = per_get_few_bits(pd,8);
        edx+=specs->ext_after+1; //arrange index of element
        repeat=0;
		ext_length = uper_get_length(pd, -1, &repeat);

        //decoding extension
        
        elm = &td->elements[edx]; //get element

        // Fetch the pointer to this member 
        if(elm->flags & ATF_POINTER) 
        {
            memb_ptr2 = (void **)((char *)st + elm->memb_offset);
        } 
        else 
        {
            memb_ptr = (char *)st + elm->memb_offset;
            memb_ptr2 = &memb_ptr;
        }
        // Fetch the member from the stream 
        ASN_DEBUG("Decoding extension member %s in %s", elm->name, td->name);
        rv = elm->type->uper_decoder(opt_codec_ctx, elm->type, elm->per_constraints, memb_ptr2, pd);
        if(rv.code != RC_OK) 
        {
            ASN_DEBUG("Failed decode %s in %s", elm->name, td->name);
            FREEMEM(opres);
            return rv;
        }
    }*/
		
        uint32_t ext_presents=0;
		ssize_t ext_length=0;
        
		edx=specs->ext_after+1; //arrange index of element
		//read total extension
		total_extension = uper_get_nsnnwn(pd);
		ext_presents=per_get_few_bits(pd,total_extension+1);
		
		repeat=0;
		ext_length = uper_get_length(pd, -1, &repeat) ;
		//ext_presents
		while(total_extension>=0)//should be extended to use future extensions
		{
			if(ext_presents & (1<<total_extension))
			{
				//decoding extension
				asn_TYPE_member_t *elm;
				void *memb_ptr;		// Pointer to the member 
				void **memb_ptr2;	// Pointer to that pointer 
				elm = &td->elements[edx]; //get element
				
				// Fetch the pointer to this member 
				if(elm->flags & ATF_POINTER) 
				{
					memb_ptr2 = (void **)((char *)st + elm->memb_offset);
				} 
				else 
				{
					memb_ptr = (char *)st + elm->memb_offset;
					memb_ptr2 = &memb_ptr;
				}
				// Fetch the member from the stream 
				ASN_DEBUG("Decoding extension member %s in %s", elm->name, td->name);
				rv = elm->type->uper_decoder(opt_codec_ctx, elm->type, elm->per_constraints, memb_ptr2, pd);
				if(rv.code != RC_OK) 
				{
					ASN_DEBUG("Failed decode %s in %s", elm->name, td->name);
					FREEMEM(opres);
					return rv;
				}
			}
			edx++;
			total_extension--;
		}

	}

	rv.consumed = 0;
	rv.code = RC_OK;
	FREEMEM(opres);
	return rv;
}

asn_enc_rval_t 
SEQUENCE_encode_uper(asn_TYPE_descriptor_t *td,
					 asn_per_constraints_t *constraints, 
					 void *sptr, asn_per_outp_t *po) 
{
	asn_SEQUENCE_specifics_t *specs = (asn_SEQUENCE_specifics_t *)td->specifics;
	asn_enc_rval_t er;
	int edx;
	int i;
	int total_extension=0;
	//asn_per_outp_t old_po;
	uint32_t start_of_ext=0;

	(void)constraints;
	if(!sptr) _ASN_ENCODE_FAILED;
	er.encoded = 0;

	ASN_DEBUG("Encoding %s as SEQUENCE (UPER)", td->name);
	if(specs->ext_before >= 0) /* Extensions stop before this member is decoded*/
	{
		//_ASN_ENCODE_FAILED;	/* We don't encode extensions yet */
		//rsuvorov addition extension processing 
		//for(i=specs->ext_before-2; i>specs->ext_after; i--)
		for(i=specs->ext_after+1; i<specs->ext_before-1; i++)
		{
			asn_TYPE_member_t *elm;
			void **memb_ptr2;	/* Pointer to that pointer */
			elm = &td->elements[i];//get request element 
			memb_ptr2 = (void **)((char *)sptr + elm->memb_offset);
			if(*memb_ptr2 != 0)
			{//if there is extension then set a extension bit
				total_extension<<=1;
				total_extension++;
				//break; //exist from search extension cycle
			}
			else
			{
				total_extension<<=1;
			}

		}
		//set extension bit
		if(total_extension>0){ if(per_put_few_bits(po, 1, 1)) _ASN_ENCODE_FAILED;}
		else { if(per_put_few_bits(po, 0, 1)) _ASN_ENCODE_FAILED;}
	}
	else
	{
		if(specs->ext_after >= 0) 
		{
			if(per_put_few_bits(po, 0, 1))_ASN_ENCODE_FAILED;
		}
	}
	
	/* Encode a presence bitmap */
	//search all optional elements that presents in sequence and create optional bitmap
	for(i = 0; i < specs->roms_count; i++) 
	{
		asn_TYPE_member_t *elm;
		void *memb_ptr;		/* Pointer to the member */
		void **memb_ptr2;	/* Pointer to that pointer */
		int present;

		edx = specs->oms[i];//get optional member
		elm = &td->elements[edx];//get request element 

		/* Fetch the pointer to this member */
		//if element have optional flag
		if(elm->flags & ATF_POINTER) 
		{
			memb_ptr2 = (void **)((char *)sptr + elm->memb_offset);
			present = (*memb_ptr2 != 0);
		}
		else 
		{
			memb_ptr = (void *)((char *)sptr + elm->memb_offset);
			memb_ptr2 = &memb_ptr;
			present = 1;
		}

		/* Eliminate default values */
		if(present && elm->default_value && elm->default_value(0, memb_ptr2) == 1)
			present = 0;

		ASN_DEBUG("Element %s %s %s->%s is %s",elm->flags & ATF_POINTER ? "ptr" : "inline",
											   elm->default_value ? "def" : "wtv",
											   td->name, 
											   elm->name, 
											   present ? "present" : "absent");

		//add present bit to bit map
		if(per_put_few_bits(po, present, 1)) _ASN_ENCODE_FAILED;
	}

	/*
	 * Get the sequence ROOT elements.
	 */
	//go through the whole sequence without extension root
	for(edx=0; edx<((specs->ext_before < 0) ? td->elements_count : specs->ext_after + 1); edx++) 
	{
		asn_TYPE_member_t *elm = &td->elements[edx];
		void *memb_ptr;		/* Pointer to the member */
		void **memb_ptr2;	/* Pointer to that pointer */

		/* Fetch the pointer to this member */
		if(elm->flags & ATF_POINTER) 
		{//get pointer to present optional member 
			memb_ptr2 = (void **)((char *)sptr + elm->memb_offset);
			if(!*memb_ptr2) 
			{//check pointer to equality to null
				ASN_DEBUG("Element %s %d not present",elm->name, edx);
				if(elm->optional) continue; //if member is optional then continue else error
				/* Mandatory element is missing */
				_ASN_ENCODE_FAILED;
			}
		} 
		else //if member does not be optional then step to a next member  
		{
			memb_ptr = (void *)((char *)sptr + elm->memb_offset);
			memb_ptr2 = &memb_ptr;
		}

		/* Eliminate default values */
		if(elm->default_value && elm->default_value(0, memb_ptr2) == 1) continue;

		//encoding current value
		er = elm->type->uper_encoder(elm->type, elm->per_constraints,*memb_ptr2, po);
		//test error
		if(er.encoded == -1) return er;
	}
	if(total_extension==0) _ASN_ENCODED_OK(er);
	/////////////////////////////
	//encoding extension fields//
	/////////////////////////////
	//coding number of total extension is presented 
    //rsuvorov comment
    //per_put_few_bits(po, ((specs->ext_before-2) - (specs->ext_after)), 8);
	
	//record start position coded extension
    ////rsuvorov comment
	//buf_normalize(po);
    //rsuvorov comment
	//start_of_ext=po->flushed_bytes*8+po->nboff;

	//go through the extension root
	for(edx=specs->ext_after + 1; edx<specs->ext_before-1 ; edx++) 
	{
		asn_TYPE_member_t *elm = &td->elements[edx];
		void *memb_ptr;		/* Pointer to the member */
		void **memb_ptr2;	/* Pointer to that pointer */
		//int length_of_ext=0;
		/* Fetch the pointer to this member */
		memb_ptr2 = (void **)((char *)sptr + elm->memb_offset);
		if(!*memb_ptr2) 
		{//check pointer to equality to null
			ASN_DEBUG("Extension Element %s %d not present",elm->name, edx);
			if(elm->optional) continue; //if member is optional then continue else error
			/* Mandatory element is missing */
			_ASN_ENCODE_FAILED;
		}
        //coding number of extension is presented 
        per_put_few_bits(po, edx - specs->ext_after , 8);
        //record start position coded extension   
	    buf_normalize(po);
	    start_of_ext=po->flushed_bytes*8+po->nboff;

		//encoding current extension value
		er = elm->type->uper_encoder(elm->type, elm->per_constraints,*memb_ptr2, po);

		if(er.encoded == -1) return er;
        
        //shift extension data
        insert_data(po,start_of_ext);
	}

    //rsuvorov comment
	//insert_data(po,start_of_ext);

	_ASN_ENCODED_OK(er);
}


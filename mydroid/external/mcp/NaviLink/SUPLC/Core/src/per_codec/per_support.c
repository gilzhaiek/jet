/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */

#include "per_codec/asn_system.h"
#include "per_codec/asn_internal.h"
#include "per_codec/per_support.h"

/*
 * Extract a small number of bits (<= 31) from the specified PER data pointer.
 */
int32_t
per_get_few_bits(asn_per_data_t *pd, int nbits) 
{
	size_t off;	/* Next after last bit offset */
	uint32_t accum;
	const uint8_t *buf;

	if(nbits < 0 || pd->nboff + nbits > pd->nbits)
		return -1;

	ASN_DEBUG("[PER get %d bits from %p+%d bits]", nbits, pd->buffer, pd->nboff);

	/*
	 * Normalize position indicator.
	 */
	if(pd->nboff >= 8) 
	{
		pd->buffer += (pd->nboff >> 3);
		pd->nbits  -= (pd->nboff & ~0x07);
		pd->nboff  &= 0x07;
	}
	off = (pd->nboff += nbits);
	buf = pd->buffer;

	/*
	 * Extract specified number of bits.
	 */
	if(off <= 8)
		accum = nbits ? (buf[0]) >> (8 - off) : 0;
	else if(off <= 16)
		accum = ((buf[0] << 8) + buf[1]) >> (16 - off);
	else if(off <= 24)
		accum = ((buf[0] << 16) + (buf[1] << 8) + buf[2]) >> (24 - off);
	//else if(off <= 31) //rsuvorov
	else if(off <= 32)
		accum = ((buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + (buf[3])) >> (32 - off);
	//else if(nbits <= 31) //rsuvorov change
	//else if(nbits <= 32) //rsuvorov change
	else //rsuvorov change 
	{
		//asn_per_data_t tpd = *pd; //rsuvorov optimization
		/* Here are we with our 31-bits limit plus 1..7 bits offset. */
		//tpd.nboff -= nbits; //rsuvorov optimization
		//accum  = per_get_few_bits(&tpd, nbits - 24) << 24;//rsuvorov optimization
		//accum |= per_get_few_bits(&tpd, 24);//rsuvorov optimization

		pd->nboff-=nbits;//rsuvorov add optimization
		accum  = per_get_few_bits(pd, nbits - 24) << 24;//rsuvorov add optimization
		accum |= per_get_few_bits(pd, 24);//rsuvorov add optimization
	}//rsuvorov change 
	/* else {
		pd->nboff -= nbits;	// Oops, revert back 
		return -1;
	}*/
if(nbits<32)
	return (accum & (((uint32_t)1 << nbits) - 1));
else
	return accum;
}

/*
 * Extract a large number of bits from the specified PER data pointer.
 */
int
per_get_many_bits(asn_per_data_t *pd, uint8_t *dst, int alright, int nbits) {
	int32_t value;

	if(alright && (nbits & 7)) {
		/* Perform right alignment of a first few bits */
		value = per_get_few_bits(pd, nbits & 0x07);
		if(value < 0) return -1;
		*dst++ = value;	/* value is already right-aligned */
		nbits &= ~7;
	}

	while(nbits) {
		if(nbits >= 24) {
			value = per_get_few_bits(pd, 24);
			if(value < 0) return -1;
			*(dst++) = value >> 16;
			*(dst++) = value >> 8;
			*(dst++) = value;
			nbits -= 24;
		} else {
			value = per_get_few_bits(pd, nbits);
			if(value < 0) return -1;
			if(nbits & 7) {	/* implies left alignment */
				value <<= 8 - (nbits & 7),
				nbits += 8 - (nbits & 7);
				if(nbits > 24)
					*dst++ = value >> 24;
			}
			if(nbits > 16)
				*dst++ = value >> 16;
			if(nbits > 8)
				*dst++ = value >> 8;
			*dst++ = value;
			break;
		}
	}

	return 0;
}

/*
 * Get the length "n" from the stream.
 */
ssize_t
uper_get_length(asn_per_data_t *pd, int ebits, int *repeat) {
	ssize_t value;

	*repeat = 0;

	if(ebits >= 0) return per_get_few_bits(pd, ebits);

	value = per_get_few_bits(pd, 8);
	if(value < 0) return -1;
	if((value & 128) == 0)	/* #10.9.3.6 */
		return (value & 0x7F);
	if((value & 64) == 0) {	/* #10.9.3.7 */
		value = ((value & 63) << 8) | per_get_few_bits(pd, 8);
		if(value < 0) return -1;
		return value;
	}
	value &= 63;	/* this is "m" from X.691, #10.9.3.8 */
	if(value < 1 || value > 4)
		return -1;
	*repeat = 1;
	return (16384 * value);
}

/*
 * Get the normally small non-negative whole number.
 * X.691, #10.6
 */
ssize_t
uper_get_nsnnwn(asn_per_data_t *pd) {
	ssize_t value;

	value = per_get_few_bits(pd, 7);
	if(value & 64) {	/* implicit (value < 0) */
		value &= 63;
		value <<= 2;
		value |= per_get_few_bits(pd, 2);
		if(value & 128)	/* implicit (value < 0) */
			return -1;
		if(value == 0)
			return 0;
		if(value >= 3)
			return -1;
		value = per_get_few_bits(pd, 8 * value);
		return value;
	}

	return value;
}

/*
 * Put the normally small non-negative whole number.
 * X.691, #10.6
 */
int
uper_put_nsnnwn(asn_per_outp_t *po, int n) {
	int bytes;

	if(n <= 63) {
		if(n < 0) return -1;
		return per_put_few_bits(po, n, 7);
	}
	if(n < 256)
		bytes = 1;
	else if(n < 65536)
		bytes = 2;
	else if(n < 256 * 65536)
		bytes = 3;
	else
		return -1;	/* This is not a "normally small" value */
	if(per_put_few_bits(po, bytes, 8))
		return -1;

	return per_put_few_bits(po, n, 8 * bytes);
}


/*
 * Put a small number of bits (<= 31).
 */
//bits - write value 
//obits - number of bits will be wrote
int
per_put_few_bits(asn_per_outp_t *po, uint32_t bits, int obits) 
{
	size_t off;	/* Next after last bit offset */
	size_t omsk;	/* Existing last byte meaningful bits mask */
	uint8_t *buf;

	//if(obits <= 0 || obits >= 32) return obits ? -1 : 0; //rsuvorov change
	if(obits <= 0 || obits > 32) return obits ? -1 : 0; 

	ASN_DEBUG("[PER put %d bits to %p+%d bits]", obits, po->buffer, po->nboff);

	/*
	 * Normalize position indicator.
	 */
	if(po->nboff >= 8) //check byte align 
	{//arrange shift params
		po->buffer += (po->nboff >> 3);
		po->nbits  -= (po->nboff & ~0x07);
		po->nboff  &= 0x07;
	}

	/*
	 * Flush whole-bytes output, if necessary.
	 */
	if(po->nboff + obits > po->nbits) //check overflow inside buffer by wrote data
	{//overflow reached
		int complete_bytes = (po->buffer - po->tmpspace);
		//if(po->outper(po->buffer, complete_bytes, po->op_key) < 0) //rsuvorov change
		if(po->outper(&(po->tmpspace), complete_bytes, po->op_key) < 0) //rsuvorov change
             return -1;

		if(po->nboff) po->tmpspace[0] = po->buffer[0]; //add rest bits 

		po->buffer = po->tmpspace;//init point to start buffer
		//po->nbits = 8 * sizeof(po->tmpspace); //init count of bits in the buffer //rsuvorov change
		po->nbits = 8 * sizeof(po->tmpspace) - po->nboff; //arrange count of left bits in the buffer 

		po->flushed_bytes += complete_bytes;
	}

	/*
	 * Now, due to sizeof(tmpspace), we are guaranteed large enough space.
	 */
	buf = po->buffer;//get start position
	omsk = ~((1 << (8 - po->nboff)) - 1);//mask for empty bits that starts from buffer to byte bound
	off = (po->nboff += obits);//point to the end bit

	/* Clear data of debris before meaningful bits */
	//bits &= (((uint32_t)1 << obits) - 1);// put mask to meaningful bits //rsuvorov change   
	if(obits<32) 
		bits &= (((uint32_t)1 << obits) - 1);// put mask to meaningful bits  

	ASN_DEBUG("[PER out %d %u/%x (t=%d,o=%d) %x&%x=%x]", obits, 
														 bits, 
														 bits,
														 po->nboff - obits, 
														 off, 
														 buf[0],
														 omsk&0xff, 
														 buf[0] & omsk);
	if(off <= 8)	/* Completely within 1 byte */
		bits <<= (8 - off),
		buf[0] = (buf[0] & omsk) | bits;//put to the buffer data that smaller then 8 bit
	else if(off <= 16)
		bits <<= (16 - off),
		buf[0] = (buf[0] & omsk) | (bits >> 8),
		buf[1] = bits;
	else if(off <= 24)
		bits <<= (24 - off),
		buf[0] = (buf[0] & omsk) | (bits >> 16),
		buf[1] = bits >> 8,
		buf[2] = bits;
	//else if(off <= 31) //rsuvorov change
	else if(off <= 32) //rsuvorov change
		bits <<= (32 - off),
		buf[0] = (buf[0] & omsk) | (bits >> 24),
		buf[1] = bits >> 16,
		buf[2] = bits >> 8,
		buf[3] = bits;
	else {
		ASN_DEBUG("->[PER out split %d]", obits);
		//per_put_few_bits(po, bits >> 8, 24);////?????? //rsuvorov
		//per_put_few_bits(po, bits, obits - 24);		 //rsuvorov
		po->nboff -= obits;
		per_put_few_bits(po, bits >> 8, obits - 8);
		per_put_few_bits(po, bits & 0x000000ff, 8);
		ASN_DEBUG("<-[PER out split %d]", obits);
	}

	ASN_DEBUG("[PER out %u/%x => %02x buf+%d]",
		bits, bits, buf[0], po->buffer - po->tmpspace);

	return 0;
}


/*
 * Output a large number of bits.
 */
int
per_put_many_bits(asn_per_outp_t *po, const uint8_t *src, int nbits) {

	while(nbits) {
		uint32_t value;

		if(nbits >= 24) {
			value = (src[0] << 16) | (src[1] << 8) | src[2];
			src += 3;
			nbits -= 24;
			if(per_put_few_bits(po, value, 24))
				return -1;
		} else {
			value = src[0];
			if(nbits > 8)
				value = (value << 8) | src[1];
			if(nbits > 16)
				value = (value << 8) | src[2];
			if(nbits & 0x07)
				value >>= (8 - (nbits & 0x07));
			if(per_put_few_bits(po, value, nbits))
				return -1;
			break;
		}
	}

	return 0;
}

/*
 * Put the length "n" (or part of it) into the stream.
 */
ssize_t
uper_put_length(asn_per_outp_t *po, size_t length) {

	if(length <= 127)	/* #10.9.3.6 */
		return per_put_few_bits(po, length, 8)
			? -1 : (ssize_t)length;
	else if(length < 16384)	/* #10.9.3.7 */
		return per_put_few_bits(po, length|0x8000, 16)
			? -1 : (ssize_t)length;

	length >>= 14;
	if(length > 4) length = 4;

	return per_put_few_bits(po, 0xC0 | length, 8)
			? -1 : (ssize_t)(length << 14);
}

typedef struct enc_to_buf_arg 
{
	void *buffer;
	size_t left;
} enc_to_buf_arg;

/* 
	iabdulli function 
*/

int uper_put_length_of_open_type(asn_per_outp_t *po,size_t bits)
{
	size_t off;	/* Next after last bit offset */
	size_t omsk;	/* Existing last byte meaningful bits mask */
	uint8_t *buf;
	uint8_t remainderNonZero;
	uint32_t num_bytes_to_flush=0;
	uint32_t a,b,i=0;

	enc_to_buf_arg *arg = (enc_to_buf_arg *)po->op_key;
	uint8_t* bit_field = (uint8_t*)arg->buffer;
	/*
	* Normalize position indicator.
	*/
	if(po->nboff >= 8) {
		po->buffer += (po->nboff >> 3);
		po->nbits  -= (po->nboff & ~0x07);
		po->nboff  &= 0x07;
	}

	/*
	* Flush whole-bytes output, if necessary.
	*/
	if(po->nboff + 8 > po->nbits) {
		int complete_bytes = (po->buffer - po->tmpspace);
		// 		if(po->outper(po->buffer, complete_bytes, po->op_key) < 0)
		// 			return -1;
		if(po->outper(&(po->tmpspace), complete_bytes, po->op_key) < 0) //rsuvorov change
			return -1;
		if(po->nboff)
			po->tmpspace[0] = po->buffer[0];
		po->buffer = po->tmpspace;
		po->nbits = 8 * sizeof(po->tmpspace);
		po->flushed_bytes += complete_bytes;
	}

	/*
	* Now, due to sizeof(tmpspace), we are guaranteed large enough space.
	*/
	buf = po->buffer;
	omsk = ~((1 << (8 - po->nboff)) - 1);
	off = (po->nboff += 8);

	/* Clear data of debris before meaningful bits */
	bits &= (((uint32_t)1 << 8) - 1);

	if(off <= 8)	/* Completely within 1 byte */
		bits <<= (8 - off),
		buf[0] = (buf[0] & omsk) | bits;
	else 
	{
		bits <<= (16 - off),
		buf[0] = (buf[0] & omsk) | (bits >> 8),
		buf[1] = bits;
	}

	if(po->nboff >= 8) {
		po->buffer += (po->nboff >> 3);
		po->nbits  -= (po->nboff & ~0x07);
		po->nboff  &= 0x07;
	}
	
	a = ((8 * sizeof(po->tmpspace)) - po->nbits);
	b = 8;

	num_bytes_to_flush = (uint32_t)a/b;

	remainderNonZero = ((a-(b*num_bytes_to_flush)) > 0) ? 1 : 0;

	if (remainderNonZero == 1) 
	{
		num_bytes_to_flush++;
	}

	for(i=0; i<(num_bytes_to_flush);i++)
	{
		if(i == (num_bytes_to_flush-1))
		{
			bit_field[i] |= (po->tmpspace[i] & (0xff<<(8-po->nboff)));
			break;
		}
		bit_field[i] = po->tmpspace[i];
	}

	return 0;
}
/*
int uper_open_type_insert_length(asn_per_outp_t *old_po,asn_per_outp_t *cur_po)
{
	// Normalize old position indicator.
	if(old_po->nboff >= 8) //check byte align 
	{//arrange shift params
		old_po->buffer += (old_po->nboff >> 3);
		old_po->nbits  -= (old_po->nboff & ~0x07);
		old_po->nboff  &= 0x07;
	}

	// Normalize current position indicator.
	if(cur_po->nboff >= 8) //check byte align 
	{//arrange shift params
		cur_po->buffer += (cur_po->nboff >> 3);
		cur_po->nbits  -= (cur_po->nboff & ~0x07);
		cur_po->nboff  &= 0x07;
	}

	
///***************************output bytes are contained in cur_po buffer***********************************	
	int complete_bytes = (cur_po->buffer - cur_po->tmpspace); //total bytes to output 
	//output bytes
	if(cur_po->outper(&(cur_po->tmpspace), complete_bytes, cur_po->op_key) < 0) return -1;
	if(cur_po->nboff) cur_po->tmpspace[0] = cur_po->buffer[0]; //add rest bits to start of internal buffer
	cur_po->buffer = cur_po->tmpspace;//arrange point to start of internal buffer
	cur_po->nbits = 8 * sizeof(cur_po->tmpspace) - cur_po->nboff; //arrange count of left bits in the buffer 
	cur_po->flushed_bytes += complete_bytes; //arrange fullness of external buffer
///***************************end of output bytes are contained in cur_po buffer***********************************		
	//asn_per_outp_t temp_po; //preliminary buffer for copy data
	
	asn_per_data_t current_buf;
	enc_to_buf_arg *arg = (enc_to_buf_arg *)cur_po->op_key;
	
	current_buf.buffer=(arg->buffer)-(cur_po->flushed_bytes);	//arrange pointer to the start
	current_buf.nboff=cur_po->nboff;	//Bit offset to the meaningful bit 
	//current_buf.nbits=?????		//Number of bits in the stream 
	uint8_t swap_buf[32]; //swap buffer
	int alright=1;
	if(current_buf.nboff==8) alright=0;
	
	//get 32 bits from flow
	if(per_get_many_bits(current_buf, &swap_buf[0], alright, 32)==-1)return -1; 	
}
*/
int uper_insert_length(asn_per_outp_t *start_po,asn_per_outp_t *end_po)
{
	//1. Normalize start_po
	//2. Normalize end_po
	//3. Download accumulated data from end_po to main buffer
	//4. equate difference end_po - start_po 
	//5. determine size of length
	//6. load from main buffer needing number of bits for place the length value
	//7. write the length value to the buffer
	//8. load new portion data from buffer
	//9. write loaded before data to the buffer
return 0;
}


uint32_t uper_open_type_aligment(asn_per_outp_t *old_po,asn_per_outp_t *cur_po)
{
	enc_to_buf_arg *old_arg = (enc_to_buf_arg *)old_po->op_key;
	enc_to_buf_arg *cur_arg = (enc_to_buf_arg *)cur_po->op_key;

	// Normalize position indicator.
	//if(po->nboff >= 8) //check byte align 
	//{//arrange shift params
	//	po->buffer += (po->nboff >> 3);
	//	po->nbits  -= (po->nboff & ~0x07);
	//	po->nboff  &= 0x07;
	//}
	if(cur_po->nboff != 0)
	{
		uint32_t num_bits = 0;
		if(cur_po->nboff >= 8)	num_bits = cur_po->nboff & 0x07;

		if(per_put_few_bits(cur_po, 0x0, (8 - num_bits)) == -1)
			return 0;
	}

	if((uint8_t*)cur_arg->buffer == (uint8_t*)old_arg->buffer)
	{
		uint32_t num_bytes = 0;
		uint32_t remainderNonZero=0;
		uint32_t num_bits=0;
		//equate tail of buffer 
		num_bits = old_po->nbits - cur_po->nbits;//how many bits left from the end of buffers
		num_bytes = num_bits / 8;//equate total bytes
		remainderNonZero = ((num_bits-(8*num_bytes)) > 0) ? 1 : 0;
		if (remainderNonZero == 1) num_bytes++;

		return num_bytes;
	}
	else
	{
		uint32_t num_bytes;
		uint8_t* old_addr = (uint8_t*)old_arg->buffer;
		uint8_t* cur_addr = (uint8_t*)cur_arg->buffer;
		if(old_po->nbits != (8 * sizeof(old_po->tmpspace)))//process needed size 
		{//
			uint32_t occupied_bytes=0;
			occupied_bytes = sizeof(old_po->tmpspace) - (old_po->nbits / 8);
			 
			old_addr += occupied_bytes;
		}
		else
		{
			if(old_po->nboff) old_addr++;
		}

		if(cur_po->nbits != (8 * sizeof(cur_po->tmpspace)))
		{
			uint32_t occupied_bytes=0;
			occupied_bytes = sizeof(cur_po->tmpspace) - (cur_po->nbits / 8);

			cur_addr += occupied_bytes;
		}
		else
		{
			if(cur_po->nboff)
				cur_addr++;
		}

		num_bytes = cur_addr - old_addr;

		return num_bytes;
	}
}

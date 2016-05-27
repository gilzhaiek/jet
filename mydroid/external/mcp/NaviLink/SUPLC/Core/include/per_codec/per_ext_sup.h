/*-
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 Lev Walkin <vlm@lionet.info>
 * All rights reserved.
 */


#ifndef __PER_EXT_SUP_H__
#define __PER_EXT_SUP_H__


#ifdef __cplusplus
extern "C" {
#endif

#ifndef bool
	#define bool int
	#define true 1
	#define false 0
#endif

bool insert_data(asn_per_outp_t *write_po,int32_t ins_pos);
void buf_normalize(asn_per_outp_t *write_po);

#ifdef __cplusplus
}
#endif

#endif //__PER_EXT_SUP_H__

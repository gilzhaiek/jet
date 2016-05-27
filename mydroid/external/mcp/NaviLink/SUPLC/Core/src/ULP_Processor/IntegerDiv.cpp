/**
*  @file  ULP_Common.h
*  @brief ulp common routines - init,encode,decode.
*/

/***************************************************************************************************
====================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 

====================================================================================================
Revision History:
Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  --------------------------------------------
Ildar Abdullin				01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _INTEGER_DIV_H_
#define _INTEGER_DIV_H_

#include "ulp_processor/ULP_Common.h"

using namespace Codec;

int IntegerDiv(int32_t a, int32_t b, INTEGER_DIV_ROUNDING_T divType)
{
     uint32_t  resultNegative = 0;
     uint32_t  remainderNonZero;
     int32_t  result;
 
     if (a < 0)
     {
         a = -a;
         resultNegative = 1;
     }
 
     if (b < 0)
     {
         b = -b;
         resultNegative = (resultNegative == 1) ? 0 : 1;
     }
 
     result = (int)(a/b);
     
     remainderNonZero = ((a-(b*result)) > 0) ? 1 : 0;
 
     if (resultNegative == 1)
     {
         result = -result;
 
         if ( (divType == INTEGER_DIV_ROUNDING_FLOORING)
         && (remainderNonZero == 1) )
         {
             result--;
         }
     }
     else
     {
         if ( (divType == INTEGER_DIV_ROUNDING_CEILING)
         && (remainderNonZero == 1) )
         {
             result++;
         }
     }
     return result;
 }

#endif // _INTEGER_DIV_H_

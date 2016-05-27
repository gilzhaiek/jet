/*****< ARBTJBBTAS.h >*********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTAS - Stonestreet One Android Runtime Bluetooth JNI Bridge Type    */
/*               Definitions, Constants, and Prototypes for the               */
/*               BluetoothA2dpService module.                                 */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   10/12/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __ARBTJBBTASH__
#define __ARBTJBBTASH__

#ifdef HAVE_BLUETOOTH

namespace android
{

void BTAS_SignalBluetoothEnabled();
void BTAS_SignalBluetoothDisabled();

}

#endif /* HAVE_BLUETOOTH */

#endif /* __ARBTJBBTASH__ */


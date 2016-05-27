/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : Device.cpp                                        *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
*                                                                *
* MODULE SUMMARY : This module contains implementation of        *
* CIMSI class. This is a class for representing the IMSI data    *
* Module also contains implementation of IDevice class           *
* non-absract methods                                            *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - Initial version                             *
******************************************************************
*/
#include "android/ags_androidlog.h"
#include "device/ags_Device.h"
#include "device/ags_LinDevice.h"


namespace Platform {

IDevice::IDevice()
{

}

IDevice::~IDevice()
{

}

/*
*******************************************************************
* Function: NewDeviceIF
*
* Description : Create the IDevice instance
*
* Parameters : None
*
* Returns : Pointer to IDevice class object
*
*******************************************************************
*/
IDevice* IDevice::NewDeviceIF()
{
#if defined(_DEBUG_IMSI) || defined(_ANDROID_IMSI_)
    // create platform specific device
    return new(std::nothrow) CLinDevice;
#endif
}

/*
*******************************************************************
* Function: CIMSI
*
* Description : Constructor with arguments
*
* Parameters : 
* pData     r pointer to IMSI data buffer
* size      r size of valid data in buffer referenced by data
* Returns : None
*
*******************************************************************
*/
CIMSI::CIMSI(Engine::uint8_t* pData, Engine::uint32_t size)
{
#if defined(_DEBUG_IMSI)
    m_Size = IMSI_CODE_SIZE;
    memset(m_Imsi, 0, m_Size);
#elif defined(_ANDROID_IMSI_)
    m_pDecodedIMSI = NULL;
    assignIMSICode(pData, size);
#endif
}


#if defined(_DEBUG_IMSI)
/*
*******************************************************************
* Function: SetIMSI
*
* Description : Sets the IMSI code
*
* Parameters : 
* pData     r pointer to IMSI data.
* size      r size of valid data in buffer
* Returns : None
*
*******************************************************************
*/

void CIMSI::SetIMSI(Engine::uint8_t* pData, Engine::uint32_t size)
{
    memcpy(m_Imsi, pData, size);
    m_Size = size;
}
#endif

/*
*******************************************************************
* Function: getCodedIMSI
*
* Description : Creates the unique byte sequence based on IMSI code.
* This byte sequence can be used for creation the SUPL session id.
* Method stores data in buffer referenced by *lpBuf and keeps the size
* of coped data to memory referenced by size. Last two bytes are set 
* to 0xFF.
*
* Parameters : 
* *lpBuf     w valid pointer to buffer sufficient for data storing
* size     w pointer to size of data that will be stored
* Returns : None
*
*******************************************************************
*/

void CIMSI::getCodedIMSI(Engine::uint8_t** lpBuf, Engine::uint32_t* size)
{
    Engine::uint32_t nTmpSize = UNIQUE_OCTET - 2;
#if defined(_ANDROID_IMSI_)
    Engine::uint32_t    index = 0;
    Engine::bool_t bSwapNibble = 0; /* if 0 - 0, 2, 4, 6... in lower nibble
                                              1, 3, 5, 7... in upper nibble
                                       if 1 - 0, 2, 4, 6... in upper nibble
									          1, 3, 5, 7... in lower nibble */

    /*
     * Reference - OMA-TS-ULP-V1_0-20070615-A
     * msisdn, mnd and imsi are a BCD (Binary Coded Decimal) string
     * represent digits from 0 through 9,
     * two digits per octet, each digit encoded 0000 to 1001 (0 to 9)
     * bits 8765 of octet n encoding digit 2n
     * bits 4321 of octet n encoding digit 2(n-1) +1
     * not used digits in the string shall be filled with 1111
     */
	debugMessage("CIMSI::getCodedIMSI: bSwapNibble = %d", bSwapNibble);
    for (index = 0; index < UNIQUE_OCTET; index++)
    {
        if (0 == bSwapNibble)
        {
            //Set Lower Nibble
            (*lpBuf)[index] = (index * 2 < m_nDecodedSize) ? (m_szStrRep[index * 2] & 0xF) : 0x0F;
            //Set Upper Nibble
            (*lpBuf)[index] |= (index * 2 + 1 < m_nDecodedSize) ? ((m_szStrRep[index * 2 + 1] & 0xF) << 4) : 0xF0;
	    }
        else
		{
			//Set Lower Nibble
			(*lpBuf)[index] = (index * 2 + 1 < m_nDecodedSize) ? (m_szStrRep[index * 2 + 1] & 0xF) : 0x0F;
			//Set Upper Nibble
			(*lpBuf)[index] |= (index * 2  < m_nDecodedSize) ? ((m_szStrRep[index * 2] & 0xF) << 4) : 0xF0;
		}
	}

    *size = UNIQUE_OCTET;
    for(index = 0; index < *size; index++)
	{
		debugMessage("CIMSI::getCodedIMSI [%d]= Dec: %d hex: 0x%x", index, (*lpBuf)[index], (*lpBuf)[index]);
	}

#elif defined(_DEBUG_IMSI)
    memcpy(*lpBuf,m_Imsi,nTmpSize);
    (*lpBuf)[UNIQUE_OCTET - 2] = IMSI_END;
    (*lpBuf)[UNIQUE_OCTET - 1] = IMSI_END;
    *size = UNIQUE_OCTET;
#endif
}


#if defined(_ANDROID_IMSI_)
/*
*******************************************************************
* Function: assignIMSICode
*
* Description : Sets the IMSI code
*
* Parameters : 
* pData     r pointer to IMSI data.
* size      r size of valid data in buffer
* Returns : None
*
*******************************************************************
*/
void CIMSI::assignIMSICode(Engine::uint8_t* pData, Engine::uint32_t size)
{

    if (m_pDecodedIMSI)
    {
        delete[] m_pDecodedIMSI;
    }

    if (m_szStrRep)
    {
        delete[] m_szStrRep;
    }

    m_pDecodedIMSI = new Engine::uint8_t[size];
    m_szStrRep     = new Engine::char_t[size];
    m_nDecodedSize = size;
    for (Engine::uint32_t i = 0; i < size; i++)
    {
        m_pDecodedIMSI[i] = pData[i];
        sprintf(m_szStrRep + i, "%d", m_pDecodedIMSI[i]);
    }
}
#endif

#if defined(_DEBUG_IMSI)
/*
*******************************************************************
* Function: getMCC
*
* Description : Gets the MCC code
*
* Parameters :
* pchMcc    w buffer for storing the MCC code  
* Returns : None
*
*******************************************************************
*/
void CIMSI::getMCC(char* pchMcc)
{
    memcpy(pchMcc, m_Imsi, 3);
}
/*
*******************************************************************
* Function: getMNC
*
* Description : Gets the MNC code
*
* Parameters : None
* pchMnc     w buffer for storing the MNC code 
* Returns : None
*
*******************************************************************
*/
void CIMSI::getMNC(char* pchMnc)
{
    char* p = 0;
    p = ((char*)m_Imsi) + 3;
    memcpy(pchMnc, p, 2);
}
#elif defined(_ANDROID_IMSI_)
/*
*******************************************************************
* Function: getMCC
*
* Description : Gets the MCC code
*
* Parameters : None

* Returns : MCC code
*
*******************************************************************
*/
Engine::uint16_t CIMSI::getMCC()
{    
    if (m_pDecodedIMSI == NULL)
    {    
        return 0;
    }
    return (m_pDecodedIMSI[0] * 100 + 
            m_pDecodedIMSI[1] * 10 + 
            m_pDecodedIMSI[2]);
}

/*
*******************************************************************
* Function: getMNC
*
* Description : Gets the MNC code
*
* Parameters : None

* Returns : MNC code
*
*******************************************************************
*/
Engine::uint16_t CIMSI::getMNC()
{
    if (m_pDecodedIMSI == NULL)
        return 0;
        
    int nMccMncSize = getMccMncSize();
    int nMnc = 0;
    if (nMccMncSize < 3)
    {
        return 0;
    }
    
    for(int i = nMccMncSize - 1; i >= 3; i--)
    {
        if (i == nMccMncSize - 1)
        {
            nMnc += m_pDecodedIMSI[i];
        }
        else
        {
            nMnc += m_pDecodedIMSI[i] * 10 * (nMccMncSize - (i + 1));
        }
    }
    
    return nMnc;
}
#endif

CIMSI::CIMSI()
{
#if defined(_DEBUG_IMSI)
    m_Size = IMSI_CODE_SIZE;
    memset(m_Imsi, 0, m_Size);
#elif defined(_ANDROID_IMSI_)
    m_pDecodedIMSI = NULL;
    m_szStrRep     = NULL;
    m_nDecodedSize = 0;
#endif
}

CIMSI::~CIMSI()
{
#if defined(_ANDROID_IMSI_)
    if (m_pDecodedIMSI)
    {
        delete[] m_pDecodedIMSI;
    }
    
    if (m_szStrRep)
    {
        delete[] m_szStrRep;
    }
#endif
}

CMSISDN::CMSISDN()
{
	m_Size = 32;
	memset(m_Msisdn,0,m_Size);
	m_pDecodedMSISDN = NULL;
    m_szStrRep     = NULL;
    m_nDecodedSize = 0;
}
CMSISDN::~CMSISDN()
{
	if (m_pDecodedMSISDN)
    {
        delete[] m_pDecodedMSISDN;
    }
    if (m_szStrRep)
    {
        delete[] m_szStrRep;
    }
}
CMSISDN:: CMSISDN( uint8_t* pData, Engine :: uint32_t size)
{
	m_Size = 32;
    memset(m_Msisdn, 0, m_Size);
	m_pDecodedMSISDN = NULL;
	assignMSISDNCode(pData,size);
}
void CMSISDN::assignMSISDNCode( uint8_t* pData ,Engine :: uint32_t size)
{
    if (m_pDecodedMSISDN)
    {
        delete[] m_pDecodedMSISDN;
    }
    if (m_szStrRep)
    {
        delete[] m_szStrRep;
    }
    m_pDecodedMSISDN = new Engine::uint8_t[size];
    m_szStrRep     = new Engine::char_t[size];
    m_nDecodedSize = size;
    for (Engine::uint32_t i = 0; i < size; i++)
    {
        m_pDecodedMSISDN[i] = pData[i];
        sprintf(m_szStrRep + i, "%d", m_pDecodedMSISDN[i]);
    }
}
/*
*******************************************************************
* Function: getCodedMSISDN
*
* Description : Sets the MSISDN code
*
* Parameters : 
* Parameters : 
* *buf     w valid pointer to buffer sufficient for data storing
* size     w pointer to size of data that will be stored
* Returns : None
*
*******************************************************************
*/
void CMSISDN::getCodedMSISDN( uint8_t **buf,Engine :: uint32_t *size)
{
    Engine::uint32_t nTmpSize = UNIQUE_OCTET - 2;
    Engine::uint32_t    index = 0;
    Engine::bool_t bSwapNibble = 0; /* if 0 - 0, 2, 4, 6... in lower nibble
                                              1, 3, 5, 7... in upper nibble
                                       if 1 - 0, 2, 4, 6... in upper nibble
									          1, 3, 5, 7... in lower nibble */
    LOGD("\n----Entering getCodedMSISDN----\n");
    /*
     * Reference - OMA-TS-ULP-V1_0-20070615-A
     * msisdn, mnd and imsi are a BCD (Binary Coded Decimal) string
     * represent digits from 0 through 9,
     * two digits per octet, each digit encoded 0000 to 1001 (0 to 9)
     * bits 8765 of octet n encoding digit 2n
     * bits 4321 of octet n encoding digit 2(n-1) +1
     * not used digits in the string shall be filled with 1111
     */
    debugMessage("CIMSI::getCodedMSISND: bSwapNibble = %d", bSwapNibble);
    for (index = 0; index < UNIQUE_OCTET; index++)
    {
        if (0 == bSwapNibble)
        {
            //Set Lower Nibble
            (*buf)[index] = (index * 2 < m_nDecodedSize) ? (m_szStrRep[index * 2] & 0xF) : 0x0F;
            //Set Upper Nibble
            (*buf)[index] |= (index * 2 + 1 < m_nDecodedSize) ? ((m_szStrRep[index * 2 + 1] & 0xF) << 4) : 0xF0;
	    }
        else
		{
			//Set Lower Nibble
			(*buf)[index] = (index * 2 + 1 < m_nDecodedSize) ? (m_szStrRep[index * 2 + 1] & 0xF) : 0x0F;
			//Set Upper Nibble
			(*buf)[index] |= (index * 2  < m_nDecodedSize) ? ((m_szStrRep[index * 2] & 0xF) << 4) : 0xF0;
		}
	}

    *size = UNIQUE_OCTET;
    for(index = 0; index < *size; index++)
    {
        debugMessage("CIMSI::getCodedMSISND [%d]= Dec: %d hex: 0x%x", index, (*buf)[index], (*buf)[index]);
    }


#if 0
	Engine::uint32_t nTmpSize = UNIQUE_OCTET - 2;
	if (m_nDecodedSize >= nTmpSize) 
	{
		memcpy(*buf, m_szStrRep + m_nDecodedSize - nTmpSize, nTmpSize);
	}
	else 
	{
		memset(*buf, IMSI_PAD, UNIQUE_OCTET);
		memcpy(*buf, m_szStrRep, m_nDecodedSize);    
	}
	(*buf)[UNIQUE_OCTET - 2] = IMSI_END;
	(*buf)[UNIQUE_OCTET - 1] = IMSI_END;
	*size = UNIQUE_OCTET;
	for(int i = 0; i < UNIQUE_OCTET; i++)
	{
		LOGD("CMSDN::getCodedMSISDN [%d]= 0x%x", i,  (*buf)[i]);
	}
#endif

}
} /* end of namespace Platform */


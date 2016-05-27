/**
*  @file  IGPS.h
*  @brief IGPS declaration.
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
Alexander V. Morozov         07.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "gps/GPS.h"
#include "gps/GPSCommand.h"

#if defined(_LINUX_)
#include "gps/LinGPS.h"
#endif // _LINUX_


namespace Platform {

IGPS* IGPS::NewGpsIF()
{
#if defined(_LINUX_)
	return new(std::nothrow) CLinGPS;
#endif // _LINUX_
}

IGPS::IGPS()
{

}

IGPS::~IGPS()
{
	CGPSCommand* command;

	if(m_CmdMap.size() != 0)
	{
		CommandIter iter;
		//for(OutputCommandMap::iterator iter=m_CmdMap.begin(); iter != m_CmdMap.end(); iter++)
		for(unsigned int i = 0; i < m_CmdMap.size(); ++i) //Android STL
		{
			iter = m_CmdMap.valueAt(i); //Android STL
			//command = iter->second; 
			command = iter; //Android STL
			delete command;
			//m_CmdMap.erase(iter);
			m_CmdMap.removeItem(m_CmdMap.keyAt(i));
		}
	}
}

CGPSCommand* IGPS::GetCommand()
{
	CGPSCommand* cmd = NULL;
	Engine::uint32_t app_id;
	Lock();
	
	//if(!m_CmdMap.empty())
	if(!m_CmdMap.isEmpty()) //Android STL
	{
		//app_id = m_CmdMap.begin()->first;
		//cmd = m_CmdMap.begin()->second;
		//if (cmd != NULL) 
		//{
		//	m_CmdMap.erase(app_id);
		//}
		app_id = m_CmdMap.keyAt(0); // Android STL
		cmd = m_CmdMap.valueAt(0); // Android STL
		if (cmd != NULL) 
		{
			//m_CmdMap.erase(app_id);
			m_CmdMap.removeItem(app_id); //Android STL
		}
	}
	else
		cmd = NULL;
	Unlock();
	return cmd;
}

Engine::bool_t IGPS::AddCommand(CGPSCommand* cmd)
{
	int index = -1; //Android STL
	if ((cmd->GetCmdCode() & 0x8000) == 0)
	{
		Lock();
		//m_CmdMap[cmd->GetAppID()] = cmd;
		m_CmdMap.add(cmd->GetAppID(), cmd); //Android STL
		Unlock();
	}
	else
	{	
		return FALSE;
	}
	
	return TRUE;
}

Engine::bool_t IGPS::RemoveAllCmd()
{
	CGPSCommand* cmd = NULL;
	Engine::uint32_t app_id;	
	
	Lock();

	for(int index=0;index<m_CmdMap.size();index++)
	{
		app_id = m_CmdMap.keyAt(index); // Android STL
		cmd = m_CmdMap.valueAt(index); // Android STL
		if (cmd != NULL) 
		{			
			m_CmdMap.removeItem(app_id); //Android STL
			delete cmd;
			cmd=NULL;
		}
	}
	
	Unlock();	
	return TRUE;
}
Engine::bool_t IGPS::DataReady()
{
	Engine::bool_t empty = FALSE;
	Lock();
	//if(m_StartCommands.empty() && m_CmdMap.empty())
	//if(m_CmdMap.empty())
	if(m_CmdMap.isEmpty()) //Android STL
		empty = TRUE;
	else
		empty = FALSE;
	
	Unlock();
	return !empty;
}

}

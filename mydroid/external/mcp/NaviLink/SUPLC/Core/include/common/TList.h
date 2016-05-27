/*================================================================================================*/
/**
   @file   TList.h

   @brief TList implementation (declaration)
*/
/*==================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 
     
====================================================================================================
Revision History:
                            Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  -------------------------------------------
Roman Suvorov                26.10.2007	                 initial version
====================================================================================================
Portability:  GCC  compiler
==================================================================================================*/

#ifndef __TLIST_H__
#define __TLIST_H__

#ifdef Debug
#include <stdio.h>
#endif

#include "types.h"


namespace Engine {

#define DEFAULT_LIST_CAPACITY 16

/*==================================================================================================
GLOBAL CONSTANTS
===================================================================================================*/
/*==================================================================================================
ENUMS
==================================================================================================*/
/*==================================================================================================
CLASS DECLARATIONS
==================================================================================================*/
class TElement
{
private:
	TElement *iNext; //pointer to next element
public:
	virtual ~TElement(){
#ifdef Debug
		printf("TElement destructor\n");
#endif
	};


	TElement *GetNextElement(void) {return iNext;}
	bool_t IsEnd(){if(iNext==0) return true; else return false;}; //method return signal to end element 
	void AddNextElement(TElement *aElement) {iNext=aElement;};
	void ChangeNextElem(TElement *aElement){iNext=aElement;}
protected:
	TElement() { iNext=0;}
};

//class TList: public TElement
class TList
{
private:
	TElement *iFirstElement;    //pointer to first element 
	TElement *iCurrentElement;  //pointer to current element
	
	void Init(TElement *aElement);
	bool_t CreateNewElement();
	int iCount; //Number elements in the list
public:
	~TList();
	void Add(TElement *aElement);
	bool_t Delete(TElement *aElement);
	TElement *GetElement(void);
	TElement *NextElement(void);
	void SetToFirstElement(void);
	TElement *GetLastElement();
protected:

	TList();
	TList(TElement *aElement);
};



template<class T>
class CQueue
{
public:
	 CQueue()
	 {
		m_pBegin = m_pEnd = 0;
	 }
	~CQueue()
	{
		Clean();
	}
	
	void PushItem(T data)
	{
		Elem* tmp;
		
		tmp = new Elem();
		tmp->data = data;
		tmp->m_pPrevious = NULL;
		if (m_pBegin == NULL)
		{			
			m_pBegin = tmp;
			m_pEnd = m_pBegin;			
		}
		m_pBegin->m_pPrevious = tmp;
		m_pBegin = tmp;
	}

	T	PopItem()
	{
		Elem* tmp;
		T data;
		tmp = m_pEnd;
		if (m_pEnd == m_pBegin)
		{
			m_pBegin = m_pEnd = NULL;
		}
		else
		{
			m_pEnd = m_pEnd->m_pPrevious;
		}
		data = tmp->data;
		delete tmp;
		return data;
	}

	bool_t IsEmpty()
	{
		return (m_pBegin == NULL);
	}

	bool_t Contains(const T& elm)
	{
		return (Serch(elm) == NULL);
	}

	void Clean()
	{
		while(m_pEnd != NULL)
		{
			Elem* tmp = m_pEnd->m_pPrevious;
			delete m_pEnd;
			m_pEnd = tmp;
		}		
	}

private:
	struct Elem
	{
		T data;
		Elem* m_pPrevious;		

	} *m_pEnd, *m_pBegin;

	Elem* Serch(const T& elm)
	{
		Elem* tmp = m_pEnd;
		while(tmp != NULL)
		{
			if(tmp->data == elm)
			{
				return tmp;
			}
			tmp = tmp->m_pPrevious;
		}
		return NULL;
	}
};

/**
 * @breif CList this class represent list
 *
 */

template<class Data>
class CList
{
public:
	CList():m_Size(0), m_Capacity(DEFAULT_LIST_CAPACITY)
	{
		m_pDataStore = new Data[m_Capacity];		
	}

	CList(uint32_t size):m_Size(size), m_Capacity(size + DEFAULT_LIST_CAPACITY)
	{
		m_pDataStore = new Data[m_Capacity];		
	}

	CList(const CList<Data>& cpy)
	{
		Copy(cpy);
	}

	~CList()
	{
		Clean();
	}

	CList& operator = (const CList<Data>& cpy)
	{
		if (&cpy == this)
		{
			return *this;
		}
		
		Clean();

		Copy(cpy);
	}
private:
	void Copy(const CList<Data>& cpy)
	{
		m_Capacity = cpy.m_Capacity;
		m_Size = cpy.m_Size;		
		m_pDataStore = new Data[m_Capacity];
		for (uint32_t i = 0; i < m_Capacity; i++)
		{
			m_pDataStore[i] = cpy.m_pDataStore[i];
		}
	}
public:
	/**
	 * @breif Get size of list
	 */
	uint32_t	GetSize() const
	{
		return m_Size;
	}

	/**
	 * @breif Add item to the end of the list
	 */
	void	AddItem(const Data& data)
	{
		if (++m_Size > m_Capacity)
		{
			ExtendStorage();
		}
		// copy value
		m_pDataStore[m_Size - 1] = data;
	}

	void	RemoveItem(uint32_t index)
	{
		delete m_pDataStore[index];

		for (uint32_t i = index; i < m_Size - 1; i++)
		{
			m_pDataStore[i] = m_pDataStore[i + 1];
		}
	}

	void	RemoveItem(const Data& data)
	{	
		for (uint32_t i = 0; i < m_Size; i++)
		{
			if (data == m_pDataStore[i])
			{
				RemoveItem(i);
				return;
			}
		}
	}

public:
	Data operator[] (uint32_t index)
	{
		if (index >= m_Size)
		{
			throw; 
		}
		return m_pDataStore[index];
	}

private:
	Data*	m_pDataStore;
	uint32_t	m_Capacity;
	uint32_t	m_Size;

private:
	void ExtendStorage()
	{
		uint32_t	tmp = m_Capacity;
		Data*	new_storage = NULL;

		m_Capacity += DEFAULT_LIST_CAPACITY;		
		new_storage = new Data[m_Capacity];

		for (uint32_t i = 0; i < tmp; i++)
		{
			new_storage[i] = m_pDataStore[i];	
		}
		Clean();
	}

	void Clean()
	{
		delete[] m_pDataStore;	
	}
};


} // namespace Engine

#endif //__TLIST_H__

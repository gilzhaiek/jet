/*================================================================================================*/
/**
   @file   TList.cpp

   @brief TList implementation 
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
#include "common/TList.h" 

namespace Engine {

//default constructor
TList::TList()
{
	Init(0);
}

//constructor with first element
TList::TList(TElement *aElement)
{
	Init(aElement);
}
//Constructor resolver, define value of first list element equal null or pointer 
//aElement - pointer to first list element
void TList::Init(TElement *aElement)
{
	if(aElement==0) 
	{
		iCurrentElement=0;
		iFirstElement=0;
		iCount=0;
	}
	else
	{
		iFirstElement=aElement;
		iCurrentElement=iFirstElement;
		iCount=1;
	}
}
//destructor
TList::~TList()
{
	TElement *element;
	do {
		SetToFirstElement();
		element=GetElement();
		if(element==0) break;
		Delete(element);
	}while(element!=0);
}
//return last element from the list 
//return pointer to the element
TElement *TList::GetLastElement()
{
	TElement *CurElem=iFirstElement; //current element pointer
	if(CurElem==0) return 0;
	while(CurElem->IsEnd()!=TRUE)
	{
		CurElem=(TElement *)CurElem->GetNextElement();
	}
	return CurElem;
}

//add element to the list
//aElement - pointer to new list element
void TList::Add(TElement *aElement)
{
	if(iCount==0) 
	{
		iFirstElement=aElement; 
		iCurrentElement=iFirstElement;
	}
	else
	{
		TElement *ptr;
		ptr=GetLastElement();
		ptr->AddNextElement(aElement);
	}
	++iCount;
#ifdef Debug
	printf("ADD Element %x\n",aElement);
#endif
}
//delete element from the list
//aElement - pointer to deleted list element
//return true - successful operation 
bool_t TList::Delete(TElement *aElement)
{
	TElement *CurElem; 
	TElement *PrevElem;
	PrevElem=0; //previous element equal null
	
	CurElem=iFirstElement;
	while(CurElem!=aElement)
	{
		if(CurElem==0) return false; //element not found
		PrevElem=CurElem;
		CurElem=CurElem->GetNextElement(); //get next element for the list
	}

	if(CurElem==iFirstElement) 
	{
		iFirstElement=iFirstElement->GetNextElement();
	}
	else
	{	
		if(CurElem==0) return false; //element not found
		PrevElem->ChangeNextElem(CurElem->GetNextElement()); //change pointer to element in the list//меняем указатель в списке
	}
#ifdef Debug
	printf("DELETE %x\n",CurElem);
#endif
	delete CurElem; 
	--iCount;
	return true;
}

//return current element from the list
//return pointer to the element
TElement *TList::GetElement(void)
{
	return iCurrentElement;
}
//pass to next element form list and return pointer for one
//return pointer to the element
TElement *TList::NextElement(void)      
{
	if(iCurrentElement->IsEnd()==TRUE) return 0;
	iCurrentElement=(TElement *)iCurrentElement->GetNextElement();
	return iCurrentElement;
}
//Init list to the start element of list
//return none
void TList::SetToFirstElement(void)
{
	iCurrentElement=iFirstElement;
}

}

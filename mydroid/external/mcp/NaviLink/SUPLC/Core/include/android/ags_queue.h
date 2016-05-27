/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : ags_queue.h                                       *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov                                  *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : Implements Queue container template           *
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Porting to Android            *
******************************************************************
*/

#ifndef __AGS_QUEUE_H__
#define __AGS_QUEUE_H__
#include "utils/List.h"

using namespace android;
namespace android 
{

template <class T> class Queue
{
public:
    typedef android::List<T> _List;
    typedef typename _List::iterator iterator;
    typedef typename _List::const_iterator const_iterator;

    Queue(void)
    {
        prep();
    }

    virtual ~Queue(void)
    {
        clear();
        delete mpQeue;
    }

    /*
    * Returns the first element of queue. 
    */
    iterator begin()
    {
        return mpQeue->begin();
    }
    const_iterator begin() const
    {
        return mpQeue->begin();
    }
    /*
    * Returns iterator poiting to space after the last element of queue.
    */
    iterator end()
    {
        return mpQeue->end();
    }
    const_iterator end() const
    {
        return mpQeue->end();
    }

     /*
     * Returns the last element of queue. 
     */
    T& back()
    {
        iterator iter = mpQeue->end();
        return *(--iter);
    }

    /*
    * Returns the first element of queue. 
    */
    T& front()
    {
        return *(mpQeue->begin());
    }
    
    /*
    * Removes the first element of queue. 
    */
    void pop()
    {
        if (!mpQeue->empty())
        {
            mpQeue->erase(mpQeue->begin());
        }
    }
    
    /*
    * Adds element 'val' to end of queue. 
    */
    void push(const T& val)
    {
        mpQeue->push_back(val);
    }

    /*
    * removes element pointed by  'posn'; returns iterator to next element
    */    
    iterator erase(iterator posn)
    {
        return mpQeue->erase(posn);
    }
    
    /*
    * returns true if the queue is empty 
    */
    bool empty(void) const
    {
        return mpQeue->empty();
    }

    /*
    * removes all content from the queue
    */
    void clear(void)
    {
        mpQeue->clear();
    }
    /*
    * returns number of elements in queue
    */
    unsigned int size(void) const
    {
        return mpQeue->size();
    }

private:
    void prep(void) {
        mpQeue = new _List;
    }
    /*
    * This node plays the role of "pointer to list" 
    */
    _List*      mpQeue;
};

}; // namespace android

#endif // __AGS_QUEUE_H__

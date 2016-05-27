
#include <utils/Log.h>

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <time.h>
#include <semaphore.h>
#include "suplcontroller/supl_timer.h"
#include "ti_client_wrapper/suplc_hal_os.h"

timer_t timerid;
sigset_t mask;	  
struct sigaction sa;	
struct sigevent sev;	
struct itimerspec its;	
struct itimerspec oitval; 
SEM_HANDLE gp_semHandle;


int supl_timer_start()
{

    /* Establish handler for timer signal */    

//	LOGD("entered supl_timer_start");    
 	//start timer
	//sa.sa_flags = SA_SIGINFO;    
	sa.sa_sigaction = (void (*) (int, siginfo_t*, void *))supl_timer_handler;    
	sa.sa_handler = supl_timer_handler;    

    	sigemptyset(&sa.sa_mask);
    	if (sigaction(SIGRTMAX-1, &sa, NULL) == -1)
       	LOGD("error sigaction failed");

	sev.sigev_notify = SIGEV_SIGNAL;    
	sev.sigev_signo = SIGRTMAX-1;    
	sev.sigev_value.sival_ptr = &timerid;    
    /* Create the timer */    
	if (timer_create(CLOCK_REALTIME, &sev, &timerid) == -1)        
		LOGD("error timer_create failed");    

	its.it_value.tv_sec = 10 + 1 /* NW delay */;    
	its.it_value.tv_nsec = 0;    
	its.it_interval.tv_sec = 0;     
	its.it_interval.tv_nsec = 0;  

	/* Start the timer */    
	if (timer_settime(timerid, 0, &its, &oitval) == -1)         
		LOGD("error timer_settime failed");

//	LOGD("timer created, ID is 0x%lx\n", (long) timerid);    
return 0;
}


int supl_timer_stop()
{
//	LOGD("entered supl_timer_stop");
	int ret;
	ret = timer_delete(timerid);	
	return ret;

}


void supl_timer_handler(int a_sNo)
	{
//	LOGD("entered supl_timer_handler %d", a_sNo);	
    if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
    {
        LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
    }	

	int ret=supl_timer_stop();
	if(ret == -1)
		LOGD("supl_timer_handler, Stop timer failed");
	
	}



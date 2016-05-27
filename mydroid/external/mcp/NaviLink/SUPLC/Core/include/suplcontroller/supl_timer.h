#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <time.h>
#include <semaphore.h>

#define CLOCKID CLOCK_REALTIME
#define SIG SIGRTMIN

//supl timer values
#define SUPL_TIMER_TVSEC		10
#define SUPL_TIMER_TVNSEC		0
#define SUPL_TIMER_INT_TVSEC 	10
#define SUPL_TIMER_INT_TVNSEC 	0
#ifdef MCP_LOGD
#define LOGD ALOGD
#else
#define LOGD
#endif
//supl timer handlers
void supl_timer_handler(int);    
int supl_timer_stop();   
int supl_timer_start();   





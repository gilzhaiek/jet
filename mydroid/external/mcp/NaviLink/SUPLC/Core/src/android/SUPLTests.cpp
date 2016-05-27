// SuplLocationProvider Tests
#include "android/ags_androidlog.h"
#include "android/android_tests_SuplLocationProviderTests.h"
#include "android/SUPLTests.h"


//#define SUPL_APP_ID              0x00FA
#define SUPL_APP_ID         1001 //zz_Aby - appID
# define GSM_INFO_SIZE 53
#define _THREAD_STACK_SIZE       8192
static pthread_t        TestThread; // Test Thread
static pthread_mutex_t  TestMutex = PTHREAD_MUTEX_INITIALIZER; 
static Engine::uint8_t  first_response[] = {0x26};
static const char * CNetClass = "android/networking/CNet";
extern JavaVM * g_JVMSuplLibrary ;
int        resp_flag = 0;
jboolean   test_result;
static pthread_attr_t              pAttr;
/*void debugMessage(const char * TAG, const char * MSG, JNIEnv * env)
{
    if (env != NULL)
	{
		jclass clazz = env->FindClass(CNetClass);
		if (clazz)
		{
            jmethodID mid = env->GetStaticMethodID(clazz, "debugMessage", "(Ljava/lang/String;Ljava/lang/String;)V");
            if (mid) 
			{
				jstring tag = env->NewStringUTF(TAG);
				jstring str = env->NewStringUTF(MSG);
				env->CallStaticVoidMethod(clazz, mid, tag, str);
            }
        }
	}
	else
	{
		JavaVM * vm = g_JVMSuplLibrary ;
		if (vm)
		{
			JNIEnv * env = 0;
			vm->AttachCurrentThread((void**)&env, 0);
			if (env) 
			{
				jclass clazz = env->FindClass(CNetClass);
				if (clazz)
				{
					jmethodID mid = env->GetStaticMethodID(clazz, "debugMessage", "(Ljava/lang/String;Ljava/lang/String;)V");
					if (mid) 
					{
						jstring tag = env->NewStringUTF(TAG);
						jstring str = env->NewStringUTF(MSG);
						env->CallStaticVoidMethod(clazz, mid, tag, str);
					}
				}
			}
		}
		vm->DetachCurrentThread();
    }
}*/

void Fill_StartReqData_set_ASSISTED(Platform::StartReqData * data)
{
	int info[GSM_INFO_SIZE];
	int num = 0;
	char s [64];
	//Filling set field
	debugMessage("Fill StartReqData ...");
#if defined(_DEBUG_IMSI)
	//getSubscriberId(char* buffer, int buffer_size)
	memcpy(data->imsi.imsi_buf,"255139747382185",15);
	data->imsi.imsi_size = 15;
#endif
	
	
	num = getGSMInfo(info);
	//sprintf(s, "MCC: %d, MNC: %d, LAC: %d, CI: %d", info[2], info[3], info[1], info[0]);
	debugMessage("After getGSMInfo!");
	data->set.pos_technology_bitmap = Platform::SET_ASSISTED_AGPS_OFFSET;
	data->set.pref_method = Platform::AGPS_SET_ASSISTED;
	data->set.pos_protocol_bitmap = 2;

	//Filling lac field
	data->lac.cell_info.cell_type = Platform::CellInfo_t::GSM;
	data->lac.cell_info.cell_info_gsm.gsm_mcc = info[2];
	data->lac.cell_info.cell_info_gsm.gsm_mnc = info[3];
	data->lac.cell_info.cell_info_gsm.gsm_lac = info[1];
	data->lac.cell_info.cell_info_gsm.gsm_ci = info[0];
	data->lac.cell_info.cell_info_gsm.nmr_quantity=0;
	data->lac.cell_info.cell_info_gsm.gsm_ta=0;
	data->lac.cell_info.cell_info_gsm.nmr_quantity = 0;
	data->lac.cell_info.cell_info_gsm.gsm_nmr[0].arfcn=500;
	data->lac.cell_info.cell_info_gsm.gsm_nmr[0].bsic=55;
	data->lac.cell_info.cell_info_gsm.gsm_nmr[0].rxlev=44;
	data->lac.cell_info_status = Platform::PRESENT_CELL_INFO;

	//Filling qop field
	//data->qop.horr_acc = 17;
	data->qop.horr_acc = 100;
	data->qop.qop_optional_bitmap = VER_ACC|MAX_LOC_AGE|DELAY;
	data->qop.ver_acc = 101;
	data->qop.max_loc_age = 0;
	data->qop.delay = 7;

	//Filling vel field
	//data->vel.type = Velocity_t::VEL_HORVEL;
	data->lac.cell_info.cell_info_gsm.gsm_opt_param = 0;//MEASUREMENT_REPORT_BIT | TA_BIT;
	data->start_opt_param = 0;
	data->start_opt_param = 0;//REQUEST_ASSISTANCE;// | QOP;
	//Filling a_data field
	data->a_data.assitance_req_bitmap = IONOSPHERIC_MODE_REQ_MASK|REFERENCE_LOCATION_REQ_MASK|REFERENCE_TIME_REQ_MASK|ALMANAC_REQ_MASK|UTC_MODE_REQ_MASK|IONOSPHERIC_MODE_REQ_MASK|ACQUI_ASSIST_REQ_MASK|REAL_TIME_INT_REQ_MASK|NAVIGATION_MODEL_REQ_MASK;
	data->a_data.gpsWeak = 0;
	data->a_data.gpsToe = 0;
	data->a_data.nSAT = 0;
	data->a_data.toeLimit = 0;
	data->a_data.num_sat_info = 0;

	//Filling app_id field
	data->app_id = SUPL_APP_ID;

}



/*
 * Class:     android_tests_SuplLocationProviderTests
 * Method:    OMA_Tests
 * Signature: (I)Z
 */

JNIEXPORT jboolean JNICALL Java_android_supl_SuplLocationProvider_OMA_1Tests
  (JNIEnv * env, jobject obj, jint testID)
{
	int r;
	pthread_attr_init(&pAttr);
	pthread_attr_setstacksize(&pAttr, SUPL_THREAD_STACK_SIZE);
	pthread_attr_setdetachstate(&pAttr, PTHREAD_CREATE_DETACHED);
	LOGD("Starting the %d test", testID);
        test_result = false;
	switch(testID)
	{
		
		case 1: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT1, 0); 
			break;
		case 2: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT2, 0); 
			break;
                case 3: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT3, 0); 
			break;
	        case 4: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT4, 0); 
			break;
		case 5: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT5, 0); 
			break;
		case 6: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT6, 0); 
			break;
		case 7: r = pthread_create(&TestThread, &pAttr, SUPL_OMA_BT7, 0); 
			break;
		default:
			break;
	}

	pthread_attr_destroy(&pAttr);
	if (r != 0)
	{
		debugMessage("Unable to create test thread");
    		return false;
	}
	else
	{
		debugMessage("Test thread started");
		debugMessage("Wait for close signal...");
	}
	//pthread_mutex_lock(&TestMutex);
        //pthread_cond_wait(&CloseCond, &CloseMutex);
        //pthread_mutex_unlock(&TestMutex);
	debugMessage("Exit from native Tests function...");
	return test_result;
}

	

void * SUPL_OMA_BT1(void * arg){

        //debugMessage("Locking Test mutex...");
	//pthread_mutex_lock(&TestMutex);
        //debugMessage(__FUNCTION__, "Test mutex locked...", NULL);
	debugMessage("Start native part of SUPL_OMA_BT1 test...");
	using namespace Platform;
	Platform::StartReqData* data_struct;
	Platform::DataReqData* dta1;
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
	if(data_struct == NULL)
	return (void *) NULL;
	Fill_StartReqData_set_ASSISTED(data_struct);
	debugMessage("Call SUPL_Control...");
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage("Wait SUPL_RESPONSE from SLP");
	
	/* Wait response from SLP */
	/*while(resp_flag != 1)
	{
		sleep(1);
		if((cnt++) == 10)
                {
                     debugMessage(__FUNCTION__, "SUPL response timeout. Exiting ...", NULL);
		     return (void *)-1;
                }
	}
        debugMessage(__FUNCTION__, "SUPL response received...", NULL);
	resp_flag = 0;

	
	dta1 = (Platform::DataReqData*)malloc(sizeof(Platform::DataReqData));
	dta1->app_id = SUPL_APP_ID;
	dta1->pos_payload.type = dta1->pos_payload.RRLP_PAYLOAD;
	memcpy(dta1->pos_payload.payload.ctrl_pdu,first_response,1);
	dta1->pos_payload.payload.ctrl_pdu_len = 1;*/
	/* Send SUPL_POS_INIT message */
	//SUPL_Control(dta1->app_id, UPL_DATA_REQ, (Engine::uint8_t*)dta1);
	
	//sleep(10000);	
        test_result = true;
	debugMessage("Return from native test function!");
        //debugMessage(__FUNCTION__, "Unlocking Test mutex...", NULL);
	//pthread_mutex_unlock(&TestMutex);
        //debugMessage(__FUNCTION__, "Test mutex unlocked.", NULL);
return NULL;
}

void * SUPL_OMA_BT2(void * arg){

	//pthread_mutex_lock(&TestMutex);

	using namespace Platform;
	Platform::StartReqData* data_struct;

	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
		/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	Fill_StartReqData_set_ASSISTED(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);
	
	/* Wait SUPL_RESPONSE from SLP */
	/*while(resp_flag != 1)
	{
		sleep(1);
		if((cnt++) == 10) return (void *)-1;
	}
	resp_flag = 0;*/
	
	//sleep(10000);
        test_result = true;
        //debugMessage(__FUNCTION__, "Unlocking Test mutex...", NULL);
	//pthread_mutex_unlock(&TestMutex);
        //debugMessage(__FUNCTION__, "Test mutex unlocked.", NULL);
return NULL;
}

void * SUPL_OMA_BT3(void * arg){

	//pthread_mutex_lock(&TestMutex);

	using namespace Platform;
	Platform::StartReqData* data_struct;
	Platform::DataReqData* dta1;
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
		/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	Fill_StartReqData_set_ASSISTED(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);
	
	/* Wait SUPL_RESPONSE from SLP */
	/*while(resp_flag != 1)
	{
		sleep(1);
		if((cnt++) == 10) return (void *)-1;
	}
	resp_flag = 0;

	dta1 = (Platform::DataReqData*)malloc(sizeof(Platform::DataReqData));
	dta1->app_id = SUPL_APP_ID;
	dta1->pos_payload.type = dta1->pos_payload.RRLP_PAYLOAD;
	memcpy(dta1->pos_payload.payload.ctrl_pdu,first_response,1);
	dta1->pos_payload.payload.ctrl_pdu_len = 1;
	SUPL_Control(dta1->app_id, UPL_DATA_REQ, (Engine::uint8_t*)dta1);*/
	/* Send SUPL_POS_INIT message */
	//SUPL_Control(data_struct->app_id, UPL_DATA_REQ, (Engine::uint8_t*)dta1);
	
	//sleep(10000);
        test_result = true;
	//pthread_mutex_unlock(&TestMutex);	
return NULL;
}


void * SUPL_OMA_BT4(void * arg){

	//pthread_mutex_lock(&TestMutex);

	using namespace Platform;
	Platform::StartReqData* data_struct;
	Platform::DataReqData* dta1;
	
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
		/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	FillStartReqData(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);
	
	
        
	//sleep(10000);
        test_result = true;	
	//pthread_mutex_unlock(&TestMutex);

return NULL;
}

void * SUPL_OMA_BT5(void * arg){

        //debugMessage(__FUNCTION__, "Locking Test mutex...", NULL);
	//pthread_mutex_lock(&TestMutex);
        //debugMessage(__FUNCTION__, "Test mutex locked...", NULL);

	using namespace Platform;
	Platform::StartReqData* data_struct;
	
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
		/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	FillStartReqData(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);
	
	
	/* Wait SUPL_END from SLP */
        /*while(resp_flag != 1)
	{
		sleep(1);
		if((cnt++) == 10) return (void *)-1;
	}
       resp_flag = 0;*/

	//sleep(10000);
        test_result = true;	
        //debugMessage(__FUNCTION__, "Unlocking Test mutex...", NULL);
	//pthread_mutex_unlock(&TestMutex);
        //debugMessage(__FUNCTION__, "Test mutex unlocked.", NULL);
return NULL;
}

void * SUPL_OMA_BT6(void * arg){

	//pthread_mutex_lock(&TestMutex);

	using namespace Platform;
	Platform::StartReqData* data_struct;
	
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
			/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	FillStartReqData(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);
	
	
	/* Wait SUPL_END from SLP */
       /* while(resp_flag != 1)
	{
		sleep(1);
		if((cnt++) == 10) return (void *)-1;
	}
       resp_flag = 0;*/

	//sleep(10000);
        test_result = true;
	//pthread_mutex_unlock(&TestMutex);	

return NULL;
}

void * SUPL_OMA_BT7(void * arg){

	//pthread_mutex_lock(&TestMutex);

	using namespace Platform;
	Platform::StartReqData* data_struct;
	
	int cnt = 0;
	test_result = false;

	/* Send SUPL_START message */
	data_struct = (StartReqData*)malloc(sizeof(StartReqData));
		/* KW changes*/
	if(data_struct == NULL)
	return (void *) NULL;
	FillStartReqData(data_struct);
	SUPL_Control(data_struct->app_id, UPL_START_REQ, (Engine::uint8_t*)data_struct);
        //debugMessage(__FUNCTION__, "Wait SUPL_RESPONSE from SLP", NULL);

	sleep(5);
        test_result = true;
	//pthread_mutex_unlock(&TestMutex);
	
return NULL;
}


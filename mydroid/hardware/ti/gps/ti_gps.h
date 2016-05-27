
#ifdef GPS_DEBUG
#define  D(...)   LOGD(__VA_ARGS__)
#else
#define  D(...)   ((void)0)
#endif /* GPS_DEBUG */

/**
 * Function:        gps_get_hardware_interface
 * Brief:           It gets the GPS hardware interface.
 * Description:
 * Note:            Internal function.
 * Params:          None.
 * Return:          Handle to GPS Hardware Interface.
 */
//const GpsInterface* gps_get_hardware_interface();


/* Returns the qemu hardware interface GPS interface. */
const GpsInterface* gps_get_qemu_interface();

/**
 * Function:        gps_get_interface
 * Brief:           This is called from GpsLocationProvider for getting GPS Interface.
 * Description:
 * Note:            External Function.
 * Params:          None.
 * Return:          Handle to GPS Interface.
 */
const GpsInterface* gps_get_interface();


/**
 * Function:        hgps_init
 * Brief:           It connects with MCPF.s socket server thread and
                    requests for the creation of NAVC.
 * Description:
 * Note:
 * Params:          callbacks - GPS callback structure.
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_init(GpsCallbacks* callbacks);


/**
 * Function:        hgps_start
 * Brief:           It connects with MCPF.s socket server thread and
                    requests it to start the baseband.
 * Description:
 * Note:
 * Params:          None.
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_start();


/**
 * Function:        hgps_cleanup
 * Brief:           Clean up.
 * Description:
 * Note:
 * Params:          None.
 * Return:          None.
 */
void hgps_cleanup(void);

/**
 * Function:        hgps_start
 * Brief:           It connects with MCPF.s socket server thread and
                    requests it to stop the baseband.
 * Description:
 * Note:
 * Params:          None.
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_stop();

/**
 * Function:        hgps_inject_time
 * Brief:
 * Description:
 * Note:
 * Params:          time -
                    timeReference -
                    uncertainty -
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_inject_time(GpsUtcTime time, int64_t timeReference, int uncertainty);

int hgps_inject_location(double latitude, double  longitude, float accuracy);


/**
 * Function:        hgps_delete_aiding_data
 * Brief:
 * Description:
 * Note:
 * Params:          flags -
 * Return:          None.
 */
void hgps_delete_aiding_data(GpsAidingData flags);


/**
 * Function:        hgps_set_position_mode
 * Brief:           Set location fix mode and frequency in seconds.
 * Description:
 * Note:
 * Params:          mode - Autonomous or MS-Based or MS-Assisted.
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_set_position_mode(GpsPositionMode mode, GpsPositionRecurrence recurrence, uint32_t fix_frequency,
		uint32_t preferred_accuracy, uint32_t preferred_time);


/**
 * Function:        hgps_get_extension
 * Brief:           Gets the extra interface provided by GPS_AL for supporting
                    SUPL or any other feature.
 * Description:
 * Note:
 * Params:          name - XTRA or SUPL (Supported feature).
 * Return:          Handle to structure which has implemention for supporting the
                    requested feature.
 */
const void* hgps_get_extension(const char* name);

/**
 * Function:        hgps_ni_init.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:          data -
 length -
 * Return:          Void.
 */
void hgps_ni_init(GpsNiCallbacks* nicb);

/**
 * Function:        hgps_ni_response.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:          data -
 length -
 * Return:          Void.
 */
void hgps_ni_response(int notifyID, int response);


/**
 * Function:        hgps_xtra_init
 * Brief:
 * Description:
 * Note:
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_xtra_init(GpsXtraCallbacks* callbacks);


/**
 * Function:        hgps_xtra_data
 * Brief:
 * Description:
 * Note:
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_xtra_data(char *data,int length);

void hgps_agps_init(AGpsCallbacks *agpscb);
void hgps_agps_deinit();
int hgps_agps_data_conn_open(const char *apn);
int hgps_agps_data_conn_closed();
int hgps_agps_data_conn_failed();
int hgps_agps_set_server(AGpsType type, const char* hostname, int port );


/**
 * Function:        hgps_supl_set_apn
 * Brief:
 * Description:
 * Note:
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_supl_set_apn(const char* apn);

/**
 * Function:        hgps_supl_set_server
 * Brief:           Requests for the creation of SUPLC.
 * Description:
 * Note:
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_supl_set_server(uint32_t addr, int port);



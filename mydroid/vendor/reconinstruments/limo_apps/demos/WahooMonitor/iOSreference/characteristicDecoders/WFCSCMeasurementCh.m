//
//  WFCSCMeasurementCh.m
//  WFConnector
//
//  Created by Michael Moore on 3/22/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFCSCMeasurementCh.h"
#import "WFTimestamp.h"


////////////////////////////////////////////////////////////////////////////////
// BTLE Macros and Definitions.
////////////////////////////////////////////////////////////////////////////////

#define MAX_CRANK_TICKS_PER_SECOND  10.0  // 10 ticks per second is 600 RPM for crank.
#define MAX_WHEEL_TICKS_PER_SECOND  50.0  // 50 ticks per second is roughly 231 mph.

#define CSC_FLAG_OFFSET                             0
#define CSC_FLAG_SPEED_DATA(x)                      (x & 0x01)
#define CSC_FLAG_CADENCE_DATA(x)                    (x & 0x02)

#define CSC_WHEEL_REVS_SIZE                         4
#define CSC_WHEEL_TIME_SIZE                         2
#define CSC_CRANK_REVS_SIZE                         2
#define CSC_CRANK_TIME_SIZE                         2



////////////////////////////////////////////////////////////////////////////////
// WFCSCMeasurementCh Interface Implementation
////////////////////////////////////////////////////////////////////////////////

@implementation WFCSCMeasurementCh

@synthesize hasSpeedData;
@synthesize hasCadenceData;

@synthesize stCBSCPage0Data;
@synthesize stPrevCBSCPage0Data;
@synthesize stCalculatedData;
@synthesize ulTotalWheelRevs;

@synthesize timestampSpeed;
@synthesize timestampCadence;
@synthesize lastDataTime;
@synthesize lastWheelDataTime;
@synthesize lastCadenceDataTime;


#pragma mark -
#pragma mark NSObject Implementation

//--------------------------------------------------------------------------------
- (void)dealloc
{
    [timestampSpeed release];
    [timestampCadence release];
    
    [super dealloc];
}
//--------------------------------------------------------------------------------
- (id)init
{
    if ( (self=[super init]) )
    {
        characteristicType = BTLE_CHARACTERISTIC_CSC_MEASUREMENT;
    }
    
    return self;
}


#pragma mark -
#pragma mark WFCharacteristicDecoder Implementation

//--------------------------------------------------------------------------------
- (BOOL)decodeCharacteristicData
{
    UCHAR* pData = self.dataPtr;
    BOOL retVal = (pData != nil);
    
    if ( retVal )
    {
        DEBUGLOG(DBG_FLAG_BTLE_CH, @"decodeCharacteristicData: first byte = %d", pData[0]);

        // get the flags.
        UCHAR ucOffset = CSC_FLAG_OFFSET;
        UCHAR ucFlags = pData[ucOffset++];
        //
        hasSpeedData = CSC_FLAG_SPEED_DATA(ucFlags) ? TRUE : FALSE;
        hasCadenceData = CSC_FLAG_CADENCE_DATA(ucFlags) ? TRUE : FALSE;
        
		// update the previous page data structure.
		stPrevCBSCPage0Data = stCBSCPage0Data;
        
        // process the speed data.
        if ( hasSpeedData )
        {
            // decode the wheel revs.
            ULONG ulWheelRevs = pData[ucOffset++];
            ulWheelRevs |= ((ULONG)pData[ucOffset++] << 8);
            ulWheelRevs |= ((ULONG)pData[ucOffset++] << 16);
            ulWheelRevs |= ((ULONG)pData[ucOffset++] << 24);
            
            stCBSCPage0Data.usCumSpeedRevCount = (USHORT)ulWheelRevs;
            ulTotalWheelRevs = ulWheelRevs;
            
            // decode the wheel event time.
            stCBSCPage0Data.usLastTime1024  = pData[ucOffset++];
            stCBSCPage0Data.usLastTime1024 |= ((USHORT)pData[ucOffset++] << 8);
        }
        
        // process the cadence data.
        if ( hasCadenceData )
        {
            // decode the crank revs.
            stCBSCPage0Data.usCumCadenceRevCount  = pData[ucOffset++];
            stCBSCPage0Data.usCumCadenceRevCount |= ((USHORT)pData[ucOffset++] << 8);
            
            // decode the crank event time.
            stCBSCPage0Data.usLastCadence1024  = pData[ucOffset++];
            stCBSCPage0Data.usLastCadence1024 |= ((USHORT)pData[ucOffset++] << 8);
        }
        
		
		// update the calculated values.
		if (dataInitialized)
		{
			// update the calculated values for speed.
			if (stCBSCPage0Data.usLastTime1024 != stPrevCBSCPage0Data.usLastTime1024)
			{
                // MUZZ
                // The timestamp upload was moved into here so it is only updated
                // if the last 2 timestamps from the sensor are different. This is
                // required otherwise the timestamp class does not detect the rollover
                // correctly.
                if ( [timestampSpeed updateTimestamp:stCBSCPage0Data.usLastTime1024] )
                {
                    // update the valid data time.
                    lastDataTime = [NSDate timeIntervalSinceReferenceDate];
                    lastWheelDataTime = lastDataTime;
                }
                
				// the revolution count is a two byte integer which
				// will roll over after 65536 revolutions.  check
				// for rollover and adjust revolution accordingly.
				//
				// rollover is accounted for by subtracting the previous
				// value from the current, and then masking the result
				// by the maximum possible value (value just before rollover).
				// if a rollover has occured, the current value will
				// be less than the previous value.  the difference calculation
				// will still be valid as long as only two bytes of the
				// result is evaluated, hence the mask.
				USHORT wheelRevs = ((stCBSCPage0Data.usCumSpeedRevCount - stPrevCBSCPage0Data.usCumSpeedRevCount) & MAX_USHORT);
				
				// the time count is 1/1024 second stored in a
				// two byte integer which rolls over after 64
				// seconds.  check for rollover and adjust.
				USHORT timeOffset = ((stCBSCPage0Data.usLastTime1024 - stPrevCBSCPage0Data.usLastTime1024) & MAX_USHORT);
				
				// check that the offset value is reasonable.
				//
				// when some models of s/c sensors wake from sleep, they
				// send the revolutions value as 0xFFFF, to prevent taking
				// the difference of this value (or other anomolies), the
				// difference is checked to be sure it is reasonable.  the
				// max ticks/second should give a speed roughly 100mph.
				// anything over this value is considered to be invalid, in
				// which case the accumulators are not updated for this message.
				float ticksPerSecond = (float)wheelRevs / ((float)timeOffset/1024.0);
				if ( ticksPerSecond < MAX_WHEEL_TICKS_PER_SECOND )
				{
					stCalculatedData.accumWheelRevolutions += wheelRevs;
					stCalculatedData.accumSpeedTime += timeOffset;
					
					// calculate Wheel cadence in RPM.
					//
					// here we calculate the instantaneous angular velocity
					// of the pedals based on the last two measurements
					// from the sensor.  the time offset is in 1/1024 second
					// units, so to get a per-second value, multiply by 1024.
					// to get a per-minute value, multiply this by 60.  therefore
					// the multiplier constant 0xF000 (60*1024) is used.
					stCalculatedData.instantWheelRPM = (USHORT)( ((ULONG)wheelRevs * 0xF000) / (ULONG)timeOffset);
				}
			}
			
			// update the calculated values for cadence.
			if (stCBSCPage0Data.usLastCadence1024 != stPrevCBSCPage0Data.usLastCadence1024)
			{
                if ( [timestampCadence updateTimestamp:stCBSCPage0Data.usLastCadence1024] )
                {
                    // update the valid data time.
                    lastDataTime = [NSDate timeIntervalSinceReferenceDate];
                    lastCadenceDataTime = lastDataTime;
                }
                
				// the revolution count is a two byte integer which
				// will roll over after 65536 revolutions.  check
				// for rollover and adjust revolution accordingly.
				//
				// rollover is accounted for by subtracting the previous
				// value from the current, and then masking the result
				// by the maximum possible value (value just before rollover).
				// if a rollover has occured, the current value will
				// be less than the previous value.  the difference calculation
				// will still be valid as long as only two bytes of the
				// result is evaluated, hence the mask.
				USHORT pedalRevs = ((stCBSCPage0Data.usCumCadenceRevCount - stPrevCBSCPage0Data.usCumCadenceRevCount) & MAX_USHORT);
				
				// the time count is 1/1024 second stored in a
				// two byte integer which rolls over after 64
				// seconds.  check for rollover and adjust.
				USHORT timeOffset = (USHORT)((stCBSCPage0Data.usLastCadence1024 - stPrevCBSCPage0Data.usLastCadence1024) & MAX_USHORT);
				
				// check that the offset value isreasonable.
				//
				// when some models of s/c sensors wake from sleep, they
				// send the revolutions value as 0xFFFF, to prevent taking
				// the difference of this value (or other anomolies), the
				// difference is checked to be sure it is reasonable.  the
				// max ticks/second should give a cadence of 100 RPM.
				// anything over this value is considered to be invalid, in
				// which case the accumulators are not updated for this message.
				float ticksPerSecond = (float)pedalRevs / ((float)timeOffset/1024.0);
				if ( ticksPerSecond < MAX_CRANK_TICKS_PER_SECOND )
				{
					stCalculatedData.accumCrankRevolutions += pedalRevs;
					stCalculatedData.accumCadenceTime += timeOffset;
					
					// calculate Crank cadence in RPM.
					//
					// here we calculate the instantaneous angular velocity
					// of the pedals based on the last two measurements
					// from the sensor.  the time offset is in 1/1024 second
					// units, so to get a per-second value, multiply by 1024.
					// to get a per-minute value, multiply this by 60.  therefore
					// the multiplier constant 0xF000 (60*1024) is used.
					stCalculatedData.instantCrankRPM = (UCHAR)( ((ULONG)pedalRevs * 0xF000) / (ULONG)timeOffset);
				}
			}
		}
		else
		{
			// set default values.
			stCalculatedData.accumWheelRevolutions = 0;
			stCalculatedData.accumSpeedTime = 0;
			stCalculatedData.instantWheelRPM = 0;
			stCalculatedData.accumCrankRevolutions = 0;
			stCalculatedData.accumCadenceTime = 0;
			stCalculatedData.instantCrankRPM = 0;
		}
		
		dataInitialized = TRUE;
        
        // update the data flag.
        hasData = TRUE;
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (void)reset
{
    [super reset];
    
    hasSpeedData = FALSE;
    hasCadenceData = FALSE;

	dataInitialized = FALSE;
	
	// initialize data page state structures.
	stCBSCPage0Data.usLastTime1024 = 0;
	stCBSCPage0Data.usCumSpeedRevCount = 0;
	stCBSCPage0Data.usLastCadence1024 = 0;
	stCBSCPage0Data.usCumCadenceRevCount = 0;
	
	stPrevCBSCPage0Data = stCBSCPage0Data;
	
	stCalculatedData.accumWheelRevolutions = 0;
	stCalculatedData.accumSpeedTime = 0;
	stCalculatedData.instantWheelRPM = 0;
	stCalculatedData.accumCrankRevolutions = 0;
	stCalculatedData.accumCadenceTime = 0;
	stCalculatedData.instantCrankRPM = 0;
	
	// initialize the timestamp.
    [timestampSpeed release];
	timestampSpeed = [[WFTimestamp alloc] init];
	timestampSpeed.dataTime = 0;
	timestampSpeed.rolloverTime = 64;  // time rollover 64 seconds.
	timestampSpeed.unitFactor = 1024.0;
	timestampSpeed.rolloverOverflow = TRUE;
	
    [timestampCadence release];
	timestampCadence = [[WFTimestamp alloc] init];
	timestampCadence.dataTime = 0;
	timestampCadence.rolloverTime = 64;  // time rollover 64 seconds.
	timestampCadence.unitFactor = 1024.0;
	timestampCadence.rolloverOverflow = TRUE;
    
    lastWheelDataTime = 0;
    lastCadenceDataTime = 0;
}

@end

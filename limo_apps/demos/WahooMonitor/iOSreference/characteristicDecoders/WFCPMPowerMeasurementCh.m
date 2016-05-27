//
//  WFCPMPowerMeasurementCh.m
//  WFConnector
//
//  Created by Michael Moore on 6/7/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFCPMPowerMeasurementCh.h"
#import "WFTimestamp.h"


////////////////////////////////////////////////////////////////////////////////
// BTLE Macros and Definitions.
////////////////////////////////////////////////////////////////////////////////

#define MAX_CRANK_TICKS_PER_SECOND  10.0  // 10 ticks per second is 600 RPM for crank.
#define MAX_WHEEL_TICKS_PER_SECOND  50.0  // 50 ticks per second is roughly 231 mph.

#define CPM_FLAG_OFFSET                             0
#define CPM_FLAG_SIZE                               2
#define CPM_FLAG_LEFT_PEDAL_CONTRIB(x)              (x & 0x0001)
#define CPM_FLAG_ACCUM_TORQUE(x)                    (x & 0x0002)
#define CPM_FLAG_SPEED_DATA(x)                      (x & 0x0004)
#define CPM_FLAG_CADENCE_DATA(x)                    (x & 0x0008)
#define CPM_FLAG_EXTREME_FORCES(x)                  (x & 0x0010)
#define CPM_FLAG_ANGLES_OF_EXTREME_FORCES(x)        (x & 0x0020)
#define CPM_FLAG_TOP_DEAD_SPOT_ANGLE(x)             (x & 0x0040)
#define CPM_FLAG_BOTTOM_DEAD_SPOT_ANGLE(x)          (x & 0x0080)

#define CPM_INST_POWER_OFFSET                       (CPM_FLAG_OFFSET+CPM_FLAG_SIZE)
#define CPM_INST_POWER_SIZE                         2
#define CPM_WHEEL_REVS_SIZE                         4
#define CPM_WHEEL_TIME_SIZE                         2
#define CPM_CRANK_REVS_SIZE                         2
#define CPM_CRANK_TIME_SIZE                         2



////////////////////////////////////////////////////////////////////////////////
// WFCPMPowerMeasurementCh Interface Implementation
////////////////////////////////////////////////////////////////////////////////

@implementation WFCPMPowerMeasurementCh

@synthesize hasSpeedData;
@synthesize hasCadenceData;
@synthesize hasLeftPedalPower;
@synthesize hasAccumTorque;

@synthesize ssInstPower;
@synthesize ucLeftPedalPower;
@synthesize ulAccumTorque;

@synthesize stCBSCPage0Data;
@synthesize stPrevCBSCPage0Data;
@synthesize stCalculatedData;

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
        characteristicType = BTLE_CHARACTERISTIC_CPM_POWER_MEASUREMENT;
    }
    
    return self;
}


#pragma mark -
#pragma mark WFCharacteristicDecoder Implementation


- (void)reinitializeWithCharacteristic:(CBCharacteristic *)ch peripheral:(CBPeripheral *)periph updateType:(WFCharacteristicUpdateType_t)updateType
{
    [super reinitializeWithCharacteristic:ch peripheral:periph updateType:updateType];
    decoederReinitialized=TRUE;
}

//--------------------------------------------------------------------------------
- (BOOL)decodeCharacteristicData
{
    UCHAR* pData = self.dataPtr;
    BOOL retVal = (pData != nil);
    
    if ( retVal )
    {
        BOOL ticksDidMax=FALSE;
        
        // TODO:  confirm flag byte count in final spec.
        //
        // get the flags (2 bytes in the preliminary spec).
        UCHAR ucOffset = CPM_FLAG_OFFSET;
        USHORT usFlags = pData[ucOffset++];
        usFlags |= ((USHORT)pData[ucOffset++] << 8);
        //
        hasLeftPedalPower = CPM_FLAG_LEFT_PEDAL_CONTRIB(usFlags) ? TRUE : FALSE;
        hasAccumTorque = CPM_FLAG_ACCUM_TORQUE(usFlags) ? TRUE : FALSE;
        hasSpeedData = CPM_FLAG_SPEED_DATA(usFlags) ? TRUE : FALSE;
        hasCadenceData = CPM_FLAG_CADENCE_DATA(usFlags) ? TRUE : FALSE;
        DEBUGLOG(DBG_FLAG_BTLE, @"  - hasLeftPedalPower: %@, hasAccumTorque: %@, hasSpeedData: %@, hasCadenceData: %@", hasLeftPedalPower?@"TRUE":@"FALSE", hasAccumTorque?@"TRUE":@"FALSE", hasSpeedData?@"TRUE":@"FALSE", hasCadenceData?@"TRUE":@"FALSE");
        
        ///////////////////////////////////////////////////////////////////////
        //
        // power data.
        
        // process the instantaneous power.
        ucOffset = CPM_INST_POWER_OFFSET;
        ssInstPower = (SSHORT)(pData[ucOffset++] | (pData[ucOffset++] << 8));

        // process the left pedal power.
        if ( hasLeftPedalPower )
        {
            // decode the left pedal power contribution.
            ucLeftPedalPower = pData[ucOffset++];
        }
        
        USHORT accumTorque=0;
        
        // process the accumulated torque.
        if ( hasAccumTorque )
        {
            // decode the accumulated torque.
            accumTorque = ((USHORT)(pData[ucOffset++] | (pData[ucOffset++] << 8)));
            //accumTorque is adding at the end only if the wheel/crank ticks are recorded. This
            //Fixes a issues with sensors resetting
        }
        
        
        ///////////////////////////////////////////////////////////////////////
        //
        // speed and cadence data.
        
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
            
            // decode the wheel event time.
            stCBSCPage0Data.usLastTime1024  = pData[ucOffset++];
            stCBSCPage0Data.usLastTime1024 |= ((USHORT)pData[ucOffset++] << 8);
            DEBUGLOG(DBG_FLAG_KICKR, @"time:%d, accTq:%ld, ulWRevs:%ld, iPower:%d",stCBSCPage0Data.usLastTime1024, ulAccumTorque, ulWheelRevs, ssInstPower);

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
            DEBUGLOG(DBG_FLAG_KICKR, @"time:%d, accTq:%ld, ulCadRevs:%d, iPower:%d",stCBSCPage0Data.usLastCadence1024, ulAccumTorque, stCBSCPage0Data.usCumCadenceRevCount, ssInstPower);

        }
        
		// update the calculated values.
		if (dataInitialized)
		{
			// update the calculated values for speed.
			if (stCBSCPage0Data.usLastTime1024 != stPrevCBSCPage0Data.usLastTime1024)
			{
                // update the time stamp.
                if ( [timestampSpeed updateTimestampMurraySpecialEdition:stCBSCPage0Data.usLastTime1024] )
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
				
				// the time count is 1/2048 second stored in a
				// two byte integer which rolls over after 32
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
				float ticksPerSecond = (float)wheelRevs / ((float)timeOffset/2048.0);
				if ( ticksPerSecond < MAX_WHEEL_TICKS_PER_SECOND )
				{
					stCalculatedData.accumWheelRevolutions += wheelRevs;
					stCalculatedData.accumSpeedTime += timeOffset;

                    stCalculatedData.accumPower += (ssInstPower * wheelRevs);

					// calculate Wheel cadence in RPM.
					//
					// here we calculate the instantaneous angular velocity
					// of the pedals based on the last two measurements
					// from the sensor.  the time offset is in 1/1024 second
					// units, so to get a per-second value, multiply by 1024.
					// to get a per-minute value, multiply this by 60.  therefore
					// the multiplier constant 0xF000 (60*1024) is used.
					stCalculatedData.instantWheelRPM = (USHORT)( ((ULONG)wheelRevs * 0x1E000) / (ULONG)timeOffset);
				}
                else
                {
                    ticksDidMax=YES;
                }
			}
			
			// update the calculated values for cadence.
			if (stCBSCPage0Data.usLastCadence1024 != stPrevCBSCPage0Data.usLastCadence1024)
			{
                if ( [timestampCadence updateTimestampMurraySpecialEdition:stCBSCPage0Data.usLastCadence1024] )
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
				float ticksPerSecond = (float)pedalRevs / ((float)timeOffset/2048.0);
				if ( ticksPerSecond < MAX_CRANK_TICKS_PER_SECOND )
				{
					stCalculatedData.accumCrankRevolutions += pedalRevs;
					stCalculatedData.accumCadenceTime += timeOffset;

                    stCalculatedData.accumPower += (ssInstPower * pedalRevs);

					// calculate Crank cadence in RPM.
					//
					// here we calculate the instantaneous angular velocity
					// of the pedals based on the last two measurements
					// from the sensor.  the time offset is in 1/1024 second
					// units, so to get a per-second value, multiply by 1024.
					// to get a per-minute value, multiply this by 60.  therefore
					// the multiplier constant 0xF000 (60*1024) is used.
					stCalculatedData.instantCrankRPM = (UCHAR)( ((ULONG)pedalRevs * 0x1E000) / (ULONG)timeOffset);
				}
                else
                {
                    ticksDidMax=YES;
                }
			}
            
            // process the accumulated torque.
            if ( hasAccumTorque)
            {
                
                // Detect a reset? This is when the sensor disconnected but also reset its own
                // internal counters
                if(decoederReinitialized && ticksDidMax)
                {
                    //Device has hardware reset and the data is probably not any good..
                    NSLog(@" CPM Power ch:  DEVICE PROBABLY HARD RESET!");
                    
                }
                else
                {
                    ULONG accumTorqueDiff = (ULONG)((accumTorque - usPreviousAccumTorque) & MAX_USHORT);
                    ulAccumTorque += accumTorqueDiff;
                }
                
                usPreviousAccumTorque = accumTorque;
                
                //NSLog(@"ACCUM TORQUE:  %lu", ulAccumTorque);
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
        decoederReinitialized = FALSE;
        
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
    hasLeftPedalPower = FALSE;
    hasAccumTorque = FALSE;

    ssInstPower = 0;
    ucLeftPedalPower = 0;
    ulAccumTorque = 0;
    usPreviousAccumTorque = 0;
    
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
	timestampSpeed.rolloverTime = 32;  // time rollover 32 seconds.
	timestampSpeed.unitFactor = 2048.0;
	timestampSpeed.rolloverOverflow = TRUE;
	
    [timestampCadence release];
	timestampCadence = [[WFTimestamp alloc] init];
	timestampCadence.dataTime = 0;
	timestampCadence.rolloverTime = 32;  // time rollover 32 seconds.
	timestampCadence.unitFactor = 2048.0;
	timestampCadence.rolloverOverflow = TRUE;
    
    lastWheelDataTime = 0;
    lastCadenceDataTime = 0;
}

@end

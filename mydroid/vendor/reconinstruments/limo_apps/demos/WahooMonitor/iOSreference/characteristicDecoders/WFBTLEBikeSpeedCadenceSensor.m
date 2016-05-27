//
//  WFBTLEBikeSpeedCadenceSensor.m
//  WFConnector
//
//  Created by Michael Moore on 1/25/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFBTLEBikeSpeedCadenceSensor.h"
#import "WFHealthThermometerData.h"
#import "WFAPIUtilities.h"
#import "WFTimestamp.h"
#import "WFBTLEBikeSpeedCadenceData.h"
#import "WFWahooCSCData.h"
#import "WFCSCMeasurementCh.h"
#import "WFTemperatureMeasurementCh.h"
#import "WFCSCFeatureCh.h"
#import "WFSensorLocationCh.h"
#import "WFCSCControlPointCh.h"
#import "WFWahooCSCControlPointCh.h"
#import "WFOdometerHistory.h"


@interface WFBTLEBikeSpeedCadenceSensor()

- (void)delegateOdometerHistory;
- (void)delegateOdometerReset:(NSArray*)userInfo;
- (void)delegateSetGearRatioResponseWithSuccessFlag:(NSNumber*)success;
- (void)delegateGetGearRatioResponseWithSuccessFlag:(NSNumber*)success;

@end


@implementation WFBTLEBikeSpeedCadenceSensor

@synthesize delegate;


#pragma mark -
#pragma mark NSObject Implementation

//--------------------------------------------------------------------------------
- (id)init
{
	if ( (self = [super init]) )
	{
		sensorType = WF_SENSORTYPE_BIKE_SPEED_CADENCE;
        
        [self reset];
	}
	
	return self;
}

//--------------------------------------------------------------------------------
- (void)dealloc
{
    [delegate release];
    delegate = nil;
    
    [cscMeasurementCh release];
    cscMeasurementCh = nil;
    
    [temperatureMeasurementCh release];
    temperatureMeasurementCh = nil;
    
    [cscFeatureCh release];
    cscFeatureCh = nil;
    
    [sensorLocationCh release];
    sensorLocationCh = nil;
    
    [cscControlPointCh release];
    cscControlPointCh = nil;
    
    [wfcscControlPointCh release];
    wfcscControlPointCh = nil;
    
    [super dealloc];
}


#pragma mark -
#pragma mark CBPeripheralDelegate Implementation

//--------------------------------------------------------------------------------
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    [super peripheral:peripheral didUpdateValueForCharacteristic:characteristic error:error];
    
    if( error == nil )
    {
        // check for the CSC Measurement characteristic.
        if ( characteristic == cscMeasurementCh.characteristic )
        {
            // set the last message time.
            lastMessageTime = [NSDate timeIntervalSinceReferenceDate];
            
            // decode the characteristic data.
            hasData = [cscMeasurementCh decodeCharacteristicData];
            
            // update the odometer history.
            if ( wfcscControlPointCh && [wfcscControlPointCh updateCurrentOdometerRevs:cscMeasurementCh.ulTotalWheelRevs] )
            {
                // if this is the first odometer update since history was
                // requested, send the history received to the delegate.
                [self delegateOdometerHistory];
                
                DEBUGLOG(DBG_FLAG_BTLE_SENSOR, @"DELEGATING ODOMETER HISTORY RECEIVED (CSCM)");
            }
		}
        
        // check for the Temperature Measurement characteristic.
        else if ( characteristic == temperatureMeasurementCh.characteristic )
        {
            // decode the characteristic data.
            hasData = [temperatureMeasurementCh decodeCharacteristicData];
        }
        
        // check for the CSC Feature characteristic.
        else if ( characteristic == cscFeatureCh.characteristic )
        {
            // decode the characteristic data.
            hasData = [cscFeatureCh decodeCharacteristicData];
        }
        
        // check for the Sensor Location characteristic.
        else if ( characteristic == sensorLocationCh.characteristic )
        {
            // decode the characteristic data.
            hasData = [sensorLocationCh decodeCharacteristicData];
        }
        
        // process the CSC Control Point characteristic.
        else if ( characteristic == cscControlPointCh.characteristic )
        {
            // decode the characteristic data.
            [cscControlPointCh decodeCharacteristicData];
            
            // check for an odometer reset response.
            if ( cscControlPointCh.didReceiveResetResponse )
            {
                // reset the history and current measurement.
                [wfcscControlPointCh resetHistory];
                [cscMeasurementCh reset];
                
                // send the command response to the delegate.
                [self delegateOdometerReset:[NSArray arrayWithObject:[NSNumber numberWithBool:cscControlPointCh.bResetSuccess]]];
            }
        }
        
        // process the WF CSC Control Point characteristic.
        else if ( characteristic == wfcscControlPointCh.characteristic )
        {
            // decode the characteristic data.
            [wfcscControlPointCh decodeCharacteristicData];
            
            // check for an odometer history response.
			if ( wfcscControlPointCh.eResponse == WF_WAHOOCSCCP_RESPONSE_TO_HISTORY_REQUEST )
			{
                DEBUGLOG(DBG_FLAG_BTLE_SENSOR, @"DELEGATING ODOMETER HISTORY RECEIVED (WFCSCCP)");
                
                // send the command response to the delegate.
                [self delegateOdometerHistory];
            }
			
			
			// check for a set gear ratio response.
			if ( wfcscControlPointCh.eResponse == WF_WAHOOCSCCP_RESPONSE_TO_SET_GEAR_RATIO )
			{
				DEBUGLOG(DBG_FLAG_BTLE_SENSOR, @"WF_WAHOOCSCCP_RESPONSE_TO_SET_GEAR_RATIO success:%@", (wfcscControlPointCh.bSetGearRatioSuccess?@"YES":@"NO"));
                // send the command response to the delegate.
                [self delegateSetGearRatioResponseWithSuccessFlag:[NSNumber numberWithBool:wfcscControlPointCh.bSetGearRatioSuccess]];
            }

			
			// check for a get gear ratio response.
			if ( wfcscControlPointCh.eResponse == WF_WAHOOCSCCP_RESPONSE_TO_GET_GEAR_RATIO )
			{
				DEBUGLOG(DBG_FLAG_BTLE_SENSOR, @"WF_WAHOOCSCCP_RESPONSE_TO_GET_GEAR_RATIO success:%@", (wfcscControlPointCh.bSetGearRatioSuccess?@"YES":@"NO"));
                // send the command response to the delegate.
                [self delegateGetGearRatioResponseWithSuccessFlag:[NSNumber numberWithBool:wfcscControlPointCh.bGetGearRatioSuccess]];
            }

        }
    }
}


#pragma mark -
#pragma mark WFBTLEDevice Implementation

//--------------------------------------------------------------------------------
- (BOOL)startUpdatingForService:(CBService*)service
{
    BOOL retVal = [super startUpdatingForService:service];
    
    if ( !retVal )
    {
        // handle the Bike Speed Cadence Service.
        if ( [service.UUID shortUUID] == BTLE_SERVICE_CYCLING_SPEED_AND_CADENCE )
        {
            // initialize the characteristics.
            for ( CBCharacteristic* ch in [service characteristics] )
            {
                // get the service UUID.
                USHORT uuid = [ch.UUID shortUUID];
                
                switch ( uuid )
                {
                    case BTLE_CHARACTERISTIC_CSC_MEASUREMENT:
                    {
                        // create a WFCSCMeasurementCh characteristic decoder.
                        if(cscMeasurementCh == nil)
                        {
                            cscMeasurementCh = [[WFCSCMeasurementCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_NOTIFY];
                        }
                        else {
                            [cscMeasurementCh reinitializeWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_NOTIFY];
                        }
                        break;
                    }
                        
                    case BTLE_CHARACTERISTIC_TEMPERATURE_MEASUREMENT:
                        
                        // create a WFTemperatureMeasurementCh characteristic decoder.
                        [temperatureMeasurementCh release];
                        temperatureMeasurementCh = [[WFTemperatureMeasurementCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_NOTIFY];
                        break;
                        
                    case BTLE_CHARACTERISTIC_CSC_FEATURE:
                        
                        // create a WFCSCFeatureCh characteristic decoder.
                        [cscFeatureCh release];
                        cscFeatureCh = [[WFCSCFeatureCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_READ];
                        break;
                        
                    case BTLE_CHARACTERISTIC_SENSOR_LOCATION:
                        
                        // create a WFSensorLocationCh characteristic decoder.
                        [sensorLocationCh release];
                        sensorLocationCh = [[WFSensorLocationCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_READ];
                        break;
                        
                    case BTLE_CHARACTERISTIC_CSC_CONTROL_POINT:
                        
                        // create a WFCSCControlPointCh characteristic decoder.
                        [cscControlPointCh release];
                        cscControlPointCh = [[WFCSCControlPointCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_INDICATE];
                        break;
                        
                    case BTLE_CHARACTERISTIC_WFCSC_CONTROL_POINT:
                        
                        // create a WFWahooCSCControlPointCh characteristic decoder.
                        [wfcscControlPointCh release];
                        wfcscControlPointCh = [[WFWahooCSCControlPointCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_INDICATE];
                        
                        // this is a Wahoo BlueSC device - set the device time.
                        [self setDeviceTime];

                        break;
                }
            }
        }
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (WFSensorData*)getData
{
	WFBTLEBikeSpeedCadenceData* retVal = nil;
	hasData = FALSE;
	
    // ensure the characteristic is present, and that it has a valid value.
    if ( cscMeasurementCh && cscMeasurementCh.hasData )
    {
        BOOL hasFeatures = (cscFeatureCh && cscFeatureCh.hasData);
        retVal = [[[WFBTLEBikeSpeedCadenceData alloc] initWithTime:cscMeasurementCh.lastDataTime
                                                         wheelTime:cscMeasurementCh.lastWheelDataTime
                                                       cadenceTime:cscMeasurementCh.lastCadenceDataTime
                                                       hasFeatures:hasFeatures] autorelease];
        
        // configure the cadence data.
        retVal.accumCrankRevolutions = cscMeasurementCh.stCalculatedData.accumCrankRevolutions;
        retVal.accumCadenceTime = cscMeasurementCh.stCalculatedData.accumCadenceTime / 0x400;
        retVal.instantCrankRPM = cscMeasurementCh.stCalculatedData.instantCrankRPM;
        retVal.cadenceTimestamp = cscMeasurementCh.timestampCadence.dataTime;
        retVal.cadenceTimestampOverflow = cscMeasurementCh.timestampCadence.rolloverOverflow;
        
        // configure the speed data.
        retVal.ulOdometerWheelRevolutions = cscMeasurementCh.ulTotalWheelRevs;
        retVal.accumWheelRevolutions = cscMeasurementCh.stCalculatedData.accumWheelRevolutions;
        retVal.accumSpeedTime = cscMeasurementCh.stCalculatedData.accumSpeedTime / 0x400;
        retVal.instantWheelRPM = cscMeasurementCh.stCalculatedData.instantWheelRPM;
        retVal.speedTimestamp = cscMeasurementCh.timestampSpeed.dataTime;
        retVal.speedTimestampOverflow = cscMeasurementCh.timestampSpeed.rolloverOverflow;
        
        // configure the features.
        if ( hasFeatures )
        {
            memcpy(retVal.pstFeatures, cscFeatureCh.pstFeatures, sizeof(WFBTLECSCFeatures_t));
        }
    }
    
    // check presence of the sensor location characteristic.
    if ( sensorLocationCh && sensorLocationCh.hasData )
    {
        // configure the sensor location data.
        retVal.eSensorLocation = sensorLocationCh.eSensorLocation;
    }
    
    // check whether the temperature characteristic is available.
    if ( retVal && temperatureMeasurementCh && temperatureMeasurementCh.hasData )
    {
        // create the Wahoo Extended data instance.
        WFWahooCSCData* wahooData = [[WFWahooCSCData alloc] init];
        wahooData.temperature = temperatureMeasurementCh.temperature;
        retVal.wahooData = wahooData;
        
        // release resources.
        [wahooData release];
        wahooData = nil;
    }
    
    // check whether odometer history is available.
    if ( retVal && wfcscControlPointCh && wfcscControlPointCh.isHistoryAvailable )
    {
        // create the Wahoo Extended data, if necessary.
        if ( !retVal.wahooData )
        {
            WFWahooCSCData* wahooData = [[WFWahooCSCData alloc] init];
            retVal.wahooData = wahooData;
            [wahooData release];
            wahooData = nil;
        }
        
        // set the odometer history instance.
        retVal.wahooData.odometerHistory = wfcscControlPointCh.odometerHistory;
    }
	
    // configure common data.
    if ( retVal )
    {
        [super configureCommonData:retVal.btleCommonData];
    }

	return retVal;
}

//--------------------------------------------------------------------------------
- (void)reset
{
    [super reset];
    
    [cscMeasurementCh reset];
    [temperatureMeasurementCh reset];
}



#pragma mark -
#pragma mark WFBTLEBikeSpeedCadenceSensor Implementation

#pragma mark Private Methods

//--------------------------------------------------------------------------------
- (void)delegateOdometerHistory
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateOdometerHistory) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cscConnection:nil didReceiveOdometerHistory:wfcscControlPointCh.odometerHistory];
}

//--------------------------------------------------------------------------------
- (void)delegateOdometerReset:(NSArray*)userInfo
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateOdometerReset:) withObject:userInfo waitUntilDone:FALSE];
        return;
    }
    
    // ensure all params are present.
    if ( [userInfo count] > 0 )
    {
        // parse the info data.
        BOOL bSuccess = [(NSNumber*)[userInfo objectAtIndex:0] boolValue];
        
        // alert the delegate.
        [delegate cscConnection:nil didResetOdometer:bSuccess];
    }
}

//--------------------------------------------------------------------------------
- (void)delegateSetGearRatioResponseWithSuccessFlag:(NSNumber*)success
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetGearRatioResponseWithSuccessFlag:) withObject:success waitUntilDone:FALSE];
        return;
    }
    // alert the delegate.
    [delegate cscConnection:nil didReceiveSetGearRatioResponse:[success boolValue]];
}

//--------------------------------------------------------------------------------
- (void)delegateGetGearRatioResponseWithSuccessFlag:(NSNumber*)success
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateGetGearRatioResponseWithSuccessFlag:) withObject:success waitUntilDone:FALSE];
        return;
    }    
    // alert the delegate.
    [delegate cscConnection:nil didReceiveGearRatio:[success boolValue] numerator:wfcscControlPointCh.usGearRatioNumerator denominator:wfcscControlPointCh.usGearRatioDenomonator];
}

#pragma mark Public Methods

//--------------------------------------------------------------------------------
- (BOOL)requestHistoryFrom:(UCHAR)ucStart to:(UCHAR)ucEnd
{
    DEBUGLOG(DBG_FLAG_TRACE, @"requestHistoryFrom");
    BOOL retVal = FALSE;
    if ( wfcscControlPointCh )
    {
        retVal = [wfcscControlPointCh requestHistoryFrom:ucStart to:ucEnd];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)requestOdometerReset:(ULONG)ulResetVal
{
    DEBUGLOG(DBG_FLAG_TRACE, @"requestOdometerReset");
    BOOL retVal = FALSE;
    if ( cscControlPointCh )
    {
        retVal = [cscControlPointCh requestOdometerReset:ulResetVal];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendRecordCommand:(WFBTLEWahooCSCOpCode_t)opCode withOperator:(WFBTLERecordOperator_t)op operands:(NSData*)operands
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendRecordCommand");
    BOOL retVal = FALSE;
    
    if ( cscControlPointCh )
    {
        retVal = [wfcscControlPointCh sendRecordCommand:opCode withOperator:op operands:operands];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)setDeviceTime
{
    DEBUGLOG(DBG_FLAG_TRACE, @"setDeviceTime");
    BOOL retVal = FALSE;
    
    if ( cscControlPointCh )
    {
        retVal = [wfcscControlPointCh setDeviceTime];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)setDeviceGearRatioWithNumerator:(USHORT)numerator andDenomonator:(USHORT)denomonator
{
	DEBUGLOG(DBG_FLAG_TRACE, @"setDeviceGearRatio %u,%u", numerator, denomonator);
    if (!cscControlPointCh) return FALSE;
	return [wfcscControlPointCh setDeviceGearRatioWithNumerator:numerator andDenomonator:denomonator];
}


//--------------------------------------------------------------------------------
- (BOOL)getDeviceGearRatio
{
	DEBUGLOG(DBG_FLAG_TRACE, @"getDeviceGearRatio");
    if (!cscControlPointCh) return FALSE;
    return [wfcscControlPointCh getDeviceGearRatio];
}


@end

//
//  WFBTLEBikePowerSensor.m
//  WFConnector
//
//  Created by Michael Moore on 6/7/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFBTLEBikePowerSensor.h"
#import "WFCPMPowerMeasurementCh.h"
#import "WFCPMControlPointCh.h"
#import "WFWahooCPMControlPointCh.h"
#import "WFBTLEBikePowerData.h"
#import "WFTimestamp.h"
#import "WFFileLogger.h"

////////////////////////////////////////////////////////////////////////////////
// WFBTLEBikePowerSensor Interface Implementation
////////////////////////////////////////////////////////////////////////////////

@interface WFBTLEBikePowerSensor()

- (void)delegateAntIdResponse;
- (void)delegateCalibrationResponse;
- (void)delegateFactoryCalibrateResponse;
- (void)delegateReadAccelerometerResponse;
- (void)delegateReadTemperatureResponse;
- (void)delegateSetSlopeResponse;
- (void)delegateSetTemperatureSlopeResponse;
- (void)delegateSetCrankLengthResponse;
- (void)delegateReadCalibrationResponse;
- (void)delegateReadDeviceInfoResponse;

- (void)delegateSetTrainerMode;
- (void)delegateSetTrainerGrade;
- (void)delegateSetTrainerWindResistance;
- (void)delegateSetTrainerRollingResistance;
- (void)delegateSetTrainerWindSpeed;
- (void)delegateSetTrainerWheelCircumference;
- (void)delegateTrainerReadModeResponse;
- (void)delegateTrainerRequestAntConnectionResponse;
- (void)delegateTrainerInitSpindownResponse;
- (void)delegateTrainerSpindownResult;
- (void)delegateTrainerTestOpticalResult;

@end


@implementation WFBTLEBikePowerSensor

@synthesize delegate;
@synthesize btDelegate;

#pragma mark -
#pragma mark NSObject Implementation

//--------------------------------------------------------------------------------
- (id)init
{
	if ( (self = [super init]) )
	{
		sensorType = WF_SENSORTYPE_BIKE_POWER;
        fileLogger = nil;
	}
	
	return self;
}

//--------------------------------------------------------------------------------
- (void)dealloc
{

    [delegate release];
    delegate = nil;
    
    [btDelegate release];
    btDelegate = nil;
    
    [powerMeasurementCh release];
    powerMeasurementCh = nil;
    
    [wahooCPMControlPointCh release];
    wahooCPMControlPointCh = nil;
    
    [cpmControlPointCh release];
    cpmControlPointCh = nil;
    
    [fileLogger release];
    fileLogger = nil;
    
    [super dealloc];
}


#pragma mark -
#pragma mark CBPeripheralDelegate Implementation

//--------------------------------------------------------------------------------
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    [super peripheral:peripheral didUpdateValueForCharacteristic:characteristic error:error];
    
    if(error==nil)
    {
        // check for the Power Measurement characteristic.
        if ( characteristic == powerMeasurementCh.characteristic )
        {
            // decode the characteristic data.
            hasData = [powerMeasurementCh decodeCharacteristicData];
        }
        
        // check for the CPM Control Point characteristic.
        else if ( characteristic == cpmControlPointCh.characteristic )
        {
            // decode the characteristic data.
            [cpmControlPointCh decodeCharacteristicData];
            
            // delegate command responses.
            if ( (cpmControlPointCh.eResponse & WF_CPMCP_RESPONSE_TO_MANUAL_ZERO) )
            {
                [self delegateCalibrationResponse];
            }
            else if ( (cpmControlPointCh.eResponse & WF_CPMCP_RESPONSE_TO_SET_CRANK_LENGTH) )
            {
                [self delegateSetCrankLengthResponse];
            }
        }
        
        // check for the Wahoo CPM Control Point characteristic.
        else if ( characteristic == wahooCPMControlPointCh.characteristic )
        {
            // decode the characteristic data.
            [wahooCPMControlPointCh decodeCharacteristicData];
            
            // delegate command responses.
            if ( (wahooCPMControlPointCh.eResponse &
                       (WF_WCPMCP_RESPONSE_TO_TRAINER_SET_STANDARD_MODE |
                        WF_WCPMCP_RESPONSE_TO_TRAINER_SET_ERG_MODE |
                        WF_WCPMCP_RESPONSE_TO_TRAINER_SET_SIM_MODE |
                        WF_WCPMCP_RESPONSE_TO_TRAINER_SET_RESISTANCE_MODE)) )
            {
                [self delegateSetTrainerMode];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_SET_GRADE) )
            {
                [self delegateSetTrainerGrade];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_SET_CRR) )
            {
                [self delegateSetTrainerRollingResistance];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_SET_C) )
            {
                [self delegateSetTrainerWindResistance];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_SET_WIND_SPEED) )
            {
                [self delegateSetTrainerWindSpeed];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_SET_WHEEL_CIRCUMFERENCE) )
            {
                [self delegateSetTrainerWheelCircumference];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_READ_MODE) )
            {
                [self delegateTrainerReadModeResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_CONNECT_ANT_SENSOR) )
            {
                [self delegateTrainerRequestAntConnectionResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_INIT_SPINDOWN) )
            {
                [self delegateTrainerInitSpindownResponse];
            }
            else if ( wahooCPMControlPointCh.bTrainerSpindownResult )
            {
                [self delegateTrainerSpindownResult];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_FACTORY_CALIBRATE) )
            {
                [self delegateFactoryCalibrateResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_SET_SLOPE) )
            {
                [self delegateSetSlopeResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_SET_TEMP_SLOPE) )
            {
                [self delegateSetTemperatureSlopeResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_READ_CALIBRATION) )
            {
                [self delegateReadCalibrationResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_READ_DEVICE_INFO) )
            {
                [self delegateReadDeviceInfoResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_ASSIGN_DEVICE_INFO) )
            {
                [self delegateAntIdResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_READ_ACCELEROMETER) )
            {
                [self delegateReadAccelerometerResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_READ_TEMPERATURE) )
            {
                [self delegateReadTemperatureResponse];
            }
            else if ( (wahooCPMControlPointCh.eResponse & WF_WCPMCP_RESPONSE_TO_TRAINER_TEST_OP) )
            {
                [self delegateTrainerTestOpticalResult];
            }
        }
    }
}


#pragma mark -
#pragma mark WFBTLEDevice Implementation

//--------------------------------------------------------------------------------
- (void)reset
{
    [super reset];
    
    [powerMeasurementCh reset];
    [wahooCPMControlPointCh reset];
}

//--------------------------------------------------------------------------------
- (BOOL)startUpdatingForService:(CBService*)service
{
    BOOL retVal = [super startUpdatingForService:service];
    
    if ( !retVal )
    {
        // handle the Cycling Power Meter Service.
        if ( [service.UUID shortUUID] == BTLE_SERVICE_CYCLING_POWER_METER || [service.UUID shortUUID] == BTLE_SERVICE_CYCLING_POWER_METER_DEPRECATED )
        {
            // initialize the characteristics.
            for ( CBCharacteristic* ch in [service characteristics] )
            {
                // get the service UUID.
                USHORT uuid = [ch.UUID shortUUID];
                
                switch ( uuid )
                {
                    case BTLE_CHARACTERISTIC_CPM_POWER_MEASUREMENT:
                    case BTLE_CHARACTERISTIC_CPM_POWER_MEASUREMENT_DEPRECATED:
                        
                        // create a WFCPMPowerMeasurementCh characteristic decoder.
                        if(powerMeasurementCh==nil)
                        {
                            powerMeasurementCh = [[WFCPMPowerMeasurementCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_NOTIFY];
                        }
                        else
                        {
                            [powerMeasurementCh reinitializeWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_NOTIFY];
                        }
                        
                        break;
                        
                    case BTLE_CHARACTERISTIC_CPM_CONTROL_POINT:
                        
                        // create a WFCPMControlPointCh characteristic decoder.
                        [cpmControlPointCh release];
                        cpmControlPointCh = [[WFCPMControlPointCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_INDICATE];
                        break;
                        
                    case BTLE_CHARACTERISTIC_WAHOO_CPM_CONTROL_POINT:
                        
                        // create a WFWahooCPMControlPointCh characteristic decoder.
                        [wahooCPMControlPointCh release];
                        wahooCPMControlPointCh = [[WFWahooCPMControlPointCh alloc] initWithCharacteristic:ch peripheral:self.btlePeripheral updateType:WF_CHARACTERISTIC_UPDATE_INDICATE];
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
    hasData = FALSE;
    WFBTLEBikePowerData* retVal = nil;
    
    // ensure the characteristic is present, and that it has a valid value.
    if ( powerMeasurementCh )
    {
        // create and configure the Bike Power data instance.
        retVal = [[[WFBTLEBikePowerData alloc] initWithTime:powerMeasurementCh.lastDataTime
                                                  wheelTime:powerMeasurementCh.lastWheelDataTime
                                                cadenceTime:powerMeasurementCh.lastCadenceDataTime] autorelease];
        
        retVal.sensorType = WF_BIKE_POWER_TYPE_BTLE;
        
        //
        // configure the power data.
        retVal.instantPower = powerMeasurementCh.ssInstPower;
        retVal.instantCadence = powerMeasurementCh.stCalculatedData.instantCrankRPM;
        retVal.instantWheelRPM = powerMeasurementCh.stCalculatedData.instantWheelRPM;
        //
        // configure accumulated torque data.
        retVal.accumulatedTorque = (powerMeasurementCh.ulAccumTorque / 32.0);
        //
        // configure the cadence data.
        retVal.crankRevolutionSupported = powerMeasurementCh.hasCadenceData;
        if( retVal.crankRevolutionSupported )
        {
            retVal.crankRevolutions = powerMeasurementCh.stCalculatedData.accumCrankRevolutions;
            retVal.crankTimestamp = powerMeasurementCh.timestampCadence.dataTime;
            retVal.crankTime = powerMeasurementCh.timestampCadence.timeSinceFirst;
            retVal.crankTimestampOverflow = powerMeasurementCh.timestampCadence.rolloverOverflow;
        }
        
        //
        // configure the speed data.
        retVal.wheelRevolutionSupported = powerMeasurementCh.hasSpeedData;
        if(retVal.wheelRevolutionSupported)
        {
            retVal.wheelRevolutions = powerMeasurementCh.stCalculatedData.accumWheelRevolutions;
            retVal.wheelTimestamp = powerMeasurementCh.timestampSpeed.dataTime;
            retVal.wheelTime = powerMeasurementCh.timestampSpeed.timeSinceFirst;
            retVal.wheelTimestampOverflow = powerMeasurementCh.timestampSpeed.rolloverOverflow;
        }
        

        if(retVal.wheelRevolutionSupported)
        {
            retVal.accumulatedPower = powerMeasurementCh.stCalculatedData.accumPower;
            retVal.accumulatedEventCount = retVal.wheelRevolutions;
            retVal.accumulatedTimestamp = retVal.wheelTimestamp;
            retVal.accumulatedTimestampOverflow = retVal.wheelTimestampOverflow;
            retVal.accumulatedTime = retVal.wheelTime;

        }
        else if(retVal.crankRevolutionSupported)
        {
            retVal.accumulatedPower = powerMeasurementCh.stCalculatedData.accumPower;
            retVal.accumulatedEventCount = retVal.crankRevolutions;
            retVal.accumulatedTimestamp = retVal.crankTimestamp;
            retVal.accumulatedTimestampOverflow = retVal.crankTimestampOverflow;
            retVal.accumulatedTime = retVal.accumulatedTime;
        }
    }
	
    // configure common data.
    [super configureCommonData:retVal.btleCommonData];

	return retVal;
}

//--------------------------------------------------------------------------------
- (NSString *)getMinimumFirmwareVersion
{
    NSString *minimumFirmwareVersion = nil;
    
    if ([deviceName rangeOfString:@"KICKR"].location != NSNotFound)
    {
        minimumFirmwareVersion = WF_KICKR_MINIMUM_FIRMWARE_VERSION_FOR_API;
    }
    else if ([deviceName rangeOfString:@"Effekt"].location != NSNotFound)
    {
        minimumFirmwareVersion = WF_KICKR_MINIMUM_FIRMWARE_VERSION_FOR_API;
    }
    
    return minimumFirmwareVersion;
}


#pragma mark -
#pragma mark WFBTLEBikePowerSensor Implementation

#pragma mark Private Methods

//--------------------------------------------------------------------------------
- (void)delegateAntIdResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateAntIdResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveAntIdResponse:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateCalibrationResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateCalibrationResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveCalibrationResponse:cpmControlPointCh.ucResponseStatus offset:cpmControlPointCh.usStrainTicks temperature:cpmControlPointCh.scTemperature];
}

//--------------------------------------------------------------------------------
- (void)delegateFactoryCalibrateResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateFactoryCalibrateResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveFactoryCalibrateResponse:wahooCPMControlPointCh.ucResponseStatus strainTicks:wahooCPMControlPointCh.usStrainTicks];
}

//--------------------------------------------------------------------------------
- (void)delegateReadAccelerometerResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateReadAccelerometerResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // convert the acceleration values (transmitted in milli-g).
    acceleration_t stAccel = wahooCPMControlPointCh.stAcceleration;
    float x = (float)stAccel.x_axis / 1000.0;
    float y = (float)stAccel.y_axis / 1000.0;
    float z = (float)stAccel.z_axis / 1000.0;

    // alert the delegate.
    [delegate cpmConnection:nil didReceiveReadAccelerometerResponse:wahooCPMControlPointCh.ucResponseStatus xAxis:x yAxis:y zAxis:z];
}

//--------------------------------------------------------------------------------
- (void)delegateReadTemperatureResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateReadTemperatureResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveReadTemperatureResponse:wahooCPMControlPointCh.ucResponseStatus temperature:wahooCPMControlPointCh.scTemperature];
}

//--------------------------------------------------------------------------------
- (void)delegateSetSlopeResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetSlopeResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveSetSlopeResponse:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTemperatureSlopeResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTemperatureSlopeResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveSetTemperatureSlopeResponse:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateReadCalibrationResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateReadCalibrationResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveReadCalibrationResponse:wahooCPMControlPointCh.ucResponseStatus slope:wahooCPMControlPointCh.strainGaugeSlope temperatureSlope:wahooCPMControlPointCh.temperatureSlope dpotValue:wahooCPMControlPointCh.ucDpot];
}

//--------------------------------------------------------------------------------
- (void)delegateReadDeviceInfoResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateReadDeviceInfoResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [delegate cpmConnection:nil didReceiveReadDeviceInfoResponse:wahooCPMControlPointCh.ucResponseStatus serialNumber:wahooCPMControlPointCh.ulSerialNumber antId:wahooCPMControlPointCh.usAntId];
}

//--------------------------------------------------------------------------------
- (void)delegateSetCrankLengthResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetCrankLengthResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    DEBUGLOG(DBG_FLAG_BTLE_CH, @"delegateSetCrankLengthResponse");
    [delegate cpmConnection:nil didReceiveSetCrankLengthResponse:cpmControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerMode
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerMode) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerMode:wahooCPMControlPointCh.eTrainerMode status:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerGrade
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerGrade) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerGrade:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerWindResistance
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerWindResistance) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerWindResistance:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerRollingResistance
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerRollingResistance) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerRollingResistance:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerWindSpeed
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerWindSpeed) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerWindSpeed:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateSetTrainerWheelCircumference
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateSetTrainerWheelCircumference) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didSetTrainerWheelCircumference:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateTrainerReadModeResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateTrainerReadModeResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didReceiveTrainerReadModeResponse:wahooCPMControlPointCh.ucResponseStatus mode:wahooCPMControlPointCh.eReportedTrainerMode];
}

//--------------------------------------------------------------------------------
- (void)delegateTrainerRequestAntConnectionResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateTrainerRequestAntConnectionResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didReceiveTrainerRequestAntConnectionResponse:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateTrainerInitSpindownResponse
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateTrainerInitSpindownResponse) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didReceiveTrainerInitSpindownResponse:wahooCPMControlPointCh.ucResponseStatus];
}

//--------------------------------------------------------------------------------
- (void)delegateTrainerSpindownResult
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateTrainerSpindownResult) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    //write spindown record to logfile
    if (fileLogger == nil)
    {
        fileLogger = [[WFFileLogger alloc] init];
    }
//    NSString *spindownRecord = [NSString stringWithFormat:@"%0.3f,%0.2f,%d", wahooCPMControlPointCh.trainerSpindownTime, wahooCPMControlPointCh.trainerSpindownTemp, wahooCPMControlPointCh.trainerSpindownOffset];
//    [fileLogger addLogRecord:spindownRecord ofType:WF_FILE_LOGGER_SPINDOWN_RESULT forDeviceName:deviceName andDeviceSerialNumber:serialNumber];
    [fileLogger submitSpindownForm:deviceName
                      serialNumber:serialNumber
                      spindownTime:wahooCPMControlPointCh.trainerSpindownTime
                      spindownTemp:wahooCPMControlPointCh.trainerSpindownTemp
                    spindownOffset:wahooCPMControlPointCh.trainerSpindownOffset
                   firmwareVersion:firmwareRevision];
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didReceiveTrainerSpindownResult:wahooCPMControlPointCh.trainerSpindownTime
                  temperature:wahooCPMControlPointCh.trainerSpindownTemp
                  offset:wahooCPMControlPointCh.trainerSpindownOffset];
}

//--------------------------------------------------------------------------------
- (void)delegateTrainerTestOpticalResult
{
    // the delegate should be called only on the main thread.
    if ( ![NSThread isMainThread] )
    {
        [self performSelectorOnMainThread:@selector(delegateTrainerTestOpticalResult) withObject:nil waitUntilDone:FALSE];
        return;
    }
    
    // alert the delegate.
    [btDelegate cpmConnection:nil didReceiveTrainerTestOpticalResult:wahooCPMControlPointCh.bResponseSuccess];
}


#pragma mark Public Methods

#pragma mark Public Power Methods

//--------------------------------------------------------------------------------
- (BOOL)sendAssignDeviceInfo:(USHORT)usDeviceId serialNumber:(ULONG)ulSerial
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendAssignDeviceInfo:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendAssignDeviceInfo:usDeviceId serialNumber:ulSerial];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendEnableAntRadio:(BOOL)bEnable
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendEnableAntRadio:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendEnableAntRadio:bEnable];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendFactoryCalibrateRequest:(UCHAR)ucPotTicks
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendFactoryCalibrateRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendFactoryCalibrateRequest:ucPotTicks];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendReadAccelerometerRequest
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendReadAccelerometerRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendReadAccelerometerRequest];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendReadTemperatureRequest
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendReadTemperatureRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendReadTemperatureRequest];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendManualZeroRequest
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendManualZeroRequest");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( cpmControlPointCh )
    {
        retVal = [cpmControlPointCh sendManualZeroRequest];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendSetSlopeRequest:(USHORT)usSlope
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendSetSlopeRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendSetSlopeRequest:usSlope];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendSetTemperatureSlopeRequest:(float)slope
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendSetTemperatureSlopeRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendSetTemperatureSlopeRequest:slope];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendSetCrankLengthRequest:(float)crankLength
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendSetCrankLengthRequest:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( cpmControlPointCh )
    {
        retVal = [cpmControlPointCh sendSetCrankLengthRequest:crankLength];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendReadCalibrationRequest
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendReadCalibrationRequest");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendReadCalibrationRequest];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)sendReadDeviceInfoRequest
{
    DEBUGLOG(DBG_FLAG_TRACE, @"sendReadCalibrationRequest");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh sendReadDeviceInfoRequest];
    }
    
    return retVal;
}


#pragma mark Public Trainer Methods

//--------------------------------------------------------------------------------
- (BOOL)trainerSetErgMode:(USHORT)usWatts
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetErgMode:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetErgMode:usWatts];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetResistanceMode:(float)fpScale
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetResistanceMode:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetResistanceMode:fpScale];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetStandardMode:(WFBikeTrainerLevel_t) eLevel
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetStandardMode:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetStandardMode:eLevel];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetSimMode:(float)fWeight rollingResistance:(float)fCrr windResistance:(float)fC
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetSimMode:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetSimMode:fWeight rollingResistance:fCrr windResistance:fC];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetRollingResistance:(double)fCrr
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetRollingResistance:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetRollingResistance:fCrr];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetWindResistance:(float)fC
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetWindResistance:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetWindResistance:fC];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetGrade:(float)fGrade
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetGrade:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetGrade:fGrade];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetWindSpeed:(float)mpsWindSpeed
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetWindSpeed:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetWindSpeed:mpsWindSpeed];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerSetWheelCircumference:(float)mmCircumference
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerSetWheelCircumference:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerSetWheelCircumference:mmCircumference];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerReadMode
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerReadMode");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerReadMode];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerInitSpindown
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerInitSpindown");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerInitSpindown];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerRequestAntConnection:(WFSensorType_t)eSensorType deviceId:(USHORT)usDeviceId useForPower:(BOOL)bUseForPower
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerRequestAntConnection:");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerRequestAntConnection:eSensorType deviceId:usDeviceId useForPower:bUseForPower];
    }
    
    return retVal;
}

//--------------------------------------------------------------------------------
- (BOOL)trainerTestOptical
{
    DEBUGLOG(DBG_FLAG_TRACE, @"trainerTestOptical");
    BOOL retVal = FALSE;
    
    // ensure the control point exists.
    if ( wahooCPMControlPointCh )
    {
        retVal = [wahooCPMControlPointCh trainerTestOptical];
    }
    
    return retVal;
}

@end

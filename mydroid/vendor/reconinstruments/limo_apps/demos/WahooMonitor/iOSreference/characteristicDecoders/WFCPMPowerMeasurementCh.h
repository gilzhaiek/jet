//
//  WFCPMPowerMeasurementCh.h
//  WFConnector
//
//  Created by Michael Moore on 6/7/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFCharacteristicDecoder.h"
#import "WFBikeSpeedCadenceSensor.h"


@interface WFCPMPowerMeasurementCh : WFCharacteristicDecoder
{
	BOOL dataInitialized;
    
    BOOL decoederReinitialized;
    
    BOOL hasSpeedData;
    BOOL hasCadenceData;
    BOOL hasLeftPedalPower;
    BOOL hasAccumTorque;
    
    SSHORT ssInstPower;
    UCHAR ucLeftPedalPower;
    ULONG ulAccumTorque;
    USHORT usPreviousAccumTorque;
    
	CBSCPage0_Data stCBSCPage0Data;
	CBSCPage0_Data stPrevCBSCPage0Data;
	CBSCCalculatedData stCalculatedData;
    
	WFTimestamp* timestampSpeed;
	WFTimestamp* timestampCadence;
    NSTimeInterval lastDataTime;
	NSTimeInterval lastWheelDataTime;
	NSTimeInterval lastCadenceDataTime;
}


@property (nonatomic, readonly) BOOL hasSpeedData;
@property (nonatomic, readonly) BOOL hasCadenceData;
@property (nonatomic, readonly) BOOL hasLeftPedalPower;
@property (nonatomic, readonly) BOOL hasAccumTorque;

@property (nonatomic, readonly) SSHORT ssInstPower;
@property (nonatomic, readonly) UCHAR ucLeftPedalPower;
@property (nonatomic, readonly) ULONG ulAccumTorque;

@property (nonatomic, readonly) CBSCPage0_Data stCBSCPage0Data;
@property (nonatomic, readonly) CBSCPage0_Data stPrevCBSCPage0Data;
@property (nonatomic, readonly) CBSCCalculatedData stCalculatedData;

@property (nonatomic, readonly) WFTimestamp* timestampSpeed;
@property (nonatomic, readonly) WFTimestamp* timestampCadence;
@property (nonatomic, readonly) NSTimeInterval lastDataTime;
@property (nonatomic, readonly) NSTimeInterval lastWheelDataTime;
@property (nonatomic, readonly) NSTimeInterval lastCadenceDataTime;

@end

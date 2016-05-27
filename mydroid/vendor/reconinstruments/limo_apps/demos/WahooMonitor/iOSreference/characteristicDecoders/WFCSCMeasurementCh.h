//
//  WFCSCMeasurementCh.h
//  WFConnector
//
//  Created by Michael Moore on 3/22/12.
//  Copyright (c) 2012 Wahoo Fitness. All rights reserved.
//

#import "WFCharacteristicDecoder.h"
#import "WFBikeSpeedCadenceSensor.h"


@interface WFCSCMeasurementCh : WFCharacteristicDecoder
{
	BOOL dataInitialized;
    
    BOOL hasSpeedData;
    BOOL hasCadenceData;
    
	CBSCPage0_Data stCBSCPage0Data;
	CBSCPage0_Data stPrevCBSCPage0Data;
	CBSCCalculatedData stCalculatedData;
    ULONG ulTotalWheelRevs;
    
	WFTimestamp* timestampSpeed;
	WFTimestamp* timestampCadence;
    NSTimeInterval lastDataTime;
	NSTimeInterval lastWheelDataTime;
	NSTimeInterval lastCadenceDataTime;
}


@property (nonatomic, readonly) BOOL hasSpeedData;
@property (nonatomic, readonly) BOOL hasCadenceData;

@property (nonatomic, readonly) CBSCPage0_Data stCBSCPage0Data;
@property (nonatomic, readonly) CBSCPage0_Data stPrevCBSCPage0Data;
@property (nonatomic, readonly) CBSCCalculatedData stCalculatedData;
@property (nonatomic, readonly) ULONG ulTotalWheelRevs;

@property (nonatomic, readonly) WFTimestamp* timestampSpeed;
@property (nonatomic, readonly) WFTimestamp* timestampCadence;
@property (nonatomic, readonly) NSTimeInterval lastDataTime;
@property (nonatomic, readonly) NSTimeInterval lastWheelDataTime;
@property (nonatomic, readonly) NSTimeInterval lastCadenceDataTime;

@end

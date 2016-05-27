Jet magnetometer calibration algorithm
======================================
Document written by Darrell. Code written by Tomas.  (Last updated: August 15, 2013)
####Purpose: 
The Jet magnetic sensor shows **soft-iron distortions** (ie: it's magnitude is elongated ellipsoid instead of spheroid), so the magnetometer calibration routine needs to be able to correct both scaling and offset errors.
 
####Integration notes:
#####Data collection:
The data collection part is pretty much unchanged, which takes 15 seconds of magnetometer data before proceeding to the calculation part. The sampling speed was changed from Delay.NORMAL to Delay.FASTEST. (Average 700 samples in 15 seconds).

#####Calculation:
The algorithm uses a two-step method to find the initial condition. The first step is to formulate the locus of the measurement into linear equations. Then it can be solved with least square estimates. After the initial condition is established, the iterative least square is applied to find the optimal estimate.

#####Adaptations:
To correct the magnetometer scaling error, the actual magnetic field strength need to be known. Ideally, we can use GPS fix and obtain the magnetic field strength from Android's magnetic map method. However, to avoid the GPS requirement, we first use Ali's original calibration routine to get the offset, then use the average magnetic field strength from the sample data. 

#### Source:
The calibration algorithm is based on **"Calibration of Strapdown Magnetometers in the Magnetic Field Domain"** by Demoz Gebre-Egziabher  
(available in server @recon-nas/recondata/Research&Development/Papers/Sensors/magcal_Egziabher.pdf).

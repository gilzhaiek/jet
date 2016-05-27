% this file generates the matrices required to rotate the sensor fusion coordinate system on jet B3
% parameters: yaw, pitch, roll angles

yaw=16.8;
%yaw=45;

pitch=20;
roll=5;
% --- don't change below this point ---

% function definitions
% generate a basic rotation matrix about each axis, these will be combined later
function output = rotate_x(theta)
output = [1 0 0; 0 cosd(theta) -sind(theta); 0 sind(theta) cosd(theta)];
endfunction 

function output = rotate_y(theta)
output = [cosd(theta) 0 -sind(theta); 0 1 0; sind(theta) 0 cosd(theta)];
endfunction 

function output = rotate_z(theta)
output = [cosd(theta) -sind(theta) 0; sind(theta) cosd(theta) 0; 0 0 1];
endfunction 

% generate code for various languages: sensors.conf, C++ (google's mat.h mat33_t format)
% these functions only work for 3x3 matrices
function printmatrix_cpp(A,name,comment) % this is for google's matrix code which is column-major
 printf("// %s\n",comment)
 printf("mat33_t %s;\n%s[0][0] = %f;\n%s[0][1] = %f;\n%s[0][2] = %f;\n%s[1][0] = %f;\n%s[1][1] = %f;\n%s[1][2] = %f;\n%s[2][0] = %f;\n%s[2][1] = %f;\n%s[2][2] = %f;\n",name,name,A(1),name,A(2),name,A(3),name,A(4),name,A(5),name,A(6),name,A(7),name,A(8),name,A(9))
endfunction

function printmatrix_s(A, comment) % this is for sensors.conf 
printf("# %s\n",comment)
printf("rot_A = %f %f %f\n",A(1),A(4),A(7))
printf("rot_B = %f %f %f\n",A(2),A(5),A(8))
printf("rot_C = %f %f %f\n",A(3),A(6),A(9))
endfunction

function printmatrix_c(A,name) % this is row major, not currently used
printf("%s[0] = %ff; %s[1] = %ff; %s[2] = %ff;\n",name,A(1),name,A(4),name,A(7))
printf("%s[3] = %ff; %s[4] = %ff; %s[5] = %ff;\n",name,A(2),name,A(5),name,A(8))
printf("%s[6] = %ff; %s[7] = %ff; %s[8] = %ff;\n",name,A(3),name,A(6),name,A(9))
endfunction


function printsensconf(mata)
matg = mata; % gyro is the same as accel
matc = mata*[1 0 0; 0 1 0; 0 0 -1]; % compass has inverted Z axis on chip so we account for that here
% matc = [1 0 0; 0 1 0; 0 0 -1]*mata; % if calibration is applied before rotation, we must reverse order

% printf("--- these matrices go in sensors.conf to transform the raw sensors ---\n")
% move each matrix into it's respective section, under CONV_B then
% adb remount && adb shell rm /data/system/sensors.conf && adb push sensors_sun.conf system/lib/hw/sensors.conf && adb reboot
printmatrix_s(mata,"acceleration matrix")
printmatrix_s(matg, "gyro matrix")
printmatrix_s(matc, "compass matrix")
endfunction


function printfusion(mata)
matg = mata; % gyro is the same as accel
matc = mata%*[1 0 0; 0 1 0; 0 0 -1]; % compass has inverted Z axis on chip so we account for that here
printf("\n--- these go in mydroid/framework/base/services/sensorservice/SensorFusion.cpp - SensorFusion::process() ---\n")
% they are the inverse of the previous ones because our sensor fusion is written with board coordinates hard-coded
% so we need do the sensors.conf transform for any apps that use raw sensors, then undo it before sensor fusion
% and finally (below) re-do it at the end of sensor fusion
printmatrix_cpp(transpose(mata),"sens_inv_a", "acceleration matrix inverse")
printmatrix_cpp(transpose(matg),"sens_inv_g","gyro matrix inverse")
printmatrix_cpp(transpose(mata),"sens_inv_m", "inverse applied to compass matrix (same as other two because we don't undo z-axis negation") % using inverse of accel instead of compass so we don't undo Z correction


printf("\n--- these go in mydroid/framework/base/services/sensorservice/Fusion.cpp - Fusion::getRotationMatrix() ---\n")
% they re-apply the final transformation to world coordinates
% there's two because the transform needs to be applied from both sides
printmatrix_cpp(final_pre,"final_pre", "final coordinate transform (pre)")
printmatrix_cpp(final_post,"final_post", "final coordinate transform (post)")
endfunction



% generate the actual matrices, one each for acceleration, gyroscope and magnetometer (compass)
X = rotate_x(90);
Y = rotate_y(roll-90);
Xt = transpose(X);
mYaw = rotate_x(yaw);
mPitch = rotate_y(pitch);
mRoll = rotate_z(roll);
%final = mPitch*mYaw*Xt*Y*R*X;
final_pre = mPitch*mYaw*Xt*Y;
final_post = X; 

% correction1 = [     0.194198   0.064098  -0.978866;
%                    0.064098   0.994901   0.077864;
%                    0.978866  -0.077864   0.189099];

correction2 = [     0.238694   0.073736  -0.968291;
                   0.073736   0.992858   0.093784;
                   0.968291  -0.093784   0.231553];


% correction = [   0.20746   0.12609  -0.97008; 
%                    0.12609   0.97994   0.15433; 
%                    0.97008  -0.15433   0.18740]; 
 % yaw_correct = [0.68000   -0.73321   0.00000; 
 %                0.73321   0.68000   0.00000; 
 %                0.00000   0.00000   1.00000];

 correction = [0.22921   0.10040  -0.96818;
               0.10040   0.98692   0.12612;
               0.96818  -0.12612   0.21613];

yaw_correct = rotate_z(47.5);
% yaw_correct = eye(3);
% mata = rotate_x(yaw-90)*rotate_z(pitch); % most recent working, this doesn't include roll because it tends to mess up the other axes
% mata = rotate_z(pitch)*rotate_y(90)*mYaw;
mata = yaw_correct*correction;

%smata = eye(3);
% conv_A      = 0.014980121257304543 0.016633944637962212 0.017139404912887145 
% conv_B      = 15.391868059687777 -38.26274690845865 14.573657043967907 


printsensconf(mata)


% conv_A      = 0.015050287795636639 0.01686117182723306 0.01729490869224529 
% conv_B      = -2.704934108359892 84.5247577511044 -160.90380541328048 

% conv_A      = 0.015017826133080264 0.01699222850073841 0.017201847970789632 
% conv_B      = -4.042347206466855 74.18274373996204 -179.70716503231154 

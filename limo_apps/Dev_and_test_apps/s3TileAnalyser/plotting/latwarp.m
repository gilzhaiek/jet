function p = latwarp(latitude)

equitorialCircumfrence = 40075017.0;	% taken from wikipedia/Earth
meridionalCircumfrence = 40007860.0;	% taken from wikipedia/Earth

tileHeightInDegrees = 0.025;
tileHeightInMeters = tileHeightInDegrees/360.0 * meridionalCircumfrence;
tileWidthInMeters = tileHeightInMeters ;
tileWidthInDegreesAtEquator = tileWidthInMeters/equitorialCircumfrence * 360.0;

p = 1./ cos(latitude/180. * pi); 

endfunction
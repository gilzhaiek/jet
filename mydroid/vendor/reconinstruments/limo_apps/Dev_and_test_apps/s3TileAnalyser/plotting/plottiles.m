%t = zeros(10000);
function plottiles
display('reading tile data...');
t = dlmread('existingtiles.out',',');
display('generating map...');
size(t)
x=t(:,2);
y=t(:,1);
tileHeightInDegrees = 0.025;
numberOfTileRowsPerHemisphere = floor(90./tileHeightInDegrees);
numTileRowsNorthToSouth = floor(180./tileHeightInDegrees);
equitorialCircumfrence = 40075017.0;	% taken from wikipedia/Earth
meridionalCircumfrence = 40007860.0;	% taken from wikipedia/Earth
tileHeightInMeters = tileHeightInDegrees/360.0 * meridionalCircumfrence;
tileWidthInMeters = tileHeightInMeters ;
tileWidthInDegreesAtEquator = tileWidthInMeters/equitorialCircumfrence * 360.0;
numTilesAroundEquator = equitorialCircumfrence/tileWidthInMeters;
lats = ((y - numberOfTileRowsPerHemisphere) * tileHeightInDegrees) + tileHeightInDegrees/2.;
wx = x .* (1./ cos(lats/180. * pi));
%[img, map, alpha] = imread ('worldmap2.png');
img = imread ('worldmap2.png');
img2 = img(:,:,1);
set (0,'defaultaxesposition', [0.05, 0.1, 0.9, 0.85]) ;
h=figure
imshow(img2);
hold on;
display('plotting map...');
plot(wx/10.,720-y/10., "linestyle", "none","marker",".","color","red");
print -dpng -color -landscape s3tiles.png
%hold off
end

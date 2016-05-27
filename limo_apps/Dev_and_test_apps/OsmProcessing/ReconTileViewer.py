import sys
import webbrowser
import math

NEW = 2 # open in a NEW tab, if possible
ID = 555000243
TILE_HEIGHT_IN_DEGREE = (0.025)
TILE_WIDTH_IN_DEGREES_AT_EQUATOR = (0.024958105) #which value is same as the one in MapApp
PRECISION_ROUND = 0.0000000001
OVERLAP_IN_DEGREE = 0.000001

    

url = "http://www.openstreetmap.org/export#map=12/49.2128/-122.8332"
url = "http://www.openstreetmap.org/export/embed.html?bbox=-122.9422%2C49.1956%2C-122.7169%2C49.2546&amp;layer=mapnik"

def GetBoundaryByTileId(Id):

    latIndex = int(Id/100000 + PRECISION_ROUND)
    lngIndex = Id % 100000
    
    dLat = TILE_HEIGHT_IN_DEGREE
    latmin = (dLat * latIndex) - 90.0
    if (latmin == 90):
        latmin -= OVERLAP_IN_DEGREE
    elif (latmin == -90):
        latmin += OVERLAP_IN_DEGREE
        
    latmax = (latmin + dLat)
    
    dLng = TILE_WIDTH_IN_DEGREES_AT_EQUATOR/ (math.cos((latmin) * math.pi /180.0))
    lngmin = (TILE_WIDTH_IN_DEGREES_AT_EQUATOR * lngIndex)/(math.cos((latmin * math.pi) /180.0)) - 360.0
    lngmax = lngmin +dLng
       
    result = (("%.7f" % lngmin), ("%.7f" % latmin), ("%.7f" % lngmax), ("%.7f" % latmax))
    #result = (left, bottom, right, top)
    #result = (-122.9350, 49.1956, -122.5903, 49.2)
    print ("tielId=%i, bounds=%s" %(Id, result))
    
    return result


def SendURLToBrowser(url, NEW):
    webbrowser.open(url, new=NEW)

    
def ShowTile(Id):
    if (Id < 0):
        return
    bounds = GetBoundaryByTileId(Id)
    url = "http://www.openstreetmap.org/export/embed.html?bbox="
    url += str(bounds[0]) +"%2C"
    url += str(bounds[1]) +"%2C"
    url += str(bounds[2]) +"%2C"
    url += str(bounds[3]) +"&amp;layer=mapnik"
    print ("url=%s" %url)
    SendURLToBrowser(url, NEW)

print("----------------------------------------Main------------------------------------------------")

argv = sys.argv
if (argv and len(argv) > 1):
    print("TileId=%s" % argv[1])
    Id = int(argv[1])
else:
    Id = ID
    
ShowTile(Id)
exit(0)

print("================================END MAIN==========================================.")




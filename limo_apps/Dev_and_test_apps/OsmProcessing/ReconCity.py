

import math



EARTH_RADIUS = (6371.0)
TILE_HEIGHT_IN_DEGREE = (0.025)
TILE_WIDTH_IN_DEGREES_AT_EQUATOR = (0.024958105) #which value is same as the one in MapApp
OVERLAP_IN_KM = (0.1)
CONST_OVERLAP_RADIAN = OVERLAP_IN_KM / EARTH_RADIUS
OVERLAP_IN_DEGREE = 0.000001
PRECISION_ROUND = 0.0000001
DEGREE_90 = (90.0)
LAT_SHIFT = int(DEGREE_90/TILE_HEIGHT_IN_DEGREE + PRECISION_ROUND) #=1800.0 if degree=0.05
    
        
def GetTileList(lng, lat, x_km, y_km):
    if (lng < -180 or lng >= 180):
        return None
    if (lat < -90  or lat >= 90 ):
        return None
    if (x_km <=0 or x_km <= 0):
        return None
    
    degreex = KMToDegree(x_km, lat)
    degreey = KMToDegree(y_km, lat)
    
    lngmin = lng -degreex
    if (lngmin < -180):
        lngmin = -180
    
    lngmax = lng + degreex
    if (lngmax > 180):
        lngmax = 179.9999999
    
    latmin =lat -degreey
    if (latmin <-90):
        lngmin = -90
    
    latmax =lat + degreey
    if (latmax >90):
        latmax = -79.9999999
    
    return ProcessGeoRegionMapToTiles(lngmin, latmin, lngmax, latmax)




def KMToDegree (km, lat):
    print (180.0/math.pi) * km/ (EARTH_RADIUS*math.cos(lat))
    return (180.0/math.pi) * km/ (EARTH_RADIUS*math.cos(lat))


    
def ProcessGeoRegionMapToTiles(left, bottom, right, top):
          
        #Logger.debug( "ProcessGeoRegionMapToTiles(), (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
        if (left >right or bottom > top):
            #Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None

        if (left<=-180 or right>180 or bottom <=-90 or top >=90):
            #Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        left1 = (left +360) % 360
        right1 = (right + 360 ) % 360
        
        if (left1 <= right1):
            return ProcessGeoRegionMapToTilesSub(left1, bottom, right1, top)
        else:
            return ProcessGeoRegionMapToTilesSub(left1, bottom, 360, top)
            return ProcessGeoRegionMapToTilesSub(0, bottom, right1, top)
            
            
def ProcessGeoRegionMapToTilesSub(left, bottom, right, top):
        
        #Adjust left bottom into grid left bottom
        [left1, bottom1] = AdjustToGridLngLat(left, bottom)

        dLat = TILE_HEIGHT_IN_DEGREE #0.05 degree
        
        
        TileIdList = []    
        i = 0
        j = 0
        n = 0


        curr_lat = (bottom1)
        
        while (curr_lat < top):
                     
            #curr_lat_next = curr_lat + dLat
    
            [left2, bottom2] =  AdjustToGridLngLat(left, curr_lat)
            
            curr_lng = (left2)
            dLng = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(float(curr_lat) * math.pi /180))

            while (curr_lng < right):
                
                curr_lng_next = curr_lng + dLng
            
                n += 1    
                
                [i, j] = GetTileIndexByLocation(curr_lng, curr_lat, False)
                
                reconId = CombineTileSubIndicesString(i, j)
                TileIdList.append(reconId)
                        
                lng1 = curr_lng
                if (lng1>180):
                    lng1 -= 360
                
                lng2 = curr_lng_next
                if (lng2>180):
                    lng2 -= 360
                    
                curr_lng += dLng

    
            curr_lat += dLat

        #Logger.warn( "ProcessGeoRegionMapToTiles(), GetNumTiles=%d.",  n)
        
        return TileIdList
    


def AdjustToGridLngLat(lng, lat):
        
        
        dLat = TILE_HEIGHT_IN_DEGREE
        gridBottom_Lat = dLat * (math.floor((lat)/dLat + PRECISION_ROUND))
        #Logger.debug("lat=%.8f, gridBottom_Lat=%.8f, diff=%.8f, dlat=%.8f" % (lat, gridBottom_Lat,  ((lat) - gridBottom_Lat), dLat))
        
        dLng = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(gridBottom_Lat * (math.pi /180)))
        gridLeft_Lng = dLng * (math.floor((lng)/dLng + PRECISION_ROUND))
        #Logger.debug("lng=%.8f, gridLeft_Lng=%.8f, diff=%.8f, dlng=%.8f" % (lng, gridLeft_Lng,  ((lng) - gridLeft_Lng), dLng)) 
        
        return [gridLeft_Lng, gridBottom_Lat]

def GetTileIndexByLocation(lng, lat, isAdjustToGrid=False):
        #if (lngNew<=-180 or lngNew>180 or latNew <=-90 or latNew >=90):
        #    return None
        latNew = lat
        if (lat== 90.0):
            latNew = lat - OVERLAP_IN_DEGREE
        elif (latNew == -90.0):
            latNew = lat + OVERLAP_IN_DEGREE
 
        lngNew = lng
        lngNew = (lngNew + 360) % 360
            
        if (isAdjustToGrid):
            [lngNew, latNew] = AdjustToGridLngLat(lngNew, latNew)
            

        
        dLat = TILE_HEIGHT_IN_DEGREE #0.05 degree
        dLng = TILE_WIDTH_IN_DEGREES_AT_EQUATOR/ (math.cos((latNew) * math.pi /180.0))
        

        lng_total = (lngNew) * (math.cos((latNew) * math.pi /180.0))
     
        
        i = int(lng_total / TILE_WIDTH_IN_DEGREES_AT_EQUATOR + PRECISION_ROUND)
        j = int((latNew)/dLat + PRECISION_ROUND) + LAT_SHIFT
        
        
        return [i, j]

def CombineTileSubIndices(longIndex, latIndex):
        return latIndex * 100000 + longIndex
    
def CombineTileSubIndicesString(longIndex, latIndex):
        #return str(CombineTileSubIndices(longIndex, latIndex))
        return ('%09d' % CombineTileSubIndices(longIndex, latIndex))
    
'''=========================END RECON_MAIN=========================================='''
    
print GetTileList(-122.9280453, 49.1589864, 10, 10)
    
    

'''=========================END RECON_MAIN=========================================='''




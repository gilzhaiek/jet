#from decimal import getcontext, Decimal
import math
#import os

from ReconProcessor import Const

import datetime
import os.path
import shutil
import time
#import sys
from ReconCompressor import ReconCompressor


class GlobalSweep:
    
    SRC_ROOT = "/media/My Passport/os/map_recon_Canada/map_recon3/"
    DST_ROOT = "/media/My Passport/os/"
    
    def __init__(self, fetcher, logger):
        self._fetcher = fetcher
        self.Logger = logger
        
        if (fetcher):
            self.compressor = fetcher.compressor
        else:
            self.compressor = ReconCompressor(self.Logger)
            

    EARTH_RADIUS = (6371.0)
    PI = (3.1415926)
    #TILE_HEIGHT_IN_DEGREE = (0.05)
    TILE_HEIGHT_IN_DEGREE = (0.025)
    #TILE_WIDTH_IN_DEGREES_AT_EQUATOR = (0.049916211) #which value is same as the one in MapApp
    TILE_WIDTH_IN_DEGREES_AT_EQUATOR = (0.024958105) #which value is same as the one in MapApp
    OVERLAP_IN_KM = (0.1)
    CONST_OVERLAP_RADIAN = OVERLAP_IN_KM / EARTH_RADIUS
    OVERLAP_IN_DEGREE = 0.000001
    PRECISION_ROUND = 0.0000001
    DEGREE_90 = (90.0)
    LAT_SHIFT = int(DEGREE_90/TILE_HEIGHT_IN_DEGREE + PRECISION_ROUND) #=1800.0 if degree=0.05
    
    FOLDER_SIZE = 70
    FOLDER_SIZE_CUBIC =  (FOLDER_SIZE)**3
    FOLDER_SIZE_SQUARE = (FOLDER_SIZE)**2
        

    MERIDIONAL_CIRCUMFERENCE = 40007860.0; # meter  - from wikipedia/Earth
    EQUITORIAL_CIRCUMFERENCE = 40075017.0; # meter  - from wikipedia/Earth


       
   
    '''Hash function is being used by both side of mobile app and web server to identify a file requested or responsed.'''
    def GetTileIndexByLocation(self, lng, lat, isAdjustToGrid=False):
        #if (lngNew<=-180 or lngNew>180 or latNew <=-90 or latNew >=90):
        #    return None
        latNew = lat
        if (lat== 90.0):
            latNew = lat - self.OVERLAP_IN_DEGREE
        elif (latNew == -90.0):
            latNew = lat + self.OVERLAP_IN_DEGREE
 
        lngNew = lng
        lngNew = (lngNew + 360) % 360
            
        #self.Logger.debug("GetTileIndexByLocation, before adjust Lng=%.10f, latNew=%.10f.", lngNew, latNew)
        #Adjust to the GPS Coordinate (lngNew, latNew) of the left and bottom point of its Tile to calculate the index of tile regardless of the location inside of tile. 
        if (isAdjustToGrid):
            [lngNew, latNew] = self.AdjustToGridLngLat(lngNew, latNew)
            

        
        dLat = self.TILE_HEIGHT_IN_DEGREE #0.05 degree
        dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR/ (math.cos((latNew) * math.pi /180.0))
        #self.Logger.debug("GetTileIndexByLocation, after  adjust Lng=%.10f, latNew=%.10f.", lngNew, latNew)
        

        lng_total = (lngNew) * (math.cos((latNew) * math.pi /180.0))
     
        #print("%.15f", lng_total / self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR)
        
        i = int(lng_total / self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR + self.PRECISION_ROUND)
        j = int((latNew)/dLat + self.PRECISION_ROUND) + self.LAT_SHIFT
         
        if (self.Logger):
            self.Logger.debug("GetTileIndexByLocation, dLng=%.10f, dlat=%.10f, (lng=%.10f, lat=%.10f)  map to index (i=%i, j=%i), item#=%i..", dLng, dLat, lng, lat, i, j, self.CombineTileSubIndices(i, j))
        
        return [i, j]
    
    
    def CombineTileSubIndices(self, longIndex, latIndex):
        return latIndex * 100000 + longIndex
    
    def CombineTileSubIndicesString(self, longIndex, latIndex):
        return ('%09d' % self.CombineTileSubIndices(longIndex, latIndex))
        #return str(self.CombineTileSubIndices(longIndex, latIndex))
    
    def GetSubIndicesFromID(self, combine):
        lat = (int)(combine / 100000);
        lng = (int)(combine % 100000);
        return [lng, lat]
        
    
    def GetTileListByGeoRegion(self, left, bottom, right, top):
          
        self.Logger.debug( "GetTileListByGeoRegion(), (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
        if (bottom > top):
            self.Logger.error( "SaveBigMapToFiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None

        if (left<=-180 or right>180):
            self.Logger.error( "SaveBigMapToFiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        if (bottom <= -90):
            bottom = -90.0 + self.OVERLAP_IN_DEGREE
            
        if (top >= 90):
            top = 90 - self.OVERLAP_IN_DEGREE
        
       
        #Adjust left bottom into grid left bottom
        [left, bottom] = self. AdjustToGridLngLat(left, bottom)

        #dLat = self.GRID_SIZE_IN_KM/(Const.KM_PER_ONE_LAT_DEGREE)

        dLat = (self.TILE_HEIGHT_IN_DEGREE) #0.05 degree
        #dLng = dLat/ (math.cos(lat * math.pi /180))

        tileIdList = []
        i = 0
        j = 0
        n = 0
        ids = ""
        curr_lat = (bottom)
        
        while (curr_lat <= top):
                     
            start_lng = (left)
            end_lng =  (right)
            
            [start_i, start_j] = self.GetTileIndexByLocation(start_lng, curr_lat)
            [end_i, end_j] = self.GetTileIndexByLocation(end_lng, curr_lat)
            
            
            i = start_i
            j = start_j #it should start_j == end_j
            if (start_i < end_i):
                for i in range(start_i, (end_i + 1)):
                    tileIdList.append([i, j])
                    ids += '[{0}, {1}], '.format(str(i), str(j))
                    n += 1
                    self.Logger.debug( "GetTileListByGeoRegion(), Get[%d], (, lat=%.10f), map to index (i=%i, j=%i), id=%i.",  n, curr_lat, i, j, self.CombineTileSubIndices(i, j))
            else: #special case
                [max_i, max_j] = self.GetMaxTileIndexs()
                for i in range(start_i, (max_i + 1)):
                    tileIdList.append([i, j])
                    ids += '[{0}, {1}], '.format(str(i), str(j))
                    n += 1
                for i in range(0, (end_i + 1)):
                    tileIdList.append([i, j])
                    ids += '[{0}, {1}], '.format(str(i), str(j))
                    n += 1
                
    

    
            curr_lat += dLat

        self.Logger.debug( "GetTileListByGeoRegion(), GetNumIDs=%d, IDs=%s.",  n, ids)
        return tileIdList
    
    
    def ProcessGeoRegionMapToTiles(self, left, bottom, right, top):
          
        self.Logger.debug( "ProcessGeoRegionMapToTiles(), (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
        if (bottom > top):
            self.Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None

        if (left<=-180 or right>180 or bottom <=-90 or top >=90):
            self.Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        left1 = (left +360) % 360
        right1 = (right + 360 ) % 360
        
        if (left1 <= right1):
            self.ProcessGeoRegionMapToTilesSub(left1, bottom, right1, top)
        else:
            self.ProcessGeoRegionMapToTilesSub(left1, bottom, 360, top)
            self.ProcessGeoRegionMapToTilesSub(0, bottom, right1, top)
            
            
    def ProcessGeoRegionMapToTilesSub(self, left, bottom, right, top):
        
        #Adjust left bottom into grid left bottom
        [left1, bottom1] = self. AdjustToGridLngLat(left, bottom)

        #dLat = self.GRID_SIZE_IN_KM/(Const.KM_PER_ONE_LAT_DEGREE)
        dLat = self.TILE_HEIGHT_IN_DEGREE #0.05 degree
        
        
        TileIdList = []    
        i = 0
        j = 0
        n = 0
        isMostLeftTile = True
        isSourceLocalFolder = False

        curr_lat = (bottom1)
        #isCheckCompletion = Const.IS_RESUME_FROM_LAST_END
        isCheckCompletion = (Const.IS_WHERE_CHECK_EXIST != Const.NOT_CHECK_TILE_EXIST)
        
        while (curr_lat < top):
                     
            curr_lat_next = curr_lat + dLat
            #if (curr_lat_next > top):
            #    curr_lat_next = top
            
            #Adjust left bottom into grid left bottom
            [left2, bottom2] = self. AdjustToGridLngLat(left, curr_lat)
            
            curr_lng = (left2)
            #dLng = self.GRID_SIZE_IN_KM/(Const.KM_CONST_PER_ONE_LNG_DEGREE * math.fabs(math.cos(curr_lat)))
            dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(float(curr_lat) * math.pi /180))

            isMostLeftTile = True
            while (curr_lng < right):
                
                curr_lng_next = curr_lng + dLng
            
                n += 1    
                
                [i, j] = self.GetTileIndexByLocation(curr_lng, curr_lat, False)
                
                reconId = self.CombineTileSubIndicesString(i, j)
                #reconFile = self.GeneratePathFolder(i, j) + reconId +".xml"
                reconFile = Const.OUTPUT_PATH + reconId +".xml"
                
                osmFile = Const.OSM_MAPS_PATH +"o%s.xml" %reconId
                
                if (isCheckCompletion):
                    if (self.compressor.checkTileExisted("base/", reconId)):
                        self.Logger.debug("skip generation file=%s.", reconFile)
                        curr_lng += dLng
                        isMostLeftTile = False
                        continue
#                     else:
#                         isCheckCompletion = False
                if (os.path.exists(osmFile)):
                    isSourceLocalFolder = True
                else:
                    isSourceLocalFolder = False
                        
                lng1 = curr_lng
                if (lng1>180):
                    lng1 -= 360
                
                lng2 = curr_lng_next
                if (lng2>180):
                    lng2 -= 360
                    
                #self.Logger.warn("+++++Start new Tile No.=[%d], tileId=%s, left=%.10f, bottom=%.10f, right=%.10f, top=%.10f, dLang=%.16f, isMostLeftTile=%r, isSourceLocalFolder=%r). " %(n, reconId, lng1, curr_lat, lng2, curr_lat_next, dLng, isMostLeftTile, isSourceLocalFolder))
                self._fetcher.ProcessOneTile(lng1, curr_lat, lng2, curr_lat_next, osmFile, reconId, reconFile, isMostLeftTile, isSourceLocalFolder)
                
                isMostLeftTile = False
                curr_lng += dLng

    
            curr_lat += dLat

        self.Logger.warn( "ProcessGeoRegionMapToTiles(), GetNumTiles=%d.",  n)
        
        return TileIdList

    def GetFileNameByLocation(self, lng, lat):
        
        [i, j] = self.GetTileIndexByLocation(lng, lat, True)
        
        result = self.GetFileNameByIndex(i, j)
        
        return result
 
    
    def AdjustToGridLngLat(self, lng, lat):
                
        #http://stackoverflow.com/questions/14344207/how-to-convert-distancemiles-to-degrees
        #dLat = self.GRID_SIZE_IN_KM/(Const.KM_PER_ONE_LAT_DEGREE)
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        gridBottom_Lat = dLat * (math.floor((lat)/dLat + self.PRECISION_ROUND))
        if (self.Logger):
            self.Logger.debug("lat=%.8f, gridBottom_Lat=%.8f, diff=%.8f, dlat=%.8f" % (lat, gridBottom_Lat,  ((lat) - gridBottom_Lat), dLat))
        
        dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(gridBottom_Lat * (math.pi /180)))
        gridLeft_Lng = dLng * (math.floor((lng)/dLng + self.PRECISION_ROUND))
        if (self.Logger):
            self.Logger.debug("lng=%.8f, gridLeft_Lng=%.8f, diff=%.8f, dlng=%.8f" % (lng, gridLeft_Lng,  ((lng) - gridLeft_Lng), dLng)) 
        
        return [gridLeft_Lng, gridBottom_Lat]
    


    '''Estimate processing global Map data From OSM data format into RECON Map format'''
    def EstimateProcessingGlobalMap(self):
        
        #print ("EstimateProcessingGlobalMap.")
        j = 0
        lat = 0.0
        numberOfGrid = 0
        
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        
        MAX_LAT = 90.0 - self.OVERLAP_IN_DEGREE
        
        MAX_LNG = 360.0
          
        num_indexs_in_lat = int(math.ceil(MAX_LAT/dLat))  * 2
        max_num_Grid_in_lng = int(math.ceil(MAX_LNG/dLat)) 
        
        while (lat < MAX_LAT ):
            dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / math.cos(lat * math.pi / 180)
            numberOfGrid += int(math.ceil(MAX_LNG/dLng))
            print (str(j) +": " + str(int(math.ceil(MAX_LNG/dLng))))
            lat +=dLat
            j += 1
        
        print("NumberOfGridTotal=%d, NumberOfJ=%d, MaxNumGridInLat(j)=%d, MaxNumGridInLng(i)=%d, dlat=%f, dlng=%f" % ((numberOfGrid * 2), (j*2), num_indexs_in_lat, max_num_Grid_in_lng, dLat, dLng ))
        
    
    
    '''Process global Map data From OSM data format into RECON Map format'''
    def ProcessGlobalMap(self):
        i = 0
        j = 0
        
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        MIN_LAT = 0.0 
        MIN_LNG = 0.0
        MAX_LAT = 180.0 - dLat - self.OVERLAP_IN_DEGREE
        MAX_LNG = 360.0
        
        isCheckCompletion = True
        isMostLeftTile = True
        isSourceLocalFolder = False;
        
        lat = MIN_LAT
        startLat = 0.0
        endLat = 0.0
        startLng = 0.0
        endLng = 0.0

        
        
        n = 1;
        while (lat <= MAX_LAT ):
                
            startLat = lat - 90
            if (startLat == -90):
                startLat += self.OVERLAP_IN_DEGREE
            endLat = startLat + dLat + self.OVERLAP_IN_DEGREE

            lng = MIN_LNG
            i = 0
            dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(startLat * math.pi /180.0))
            isMostLeftTile = True
                        
            while (lng < MAX_LNG):
                
                startLng = lng
                if startLng > 180:
                    startLng -= 360
                    
                endLng = startLng + dLng + self.OVERLAP_IN_DEGREE
                
                
                if (endLng > 180):
                    endLng = 180
                
                reconId = self.CombineTileSubIndicesString(i, j)
                #reconPathFileName = self.GeneratePathFolder(i, j) + reconId +".xml"
                reconPathFileName = Const.OUTPUT_PATH + reconId +".xml"
                osmFile = Const.OSM_MAPS_PATH +"o%s.xml" %reconId
                
                self.Logger.warn("ProcessGlobalMap. index="+ str(n) +", j=" + str(j) + ", i=" +str(i) + ", lat=" +str(startLat) + ", lng=" +str(startLng) + ", reconfileName=" + reconPathFileName + ", lenght=" + str(endLng - startLng) )
                

                if (isCheckCompletion):
                    if (os.path.exists(reconPathFileName)):
                        self.Logger.warn("skip generation file=%s.", reconPathFileName)
                        lng += dLng
                        i += 1
                        n += 1
                        isMostLeftTile = False
                        continue
                    else:
                        isCheckCompletion = False
                        
                if (os.path.exists(osmFile)):
                    isSourceLocalFolder = True
                else:
                    isSourceLocalFolder = False
                        

                osmFile = Const.OSM_MAPS_PATH +"o%s.xml" %reconId
                 
                if (not Const.NORTH_HEMISPERE_ONLY or startLat >= 0 ):
                    self._fetcher.ProcessOneTile(startLng, startLat, endLng, endLat, osmFile, reconId, reconPathFileName, isMostLeftTile, isSourceLocalFolder) 
                
                
                isMostLeftTile = False                
                lng += dLng
                i += 1
                n += 1
                

            
            lat +=dLat
            j += 1
            
        self.Logger.warn("ProcessGlobalMap.total_index="+ str(n) )
        #complete all ProcessGlobalMap
            
    '''Process north Heimsphere Map data From OSM data format into RECON Map format'''
    def ProcessNorthHeimsphereMap(self):
        i = 0
        j = 0
        
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        MIN_LAT = 90.0  #north heimsphere only
        MIN_LNG = 0.0
        MAX_LAT = 180.0 - dLat - self.OVERLAP_IN_DEGREE
        MAX_LNG = 360.0
        
        isMostLeftTile = True
        isCheckCompletion = True
        isSourceLocalFolder = False
        
        lat = MIN_LAT
        startLat = 0.0
        endLat = 0.0
        startLng = 0.0
        endLng = 0.0
        
        [max_lng, max_lat] = self.GetMaxTileIndexs()
        j = int (max_lat/2)
        
        
        n = 1;
        while (lat <= MAX_LAT ):
            startLat = lat - 90
            if (startLat == -90):
                startLat += self.OVERLAP_IN_DEGREE
            endLat = startLat + dLat + self.OVERLAP_IN_DEGREE

            lng = MIN_LNG
            i = 0
            dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(startLat * math.pi /180.0))
            isMostLeftTile = True
                        
            while (lng < MAX_LNG):
                
                startLng = lng
                if startLng > 180:
                    startLng -= 360
                    
                endLng = startLng + dLng + self.OVERLAP_IN_DEGREE
                
                
                if (endLng > 180):
                    endLng = 180
                
                reconId = self.CombineTileSubIndicesString(i, j)
                #reconPathFileName = self.GeneratePathFolder(i, j) + reconId +".xml"
                reconPathFileName = Const.OUTPUT_PATH + reconId +".xml"
                osmFile = Const.OSM_MAPS_PATH +"o%s.xml" %reconId
                
                self.Logger.warn("ProcessNorthHeimsphereMap. index="+ str(n) +", j=" + str(j) + ", i=" +str(i) + ", lat=" +str(startLat) + ", lng=" +str(startLng) + ", reconfileName=" + reconPathFileName + ", lenght=" + str(endLng - startLng) )
                
                [i1,j1] = self.GetTileIndexByLocation(startLng, startLat)
                
                if (i != i1 or j != j1):
                    self.Logger.error("i=%i, i1=%i, j=%i, j1=%i", i, i1, j, j1)
                  
            
                if (isCheckCompletion):
                    if (os.path.exists(reconPathFileName)):
                        self.Logger.warn("skip generation file=%s.", reconPathFileName)
                        lng += dLng
                        i += 1
                        n += 1
                        isMostLeftTile = False
                        continue
                    else:
                        isCheckCompletion = False
                        if (os.path.exists(osmFile)):
                            isSourceLocalFolder = True
                        else:
                            isSourceLocalFolder = False
                        
                

                osmFile = Const.OSM_MAPS_PATH +"o%s.xml" %reconId
                 
                if (not Const.NORTH_HEMISPERE_ONLY or startLat >= 0 ):
                    self._fetcher.ProcessOneTile(startLng, startLat, endLng, endLat, osmFile, reconId, reconPathFileName, isMostLeftTile, isSourceLocalFolder)
                    self.Logger.debug("skip process map data.")
                
                isMostLeftTile = False
                lng += dLng
                i += 1
                n += 1
                

            
            lat +=dLat
            j += 1
            
        self.Logger.warn("ProcessGlobalMap.total_index="+ str(n) )
        #complete all ProcessNorthHeimsphereMap



    def GeneratePathFolder(self, i, j):
        
        level1Folder = Const.OUTPUT_PATH + str(j) +"/"
        
        #print "level1Folder=" +level1Folder
        if (not os.path.isdir(level1Folder)):
            os.mkdir(level1Folder)
        
        
        return level1Folder
    
    
      
    def GetMaxTileIndexs(self):
        
        #print ("GetMaxTileIndexs.")
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR/(math.cos(0.0))
        
        MAX_LAT = 90.0 - self.OVERLAP_IN_DEGREE
        MAX_LNG = 360.0
          
        max_index_in_lat = int(math.ceil(MAX_LAT/dLat))  * 2
        max_index_in_lng = int(math.ceil(MAX_LNG/dLng)) 
        
        
        self.Logger.debug("GetMaxTileIndexs, max_index_in_lng=%i, max_index_in_lng=%i.", max_index_in_lng, max_index_in_lat )
        
        return [max_index_in_lng, max_index_in_lat]
        
      
    '''----------------------------------------GetTileNameList--------------------------------------------'''
    def GetTileListByBound(self, left, bottom, right, top):
              
        #Logger.debug( "ProcessGeoRegionMapToTiles(), (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
        if (bottom > top):
            #Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None

        if (left<=-180 or right>180):
            #Logger.error( "ProcessGeoRegionMapToTiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        if (bottom <= -90):
            bottom = -90.0 + self.OVERLAP_IN_DEGREE
            
        if (top >= 90):
            top = 90 - self.OVERLAP_IN_DEGREE
            
        
        left1 = (left +360) % 360
        right1 = (right + 360 ) % 360
        
        result = []
        if (left1 <= right1):
            result = self.GetTileListByBoundSub(left1, bottom, right1, top)
        else:
            result += self.GetTileListByBoundSub(left1, bottom, 360, top)
            result += self.GetTileListByBoundSub(0, bottom, right1, top)
        
        #print ("Return %i tile name in the list as below:" % (len(result)))
        self.printList(result)
        return result
                
    def GetTileListByBoundSub(self, left, bottom, right, top):
            
        #Adjust left bottom into grid left bottom
        [left1, bottom1] = self.AdjustToGridLngLat(left, bottom)

        dLat = self.TILE_HEIGHT_IN_DEGREE #0.05 degree
        
        
        TileIdList = []    
        i = 0
        j = 0
        n = 0


        curr_lat = (bottom1)
        
        while (curr_lat < top):
                     
            #curr_lat_next = curr_lat + dLat
    
            [left2, bottom2] =  self.AdjustToGridLngLat(left, curr_lat)
            
            curr_lng = (left2)
            dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR / (math.cos(float(curr_lat) * math.pi /180))

            while (curr_lng < right):
                
                curr_lng_next = curr_lng + dLng
            
                n += 1    
                
                [i, j] = self.GetTileIndexByLocation(curr_lng, curr_lat, False)
                
                reconId = self.CombineTileSubIndicesString(i, j)
                TileIdList.append(reconId)
                        
                lng1 = curr_lng
                if (lng1>180):
                    lng1 -= 360
                
                lng2 = curr_lng_next
                if (lng2>180):
                    lng2 -= 360
                    
                curr_lng += dLng

    
            curr_lat += dLat
        
        return TileIdList
    
    def printList(self, alist):
        #print "Num oF List=" + str(len(alist))
        for item in alist:
            print item

        
        
    def GetOneReconTileFromId(self, tileId, bound, osmFile, reconFile):    
        (lngmin, latmin, lngmax,latmax) = bound
        self._fetcher.ProcessOneTile(float(lngmin), float(latmin), float(lngmax), float(latmax), osmFile, tileId, reconFile, False, False)
    
                
    '''----------------------------------------GetTileNameList--------------------------------------------'''
      
    
    
    

    def GetIDStringByIndex(self, i, j):
        
        sign_lng = 1
        sign_lat = 1
    
        positive_i = i
        positive_j = j
        
        if (i < 0):
            sign_lng = -1
            positive_i *= -1
            
        if (j <0):
            sign_lat = -1
            positive_j *= -1
        
       
        result = ''
        if (sign_lng == -1):
            result += 'n'
            
        result += str(positive_i) +"_"
        
        if (sign_lat == -1):
            result +='n'
            
        result += str(positive_j)
        
        return result
    
    def GetFileNameByIndex(self, i, j):
        
        sign_lng = 1
        sign_lat = 1
    
        positive_i = i
        positive_j = j
        
        if (i < 0):
            sign_lng = -1
            positive_i *= -1
            
        if (j <0):
            sign_lat = -1
            positive_j *= -1
        
       
        result = ''
        if (sign_lng == -1):
            result += 'n'
            
        result += str(positive_i) +"_"
        
        if (sign_lat == -1):
            result +='n'
            
        result += str(positive_j) +'.xml'
        
        return result
    
    
    def GeneratePathFolder_2level(self, i, j):
        
        ai = int(math.floor(abs(i)/self.FOLDER_SIZE))
        aj = int(math.floor(abs(j)/self.FOLDER_SIZE))
        
        level1Folder = Const.OUTPUT_PATH
        
        if (i < 0):
            level1Folder +="n"
        level1Folder += str(ai)
        level1Folder +="_"
        if (j < 0):
            level1Folder +="n"
        level1Folder += str(aj)
        
        
        
        
        print "level1Folder=" +level1Folder
        
        if (not os.path.isdir(level1Folder)):
            os.mkdir(level1Folder)
        
        
        bi = abs(i)%(self.FOLDER_SIZE)     
        bj = abs(j)%self.FOLDER_SIZE
        
        level2Folder = level1Folder
        level2Folder += "/"
        
        if (i < 0):
            level2Folder +="n"
        level2Folder += str(bi)
        level2Folder +="_"
        if (j < 0):
            level2Folder +="n"
        level2Folder += str(bj)
        level2Folder += "/"
        
    
        print "level2Folder=" +level2Folder
        
        if (not os.path.isdir(level2Folder)):
            os.mkdir(level2Folder)
        
        return level2Folder

  
    
    def CreateFolderHelper(self, basePath, i, j):
        '''path1 = basePath[0] + str(i) +"_" +str(j) + "/"
        path2 = basePath[1] + "n" + str(i) +"_" +str(j) + "/"
        path3 = basePath[2] + "n" + str(i) +"_n" +str(j) + "/"
        path4 = basePath[3] + str(i) +"_n" +str(j) + "/"'''
        
        basePath[0] += str(i) +"_" +str(j) + "/"
        basePath[1] += "n" + str(i) +"_" +str(j) + "/"
        basePath[2] += "n" + str(i) +"_n" +str(j) + "/"
        basePath[3] += str(i) +"_n" +str(j) + "/"
        
        
        if (not os.path.isdir(basePath[0])):
            os.mkdir(basePath[0])
        
        if (not os.path.isdir(basePath[1])):
            os.mkdir(basePath[1])
                
        if (not os.path.isdir(basePath[2])):
            os.mkdir(basePath[2])
            
        if (not os.path.isdir(basePath[3])):
            os.mkdir(basePath[3])
            
        return basePath
    
    
    def GeneratePathFolder_3level(self, i, j):
        
        path = [Const.OUTPUT_PATH, Const.OUTPUT_PATH, Const.OUTPUT_PATH, Const.OUTPUT_PATH]
        
        #Level 1 folders
        i1 = int(math.floor(i/self.FOLDER_SIZE_SQUARE))
        j1 = int(math.floor(j/self.FOLDER_SIZE_SQUARE))
        ir = i % self.FOLDER_SIZE_SQUARE
        jr = j % self.FOLDER_SIZE_SQUARE
        path = self.CreateFolderHelper(path, i1, j1)
        
        #Level 2 folders
        i2 = int(math.floor(ir/self.FOLDER_SIZE))
        j2 = int(math.floor(jr/self.FOLDER_SIZE))
        ir = ir % self.FOLDER_SIZE
        jr = jr % self.FOLDER_SIZE
        path = self.CreateFolderHelper(path, i2, j2)
        
        
        #Level 3 folders
        i3 = int (ir)
        j3= int (jr)
        path = self.CreateFolderHelper(path, i3, j3)
        
        return path
    
        
    def GeneratePathFolder_4level(self, i, j):
        
        path = [Const.OUTPUT_PATH, Const.OUTPUT_PATH, Const.OUTPUT_PATH, Const.OUTPUT_PATH]
        

        #print "FOLDER_SIZE_CUBIC=" + str(self.FOLDER_SIZE_CUBIC)
        #print "FOLDER_SIZE_SQUARE=" + str(self.FOLDER_SIZE_SQUARE)
        
        #Level 1 folders
        i1 = int(math.floor(i/self.FOLDER_SIZE_CUBIC))
        j1 = int(math.floor(j/self.FOLDER_SIZE_CUBIC))
        ir = i % self.FOLDER_SIZE_CUBIC
        jr = j % self.FOLDER_SIZE_CUBIC
        
        if (i1 >0 or j1>0):
            print i1, j1
                
        path = self.CreateFolderHelper(path, i1, j1)
        
        
        #Level 2 folders
        i2 = int(math.floor(ir/self.FOLDER_SIZE_SQUARE))
        j2 = int(math.floor(jr/self.FOLDER_SIZE_SQUARE))
        ir = ir % self.FOLDER_SIZE_SQUARE
        jr = jr % self.FOLDER_SIZE_SQUARE
        path = self.CreateFolderHelper(path, i2, j2)
        
        #Level 3 folders
        i3 = int(math.floor(ir/self.FOLDER_SIZE))
        j3 = int(math.floor(jr/self.FOLDER_SIZE))
        ir = ir % self.FOLDER_SIZE
        jr = jr % self.FOLDER_SIZE
        path = self.CreateFolderHelper(path, i3, j3)
        
        
        #Level 3 folders
        i4 = int (ir)
        j4= int (jr)
        path = self.CreateFolderHelper(path, i4, j4)
        
        return path

    def ConvertMToLngDegree(self, lng, lat, widthInM):
        #print "ConvertMToLngDegree" 
        widthInDegree = (360.0) * (widthInM ) / (self.EQUITORIAL_CIRCUMFERENCE * (math.cos((lat) * math.pi /180.0)))
        #print widthInDegree
        return widthInDegree
        
        
        
    def ConvertMToLatDegree(self, lng, lat, heightInM):
        #print "ConvertMToLatDegree"
        heightInDegree = (360.0) * (heightInM ) / (self.MERIDIONAL_CIRCUMFERENCE)
        #print heightInDegree
        return heightInDegree
    
    '''----------------------------------------SearchAndCopyTile--------------------------------------------'''
        
    def SearchAndCopyTile(self, tileIdList):
        print tileIdList
        tileIdList.sort();
        print tileIdList
    
        newTileList = []
        newFolderList = []
        for tileId in tileIdList:
            if (not tileId):
                continue
            newTileList.append([str(tileId), False])
            folder = tileId[:4]
            if (folder not in newFolderList):
                newFolderList.append(folder)


        now = datetime.datetime.now().strftime("%Y-%m-%d_%I_%M")
        dstbase = os.path.join(self.DST_ROOT, now)
        print dstbase
        if (not os.path.isdir(dstbase)):
            os.mkdir(dstbase)

        numCopied = 0

        startTimeStr = time.strftime("%c")
        start_time = time.time()
        print("---------Start to search tile files at time %s--------" + time.strftime("%c"))
        
        
        for path, subdirs, files in os.walk(self.SRC_ROOT):
            '''
            print "============="
            print "path="+ path
            print subdirs
            print files
            '''
            
            currFolderName=os.path.split(path)
            currFolder = currFolderName[1]
            print "search folder =" + currFolder
            if (currFolder not in newFolderList):
                continue;
            
            isAllCopied = True
            for tile in newTileList:
                [tileId1, isCopied1] = tile
                if (isCopied1):
                    continue
                fileName =tileId1 +".xml"
                src = os.path.join(path, fileName)
                if (os.path.isfile(src)):
                    dst = os.path.join(dstbase, fileName)
                    print "copying src=" + src + " to dst=" +dst
                    shutil.copy2(src, dst)
                    tile[1] = True #set isCopied1= True
                    numCopied += 1
                    
                else:
                    isAllCopied = False
                    
            #for end
            
            if (isAllCopied):
                print "Done, copied all files."
                break;
                    
                
        missedList = []
        for tileId2, isCopied2 in newTileList:
            if (isCopied2):
                continue
            missedList.append(tileId2)
            #print "Not found file tileId=" + tileId
        print "Num_Of_File_Copied=" +str(numCopied)
        print "Num_Of_File_Missed=" +str(len(missedList))
        print "Missed_Tiles=" + str(missedList)

        elapsed_time = (time.time() - start_time)/(60.00)
        print(("----------End to search tile files: the_number_copied_files=%i, start=%s, end=%s, elapsed_time=%.2f minues.") % (numCopied, startTimeStr, time.strftime("%c"), elapsed_time))
    
        
    '''----------------------------------------SearchAndCopyTile--------------------------------------------'''
    def getMapBoundariesByFile(self, inputFileName):

        MapBoundaries = []
        warning = "In the file=" + inputFileName + ", the format is: 'Region_Name';left;bottom;right;top"
        inputFile = open(inputFileName, 'r')
        lines = inputFile.read().splitlines()
        for line in lines:
            if (line==None):
                continue
            
            line = line.strip()
            if (line==""):
                continue
            
            if (line[0]=="#"):
                continue
            line = line.translate(None, "[]")
            
            fields = line.split(";")
            length = len(fields)
            if (length  < 5):
                print warning
                print "error line=" + line
                continue
                
            i = 0
            bbx = []
            for field in fields:
                if (i == 0):
                    bbx.append(field)
                elif (i==6): #tileType
                    bbx.append(int(field))
                else:
                    bbx.append(float(field))
                i += 1
            
    
            print "bbx=" + str(bbx) 
            MapBoundaries.append(bbx)
        
        inputFile.close()
        return MapBoundaries

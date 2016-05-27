'''
Created on Nov 29, 2013
Create ReconFetcher (Recon Instruments Map) class to get and process open street boundry resrouce.
@author
'''

import time
import logging.handlers

from ReconConst import Const
from ReconFetcher import ReconFetcher
from ReconProcessor import OsmParser, RimWriter
from ReconGlobalSweep import GlobalSweep
from ReconUtils import ReconUtils


IsFromExternalRequirement = True #when true, read from ReconConst


def ProcessRegionMaps():
    #MapBoundaries =[["Test", -118.1782, 37.0681, -118.0059, 37.0982],]
    #MapBoundaries =[["Test", -125.6330, 61.3168, -125.6021, 61.3340],]
    MapBoundaries  =[["Killington, USA", -72.85, 43.591, -72.75, 43.6705],]
    #MapBoundaries =[["Victoria, BC", -123.934, 48.307, -123.289, 48.930],]
    #MapBoundaries =[["Greate Vancouver, BC, Canada", -123.2748, 49.1718, -122.8680, 49.3256],]
    #MapBoundaries =[["City Vancouver, BC, Canada", -123.2618, 49.1925, -123.0067, 49.2969],]
    #MapBoundaries =[["FraserRiver, BC, Canada", -122.9206, 49.1927, -122.8189, 49.2312],]
    #MapBoundaries =[["Empty Tile with Ocean", -123.3615, 49.275005, -123.2563, 49.2867],]
    #MapBoundaries =[["Point Rebort- Empty Tile with Ocean", -123.2860, 48.8665, -123.2594, 48.9073],]
    #MapBoundaries =[["Point Rebort- Empty Tile with Ocean", -123.303937, 49.175, -123.222506, 49.2],]
    #MapBoundaries = [["hawaii", -154.984359, 19.4, -154.957899,  19.425], ]
    #MapBoundaries =[["NationalBorder, Canada", -122.7614, 49.0016, -122.7490, 49.0024],]
    #MapBoundaries =[["Toronto, ON, Canada", -79.6392727, 43.5806328, -79.1132193, 43.8554425],]
    #MapBoundaries =[["PortLand, OR, USA.", -122.8367489, 45.4325354, -122.4720252, 45.6528812],]
    #MapBoundaries =[["New York, NY, USA.", -74.25909, 40.477399, -73.7001715, 40.917577],]
    #MapBoundaries =[["Madison, WI, USA", -89.566397, 42.998071, -89.246452, 43.171916],] #['42.998071', '43.171916', '-89.566397', '-89.246452']
    
    if (IsFromExternalRequirement):
        MapBoundaries = globalSweep.getMapBoundariesByFile(Const.INPUT_GEO_REGIONS_FILE)
    
    processedBbxFileForWrite = open(Const.PROCESSED_BBX_FILE, "a")
    for boundry in MapBoundaries:
        if isBBXprocessed(boundry[0]):
            Logger.warn("---------Skipping processed bbx=" + boundry[0] )
            continue
        startTimeStr = time.strftime("%c")
        start_time = time.time()
        tileType = Const.TYPE_UNKNOWN
        if (len(boundry) >= 6):
            tileType = int(boundry[5])
            rimWriter.tileType = tileType
            #print "rimWriter.tileType=" + str(rimWriter.tileType)
        Logger.warn("---------Start Process MAP with boundary of " + boundry[0] +" at time %s--------" + time.strftime("%c"))
        globalSweep.ProcessGeoRegionMapToTiles(boundry[1], boundry[2], boundry[3], boundry[4]) #break into tiles and process each of tile 
        processedBbxFileForWrite.write(boundry[0]+"\n")
        processedBbxFileForWrite.flush()
        elapsed_time = (time.time() - start_time)/(3600.00)
        Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
    processedBbxFileForWrite.close()
        
def isBBXprocessed(name):
    if (name):
        name = name.translate(None, "\"\'")
    if (not name):
        return False
    f = open(Const.PROCESSED_BBX_FILE, "r")
    lines = f.read().splitlines()
    for line in lines:
        if (line):
            line = line.translate(None, "\"\'")
        if (line == name):
            f.close()
            return True
    #end for
    f.close()
    return False
            
def ListTileIDsByGeoRegions():
    MapBoundaries  =[["Killington, USA", -72.85, 43.591, -72.75, 43.6705],]
    
    if (IsFromExternalRequirement):
        MapBoundaries = globalSweep.getMapBoundariesByFile(Const.INPUT_GEO_REGIONS_FILE)
    
    processedBbxFileForWrite = open(Const.TILE_IDS_FILE, "w")
    idsList = []
    for boundry in MapBoundaries:
        Logger.warn("---Start Process MAP with boundary of " + boundry[0])
        processedBbxFileForWrite.write(str(boundry)+"\n")
        idsList = globalSweep.GetTileListByBound(boundry[1], boundry[2], boundry[3], boundry[4])
        for idstr in idsList:
            processedBbxFileForWrite.write(idstr+"\n")
            processedBbxFileForWrite.flush()
        Logger.warn("---End Process MAP Boundary: " + boundry[0])
    processedBbxFileForWrite.close()
    
    
def ProcessGlobalMaps():
    globalSweep.EstimateProcessingGlobalMap()
    globalSweep.ProcessGlobalMap()

    
def PrintVancouverTileIDs():
    #tileIdList = globalSweep.GetTileListByGeoRegion(-123.2618, 49.1925, -123.0067, 49.2969)
    globalSweep.GetTileListByGeoRegion(-123.2618, 49.1925, -123.0067, 49.2969)
#     print (tileIdList)

def FetchTileByName():
    from ReconTileFetcherByName import ReconTileFetcherByName
    fetcherByName = ReconTileFetcherByName()
    
    #cityName = "Madison, WI, USA"
    cityName = "Whistler, BC, Canada"
    
    bound = fetcherByName.GetBoundaryByName(cityName);
    if (not bound):
        Logger.error("CityName=%s, bound=%s, error", cityName, str(bound))
    
    globalSweep.ProcessGeoRegionMapToTiles(float(bound[2]), float(bound[0]), float(bound[3]), float(bound[1]))
       
    

'''----------------------------------------Main------------------------------------------------'''

#Setup Logger
Logger = logging.getLogger()
Logger.setLevel(Const.LOG_DEFAULT_LEVEL)


fileHandler = logging.handlers.RotatingFileHandler("{0}/{1}.log".format(Const.LOG_PATH, Const.LOG_FILE_NAME), maxBytes=Const.LOG_FILE_MAX_BYTES, backupCount=Const.LOG_BACKUP_COUNT)
logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
fileHandler.setFormatter(logFormatter)
Logger.addHandler(fileHandler)

if(Const.LOG_TO_CONSOLE):

    consoleHandler = logging.StreamHandler()
    consoleHandler.setFormatter(logFormatter)
    Logger.addHandler(consoleHandler)


Logger.warning("=========================START RECON_MAIN=========================================================.")



Logger.info("Start ReconFetcher.")
reconFetcher = ReconFetcher(-122.7843, 49.1848, Logger, True)
'''reconFetcher.Capabilities()'''

Logger.info("Start Writer.")
#rimWriter = RimWriter(reconFetcher.osmApi, reconFetcher, Logger,Const.DEBUG)
rimWriter = RimWriter(reconFetcher, Logger,Const.DEBUG)

Logger.info("Start Parser.")
#osmParser = OsmParser(reconFetcher.osmApi, reconFetcher, rimWriter, Logger, Const.DEBUG)
osmParser = OsmParser(reconFetcher, rimWriter, Logger, Const.DEBUG)
rimWriter.setParser(osmParser)
reconFetcher.setParser(osmParser)
reconFetcher.setWriter(rimWriter)

globalSweep = GlobalSweep(reconFetcher, Logger)
reconUtils = ReconUtils(osmParser, rimWriter, Logger)
osmParser.setUtils(reconUtils)
    

'''----------------Process GLobal Maps---------------------------------------------------------------------------'''
#globalSweep.EstimateProcessingGlobalMap()
#NumberOfGridTotal=66,133,560, NumberOfJ=7200, MaxNumGridInLat(j)=7200, MaxNumGridInLng(i)=14400, dlat=0.025000, dlng=57.199765
#globalSweep.GetMaxTileIndexs()
#ProcessGlobalMaps()
#globalSweep.ProcessNorthHeimsphereMap()


'''----------------Test  Maps-------------------------------------------------------------------------------------'''
#PrintVancouverTileIDs()

#tileId = globalSweep.GetTileIdByLocation(90, 0)
#print tileId


'''
#TileIdList = globalSweep.GetTileListByGeoRegion((180-0.045*3), 0, 180, 0.045*10)
globalSweep.GetTileIndexByLocation( -123.0256575000,49.2500000000, True)
globalSweep. GetTileIndexByLocation(0, 0)
globalSweep. GetTileIndexByLocation(180, 0)
globalSweep. GetTileIndexByLocation(-179.5, 0) 
globalSweep. GetTileIndexByLocation(-178, 0)
globalSweep. GetTileIndexByLocation(-1, 0)
globalSweep. GetTileIndexByLocation(-0.00001, 0)
globalSweep. GetTileIndexByLocation(-0.0000001, 0)
globalSweep. GetTileIndexByLocation(-0.000000001, 0)
'''

#globalSweep.stevFunc(10, 60)
#globalSweep. GetTileIndexByLocation(0, 89.99)




'''----------------Test Get a MAP BOX and save to file-------------------------------------------------------------------------'''
#print globalSweep.CombineTileSubIndicesString(1, 2)
#map1 = reconFetcher.GetOneMap(-118.4743, 35.4404, -118.4146, 35.4804)
# reconFetcher.SaveOneRawTileToFileByHttp(-122.8642, 49.1848, -122.7843, 49.2247, "mapdata/osm.xml")
# osmParser.ParseAndWriteOneTile("mapdata/osm_relation1.xml", "1", "mapdata/1.xml");
# osmParser.Test_VerifyXML("mapdata/1.xml")
# osmParser.clearCacheDisk()
 
 
 
 
#tileList = [555300063, 555300064, 555400062]
# tileList = [555400062]
# for tileId in tileList:
#     tileId = str(tileId)
#     bound = reconUtils.GetBoundaryByTileId(int(tileId))
#     print bound
#     osmFile = "./map_osm/o" + tileId + ".xml"
#     reconFile = "map_recon/" + tileId + ".xml"
#          
#     #osmParser.ParseAndWriteOneTile(osmFile, tileId, reconFile)
#     globalSweep.GetOneReconTileFromId(tileId, bound, osmFile, reconFile)


'''----------------Tile Fetcher By City Name----------------------------------------------------------------------'''
'''
FetchTileByName()
'''
'''----------------Process Geo Region Maps------------------------------------------------------------------------'''

ProcessRegionMaps()
#ListTileIDsByGeoRegions();

Logger.warning("=========================END RECON_MAIN==========================================.")




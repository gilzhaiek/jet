import sys
import logging.handlers

from ReconConst import Const
from ReconFetcher import ReconFetcher
from ReconProcessor import OsmParser, RimWriter
from ReconGlobalSweep import GlobalSweep
from ReconUtils import ReconUtils
import os



class ReconTileFetcherByCentralLocation():
        
    def __init__(self, globalSweep, logger):
        self.globalSweep = globalSweep
        self.Logger = logger
            
    def __del__(self):
        return [];
    
         
    def GetTilesIdListByCentralLocation(self):
        argv = sys.argv
        
        if ((not argv) or (len(argv) < 4)):
            usage = "Usage: python ReconTileFetcherByCentralLocation central_lng, central_lat, widthInKM, heightInKM"
            print usage
            return
        lng = float(argv[1])
        lat = float(argv[2])
        widthInMeter = float( argv[3] ) * 1000
        heightInMeter =  widthInMeter
        if (len(argv) >= 5):
            heightInMeter = float( argv[4] ) * 1000
        
        self.TilesIdListByCentralLocation(lng, lat, widthInMeter, heightInMeter)
    
    def TilesIdListByCentralLocation(self, lng, lat, widthInMeter, heightInMeter):
        bbx = self.ConvertFromCenterToBoundingBox(lng, lat, widthInMeter, heightInMeter)
        #print (lng, lat)
        #print bbx    
        self.globalSweep.GetTileListByBound(float(bbx[0]), float(bbx[1]), float(bbx[2]), float(bbx[3]))
        

                    
    def FetchTilesByCentralLocation(self):
        argv = sys.argv
        
        if ((not argv) or (len(argv) < 4)):
            usage = "Usage: python ReconTileFetcherByCentralLocation central_lng, central_lat, widthInKM, heightInKM "
            print usage
            return
        lng = float(argv[1])
        lat = float(argv[2])
        widthInMeter = float( argv[3] ) * 1000
        heightInMeter =  widthInMeter
        if (len(argv) >= 5):
            heightInMeter = float( argv[4]) * 1000
        
        self.QueryTilesByCentralLocation(lng, lat, widthInMeter, heightInMeter)

         
    def QueryTilesByCentralLocation(self, lng, lat, widthInMeter, heightInMeter):

        bbx = self.ConvertFromCenterToBoundingBox(lng, lat, widthInMeter, heightInMeter)
        print bbx        
        self.globalSweep.ProcessGeoRegionMapToTiles(bbx[0], bbx[1], bbx[2], bbx[3])
        
    
    def SearchTilesByCentralLocation(self):
        argv = sys.argv
        
        if ((not argv) or (len(argv) < 4)):
            usage = "Usage: python ReconTileFetcherByCentralLocation central_lng, central_lat, widthInKM, heightInKM"
            print usage
            return
        lng = float(argv[1])
        lat = float(argv[2])
        widthInMeter = float( argv[3] ) * 1000
        heightInMeter =  widthInMeter
        if (len(argv) >= 5):
            heightInMeter = float( argv[4] ) * 1000
        
        bbx = self.ConvertFromCenterToBoundingBox(lng, lat, widthInMeter, heightInMeter)
        #print (lng, lat)
        #print bbx
        tileIdList = self.globalSweep.GetTileListByBound(float(bbx[0]), float(bbx[1]), float(bbx[2]), float(bbx[3]))
        #print tileIdList
        self.globalSweep.SearchAndCopyTile(tileIdList)
        
    def ConvertFromCenterToBoundingBox(self, lng, lat, widthInMeter, heightInMeter):
        
        if ((lng <-180) or (lng>180)):
            print "Invalid Longitude lng=" + str(lng)
            exit(0)
        if ((lat<-90) or (lat>90)):
            print "Invalid Latitude lat=" + str(lat)
            exit(0)
        if(widthInMeter < 0):
            print "Invalid WidthInMeter" + str(widthInMeter)
            exit(0)
        if (heightInMeter < 0):
            print "Invalid HeightInMeter" + str(heightInMeter)
            exit(0)
            
        
        halfWidthInDegree = self.globalSweep.ConvertMToLngDegree(lng, lat, (widthInMeter/2.0))
        halfHeightInDegree = self.globalSweep.ConvertMToLatDegree(lng, lat, (heightInMeter/2.0))
        #print "halfWidthInDegree=" + str(halfWidthInDegree)
        #print "halfHeightInDegree=" + str(halfHeightInDegree)
        
        
        lngmin = lng - halfWidthInDegree
        lngmax = lng + halfWidthInDegree
        latmin = lat - halfHeightInDegree
        latmax = lat + halfHeightInDegree
        
        while (lngmin <= -180):
            lngmin += 360.0
        while (lngmax > 180):
            lngmax -= 360.0
                
        return (lngmin, latmin, lngmax, latmax)
        
        
    def GetTilesByCentralLocation_test(self):
        lng= -123.139544
        lat= 49.234235

        widthInMeter = 10 
        heightInMeter = 10 
        self.GetTilesByCentralLocation(lng, lat, widthInMeter, heightInMeter);
        #self.GetTilesIdListByCentralLocation(lng, lat, widthInMeter, heightInMeter);


#print("----------------------------------------Main------------------------------------------------")    
if __name__ == "__main__":
    isEnableLog = True
    
    '''----------------------------------------Logger------------------------------------------------'''
    if (isEnableLog):
        if (not os.path.isdir(Const.LOG_PATH)):
            os.mkdir(Const.LOG_PATH)
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
    else:
        Logger = None
    
    '''----------------------------------------Initial------------------------------------------------'''
    reconFetcher = ReconFetcher(-122.7843, 49.1848, Logger, True)
    rimWriter = RimWriter(reconFetcher, Logger,Const.DEBUG)
    osmParser = OsmParser(reconFetcher, rimWriter, Logger, Const.DEBUG)
    rimWriter.setParser(osmParser)
    reconFetcher.setParser(osmParser)
    reconFetcher.setWriter(rimWriter)
    globalSweep = GlobalSweep(reconFetcher, Logger)
    reconUtils = ReconUtils(osmParser, rimWriter, Logger)
    osmParser.setUtils(reconUtils)
    
    Const.TILE_TOP_TYPE = Const.POLYGON_TYPE_LIST[Const.TYPE_LAND]
    Const.TILE_RIGHT_TYPE = Const.POLYGON_TYPE_LIST[Const.TYPE_LAND]
    
    '''----------------------------------------GetTiles------------------------------------------------'''
    ''' "Usage: python ReconTileFetcherByCentralLocation central_lng, central_lat, widthInKM, heightInKM '''
    reconTileFetcherByCentralLocation = ReconTileFetcherByCentralLocation(globalSweep, Logger)
    #reconTileFetcherByCentralLocation.GetTilesByCentralLocation_test()
    
    reconTileFetcherByCentralLocation.GetTilesIdListByCentralLocation()
    #reconTileFetcherByCentralLocation.FetchTilesByCentralLocation()
    #reconTileFetcherByCentralLocation.SearchTilesByCentralLocation()
    

#print("----------------------------------------Main End---------------------------------------------")    


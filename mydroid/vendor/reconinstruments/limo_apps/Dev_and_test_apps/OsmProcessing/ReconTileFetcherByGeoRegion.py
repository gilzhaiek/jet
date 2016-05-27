from ReconConst import Const
from ReconFetcher import ReconFetcher
from ReconProcessor import OsmParser, RimWriter
from ReconGlobalSweep import GlobalSweep
from ReconUtils import ReconUtils
import logging.handlers
import sys
import os


class ReconTileFetcherByGeoRegion():

    
    DEFAULT_BOUND =  [0, 0,0,0] 
    '''[LEFT, BOTTOM, RIGHT, TOP]'''
    bbx = []


    
    def __init__(self, globalSweep, logger):
        self.globalSweep = globalSweep
        self.Logger = logger
            
    def __del__(self):
        return [];
    
    
        
      
        
        
    def ListTileIdsByGeoRegion(self):
        argv = sys.argv
        if (argv and len(argv) > 4):
            self.bbx = [float(argv[1]), float(argv[2]), float(argv[3]), float(argv[4])]
        else:
            self.bbx = self.DEFAULT_BOUND
            

        globalSweep = GlobalSweep(None, None)
                
        tileIdList = globalSweep.GetTileListByBound(self.bbx[0], self.bbx[1], self.bbx[2], self.bbx[3])
        print "Total_Num_Of_Tiles=" + str(len(tileIdList))
        print tileIdList
        

    def FetchTilesByGeoRegion(self):
        argv = sys.argv
        if (argv and len(argv) > 4):
            self.bbx = [float(argv[1]), float(argv[2]), float(argv[3]), float(argv[4])]
        else:
            self.bbx = self.DEFAULT_BOUND
            
        self.globalSweep.ProcessGeoRegionMapToTiles(self.bbx[0], self.bbx[1], self.bbx[2], self.bbx[3])
        

    def SearchTilesByGeoRegion(self):
        argv = sys.argv
        if (argv and len(argv) > 4):
            self.bbx = [float(argv[1]), float(argv[2]), float(argv[3]), float(argv[4])]
        else:
            self.bbx = self.DEFAULT_BOUND
            
        globalSweep = GlobalSweep(None, None)
                
        tileIdList = globalSweep.GetTileListByBound(self.bbx[0], self.bbx[1], self.bbx[2], self.bbx[3])
        print tileIdList
        globalSweep.SearchAndCopyTile(tileIdList)
        
        
        
    def SplitGeoRegion(self,defaultsize=500):
        SIZE_OF_GEO_REGION_IN_KM = defaultsize #500km
        argv = sys.argv
        srcFileName = Const.INPUT_GEO_REGION_FILE
        if (argv and len(argv) >= 2): #source input file
            srcFileName = str(argv[1])
        print srcFileName 
        
        dstFileName = Const.INPUT_GEO_REGIONS_FILE
        if (argv and len(argv) >= 3): #dstnation output file
            dstFileName = str(argv[2])
        print dstFileName

    
        MapBoundaries = globalSweep.getMapBoundariesByFile(srcFileName)
        outputFile = open(dstFileName, 'w')
#        self.writeHeader(outputFile)
        
        
                
        for boundry in MapBoundaries:
#            outputFile.writelines("\n")
#            outputFile.writelines("#" +str(boundry)+"\n")
            if (boundry[1] < boundry[3]): #left<right
                geoRegionList = self.SplitToSmallGeoRegion(boundry, SIZE_OF_GEO_REGION_IN_KM)
            else:
                geoRegionList = self.SplitToSmallGeoRegion([boundry[0], boundry[1], boundry[2], 180.0, boundry[4]], SIZE_OF_GEO_REGION_IN_KM)
                geoRegionList.extend(self.SplitToSmallGeoRegion([boundry[0], -179.999, boundry[2], boundry[3], boundry[4]], SIZE_OF_GEO_REGION_IN_KM))
                
            
            for geoRegion in geoRegionList:
                outputFile.writelines(str(geoRegion))


        outputFile.close()
            
    def SplitToSmallGeoRegion(self, boundry, sizeKm):
        
        name = boundry[0]
        if (name):
            name = name.translate(None, "\"\'")
        left = boundry[1]
        bottom = boundry[2]
        right = boundry[3]
        top = boundry[4]

        if (left<=-180 or right>180 or bottom <=-90 or top >=90):
            print ( "SplitToSmallGeoRegion(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        print ("SplitToSmallGeoRegion(), (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
        if (bottom > top):
            print  ("SplitToSmallGeoRegion(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        if (left > right):
            print ( "SplitToSmallGeoRegion(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f)." % (left, bottom, right, top))
            return None
        
        geoRegionList = []
        n = 0

        curr_lat = bottom
        dLat = globalSweep.ConvertMToLatDegree(left, bottom, (sizeKm * 1000))
        
        while (curr_lat < top):
            curr_lat_next = curr_lat + dLat
            if (curr_lat_next > top):
                curr_lat_next = top

            curr_lng = left
            dLng = globalSweep.ConvertMToLngDegree(left, bottom, (sizeKm * 1000))
            while (curr_lng < right):
                
                curr_lng_next = curr_lng + dLng
                if (curr_lng_next > right):
                    curr_lng_next = right
                n += 1    
                
                #geoRegionList.append(str([name+ str(n), curr_lng, curr_lat, curr_lng_next, curr_lat_next]) + "\n")
                geoRegionList.append("#[%s%-6i; %10f; %10f; %10f; %10f]\n" % (name, n, curr_lng, curr_lat, curr_lng_next, curr_lat_next))
                curr_lng += dLng

    
            curr_lat += dLat

        '''eof while'''
        
        print( "SplitToSmallGeoRegion(), GetNumSmallGeoRegion=%d.",  n)
        return geoRegionList
        

    def writeHeader(self, outfile):
        outfile.writelines("####################################################################################################################\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("# This is the configure one or more Geo Regions to process and get Recon Instruments Map in RECON TILE format.     #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("# You can select one or more Geo Regions to process it (by removing the comment sign '#').                         #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("# It ignores all of the comment line that starts with sign '#'                                                     #\n")
        outfile.writelines("#                                                                                                                  #\n")                                                                                                               #
        outfile.writelines("# Each Geo Region format: GEO_REGION_NAME; left_longitude; bottom_latitude; right_longitude; top_latitude          #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("#                                                                                                                  #\n")
        outfile.writelines("####################################################################################################################\n")

                    
#print("----------------------------------------Main------------------------------------------------")    
if __name__ == "__main__":
    isEnableLog = False
        
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


    argv = sys.argv
    theSize = 500
    if (len(argv) > 3):
        theSize = int(argv[3])

    '''----------------------------------------Main_Tiles------------------------------------------------'''
    seacher = ReconTileFetcherByGeoRegion(globalSweep, Logger)
    
    #seacher.ListTileIdsByGeoRegion()
    #seacher.FetchTilesByGeoRegion()
    #seacher.SearchTilesByGeoRegion()
    seacher.SplitGeoRegion(theSize)
# Usage: python ReconTileFetcherByGeoRegion.py inputfile outputfile size




#print("----------------------------------------Main------------------------------------------------")    


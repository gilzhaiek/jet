#!/usr/bin/python 
from geocoder import Geocoder
from ReconConst import Const
from ReconFetcher import ReconFetcher
from ReconProcessor import OsmParser, RimWriter
from ReconGlobalSweep import GlobalSweep
from ReconUtils import ReconUtils
import logging.handlers
import sys
import os
from optparse import OptionParser
#import codecs

class ReconTileFetcherByName():
        
    def __init__(self, globalSweep, logger):
        self.globalSweep = globalSweep
        self.Logger = logger
            
    def __del__(self):
        return [];
    
    DISPLAY_NAME = 'display_name'
    BOUNDINGBOX = 'boundingbox'
    ADDRESS = 'address'
    CITY_NAME = ''
    #CITY_NAMES = {"Vancouver, BC, Canada", "Toronto, ON, Canada"}
     
    CITY_NAMES = []


    
    def geocode_city(self, city_state_country):
        client = Geocoder("http://open.mapquestapi.com/nominatim/v1/search?format=json")
        response = client.geocode(city_state_country)    #fetcher.FetchTilesByCity()
        #fetcher.FetchTilesByCityName("Madison, WI, USA")
        
        if (not response):
            print >> sys.stderr, ("no response for city=%s" % (city_state_country))
            return None
        
        
        #print (response)
        #print response[0][self.DISPLAY_NAME]
        #print response[0][self.BOUNDINGBOX]
        #print response[0][self.ADDRESS]
        
        '''return boundary of city in format [bottom, top, left, right]'''
        return response[0][self.BOUNDINGBOX]
    
    def GetGeoRegionByCities(self):
        dstFileName = "InputGeoRegionsCities.txt"
        EXTENSION = 0.20 #20km extension for each direction
        outputFile = open(dstFileName, 'w')
        
        self.CITY_NAMES.sort()
        for city in self.CITY_NAMES:
            
            region = self.geocode_city(city)
            if (region == None):
                raise Exception("exception get georegion by city=" + city)
            outLine = ("['%s'; %10f; %10f; %10f; %10f]\n" % (city, (float(region[2]) - EXTENSION), (float(region[0]) - EXTENSION), (float(region[3]) + EXTENSION), (float(region[1]) + EXTENSION)))
            outputFile.writelines(str(outLine))
            
        outputFile.close()        
        

    def newGetGeoRegionByWorldCities(self,inputfilename,outputfilename):
        inputFile = open(inputfilename, "r")
        outputFile = open(outputfilename, 'w')
        lines = inputFile.read().splitlines()
        
        for line in lines:
            if (line==None):
                continue
            
            line = line.strip()
            if (line==""):
                continue
            
            if (line[0]=="#"):
                continue
            
            fields = line.split(",")
            length = len(fields)
            if (length  < 2):
                print >> sys.stderr,"error line=" + line
                continue
            city = fields[0] +", " + fields[1]
            #If no distance field, and format as city, state, country
            if (length >2):
                city += ", " + fields[2]
            distance = 0
            try:
                #distance =  float(fields[2])*1000
                distance =  float(fields[3])*1000
            except Exception:
                pass
                distance = 50.0*1000
            
            print >> sys.stderr, "city=" + city;
            #print "distant=" + str(distance);
            
            outLine = self.CityRegionWithPadding(city, distance)
            outputFile.writelines(str(outLine))
            
        outputFile.close()  


    def CityRegionWithPadding(self,city, distance):
        left = 0.0
        right = 0.0
        bottom = 0.0
        top = 0.0
        region = self.geocode_city(city)
        if (region == None):
            print >> sys.stderr, "exception get georegion by city=" + city
            #raise Exception("exception get georegion by city=" + city)
        else:
            print >> sys.stderr, region
            left = float(region[2])
            right = float(region[3])
            bottom = float(region[0])
            top = float(region[1])
                        
        lng = (left + right)/2.0
        lat = (bottom + top)/2.0
        distanceInDegree =  self.globalSweep.ConvertMToLngDegree(lng, lat, (distance))
        doubleDistanceInDegree = distanceInDegree*2
        
        lngDiff = (right - left)/2.0
        latDiff = (top - bottom)/2.0
        diff = lngDiff;
        if (latDiff < diff):
            diff = latDiff
        if (doubleDistanceInDegree < diff):
            print >> sys.stderr, "using city Boundary"
            outLine = ("#['%s'; %10f; %10f; %10f; %10f]\n" % (city, left, bottom, right, top))
        else:
            outLine = ("#['%s'; %10f; %10f; %10f; %10f]\n" % (city, lng-distanceInDegree, lat - distanceInDegree, lng + distanceInDegree, lat + distanceInDegree))
        return outLine
        
    def GetGeoRegionByWorldCities(self):
        inputfilename = "usa_50k.txt"
        outputfilename = "InputUSA_50k.txt"
        self.newGetGeoRegionByWorldCities(inputfilename,outputfilename)
            

    def GetTileIdListByCity(self):
        '''Get city name from passing paramater'''
        argv = sys.argv
        if (argv and len(argv) > 1):
            #print("city=%s" % argv[1])
            city = argv[1]
        else:
            city = self.CITY_NAME
            
        return self.GetTileIdListByCityName(city)
        
        
    def GetTileIdListByCityName(self, cityName,padding=0):
        '''Get Boundary of city'''
        globalSweep = GlobalSweep(None, None)
        bound = self.geocode_city(cityName)
        if (not bound):
            exit(0)
        
        '''Get Tile name List'''
        #return globalSweep.GetTileListByBound(float(bound[2]), float(bound[0]), float(bound[3]), float(bound[1]))
        bound = self.GetBoundByCityWithExtraKm(globalSweep, cityName, padding)
        return globalSweep.GetTileListByBound(bound[0], bound[1], bound[2], bound[3])    
    

    

    def newFetchTilesByCity(self,city,padding=3.0):
        bound = self.geocode_city(city)
        if (not bound):
            exit(0)
        
        bbx = self.GetBoundByCityWithExtraKm(globalSweep, city, padding)  
        print >> sys.stderr, bbx
        
        self.globalSweep.ProcessGeoRegionMapToTiles(bbx[0], bbx[1], bbx[2], bbx[3])
        
        
    def FetchTilesByCity(self):
        '''Get city name from passing paramater'''
        argv = sys.argv
        if (argv and len(argv) > 1):
            #print("city=%s" % argv[1])
            city = argv[1]
        else:
            city = self.CITY_NAME

        self.newFetchTilesByCity(city,3.0)
        # # globalSweep = GlobalSweep(None, None)
        # bound = self.geocode_city(city)
        # if (not bound):
        #     exit(0)
        
        # bbx = self.GetBoundByCityWithExtraKm(globalSweep, city, 3.0)  
        # print bbx
        
        # self.globalSweep.ProcessGeoRegionMapToTiles(bbx[0], bbx[1], bbx[2], bbx[3])
        
    
    def SearchTilesByCity(self):
        '''Get city name from passing paramater'''
        argv = sys.argv
        if (argv and len(argv) > 1):
            #print("city=%s" % argv[1])
            city = argv[1]
        else:
            city = self.CITY_NAME
        
        globalSweep = GlobalSweep(None, None)
        bound = self.geocode_city(city)
        if (not bound):
            exit(0)
        
        bbx = self.GetBoundByCityWithExtraKm(globalSweep, city, 3.0)  
        print >> sys.stderr, bbx
        
        tileIdList = globalSweep.GetTileListByBound(bbx[0], bbx[1], bbx[2], bbx[3])
        print tileIdList
        self.globalSweep.SearchAndCopyTile(tileIdList)
        
    
    def GetBoundByCityWithExtraKm(self, globalSweep, cityName, extraKm=0.0):
        '''Get Boundary of city'''
        bound = self.geocode_city(cityName) #boundary of city in format [bottom, top, left, right]
        if (not bound):
            exit(0)
        
        extraLngInDegree = 0.0
        extraLatInDegree = 0.0
        
        if (extraKm > 0):
            extraLngInDegree = globalSweep.ConvertMToLngDegree(float(bound[2]), float(bound[0]), extraKm*1000)
            extraLatInDegree = globalSweep.ConvertMToLatDegree(float(bound[2]), float(bound[0]), extraKm*1000)
            #print >> sys.stderr, "extraLngInDegree=" + str(extraLngInDegree)
            #print >> sys.stderr, "extraLatInDegree=" + str(extraLatInDegree)
            
        '''Get Tile name List'''
        newBound = [float(bound[2])-extraLngInDegree, float(bound[0])-extraLatInDegree, float(bound[3])+extraLngInDegree, float(bound[1])+extraLatInDegree] #[left, bottom, right, top]
        return newBound
    
    def GetTileIdListByGeoRegion(self, left, bottom, right, top):
        '''Get Tile name List'''
        globalSweep = GlobalSweep(None, None)
        return globalSweep.GetTileListByBound(left, bottom, right, top)
    
        
    def GetBoundaryByName(self, name):
        bound = self.geocode_city(name)
        if (bound):
            return bound
        else:
            return None
    

                    
                    
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
    
    '''----------------------------------------Main_Tiles------------------------------------------------'''
    # Parsing options:
    parser = OptionParser()
    parser.add_option("-m","--method", type="string", dest="method",
                      help="METHOD is one of [\"City2TileIds\",\"Cities2Tasks\",\
\"Region2TileIds\"]")
    parser.add_option("-a","--address",action="store", type="string", dest="address")
    parser.add_option("-p","--padding",action="store", type="float", dest="padding")
    parser.add_option("-I","--input-file",action="store", type="string", dest="inputfile")
    parser.add_option("-O","--output-file",action="store", type="string", dest="outputfile")
    parser.add_option("-l","--min-lat",action="store", type="float", dest="minlat")
    parser.add_option("-L","--max-lat",action="store", type="float", dest="maxlat")
    parser.add_option("-g","--min-lng",action="store", type="float", dest="minlng")
    parser.add_option("-G","--max-lng",action="store", type="float", dest="maxlng")
    fetcher = ReconTileFetcherByName(globalSweep, Logger)

    
    (options, args) = parser.parse_args(sys.argv) 
    if (options.method is None or options.method == "City2TileIds"):
        fetcher.GetTileIdListByCityName(options.address,options.padding)
    elif (options.method == "Cities2Tasks"):
        fetcher.newGetGeoRegionByWorldCities(options.inputfile,options.outputfile)
    elif (options.method == "Region2TileIds"):  
        fetcher.globalSweep.GetTileListByBound(options.minlng,options.minlat,
                                               options.maxlng,options.maxlat)
    elif (options.method == "City2Task"):  
        print fetcher.CityRegionWithPadding(options.address,options.padding*1000)

    #fetcher.newFetchTilesByCity("Vancouver, BC, Canada"); # Not working
    #fetcher.SearchTilesByCity();
    #fetcher.GetGeoRegionByCities();
    #fetcher.newGetGeoRegionByWorldCities("usa_50k.txt","InputUSA_50k.txt")
    

    

#print("----------------------------------------Main------------------------------------------------")    


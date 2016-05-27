'''
@author: simonwang
'''
import logging.handlers
from xml.dom.minidom import parse
import os.path
from os import listdir
from os.path import isfile, join

#from ReconConst import Const


'''GLobal variable'''

Logger = None

'''Const Variables'''
class CompareConst():
    
    LOG_PATH = "logs/"
    LOG_FILE_NAME = "osmCompare"
    LOG_TO_CONSOLE = True
    LOG_DEFAULT_LEVEL = logging.DEBUG
    #LOG_DEFAULT_LEVEL = logging.INFO
    #LOG_DEFAULT_LEVEL = logging.WARN
    LOG_FILE_MAX_BYTES = 1*1024*1024
    LOG_BACKUP_COUNT = 10
    
    
    BASE_PATH="./map_recon1/"
    TAG_BOUNDS = "bounds"
    TAG_RMO ="rmo"
    TAG_POINT = "point"
    TAG_NODE = "node"
    TAG_WAY = "way"
    TAG_RELATION = "relation"
    TAG_ATTR = "attr"
    TAG_LAT = "lat"
    TAG_LON = "lon"
    RECON_TYPE = "reconType"
    TYPE_PARK = "park"
    TYPE_WOOD = "wood"
    TYPE_LAND = "land"
    TYPE_OCEAN = "ocean"
    TYPE_CITY_TOWN = "citytown"
    PRECISE = 9
    
    polygonTypeList =[TYPE_PARK, TYPE_WOOD, TYPE_LAND, TYPE_OCEAN, TYPE_CITY_TOWN]
                

class OsmCollection:

    def __init__(self, fileName):
        self.fileName = fileName
        self.reset(fileName)
        self.isSourceRecon = True
        
    def reset(self, fileName):
        self.fileName = fileName
        self.counter = 0
        self.numCachedRmo = [0,]
        self.cachedRmos = {}
        
        self.numCachedNode = [0,]
        self.cachedNodes = {}
        self.numCachedWay = [0,]
        self.cachedWays = {}
        self.numCachedRelation = [0,]
        self.cachedRelations = {}
        
        
        self.respBounds = {}  #response bounds in string format
        
        if (fileName):
            nameList = os.path.split(fileName)
            size = len(nameList)
            pureFileName = nameList[size -1]
            
            if (pureFileName[0] == 'o'):
                self.isSourceRecon = False
            else:
                self.isSourceRecon = True
            
             
        #Logger.debug("writer.reset(), cachedNode=%d, cachedWay=%d, cachedRelation=%d", self.numCachedRmo[0], self.numCachedWay[0], self.numCachedRelation[0] )
            
            
    def _cacheElement(self, elementDict, numCachedElement, cachedElements):
        elementId = elementDict[CompareConst.TAG_ATTR][u"id"]
        
        numCachedElement[0] += 1
        cachedElements[elementId] = elementDict
        
        
'''Compare Cases'''    
class TileCompare():
    file1 = None
    file2 = None
    osm1 = None
    osm2 = None

    
    def __init__ (self, logger):
        Logger.debug("__init__")
        
        self.Logger = logger
        self.reset()
        self.osm1 = OsmCollection(None)
        self.osm2 = OsmCollection(None)


    def setFiles(self, file1, file2):
        self.file1 = file1
        self.file2 = file2
        self.osm1.reset(file1)
        self.osm2.reset(file2)
  
        
    def reset(self):
        self.file1 = None
        self.file2 = None
        self.osm1 = None
        self.osm2 = None
    
    def loadParseTile(self, filename, osm):
        print("loadParseTile=%s s." % (filename)) 
        try:
            if (os.path.exists(filename)):
                self.Logger.debug("file exist.")
        except Exception:
            self.Logger.warn("open file exception.")
            raise Exception ("File is not existed exception! ( "+ filename +" )")
                
        try:
            dom = parse(filename)
            self._ParseOsmByDom(dom, osm)
            
        except Exception:
            print Exception
            self.Logger.warn("parse file exception, file=" + filename)
            raise Exception ("parse file exception ( "+ filename + " )")
        
    
    def compareFolders(self, folder1, folder2):
        print ("compare folder1=%s, folder2=%s" % (folder1, folder2))
        if (not os.path.isdir(folder1)):
            Logger.error("folder1=%s not existed" % (folder1))
            return
            
        if (not os.path.isdir(folder2)):
            Logger.error("folder2=%s not existed" % (folder2))
            return
        
        #compartor = TileCompare(Logger)
    
        fileList1 = [ f for f in listdir(folder1) if isfile(join(folder1,f)) ]
        fileList2 = [ f for f in listdir(folder2) if isfile(join(folder2,f)) ]
        fileList1.sort()
        fileList2.sort()
        
        for file1 in fileList1:
            if file1 not in fileList2:
                Logger.error("file=%s not existed in foler2=%s" % (file1, folder2))
                continue
            
            f1 = folder1 + "/" + file1
            f2 = folder2 + "/" + file1
            print "file1=" + f1
            print "file2=" + f2
            self.setFiles(f1, f2)
            self.CompareTile()
        
        for file2 in fileList2:
            if file2 not in fileList1:
                Logger.error("file2=%s not existed in foler1=%s" % (file2, folder1))
                continue
        
    def CompareTile(self):
        Logger.debug("CompareTile.")

        self.loadParseTile(self.file1, self.osm1)
        self.loadParseTile(self.file2, self.osm2)
        if (self.osm1.isSourceRecon):
            self.compareTileReconFormat()
        else:
            self.compareTileOSMFormat()
    
    def compareTileReconFormat(self):
        osm1 = self.osm1
        osm2 = self.osm2
       
        Logger.warn("--------------------------------------------------------------------------------------------------------------------------")
        Logger.warn("osm1.file1=%s, size=%i ---vs--- osm2.file2=%s, size=%i" % (osm1.fileName, osm1.numCachedRmo[0], osm2.fileName, osm2.numCachedRmo[0]))
                
        if (osm1.numCachedRmo[0] == 0):
            Logger.error("error compare, tile1.")
            return
        if (osm2.numCachedRmo[0] == 0):
            Logger.error("error compare, tile2.")
            return

        keys1 = osm1.cachedRmos.keys()
        keys2 = osm2.cachedRmos.keys()
        
        
        '''compare bound'''
        if (not self.compareBound(osm1.respBounds, osm2.respBounds)):
            Logger.error("boundary of two tiles are different.")
            #Logger.error (self.__printRMO(None, None, osm1.respBounds[u'minlon'], osm1.respBounds[u'minlat'], osm1.respBounds[u'maxlon'], osm1.respBounds[u'maxlat']))
            #Logger.error (self.__printRMO(None, None, osm2.respBounds[u'minlon'], osm2.respBounds[u'minlat'], osm2.respBounds[u'maxlon'], osm2.respBounds[u'maxlat']))
        
        '''compare tile 1 agaist tile2'''
        numSame = 0
        for key in keys1:
            if (key in keys2):
                #self.compareSubObj(key, osm1, osm2)
                numSame += 1
            else:
                Logger.error("obj=%s not existed in file2=%s" % (key, self.osm2.fileName))
                tileDict = osm1.cachedRmos[key]
                tileType = tileDict[u"attr"]["reconType"]
                print tileType
     
        
        '''compare tile 2 agaist tile 1'''
        for key in keys2:
            if (key not in keys1):
                Logger.error("obj=%s not existed in file1=%s" % (key, self.osm1.fileName))
                
        if ((osm1.numCachedRmo[0] == osm2.numCachedRmo[0]) and (osm1.numCachedRmo[0] == numSame)):
            Logger.warn("osm1.file1=%s, osm2.file2=%s is identical." % (osm1.fileName, osm2.fileName))
        else:
            Logger.warn("num_same=%i, osm1.file1=%s, num_diff=%i; osm2.file2=%s, num_diff=%i." % (numSame, osm1.fileName, (osm1.numCachedRmo[0] - numSame), osm2.fileName, (osm2.numCachedRmo[0] - numSame)))
        Logger.warn("=============================================================================================================================")
                
    
    def compareTileOSMFormat(self):
        osm1 = self.osm1
        osm2 = self.osm2
       
        print ("osm1.file1=%s" % (osm1.fileName))
        print ("osm2.file2=%s" % (osm2.fileName))
        Logger.debug("osm1_numNodes=%i, osm2_numNodes=%i, osm1_numWays=%i, osm2_numWays=%i, osm1_numRelations=%i, osm2_numRelations=%i." 
                     %(osm1.numCachedNode[0], osm2.numCachedNode[0], osm1.numCachedWay[0], osm2.numCachedWay[0], osm1.numCachedRelation[0], osm2.numCachedRelation[0]))
        
             
        '''compare nodes'''        
        keys1 = osm1.cachedNodes.keys()
        keys2 = osm2.cachedNodes.keys()
        self.compareItems(keys1, keys2, CompareConst.TAG_NODE)
        
        '''compare ways'''        
        keys1 = osm1.cachedWays.keys()
        keys2 = osm2.cachedWays.keys()
        self.compareItems(keys1, keys2, CompareConst.TAG_WAY)
        
        '''compare relations'''        
        keys1 = osm1.cachedRelations.keys()
        keys2 = osm2.cachedRelations.keys()
        self.compareItems(keys1, keys2, CompareConst.TAG_RELATION)
        

        '''compare bound'''
        if (not self.compareBound(osm1.respBounds, osm2.respBounds)):
            Logger.error("boundary of two tiles are different.")
            #Logger.error (self.__printRMO(None, None, osm1.respBounds[u'minlon'], osm1.respBounds[u'minlat'], osm1.respBounds[u'maxlon'], osm1.respBounds[u'maxlat']))
            #Logger.error (self.__printRMO(None, None, osm2.respBounds[u'minlon'], osm2.respBounds[u'minlat'], osm2.respBounds[u'maxlon'], osm2.respBounds[u'maxlat']))
        
    def compareItems(self, keys1, keys2, tag_type):            
        '''compare tile 1 agaist tile2'''
        for key in keys1:
            if (key not in keys2):
                Logger.error("obj=%s (type=%s) not existed in file2=%s" % (key, tag_type, self.osm2.fileName))
                Logger.error("--------------------------------------------------------------------------------------------------------------------------")
        
        '''compare tile 2 agaist tile 1'''
        for key in keys2:
            if (key not in keys1):
                Logger.error("obj=%s (type=%s) not existed in file2=%s" % (key, tag_type, self.osm1.fileName))
                
                
                

            
        
    def compareBound(self, bound1, bound2):
        if ((bound1[u'minlon'] == bound2[u'minlon']) and (bound1[u'minlat'] == bound2[u'minlat']) and 
            (bound1[u'maxlon'] == bound2[u'maxlon']) and (bound1[u'maxlat'] == bound2[u'maxlat']) ):
            return True
        else:
            return False
    
    def compareSubObj(self, key, osm1, osm2):
        value1 = osm1.cachedRmos[key]
        value2 = osm2.cachedRmos[key]
        if (not self.compareDictValue(value1, value2)):
            v =1
            #Logger.error("key=%s, diff value1=%s." %(key, str(value1).translate(None, "'")))
            #Logger.error("key=%s, diff value2=%s." %(key, str(value2).translate(None, "'")))
                    
    def compareDictValue(self, val1, val2):
        value1 = val1["point"]
        value2 = val2["point"]
        size1 = len(value1)
        size2 = len(value2)
        if (size1 != size2):
            Logger.error("size1=%i, size2=%i." % (size1, size2))
            #Logger.error("pointList1=%s, pointList2=%s." % (str(value1).translate(None, "'"), str(value2).translate(None, "'")))
            return False
        #print ("num_points=%i, points=%s." % (size1, str(value1).translate(None, "'")))
        #print ("num_points=%i, points=%s." % (size2, str(value2)))
        i = 0
        while (i< size1):
            if (value1[i] != value2[i]):
                #Logger.error("i=%i, different value1=%s, value2=%s" % (i, value1[i], value2[i]))
                return False
            i += 1
        
        return True
        
    
    def _testOutofBoundary(self, osm):
        Logger.debug("_testOutofBoundary")
        
        if (not osm.cachedRmos):
            return
        isInside = True
        for rmoId, rmoDict in osm.cachedRmos.items():
            isInside = self.__IsRMOInside(rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat'])
            if (not isInside):
                Logger.debug( rmoId + " is out of boundary!!!")
                Logger.debug (self.__printRMO(rmoId, rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
            
            
        
        
    def _testPolygon(self, osm):
        Logger.debug ("_testPolygon")
        
        if (not osm.cachedRmos):
            return
        
        for rmoId, rmoDict in osm.cachedRmos.items():
            attrDict = rmoDict[CompareConst.TAG_ATTR]
            rmoType = attrDict[CompareConst.RECON_TYPE]
            
            if not rmoType in CompareConst.polygonTypeList:
                continue
            
            pointList = rmoDict[CompareConst.TAG_POINT]
            size = len(pointList)
            if ((pointList[0] == pointList[size-1])):
                continue
            Logger.debug (rmoId + " is not cycle polygon!!!")
            Logger.debug (self.__printRMO(rmoId, rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
            Logger.debug ("id=%s, type=%s, size=%d, point[0]=(%.10f, %.10f), point[%d]=(%.10f, %.10f), not is polygon" % (rmoId, rmoType, size, pointList[0][0], pointList[0][1],size, pointList[size-1][0], pointList[size-1][1]))
            
        return
             
       
    def _ParseOsmByDom(self, dom, osm):
        Logger.debug ("parseOsmByDom")
        numItem = 0
        
        if (osm.isSourceRecon):
            dom = dom.getElementsByTagName("rim")[0]
            osm.reset(osm.fileName)
            
            for elem in dom.childNodes:
                osm.counter += 1
                
                if elem.nodeName == CompareConst.TAG_RMO:
                    
                    rmoDict = self.__DomParseNode(elem)
                    osm._cacheElement(rmoDict, osm.numCachedRmo, osm.cachedRmos)
                    numItem += 1
                    
                elif elem.nodeName == CompareConst.TAG_BOUNDS:
                    osm.respBounds = self.__DomParseBounds(elem)
                    Logger.debug ("bounds minlon=%.10f, minlat=%.10f, maxlon=%.10f, maxlat=%.10f." % (osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
                    #Logger.debug ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (osm.reqBoundsInStr[u'minlon'], osm.reqBoundsInStr[u'minlat'], osm.reqBoundsInStr[u'maxlon'], osm.reqBoundsInStr[u'maxlat']))
        else:
            dom = dom.getElementsByTagName("osm")[0]
            osm.reset(osm.fileName)
            
            for elem in dom.childNodes:
                osm.counter += 1
                
                if elem.nodeName == CompareConst.TAG_NODE:
                    
                    nodeDict = self.__DomParseNode(elem)
                    osm._cacheElement(nodeDict, osm.numCachedNode, osm.cachedNodes)
                    numItem += 1
               
                elif elem.nodeName == CompareConst.TAG_WAY:
                    
                    wayDict = self.__DomParseWay(elem)
                    osm._cacheElement(wayDict, osm.numCachedWay, osm.cachedWays)
                    numItem += 1
                
                elif elem.nodeName == CompareConst.TAG_RELATION:
                    
                    relationDict = self.__DomParseRelation(elem)
                    osm._cacheElement(relationDict, osm.numCachedRelation, osm.cachedRelations)
                    numItem += 1
                                     
                elif elem.nodeName == CompareConst.TAG_BOUNDS:
                    osm.respBounds = self.__DomParseBounds(elem)
                    Logger.debug ("bounds minlon=%.10f, minlat=%.10f, maxlon=%.10f, maxlat=%.10f." % (osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
                    #Logger.debug ("bounds minlon=%s, minlat=%s, 
                
    def __DomParseBounds(self, DomElement):
        """ Returns RelationData for the relation. """
        result = self.__DomGetAttributes(DomElement)
        result['minlon'] = round(float(result['minlon']), CompareConst.PRECISE)
        result['maxlon'] = round(float(result['maxlon']), CompareConst.PRECISE)
        result['minlat'] = round(float(result['minlat']), CompareConst.PRECISE)
        result['maxlat'] = round(float(result['maxlat']), CompareConst.PRECISE)
        
        
        return result
    
    
    def __DomParseRMO(self, DomElement):
        """ Returns RMO. """
        result = {}
        result[CompareConst.TAG_ATTR] = self.__DomGetAttributes(DomElement)
        result[CompareConst.TAG_POINT]  = self.__DomGetPoints(DomElement)
        return result
    
    def __DomGetAttributes(self, DomElement):
        """ Returns a formated dictionnary of attributes of a DomElement. """
        result = {}
        for k, v in DomElement.attributes.items():
            result[k] = v
        return result
        
    def __DomGetPoints(self, DomElement):
        """ Returns the list of points, which is dictionary. """
        result = []
        for t in DomElement.getElementsByTagName(CompareConst.TAG_POINT):
            result.append((round(float(t.attributes[CompareConst.TAG_LON].value), CompareConst.PRECISE), round(float(t.attributes[CompareConst.TAG_LAT].value), CompareConst.PRECISE)))
        return result 
    
    
    def __DomParseNode(self, DomElement):
        """ Returns NodeData for the node. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"] = self.__DomGetTag(DomElement)
        return result

    def __DomParseWay(self, DomElement):
        """ Returns WayData for the way. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"] = self.__DomGetTag(DomElement)
        result[u"nd"]  = self.__DomGetNd(DomElement)        
        return result
    
    def __DomParseRelation(self, DomElement):
        """ Returns RelationData for the relation. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"]    = self.__DomGetTag(DomElement)
        result[u"member"] = self.__DomGetMember(DomElement)
        return result
    
    
    def __DomGetTag(self, DomElement):
        """ Returns the dictionnary of tags of a DomElement. """
        result = {}
        for t in DomElement.getElementsByTagName("tag"):
            k = t.attributes["k"].value
            v = t.attributes["v"].value
            result[k] = v
        #self.Logger.debug( "tag=", result)
        return result
    
    def __DomGetNd(self, DomElement):
        """ Returns the list of nodes of a DomElement. """
        result = []
        for t in DomElement.getElementsByTagName("nd"):
            result.append(int(int(t.attributes["ref"].value)))
        return result 
    
    def __DomGetMember(self, DomElement):
        """ Returns a list of relation members. """
        result = []
        for m in DomElement.getElementsByTagName("member"):
            result.append(self.__DomGetAttributes(m))
        return result
    
    
    
    def __IsPointInside(self, point, minlon, minlat, maxlon, maxlat):
        result = True
        if (not point):
            result = False
        elif (point[0] > maxlon or point[0] < minlon ):
            result = False
        elif (point[1] > maxlat or point[1] < minlat ):
            result = False
        
        #if (not result):
        #    Logger.debug ("point=(%.10f, %.10f), minlon=%.10f, maxlon=%.10f, minlat=%.10f, maxlat=%.10f" % (point[0], point[1], minlon, maxlon, minlat, maxlon))
            
        return result;
    
    def __IsRMOInside(self, rmoDict, minlon, minlat, maxlon, maxlat):
        
        if (not rmoDict):
            return False
        
        isInside = False
        pointList = rmoDict[CompareConst.TAG_POINT]
        
        for point in pointList:
            isInside |= self.__IsPointInside(point, minlon, minlat, maxlon, maxlat)
            #isInside |= self.isPointInside(point, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat'])
            
        #print isInside
        return isInside
    
    def __printRMO(self, rmoId, rmoDict, minlon, minlat, maxlon, maxlat):
        result = "rmoId="
        if (rmoId):
            result += rmoId
        
        result += ", bound [minlon=%.10f, maxlon=%.10f, minlat=%.10f, maxlat=%.10f]" % (minlon, maxlon, minlat, maxlat)
        if (not rmoDict):
            return result
        
        #attrDict = rmoDict[CompareConst.TAG_ATTR]
        rmoType = rmoDict[CompareConst.RECON_TYPE]
        
        result += ". reconType=" + rmoType    
      
        
        pointList = rmoDict[CompareConst.TAG_POINT]
        
        for point in pointList:
            if (not point):
                continue
            result += ", (%.10f, %.10f)" % (point[0], point[1])
    
        return result
    
            
                

   
      
               
print ("-----------------------------------Compare Main------------------------------------------------")
if __name__=="__main__":
    
    print "Compare Main"

    '''Setup the logging'''
    Logger = logging.getLogger()
    Logger.setLevel(CompareConst.LOG_DEFAULT_LEVEL)
    fileHandler = logging.handlers.RotatingFileHandler("{0}/{1}.log".format(CompareConst.LOG_PATH, CompareConst.LOG_FILE_NAME), maxBytes=CompareConst.LOG_FILE_MAX_BYTES, backupCount=CompareConst.LOG_BACKUP_COUNT)
    logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
    fileHandler.setFormatter(logFormatter)
    Logger.addHandler(fileHandler)

    consoleHandler = logging.StreamHandler()
    consoleHandler.setFormatter(logFormatter)
    Logger.addHandler(consoleHandler)
    
    folder1 = u"map_recon_van/5574/"
    folder2 = u"map_recon/5574/"
    
    #filename = CompareConst.BASE_PATH + "5566/55666208.xml"
    #file1 = u"map_recon_2014-05-05_GreatVan/55666206.xml"
    #file2 = u"map_recon_2014-05-09_GreatVan/55666206.xml"
    #file2 = u"mapdata/55666206.xml"
    
    '''compare osm file'''
    #file1 = "map_osm1/o55606236.xml"
    #file2 = "map_osm3/o55606236.xml"
    
    '''compare recon file'''
    file1 =  "map_recon_van/5566/55666212.xml"
    file2 =  "map_recon/5566/55666212.xml"
    
    compartor = TileCompare(Logger)
    
    
    '''compare two tile files'''
    '''
    compartor.setFiles(file1, file2)
    compartor.CompareTile()
    '''    
    
    
    '''compare two folders'''
    compartor.compareFolders(folder1, folder2)
    
    
        


Logger.debug( "-----------------------------------END Compare_MAIN--------------------------------------------")




'''
@author: simonwang
'''

import unittest
import logging.handlers
from xml.dom.minidom import parse
#from ReconConst import Const


'''GLobal variable'''
filename=None
osm = None
Logger = None

'''Const Variables'''
class TestConst():
    
    LOG_PATH = "logs/"
    LOG_FILE_NAME = "osmtest"
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

    def __init__(self):

        self.configName = u'./ReconConfig.txt'
        self.node_features = [] 
        self.node_types = []
        self.way_features = [] 
        self.way_types = []
        self.way_actions = []
        self.relation_features = []
        self.relation_types = []
        self.relation_actions = []
        self.counter = 0
        
        
        self.parser = None
        self.nodeFile = None
        self.wayFile = None
        self.relationFile = None
        self.numNodesInDisk = [0,]
        self.numWaysInDisk = [0,]
        self.numRelationsInDisk = [0,]
        self.nodeDispTags = ()
        self.wayDispTags = ()
        self.relationDispTags = ()
         
        self.reset()
        
    def reset(self):
        self.numCachedNode = [0,]
        self.cachedRmos = {}
        
        self.numCachedWay = [0,]
        self.cachedWays = {}
        self.numCachedRelation = [0,]
        self.cachedRelations = {}
        
        self.actionList = []
        self.coastLineList = []
        self.cachedOceanList = []
        self.reqBoundsInStr = {} #request bounds in string format
        self.reqBoundsInFloat = {} #request bounds in float format
        
        self.respBounds = {}  #response bounds in string format
        self.isMostLeftTile = False
        self.oceanLandSN = 0
            
        #Logger.debug("writer.reset(), cachedNode=%d, cachedWay=%d, cachedRelation=%d", self.numCachedNode[0], self.numCachedWay[0], self.numCachedRelation[0] )
            
            
    def _cacheElement(self, elementDict, numCachedElement, cachedElements):
        elementId = elementDict[TestConst.TAG_ATTR][u"id"]
        
        numCachedElement[0] += 1
        cachedElements[elementId] = elementDict
        
        
'''Test Cases'''    
class TileTestCase1(unittest.TestCase):
    def setUp(self):
        Logger.debug("setup")
        self._loadParseTile(filename)

    
    def tearDown(self):
        Logger.debug("tearDown")
    
    def testTile(self):
        Logger.debug("testTile.")
        self._testOutofBoundary()
        self._testPolygon()
        
    def _loadParseTile(self, filename):
        Logger.debug("_loadParseTile")
        dom = parse(filename)
        self._ParseOsmByDom(dom)
        
        
    
    def _testOutofBoundary(self):
        Logger.debug("_testOutofBoundary")
        
        if (not osm.cachedRmos):
            return
        
        isInside = True
        for rmoId, rmoDict in osm.cachedRmos.items():
            isInside = self.__IsRMOInside(rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat'])
            if (not isInside):
                Logger.debug( rmoId + " is out of boundary!!!")
                Logger.debug (self.__printRMO(rmoId, rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
            
            
        
        
    def _testPolygon(self):
        Logger.debug ("_testPolygon")
        
        if (not osm.cachedRmos):
            return
        
        for rmoId, rmoDict in osm.cachedRmos.items():
            attrDict = rmoDict[TestConst.TAG_ATTR]
            rmoType = attrDict[TestConst.RECON_TYPE]
            
            if not rmoType in TestConst.polygonTypeList:
                continue
            
            pointList = rmoDict[TestConst.TAG_POINT]
            size = len(pointList)
            if ((pointList[0] == pointList[size-1])):
                continue
            Logger.debug (rmoId + " is not cycle polygon!!!")
            Logger.debug (self.__printRMO(rmoId, rmoDict, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
            Logger.debug ("id=%s, type=%s, size=%d, point[0]=(%.10f, %.10f), point[%d]=(%.10f, %.10f), not is polygon" % (rmoId, rmoType, size, pointList[0][0], pointList[0][1],size, pointList[size-1][0], pointList[size-1][1]))
            
        return
             
       
    def _ParseOsmByDom(self, dom, isAddToOutput=True):
        Logger.debug ("parseOsmByDom")
        numNode = 0
        #self.Logger.info(  "***ParseOsmByDom")
        dom = dom.getElementsByTagName("rim")[0]
        osm.reset()
        
        for elem in dom.childNodes:
            osm.counter += 1
            
            if elem.nodeName == TestConst.TAG_RMO:
                
                rmoDict = self.__DomParseRMO(elem)
                osm._cacheElement(rmoDict, osm.numCachedNode, osm.cachedRmos)
                numNode += 1
                
            elif elem.nodeName == TestConst.TAG_BOUNDS:
                osm.respBounds = self.__DomParseBounds(elem)
                Logger.debug ("bounds minlon=%.10f, minlat=%.10f, maxlon=%.10f, maxlat=%.10f." % (osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat']))
                #Logger.debug ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (osm.reqBoundsInStr[u'minlon'], osm.reqBoundsInStr[u'minlat'], osm.reqBoundsInStr[u'maxlon'], osm.reqBoundsInStr[u'maxlat']))
            
                
    def __DomParseBounds(self, DomElement):
        """ Returns RelationData for the relation. """
        result = self.__DomGetAttributes(DomElement)
        result['minlon'] = round(float(result['minlon']), TestConst.PRECISE)
        result['maxlon'] = round(float(result['maxlon']), TestConst.PRECISE)
        result['minlat'] = round(float(result['minlat']), TestConst.PRECISE)
        result['maxlat'] = round(float(result['maxlat']), TestConst.PRECISE)
        
        
        return result
    
    
    def __DomParseRMO(self, DomElement):
        """ Returns RMO. """
        result = {}
        result[TestConst.TAG_ATTR] = self.__DomGetAttributes(DomElement)
        result[TestConst.TAG_POINT]  = self.__DomGetPoints(DomElement)
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
        for t in DomElement.getElementsByTagName(TestConst.TAG_POINT):
            result.append((round(float(t.attributes[TestConst.TAG_LON].value), TestConst.PRECISE), round(float(t.attributes[TestConst.TAG_LAT].value), TestConst.PRECISE)))
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
        pointList = rmoDict[TestConst.TAG_POINT]
        
        for point in pointList:
            isInside |= self.__IsPointInside(point, minlon, minlat, maxlon, maxlat)
            #isInside |= self.isPointInside(point, osm.respBounds[u'minlon'], osm.respBounds[u'minlat'], osm.respBounds[u'maxlon'], osm.respBounds[u'maxlat'])
            
        #print isInside
        return isInside
    
    def __printRMO(self, rmoId, rmoDict, minlon, minlat, maxlon, maxlat):
        result = "rmoId=" +rmoId
        if (not rmoDict):
            return result
        
        attrDict = rmoDict[TestConst.TAG_ATTR]
        rmoType = attrDict[TestConst.RECON_TYPE]
        
        result += ". reconType=" + rmoType    
        result += ", [minlon=%.10f, maxlon=%.10f, minlat=%.10f, maxlat=%.10f]" % (minlon, maxlon, minlat, maxlat)
        
        pointList = rmoDict[TestConst.TAG_POINT]
        
        for point in pointList:
            if (not point):
                continue
            result += ", (%.10f, %.10f)" % (point[0], point[1])
    
        return result
    
            
                

   
      
    '''       
    def suite():
        suite = unittest.TestSuite()
        suite.addTest(TileTestCase1("testTile"))setup
        #suite.addTest(TileTestCase1("testPolygon", 1))
        return suite
    '''
               
print ("-----------------------------------Test Main------------------------------------------------")
if __name__=="__main__":
    
    print "Test Main"

    '''Setup the logging'''
    Logger = logging.getLogger()
    Logger.setLevel(TestConst.LOG_DEFAULT_LEVEL)
    fileHandler = logging.handlers.RotatingFileHandler("{0}/{1}.log".format(TestConst.LOG_PATH, TestConst.LOG_FILE_NAME), maxBytes=TestConst.LOG_FILE_MAX_BYTES, backupCount=TestConst.LOG_BACKUP_COUNT)
    logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
    fileHandler.setFormatter(logFormatter)
    Logger.addHandler(fileHandler)

    consoleHandler = logging.StreamHandler()
    consoleHandler.setFormatter(logFormatter)
    Logger.addHandler(consoleHandler)
    
    
    #filename = TestConst.BASE_PATH + "5566/55666208.xml"
    filename = "mapdata/55736191.xml"
    osm = OsmCollection()
    unittest.main()




Logger.debug( "-----------------------------------END Test_MAIN--------------------------------------------")




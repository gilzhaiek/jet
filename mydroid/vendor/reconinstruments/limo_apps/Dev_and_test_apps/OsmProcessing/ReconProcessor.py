from configobj import ConfigObj



from xml.dom.minidom import parse
import ast
import os.path
import time
import xml.dom.minidom
import resource
import codecs
import copy


from ReconGeometric import ReconGeometric
from ReconConst import Const
#from ReconUtils import ReconUtils
from ReconXMLParser import XMLParser

###########################################################################
## OsmParser class                                                       ##
## Created on Dec. 3, 2013                                               ##
##Create osmPaser class to get and process open street map resrouce.     ##
##@author: simonwang                                                     ##
###########################################################################

class OsmParser:

    def __init__(self, reconFetcher, writer, logger, debug=True):
        self.debug = debug
        #self.osmApi = osmApi
        self.fetcher = reconFetcher
        self.writer = writer
        self.reconUtils = None
        self.Logger = logger
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
        
        self.ReadConfigs()
        self.xmlParser = XMLParser(self, self.writer, self.Logger)
            
    def __del__(self):
        return [];
    
    def OpenOsmXml(self, fileName):
        try:
            dom = parse(fileName)
        except Exception:
            self.Logger.warn("OpenOsmXml exception.")
            return None
        
        return dom
    def setUtils(self, utils):
        self.reconUtils = utils
    
    #parse the raw osm file, and get extension objects, and write into destnation xml file.
    def ParseAndWriteOneTile(self, inputXML, xmlId, outputXML, min_lat=0.0, min_lon=0.0, max_lat=0.0, max_lon=0.0): 
        #uri = "/api/0.6/boundry?bbox=%f,%f,%f,%f"%(min_lon, min_lat, max_lon, max_lat)
        startTimeStr = time.strftime("%c")
        start_time = time.time()
        self.Logger.warn("***start processing tile (id=%s) at %s. *****", xmlId, time.strftime("%c"))
    
        #self._UsingMemory("Start to parse Map.")
        fileSize = os.stat(inputXML).st_size
        #print "$$$$$$$$$$ size=" + str(fileSize)
        self.writer.usedMemory += fileSize
        dom = self.OpenOsmXml(inputXML)
        if (not dom):
            self.Logger.warn("ParseAndWriteOneTile, dom=null")
            return
        self.ParseOsmByDom(dom)
        #self.xmlParser.ParseOsmByLxml(inputXML)
        
        if ((min_lon + max_lon + min_lat + min_lon) == 0):
            self.writer.setBoundary(float(self.writer.respBounds[Const.MINLAT]), float(self.writer.respBounds[Const.MINLON]), float(self.writer.respBounds[Const.MAXLAT]), float(self.writer.respBounds[Const.MAXLON]))
        else:
            self.writer.setBoundary(min_lat, min_lon, max_lat, max_lon)
        
        self.AddTileRelatives(min_lon, min_lat, max_lon, max_lat)
        if (self.writer.exitTile):
            return
        
        self.writer.proceedAction(xmlId)

        numElementWrote = self.writer.writeToXmlFile(xmlId, outputXML);
        
        if (numElementWrote):
            self.writer.setBoundTypes()
        elif (self.writer.isMostLeftTile): #if itMOstLeftTile
            self.writer.tileRightType = self.writer.tileTopType
            
        elapsed_time = (time.time() - start_time)
        self. Logger.warn("=====End processing tile (id=%s): start=%s, end=%s, elapsed_time=%d sec/secs.$$$$$", xmlId, startTimeStr, time.strftime("%c"), elapsed_time)
    
    def AddTileRelatives(self, min_lon, min_lat, max_lon, max_lat):
        if ((not Const.ADD_RELATIVE_OSM_OBJECTS_ON) or (Const.OSM_SOURCE_DEFAULT != Const.OSM_SOURCE_LOCAL_OSM_DB)):
            self.Logger.warning( "AddTileRelatives is oFF now, mode=" + str(Const.ADD_RELATIVE_OSM_OBJECTS_ON) +", OSM_SOURCE_DEFAUL="+ str(Const.OSM_SOURCE_DEFAULT))
            return;

        self.Logger.debug('AddTileRelatives (left=%f,botton=%f,right=%f,top=%f) to file=o_tmp.xml.' % (min_lon, min_lat, max_lon, max_lat))        
        tmpFileName = Const.OSM_MAPS_PATH +"o_tmp.xml"
        
        self.fetcher.source.queryRawTileRelatives(min_lon, min_lat, max_lon, max_lat, tmpFileName)
        
        fileSize = os.stat(tmpFileName).st_size 
        self.Logger.warning( "Tile_relative_query_size=%i", fileSize)
        
        if (fileSize > Const.MAX_SIZE_RELATIVE_FILE):
            self.Logger.warn("AddTileRelatives, file_size=%d > MAX_SIZE(3MB), skip processing the whole.", fileSize)
            Const.isAddRelativeOsmObjectOn = False
            return
        self.writer.usedMemory += fileSize
        #print "$$$$$$$$$$ size=" + str(fileSize)
        if (self.isUsedMemoryTooMuch()):
            return
        
        Const.isAddRelativeOsmObjectOn = True
        dom = self.OpenOsmXml(tmpFileName)
        if (dom):
            self.ParseOsmByDom(dom, False)
        else:
            self.Logger.error("AddTileRelatives, can not get relatives from db.")
        #self.xmlParser.ParseOsmByLxml(tmpFileName)
                
        
    def AddNodeById(self, nodeId):
        self.Logger.debug("AddNodeById=%s", nodeId)
        #simon = None
        #simon.p
        if Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DB:
            tmpFileName = Const.OSM_MAPS_PATH +"o_tmp.xml"
            self.fetcher.source.queryNodeById(nodeId, tmpFileName)
            dom = self.OpenOsmXml(tmpFileName)
            if (dom):
                self.ParseOsmByDom(dom, False)
            else:
                self.Logger.error("AddNodeById, can not get node by db.")
            #self.xmlParser.ParseOsmByLxml(tmpFileName)
        else:
            node = self.fetcher.NodeGet(nodeId)
            dom =  xml.dom.minidom.parseString(node)
            self.ParseOsmByDom(dom, False)
        return
    
    def AddWayFullById(self, wayId):
        self.Logger.debug("AddWayFullById=%s", wayId)
        #simon = None
        #simon.p
        if Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DB:
            tmpFileName = Const.OSM_MAPS_PATH +"o_tmp.xml"
            self.fetcher.source.queryWayById(wayId, tmpFileName)
            dom = self.OpenOsmXml(tmpFileName)
            if (dom):
                self.ParseOsmByDom(dom, False)
            else:
                self.Logger.error("AddWayFullById, can not get way by db.")
            #self.xmlParser.ParseOsmByLxml(tmpFileName)
            
        else:
            way = self.fetcher.WayFullGet(wayId)
            if (way):
                dom =  xml.dom.minidom.parseString(way)
                self.ParseOsmByDom(dom, False)
            else:
                self.Logger.error("AddWayFullById, can not get way.")
        return
    
    def AddRelationFullById(self, relationId):
        self.Logger.debug("AddRelationFullById=%s", relationId)
        #simon = None
        #simon.p
        if Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DB:
            tmpFileName = Const.OSM_MAPS_PATH +"o_tmp.xml"
            self.fetcher.source.queryRelationById(relationId, tmpFileName)
            
            start_time = time.time()
            dom = self.OpenOsmXml(tmpFileName)
            elapsed_time = (time.time() - start_time)
            self.Logger.warn("---OpenOsmXml, elapsed_time=%.2f secs.", elapsed_time)


            if (dom):
                self.ParseOsmByDom(dom, False)
            else:
                self.Logger.error("AddRelationFullById, can not get relation by db.")
            #self.xmlParser.ParseOsmByLxml(tmpFileName)
        else:
            relation = self.fetcher.RelationFullGet(relationId)
            dom =  xml.dom.minidom.parseString(relation)
            self.ParseOsmByDom(dom, False)
        return
    
    def ParseOsmByDom(self, dom, isAddToOutput=True):
        numNode = 0
        numWay = 0
        numRelation = 0
        self.Logger.info(  "***ParseOsmByDom")
        start_time = time.time()
        dom = dom.getElementsByTagName("osm")[0]
    
    
        reconType = []
        for elem in dom.childNodes:
            if (elem.nodeName == "#text"):
                continue
            reconType = []
            self.counter += 1
            #print (self.counter)
            #print elem.nodeName
            
            if elem.nodeName == Const.TAG_NODE:
                nodeDict = self.GetNodeDict(elem)
                #if (self.debug):
                #    self.Logger.debug(  "---Parsing Node_id=%s" % nodeDict[u'attr'][u'id'])
                if (isAddToOutput):
                    isInOutput = self.IsNeed(nodeDict, Const.TAG_NODE, self.node_features, self.node_types, None, reconType, self.writer.needNodesList)
                    nodeDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                else:
                    nodeDict[Const.KEY_IS_IN_OUTPUT] = False
                if (reconType):
                    nodeDict[Const.KEY_RECON_TYPE] = reconType[0]
                #self.writer.cacheNode(nodeDict)
                self.writer.cacheElement(nodeDict, self.writer.numCachedNode, self.writer.cachedNodes, self.writer.nodeFile, self.writer.numNodesInDisk)
                numNode += 1
                
            elif elem.nodeName == Const.TAG_WAY:
                wayDict = self.GetWayDict(elem)
                if (self.debug):
                    self.Logger.debug(  "---Parsing WAY_id=%s" % wayDict[u'attr'][u'id'])
                #if ("109276905" == wayDict[u'attr'][u'id']):
                #    print "debug"
                if (isAddToOutput):
                    isInOutput = self.IsNeed(wayDict, Const.TAG_WAY, self.way_features, self.way_types, self.way_actions, reconType, self.writer.needWaysList)
                    wayDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                else:
                    wayDict[Const.KEY_IS_IN_OUTPUT] = False
                if (reconType):
                    wayDict[Const.KEY_RECON_TYPE] = reconType[0]
                #self.writer.cacheWay(wayDict, reconType)
                self.writer.cacheElement(wayDict, self.writer.numCachedWay, self.writer.cachedWays, self.writer.wayFile, self.writer.numWaysInDisk)
                numWay += 1
                    
            elif elem.nodeName == Const.TAG_RELATION:
                relationDict = self.GetRelationDict(elem)
                if (self.debug):
                    self.Logger.debug(  "---Parsing RELATION_id=%s" % relationDict[u'attr'][u'id'])
                
                if (isAddToOutput):
                    isInOutput = self.IsNeed(relationDict, Const.TAG_RELATION, self.relation_features, self.relation_types, self.relation_actions, reconType, self.writer.needRelationsList)
                    relationDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                else:
                    relationDict[Const.KEY_IS_IN_OUTPUT] = False
                if (reconType):
                    relationDict[Const.KEY_RECON_TYPE] = reconType[0]
                #self.writer.cacheRelation(relationDict, reconType);
                self.writer.cacheElement(relationDict, self.writer.numCachedRelation, self.writer.cachedRelations, self.writer.relationFile, self.writer.numRelationsInDisk)
                numRelation += 1
            elif ((elem.nodeName == Const.TAG_BOUNDS) and (isAddToOutput)):
                self.writer.respBounds = self.DomParseBounds(elem)
                #print ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (self.writer.respBounds[u'minlon'], self.writer.respBounds[u'minlat'], self.writer.respBounds[u'maxlon'], self.writer.respBounds[u'maxlat']))
                #print ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (self.writer.reqBoundsInStr[u'minlon'], self.writer.reqBoundsInStr[u'minlat'], self.writer.reqBoundsInStr[u'maxlon'], self.writer.reqBoundsInStr[u'maxlat']))
                


        self.Logger.warn('processed (numNode=%d, numWay=%d, numRelation=%d); total cachedNode=%d, cachedWay=%d, cachedRelation=%d' %(numNode, numWay, numRelation, self.writer.numCachedNode[0], self.writer.numCachedWay[0], self.writer.numCachedRelation[0] ) )
        #self.Logger.info('caches:, cachedNode=%s, cachedWay=%s, cachedRelation=%s' %(self.writer.cachedNodes, self.writer.cachedWays, self.writer.cachedRelations ))
        elapsed_time = (time.time() - start_time)
        self.Logger.warn("---End parse xml dom, elapsed_time=%.2f secs.", elapsed_time)
        self._UsingMemory("ParseOsmByDom")
         
    def clearCacheDisk(self):
        self.writer.reset()
        
    def IsNeed(self, elementDict, elementType, interestedTags, reconTypes, actions, selectedReconType, needList):
        tags = elementDict[u'tag']
        for k, v in tags.items():
            #self.Logger.debug(  'k=%s, v=%s'%(k, v))
            #print ('k=%s, v=%s'%(k, v))
            for idx, item in enumerate(interestedTags):
                #print idx, item
                if ((k =="waterway") and (item == {"Waterway":"*"})):
                    print k, v
                if ({k:v} == item or {k:"*"} == item):
                    #self.Logger.debug(  "type=%s"%reconTypes[idx])
                    #if (item == {"natural": "coastline"}):
                    #    print "coastline"
                    selectedReconType.append(reconTypes[idx])
                    elementId = str(elementDict[u"attr"][u"id"])
                    needList.append(elementId)
                    if ((not actions) or (not actions[idx]) or (not self.isPreProcessing(actions[idx]))):
                        return True
                    
                    elementId = str(elementDict[u"attr"][u"id"])
                    action = [elementId, elementType, actions[idx], elementDict, None, None]
                    elementDict[Const.TAG_ACTION_POINT_LIST] = []
                    elementDict[Const.TAG_ACTION_POINT_EXCEPTION_LIST] = []
                    
                    dictKey = actions[idx]
                    if (dictKey == Const.ACTION_RIVER_BANK):
                        dictKey = Const.ACTION_OCEAN_GEO_REGION
                    self.reconUtils.addItemIntoDict(self.writer.actionDict, dictKey, action)
                    
                        
                    #self.Logger.debug("tags_len=%d, tags=%s, IsElementNeed=true", len(tags), str(tags) )
                    return True
            
        #self.Logger.debug("tags_len=%d, tags=%s,IsElementNeed=false", len(tags), str(tags))
        return False
    
    def IsWayNeed(self, way_tag, reconType):
        if self.debug:
            self.Logger.debug('way_tags_len=%d'%len(way_tag), ",way_tags=",  str(way_tag))
        
        #values = self.way_features.items().values()
        for k, v in way_tag.items():
            #self.Logger.debug('k=%s, v=%s'%(k, v))
            for idx, item in enumerate(self.way_features):
                #self.Logger.debug( idx, item)
                if ({k:v} == item):
                    #self.Logger.debug("type=%s"%self.way_types[idx])
                    reconType.append(self.way_types[idx])
                    if self.debug:
                        self.Logger.debug("IsWayNeed=true")
                    return True
            
        if self.debug:
            self.Logger.debug("IsWayNeed=false")
        return False
    
    def IsRelationNeed(self, relation_tag):
        if self.debug:
            self.Logger.debug('Relation_tags_len=%d, string=%s'%len(relation_tag), str(relation_tag))
            
        for k, v in relation_tag.items():
            if ({k:v} in self.relation_features):
            #if ({k:v} in _interested_relation_features):
                if self.debug:
                    self.Logger.debug("IsRelationNeed=true")
                return True
        
        if self.debug:
            self.Logger.debug("IsRelationNeed=false")
        return False
    
    def GetNodeDict(self, pointNode):
        return self.DomParseNode(pointNode)
    
    def GetWayDict(self, wayNode):
        return self.DomParseWay(wayNode)
    
    def GetRelationDict(self, relationNode):
        return self.DomParseRelation(relationNode)
        
        
    def ReadConfigs(self):
        config = ConfigObj(self.configName)
        
        #read ['NODE'] section
        self.__ReadConfig(config, Const.CONFIG_NODE, self.node_features, self.node_types, None)
        
        #read ['WAY'] section
        self.__ReadConfig(config, Const.CONFIG_WAY, self.way_features, self.way_types, self.way_actions)

        
        #read ['RELATION'] section
        self.__ReadConfig(config, Const.CONFIG_RELATION, self.relation_features, self.relation_types, self.relation_actions)
        
        
        #read ['NODE_DISP_TAGS'] section
        self.writer.nodeDispTags = self.__ReadConfig(config, Const.CONFIG_NODE_DISP, self.writer.nodeDispTags)

        
        #read ['WAY_DISP_TAGS'] section
        self.writer.wayDispTags = self.__ReadConfig(config, Const.CONFIG_WAY_DISP, self.writer.wayDispTags)

        
        #read ['NODE_DISP_TAGS'] section
        self.writer.relationDispTags = self.__ReadConfig(config, Const.CONFIG_RELATION_DISP, self.writer.relationDispTags)
        
        config = None


        
    def __ReadConfig(self, config, sectionName, result, types=None, actions=None):
        section = config[sectionName]
        maxNumWayTags = int(section['MAX_NUM_TAGS'])
        i = 0
        while (i < maxNumWayTags):
            key = 'TAG%d'%i
            value = section[key];
            valueSize = len(value)
            if (sectionName == Const.CONFIG_NODE or sectionName == Const.CONFIG_WAY or sectionName == Const.CONFIG_RELATION):
                #self.Logger.debug( '%s=%s, more=%s'% (key, value[0], value[1]))
                #self.Logger.debug( (ast.literal_eval(value[0])))
                result.append(ast.literal_eval(value[0]))
                types.append(value[1])
                
                if (sectionName != Const.CONFIG_NODE):
                    if (valueSize >=3):
                        actions.append(value[2])
                    else:
                        actions.append("")
                        
                
            else:
                #self.Logger.debug( '%s=%s'% (key, value))
                result = result +(value, )
            i += 1
        if (self.Logger):
            self.Logger.info( '%s_OSM_Type=%s' %(sectionName, result))
            if (types):
                self.Logger.info( '%s_RIM_Type=%s' %(sectionName, types))
        return result
        
    def _UsingMemory(self, point=""):
        usage=resource.getrusage(resource.RUSAGE_SELF)
        self.Logger.warn( "%s MEMORY: usertime=%s systime=%s mem=%s mb" %(point, usage[0], usage[1], usage.ru_maxrss/1024 ))
        return
    def isUsedMemoryTooMuch(self):
        if (Const.IS_HOST_VM and (self.writer.usedMemory > Const.MAX_SIZE_XML_FILES_PROCESSED)):
        #if ((self.writer.usedMemory > Const.MAX_SIZE_XML_FILES_PROCESSED)):
            self.Logger.warn( "UsedMemoryTooMuch(), used MEMORY=%i, Memory_threadhold=%i" %(self.writer.usedMemory, Const.MAX_SIZE_XML_FILES_PROCESSED ))
            self.writer.exitTile = True
        
        return False
        
           
    def DomParseNode(self, DomElement):
        """ Returns NodeData for the node. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"] = self.__DomGetTag(DomElement)
        return result

    def DomParseWay(self, DomElement):
        """ Returns WayData for the way. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"] = self.__DomGetTag(DomElement)
        result[u"nd"]  = self.__DomGetNd(DomElement)        
        return result
    
    def DomParseRelation(self, DomElement):
        """ Returns RelationData for the relation. """
        result = {}
        result[u"attr"] = self.__DomGetAttributes(DomElement)
        result[u"tag"]    = self.__DomGetTag(DomElement)
        result[u"member"] = self.__DomGetMember(DomElement)
        return result
      
    def DomParseBounds(self, DomElement):
        """ Returns RelationData for the relation. """
        result = self.__DomGetAttributes(DomElement)
        return result
    
    
    def __DomGetAttributes(self, DomElement):
        """ Returns a formated dictionnary of attributes of a DomElement. """
        result = {}
        for k, v in DomElement.attributes.items():
            result[k] = v
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
    
    def Test_VerifyXML(self, fileName):
        numPoint = 0
        numline = 0
        numArea = 0
        numRmo = 0
        dom = self.OpenOsmXml(fileName)
        dom = dom.getElementsByTagName("rim")[0]
        
        for elem in dom.childNodes:
            numRmo +=1
            #print elem.nodeName
            if elem.nodeName == Const.TAG_POINT:
                numPoint +=1           
            elif elem.nodeName == Const.TAG_LINE:
                numline +=1
            elif elem.nodeName == Const.TAG_AREA:
                numArea +=1
        self.Logger.warn('processed numline=%d, numArea=%d, numPoint=%d' %(numline, numArea, numPoint))
        self.Logger.warn('processed numRMO=%d' %(numRmo))
    
    def isPreProcessing(self, action_type):
        if (action_type in Const.ACTION_LIST_FOR_PRE_PROCESS):
            return True
        return False
    
        '''
        if ((action_type == Const.ACTION_OCEAN_GEO_REGION) or (action_type == Const.ACTION_RIVER_BANK)):
            return True
        if (action_type == Const.ACTION_WOOD):
            return True
        if (action_type == Const.ACTION_LAKE):
            return True
        if (action_type == Const.ACTION_PARK):
            return True
        if (action_type == Const.ACTION_WETLAND):
            return True
        return False
        '''
    
    def isOceanCoastLine(self, action_type):
        if ((action_type == Const.ACTION_OCEAN_GEO_REGION) or (action_type == Const.ACTION_RIVER_BANK)):
            return True
    
   
        
###########################################################################
## End of RimWriter class                                                ##
###########################################################################



###########################################################################
## RimWriter class                                                       ##
## Created on Dec. 6, 2013                                               ##
##Create rimWriter (Recon Instruments Map) class to write recon OSM      ##
##map into Recon Format.                                                 ##
##@author: simonwang                                                     ##
###########################################################################

   
class RimWriter:
    
    def __init__(self, reconFetcher, logger,debug=True):
        #self.osmApi = osmApi
        self.fetcher = reconFetcher
        self.Logger = logger
        self.debug = debug
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
        self.isInsideBound = False
        
        self.tileTopType = Const.TILE_TOP_TYPE
        self.tileRightType = Const.TILE_RIGHT_TYPE
        
        if (not os.path.isdir(Const.OUTPUT_PATH)):
            os.mkdir(Const.OUTPUT_PATH)
            
        if (not os.path.isdir(Const.DISK_CACHE_PATH)):
            os.mkdir(Const.DISK_CACHE_PATH)
            
        if (not os.path.exists(Const.OSM_MAPS_PATH)):
            os.mkdir(Const.OSM_MAPS_PATH)
        
        if (not os.path.exists(Const.PICS_FILES_PATH)):
            os.mkdir(Const.PICS_FILES_PATH)
            
        
        self.geometric = ReconGeometric(self.Logger)
        
        if os.path.isfile(Const.SKIP_TILES_FILE):
            self.skipTiles = open(Const.SKIP_TILES_FILE, "a")
        else:
            self.skipTiles = open(Const.SKIP_TILES_FILE, "a")
            self.skipTiles.write("#List the tiles not processed yet, for the empty tile type, 0-'unknown', 1-'ocean', 2-'land', 3-'forest', 4-'invalid'.\n")
            
        self.reset()
            
    def __del__(self):
        self.skipTiles.close()
        return [];
    
    def setParser(self, parser):
        self.parser = parser
        
    def reset(self):
        self.numCachedNode = [0,]
        self.cachedNodes = {}
        self.needNodesList =[]
        
        self.numCachedWay = [0,]
        self.cachedWays = {}
        self.needWaysList = []
        
        self.numCachedRelation = [0,]
        self.cachedRelations = {}
        self.needRelationsList = []
        
        
        
        
        self.numCachedNode1 = [0,]
        self.cachedNodes1 = {}
        self.needNodesList1 =[]
        
        self.numCachedWay1 = [0,]
        self.cachedWays1 = {}
        self.needWaysList1 = []
        
        self.numCachedRelation1 = [0,]
        self.cachedRelations1 = {}
        self.needRelationsList1 = []
        
        
        
        
        
        self.actionDict = {}
        self.actionList = []
        #self.actionCoastLineList = []
        self.coastLineList = []
        self.cachedOceanList = []
        self.forestList = []
        
        self.reqBoundsInStr = {} #request bounds in string format
        self.reqBoundsInFloat = {} #request bounds in float format
        self.respBounds = {}  #response bounds in string format
        self.isMostLeftTile = False
        self.oceanLandSN = 0
        self.tileType= Const.TYPE_UNKNOWN
        self.exitTile = False
        self.usedMemory = 0
        self.numNodesWrote = 0
        
        if (Const.IS_DISK_SWITCH):
            try:
                if (os.path.exists(Const.WAY_FILE_NAME)):
                    if self.nodeFile:
                        self.nodeFile.close()
                    if self.wayFile:
                        self.wayFile.close()
                    if self.relationFile:
                        self.relationFile.close()
                    os.remove(Const.NODE_FILE_NAME)
                    os.remove(Const.WAY_FILE_NAME)
                    os.remove(Const.RELATION_FILE_NAME)
            except Exception:
                self.Logger.warn("close_open file exception.")
                #if (self.debug):
                #    raise BaseException
                
            self.nodeFile = open(Const.NODE_FILE_NAME, "w+")
            self.wayFile = open(Const.WAY_FILE_NAME, "w+")
            self.wayFile = open(Const.RELATION_FILE_NAME, "w+")
            
            #self.Logger.info( 'reset(), cachedNode=%s, cachedWay=%s, cachedRelation=%s' %(self.cachedNodes, self.cachedWays, self.cachedRelations ))
            self.Logger.info("writer.reset(), cachedNode=%d, cachedWay=%d, cachedRelation=%d", self.numCachedNode[0], self.numCachedWay[0], self.numCachedRelation[0] )

    def setBoundType(self, edge, isRight):
        
        #print len(self.cachedOceanList)
        isFound = False
        polygonType = Const.TYPE_UNKNOWN
        
        #if (not self.cachedOceanList):
        #    return
        edgeType = Const.TYPE_UNKNOWN
        for tileId, recon_type, dicti in self.cachedOceanList:
            polygonList = dicti[Const.TAG_ACTION_POINT_LIST]
            if (not polygonList):
                continue
             
            for polygon in polygonList:
                
                edgeType = self.geometric.getEdgeType(polygon, edge, isRight)
                if (edgeType==Const.TYPE_OCEAN):
                    isFound =True
                    break;
                elif (edgeType == Const.TYPE_LAND):
                    isFound = True
                    break;
                else:#unknow
                    if (recon_type == Const.RECON_TYPE_OCEAN):
                        newPolygonType = Const.TYPE_OCEAN
                    elif (recon_type == Const.RECON_TYPE_LAND):
                        newPolygonType = Const.TYPE_LAND
                    else:
                        newPolygonType = Const.TYPE_UNKNOWN
                            
                    if (polygonType== Const.TYPE_UNKNOWN):
                        polygonType = newPolygonType
                    elif (polygonType != newPolygonType):
                        polygonType = Const.TYPE_INVALID
                        
                    continue
            #for
            if (isFound):
                break;

        #for
        if (not isFound):
            if (polygonType == Const.TYPE_OCEAN):#LAND OR FOREST
                isFound = True
                edgeType = Const.TYPE_UNKNOWN
            elif (polygonType == Const.TYPE_LAND):
                isFound = True
                edgeType = Const.TYPE_UNKNOWN
            
        
           
        #Processing for forest type
        isFound = False
        polygonType = Const.TYPE_UNKNOWN 
        left  = float(self.reqBoundsInStr[u'minlon'])
        right = float(self.reqBoundsInStr[u'maxlon'])
        bottom = float(self.reqBoundsInStr[u'minlat'])
        top = float(self.reqBoundsInStr[u'maxlat'])
        

        
        for tileId, recon_type, dicti in self.forestList:
            polygonList = dicti[Const.TAG_ACTION_POINT_LIST]
            if (not polygonList):
                continue
             
            for polygon in polygonList:
                #make sure it is anti-clockwise
                self.correctClockwise(polygon, True)
                if (isRight):
                    edgeType1 = self.geometric.isEdgeTypeForest(polygon, [right, bottom], [right, top], isRight)
                else:
                    edgeType1 = self.geometric.isEdgeTypeForest(polygon, [left, top], [right, top], isRight)
                    
                if (edgeType1 == Const.TYPE_FOREST):
                    edgeType = edgeType1
                    isFound =True
                    break;
            

        if (edgeType!= Const.TYPE_UNKNOWN and edgeType != Const.TYPE_INVALID):
            if (isRight):
                self.tileRightType = Const.POLYGON_TYPE_LIST[edgeType]
                self.Logger.warn( "setBoundType right as=%s" % (Const.POLYGON_TYPE_LIST[edgeType]))
            else:
                self.tileTopType = Const.POLYGON_TYPE_LIST[edgeType]
                self.Logger.warn( "setBoundType top as=%s" % (Const.POLYGON_TYPE_LIST[edgeType]))
        #print "setBoundType end."
            
    def setBoundTypes(self):
        
        right = float(self.reqBoundsInStr[u'maxlon'])
        top = float(self.reqBoundsInStr[u'maxlat'])
        self.setBoundType(right, True)
        self.setBoundType(top, False)
            
            
    # write into recon xml file
    def writeToXmlFile(self, xmlid, fileFullName):
        self.Logger.warn( "===writing to %s file=%s" % (xmlid, fileFullName))
        
        numElementsWrote = 0
        
        if (Const.VERFICATION_MODE_OFF):

            fp = codecs.open(fileFullName, "w", "utf-8")
        
            #Header
            xml=self._buildXMLHeader(xmlid, None)
            fp.write(xml)
        
            #Nodes
            #self.writeNode(fp)
            numElementsWrote += self.writeElement(fp, Const.TAG_NODE, self.cachedNodes, self.numNodesInDisk, self.nodeFile, Const.NODE_FILE_NAME, self.needNodesList)
            
            #Ways
            #self.writeWay(fp)
            numElementsWrote += self.writeElement(fp, Const.TAG_WAY, self.cachedWays, self.numWaysInDisk, self.wayFile, Const.WAY_FILE_NAME, self.needWaysList)
                
            #Relations
            #self.writeRelation(fp)
            numElementsWrote += self.writeElement(fp, Const.TAG_RELATION, self.cachedRelations, self.numRelationsInDisk, self.relationFile, Const.RELATION_FILE_NAME, self.needRelationsList)
        
            if (self.tileType != Const.TYPE_UNKNOWN and self.tileType != Const.TYPE_INVALID):
                empty_tile_type = self.tileType
            else:
                if (self.isMostLeftTile):
                    empty_tile_type = self.tileTopType
                else:
                    empty_tile_type = self.tileRightType
                
            if ((numElementsWrote == 0) or ((empty_tile_type == Const.POLYGON_TYPE_LIST[Const.TYPE_FOREST]) and (len(self.forestList) == 0))):
                #write tile type for empty tile
                self.writeTileType(fp, empty_tile_type, xmlid)
            
                        
            #Footer
            xml=self._buildXMLfooter()
            fp.write(xml)
            print "wrtie footer"
            
            fp.flush()
            fp.close()
            #close files 
        else:
            self.Verificate(Const.TAG_NODE, self.cachedNodes)

        return numElementsWrote

    def writeTileType(self, xmlFile, reconType, tileId):
        if (reconType == Const.POLYGON_TYPE_LIST[Const.TYPE_INVALID]) or (reconType == Const.POLYGON_TYPE_LIST[Const.TYPE_UNKNOWN]):
            writtingStr = "[\'" + str(tileId) + "\'; " +  self.reqBoundsInStr[u'minlon'] +"; " +  self.reqBoundsInStr[u'minlat'] +"; "+  self.reqBoundsInStr[u'maxlon'] +"; "+ self.reqBoundsInStr[u'maxlat']+"; 0 ]\n" 
            self.skipTiles.write(writtingStr)
            self.skipTiles.flush()
            return
            
        #if (reconType == Const.POLYGON_TYPE_LIST[])
        xml  =  u"  <"   + Const.TAG_RMO  + u" id" + u"=\"" + u"0" + u"\"" +u" reconType" + u"=\"" + reconType + u"\">\n"
        xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + self.reqBoundsInStr[u'minlat'] + u"\"" + u" lon=\"" + self.reqBoundsInStr[u'minlon'] + u"\"" + u"/>\n" 
        xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + self.reqBoundsInStr[u'minlat'] + u"\"" + u" lon=\"" + self.reqBoundsInStr[u'maxlon'] + u"\"" + u"/>\n" 
        xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + self.reqBoundsInStr[u'maxlat'] + u"\"" + u" lon=\"" + self.reqBoundsInStr[u'maxlon'] + u"\"" + u"/>\n"
        xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + self.reqBoundsInStr[u'maxlat'] + u"\"" + u" lon=\"" + self.reqBoundsInStr[u'minlon'] + u"\"" + u"/>\n"
        xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + self.reqBoundsInStr[u'minlat'] + u"\"" + u" lon=\"" + self.reqBoundsInStr[u'minlon'] + u"\"" + u"/>\n" 
        xml +=  u"  </" + Const.TAG_RMO + u">\n"
        
        xmlFile.write(xml)
        xmlFile.flush()
        
        
                    
    
    def writeElement(self, xmlFile, elementType, cachedElements, numElementsInDisk, elementFile, elementFileName, needList):
        size = 0
        numElementWrote = 0
        self.Logger.info(">>>writting type=%s", elementType)
        #Write elements from Disk
        '''
        if (Const.IS_DISK_SWITCH and numElementsInDisk[0] > 0):
            
            position = elementFile.tell()
            self.Logger.debug("curr_position=%d" %( position))
            elementFile.seek(0, 0)
            position = elementFile.tell()
            #self.Logger.debug("curr_position=%d" %( position))
             
            fileSize = os.stat(elementFileName).st_size
            self.Logger.info("way_file_Size=%s" %fileSize)
            
            for fieldList in iter(lambda: elementFile.read(Const.PROCESSING_SIZE), ""):
                #self.Logger.debug( len(fieldList))
                fields = fieldList.spilt("\t")
                #self.Logger.debug( len(fields))
                if (len(fields) > 2):
                    self.Logger.error( "Wrong parse, length=%d, source=%s" %(len(fields), fieldList))
                self.Logger.debug("id=%s, element=%s" % (fields[0], fields[1]))
                #TODO check Const.KEY_IS_IN_OUTPUT, if not interested, skip to write to xml
                elementStr = self._buildElements(elementType, fields[1], fields[1][Const.KEY_RECON_TYPE])
                elementStr.encode(Const.ENCODE)
                self.Logger.debug( "ELementSTR=", elementStr)
                size += len(elementStr)
                if (size > 0):
                    numElementWrote += 1
                xmlFile.write(elementStr)
                #self.Logger.debug( size)
        '''
        
        #Write element from cachedElements
        for elementId in needList:
            elementDict = cachedElements[elementId]
        #for elementId, elementDict in cachedElements.items():

            self.Logger.debug("id=%s, type=%s, element=%s" % (elementId, elementType, str(elementDict)))
            if (not elementDict):
                continue
            #if (elementDict[Const.KEY_IS_IN_OUTPUT] != True):
            #    continue
            

            reconType = elementDict[Const.KEY_RECON_TYPE]

                
            self.isInsideBound = False
            #if ((not Const.isAddRelativeOsmObjectOn) and ( not "_" in elementId ) and (reconType != Const.RECON_TYPE_NATIONAL_BORDER)):
            if ((not Const.isAddRelativeOsmObjectOn) and ( not "_" in elementId ) and (not self.isLineTypeObj(reconType))):
                if (elementType == Const.TAG_RELATION):
                    self.parser.AddRelationFullById(elementId)
                elif (elementType == Const.TAG_WAY):
                    self.parser.AddWayFullById(elementId)
            elementStr = self._buildElements(elementType, elementDict, reconType)        
            elementStr.encode(Const.ENCODE)
            
            self.Logger.debug("writting id=%s, type=%s, str=%s, isInsideBound=%r", elementId, elementType, elementStr, self.isInsideBound)
            
            
            if (self.isInsideBound):
                size += len(elementStr)
                xmlFile.write(elementStr)
                if (size > 0):
                    numElementWrote += 1
            else:
                self.Logger.debug("elementId=%s, outside.", (elementId))
                
        xmlFile.flush()
        return numElementWrote
      
    def IsCached(self, elementId, numCachedElement, cachedElements, fileForElement):
        
        if (elementId in cachedElements.keys()):
            return True
        '''
        if (Const.IS_DISK_SWITCH and numCachedElement[0] > Const.MAX_NUM_CACHED_ELEMENT_SIZE):
                
            for fieldList in iter(lambda: fileForElement.read(Const.PROCESSING_SIZE), ""):
                fields = fieldList.spilt("\t")
                #self.Logger.debug( len(fields))
                if (len(fields) > 2):
                    self.Logger.error( "Wrong parse, length=%d, source=%s" %(len(fields), fieldList))
                self.Logger.debug("id=%s, element=%s" % (fields[0], fields[1]))
                if (elementId == fields[0]):
                    return True
                    
        '''
            
        return False
    
                
    def cacheElement(self, elementDict, numCachedElement, cachedElements, fileForElement, numElementInDisk):
        elementId = str(elementDict[u"attr"][u"id"])
        
        if (self.IsCached(elementId, numCachedElement, cachedElements, fileForElement)):
            self.Logger.debug("Attemping Cahche the Existed ElementId=%s", elementId)
            return
        
        numCachedElement[0] += 1
        cachedElements[elementId] = elementDict
        
        if (Const.IS_DISK_SWITCH and numCachedElement[0] > Const.MAX_NUM_CACHED_ELEMENT_SIZE):
            for currId, currElementDict in cachedElements.items():
                self.Logger.debug( "id=", currId, "NodeDict=", currElementDict)
                fileForElement.write(currId + "\t" + currElementDict + "\n")
                numElementInDisk[0] += numCachedElement[0]
                numCachedElement[0] = 0
                cachedElements = {}
        
        
    def _isArea(self, ElementType, ElementData):
        result = False
        if (ElementType == Const.TAG_WAY):
            nodeIds = ElementData.get(u"nd", [])
            size = len(nodeIds)
            if (size > 2 and nodeIds[0] == nodeIds[size-1]):
                result = True
        elif (ElementType == Const.TAG_RELATION):
            tags = ElementData.get(u"tag", {})
            keys = tags.keys()
            if (u'type' in keys):
                tag_Type = tags[u'type']
                if (tag_Type):
                    if (u"polygon" in tag_Type):
                        return True
                    elif (u"route" in tag_Type):
                        return False
                    else:
                        self.Logger.debug( "relation, type=%s, can't determin whether it is area." %tag_Type)
            
            members = ElementData.get(u"member", [])
            size = len(members)
            if (size > 2 and members[0] == members[size-1]):
                result = True
            #Do need check node inside of member?
            
        return result
        

    def _buildXMLHeader(self, xmlid, bound):
        xml=u""
        xml += u"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        xml += u"<rim id=\"" + xmlid +u"\" version=\"1.0\" >\n"
        xml += u"<bounds minlat=\"" + self.reqBoundsInStr[u'minlat'] + "\" minlon=\"" + self.reqBoundsInStr[u'minlon'] + "\" maxlat=\"" + self.reqBoundsInStr[u'maxlat'] + "\" maxlon=\"" + self.reqBoundsInStr[u'maxlon'] + "\"/>\n"
        #xml += u"<bounds minlat=\"" + self.respBounds[u'minlat'] + "\" minlon=\"" + self.respBounds[u'minlon'] + "\" maxlat=\"" + self.respBounds[u'maxlat'] + "\" maxlon=\"" + self.respBounds[u'maxlon'] + "\"/>\n"
        
        return xml.encode(Const.ENCODE)
    
    def _buildXMLfooter(self):
        xml = u"</rim>\n"
        return xml.encode(Const.ENCODE)
     
    def _isBuildFromPointList(self, ElementData):
        try:
            polygonList = ElementData[Const.TAG_ACTION_POINT_LIST]
            size = len(polygonList)
            return (size > 0)
        except Exception:
            return False
            
    def _buildElements(self, ElementType, ElementData, reconType=None):
        
        xml  = u""
        lat = u""
        lon = u""
        xmlFirstLine = u""
        elementId = u""
        sn = 0
        if ElementType not in [Const.TAG_NODE, Const.TAG_WAY, Const.TAG_RELATION]:
            return xml

        # element attr
        xml += u"  <"  #ElementType
        
        xml += Const.TAG_RMO

        # element attr id
        attrDict = ElementData.get(u"attr", {})
        if (attrDict):
            k = Const.KEY_ID
            v = attrDict[k]
            elementId = v
            xml += u" " + k + u"=\"" + v + u"\""
             
  
        # element attr - reconType          
        if (reconType):
            xml += u" reconType" + u"=\"" + reconType + u"\""

        # element attr - othes
        for k, v in ElementData.get(u"attr", {}).items():
            '''if k != "lat" and k != "lon":
                #self.Logger.debug( "skip attr:", k, "=", v)
                continue'
            xml += u" " + k + u"=\"" + v + u"\""  '''
            if k== "lat":
                lat = v
            elif k == "lon":
                lon = v
        
            
            
        # element tag
        for k, v in ElementData.get(u"tag", {}).items():
            #if (k == u"created_by" or  k =="source" ):
            #    continue
            #self.Logger.debug( k, "=", v)
            #self.Logger.debug( "all=", str(self.nodeDispTags))
            if (ElementType == Const.TAG_NODE):
                if (k not in self.nodeDispTags):
                    #self.Logger.debug( "skip tag:", k, "=", v)
                    continue
            elif (ElementType == Const.TAG_WAY):
                if (k not in self.wayDispTags):
                    #self.Logger.debug("skip tag:", k, "=", v)
                    continue
            else: #elif (ElementType == Const.TAG_RELATION):
                if (k not in self.relationDispTags):
                    #self.Logger.debug("skip tag:", k, "=", v)
                    continue
            
            xml += u" " + self._XmlEncode(k) + u"=\"" + self._XmlEncode(v) + u"\""
            
        '''if(ElementType != Const.TAG_NODE): '''
        xml += u">\n"
        xmlFirstLine = xml
        
        '''put attribus into dymatic tag  
        xml += u"    <tag k=\""+self._XmlEncode(k)+u"\" v=\""+self._XmlEncode(v)+u"\"/>\n"
        '''
    
        '''Add point into OSM Node type'''
        if (ElementType == Const.TAG_NODE):
            xml +=  u"    <" + Const.TAG_POINT + u" lat=\"" + lat + u"\"" + u" lon=\"" + lon + u"\"" + u"/>\n" 
            if (not self.isInsideBound ):
                latFloat = float(lat)
                lonFloat = float(lon)
                self.isInsideBound = self.IsInsideOfTileByValue(latFloat, lonFloat)
        
        if (not (self._isBuildFromPointList(ElementData))):
            # element nd
            #for ref in ElementData.get(u"nd", []):
            #    xml += u"    <nd ref=\""+str(ref)+u"\"/>\n"
            for nodeId in  ElementData.get(u"nd", []):
            #for nodeId in ElementData.get("u"nd", []):
                xml += self._buildNestedNodeElement(str(nodeId), "", 2, elementId, reconType)
       
            
            needDataList = []
            if  (self.isWritePartialObjs(ElementType, reconType, ElementData)):
                needDataList = self.GetNeedDataList(reconType, ElementData)
            
            # element member
            i = -1
            #ids = []
            for member in ElementData.get(u"member", []):
                #xml += u"    <member type=\""+member[u"type"]+"\" ref=\""+str(member[u"ref"])+u"\" role=\""+self._XmlEncode(member[u"role"])+"\"/>\n"
                memberType = member[u"type"]
                ref = str(member[u"ref"])
                role = member[u"role"]
                i += 1
                self.numNodesWrote = 0
                self.Logger.debug("id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
                
                #if  (reconType == Const.RECON_TYPE_NATIONAL_BORDER):
                #    ids.append(ref)
                
                needData = 1
                if needDataList:
                    if (needDataList[i] == 0):
                        continue
                    else:
                        needData = needDataList[i]
                
                if (self.isStartNewId(reconType, role)):
                    print "startNewId"
                    sn += 1
                    newFirstLine = xmlFirstLine.replace(elementId, elementId+"_"+str(sn))
                    xml += u"  </" + Const.TAG_RMO + u">\n"
                    xml +=newFirstLine
                
                if (memberType == Const.TAG_WAY):
                    xml += self._buildNestedWayElement(ref, role, 2, elementId, reconType, needData)
                elif (memberType == Const.TAG_RELATION):
                    xml += self._buildNestedRelationElement(ref, role, 2, elementId, reconType, needData)
                elif (memberType == Const.TAG_NODE):
                    xml += self._buildNestedNodeElement(ref, role, 2, elementId, reconType)
                else:
                    self.Logger.warn( "Unkown memberElement, id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
                
        else:#reconType == Const.RECON_TYPE_OCEAN
            xml += self._buildNestedNodeFromPointList(ElementData)
            self.isInsideBound = True
            
        
        '''if  ((reconType == Const.RECON_TYPE_NATIONAL_BORDER) and (needDataList)):
            print("list=%s", str(ids).translate(None, "'"))
        '''
        
        # element footer
        '''CHange point/line/area into single rmo
        if (ElementType == Const.TAG_NODE):
            xml += u"/>\n"
            #xml += Const.TAG_POINT
        else:
            xml += u"  </" #+ ElementType + 
            
            if (isArea):
                xml += Const.TAG_AREA
            else:
                xml += Const.TAG_LINE
                        
            xml += u">\n" '''
            
        xml += u"  </"    
        xml +=Const.TAG_RMO
        xml += u">\n"
        
        return xml
    
    '''def getElementFromCache(self, nodeId, nodeType):
        if nodeType == Const.TAG_NODE:
            if (self.isCached(nodeId)):
                return
        elif nodeType == Const.TAG_WAY:'''
            


    def getElementFromCache(self, elementId, numCachedElement, cachedElements, fileForElement, numElementInDisk, elementFileName, reconType=None):
        
        if (not self.IsCached(elementId, numCachedElement, cachedElements, fileForElement)):
            
            self.Logger.debug( "Not Found nested ElementId=%s in cacachedElementsched memory and Disk, try Internet fetch", elementId)
            #if (Const.FETCH_ONE_IF_NOT_EXISTS and (reconType != Const.RECON_TYPE_NATIONAL_BORDER) ):
            if (Const.FETCH_ONE_IF_NOT_EXISTS and (not self.isLineTypeObj(reconType))):
                self.Logger.debug("fetching from Internet, id=%s", elementId)
                
                if (cachedElements == self.cachedNodes):
                    self.parser.AddNodeById(elementId)
                if (cachedElements == self.cachedWays):
                    self.parser.AddWayFullById(elementId)
                if (cachedElements == self.cachedRelations):
                    self.parser.AddRelationFullById(elementId)
            else:
                return {}
            
        elementDict = {}
        try:
            elementDict = cachedElements[elementId]
        except Exception:
            self.Logger.warn( "Not found nested element id=%s in cache 'cachedNodes[%s]', try Disk." %(elementId, elementId))
            if (Const.IS_DISK_SWITCH):
                elementDict = self._getElementFromDiskById (elementId, numElementInDisk, fileForElement, elementFileName)

        return elementDict
    
    def getFullElementFromCache(self, elementId, numCachedElement, cachedElements, fileForElement, numElementInDisk, elementFileName, reconType=None):
        if (cachedElements == self.cachedNodes):
            self.parser.AddNodeById(elementId)
        if (cachedElements == self.cachedWays):
            self.parser.AddWayFullById(elementId)
        if (cachedElements == self.cachedRelations):
            self.parser.AddRelationFullById(elementId)
                    
            
        elementDict = {}
        try:
            elementDict = cachedElements[elementId]
        except Exception:
            self.Logger.warn( "Not found nested element id=%s in cache 'cachedNodes[%s]', try Disk." %(elementId, elementId))
            
        return elementDict
    
                
                
    def _buildNestedNodeElement(self, nodeId, role ="", level=2, rootId="", reconType=None):
        nodeXml  = u""

        
        nodeDict = self.getElementFromCache(nodeId, self.numCachedNode, self.cachedNodes, self.nodeFile, self.numNodesInDisk, Const.NODE_FILE_NAME, reconType)
        
            
        if (not nodeDict):
            self.Logger.error("NO NODE FOUND, nodeId=%s", nodeId)
            return nodeXml;

        nodeXml += u"    <" + Const.TAG_POINT

        nodeAttrDict = nodeDict.get(u"attr", {})
        if nodeAttrDict:
            '''
            attrList = [u"lat", u"lon"];
            for k in attrList:
                v = nodeAttrDict[k]
                nodeXml += u" " + k + "=\"" + v + u"\""
            '''    
            k = u"lat"
            v = nodeAttrDict[k]
            nodeXml += u" " + k + "=\"" + v + u"\""
            lat = float(v)
            k = u"lon"
            v = nodeAttrDict[k]
            nodeXml += u" " + k + "=\"" + v + u"\""
            lon = float(v)
#             if (nodeId == u"252686619"):
#                 print ("lon=%.8f, lat=%.8f" % (lon,lat))
            #if ((not self.isInsideBound) or (reconType ==Const.RECON_TYPE_NATIONAL_BORDER)):
            if ((not self.isInsideBound) or (self.isLineTypeObj(reconType))):
                isInsideOfTile = self.IsInsideOfTileByValue(lat, lon)
                
            if (not self.isInsideBound):
                self.isInsideBound =  isInsideOfTile
            
        '''
        # element tag
        for k, v in nodeDict.get(u"tag", {}).items():

            if k not in self.nodeDispTags:
                continue
            
            nodeXml += u" " + k + "=\"" + v + u"\""
        '''
        nodeXml += u"/>\n"   
        #nodeXml += u"></" + Const.TAG_POINT + u">\n"
    
            
        '''Move attributes into dymatic tag        
        nodeXml += u">"
            # footer
        if (hasTag):
            nodeXml += u"\n    </" + Const.TAG_POINT + u">\n"
        else:
            nodeXml += u"</" + Const.TAG_POINT + u">\n"
        
        # element tag
        hasTag = False
        for k, v in nodeDict.get(u"tag", {}).items():

            if k not in self.nodeDispTags:
                continue
            
            nodeXml += u"\n      <tag k=\""+self._XmlEncode(k)+u"\" v=\""+self._XmlEncode(v)+u"\"/>"
            hasTag = True

        # footer
        if (hasTag):
            nodeXml += u"\n    </" + Const.TAG_POINT + u">\n"
        else:
            nodeXml += u"</" + Const.TAG_POINT + u">\n"
        '''
        
        #if ((reconType == Const.RECON_TYPE_NATIONAL_BORDER) and (not isInsideOfTile)):
        if (self.isLineTypeObj(reconType) and (not isInsideOfTile)):
            return u""
        if ((reconType == Const.RECON_TYPE_NATIONAL_BORDER) and ( role== "admin_centre" or role == "label")):
            return u""
        
        self.numNodesWrote += 1
        return nodeXml

    
    def _buildNestedNodeFromPointList(self, elementDict):
        try:
            polygonList = elementDict[Const.TAG_ACTION_POINT_LIST]
        except Exception:
            self.Logger.warn("_buildNestedNodeFromPointList, TAG_ACTION_POINT_LIST no point list from input.")
            return u""
        nodeXml = u""
        

        #print len(polygonList)
        for polygon in polygonList:
            for point in polygon:
                nodeXml += u"    <" + Const.TAG_POINT + u" lat=\"" + ("%.7f" % point[1]) + u"\" lon=\"" + ("%.7f" % point[0]) + u"\"/>\n"
                #if (not self.isInsideBound):
                #    self.isInsideBound = self.IsInsideOfTileByValue(point[1], point[0])
        
        return nodeXml
    
    def _buildNestedWayElement(self, ref, role, level, rootId="", reconType=None, needDataFlag=1):
        nodeXml = u""
        
        wayDict = self.getElementFromCache(ref, self.numCachedWay, self.cachedWays, self.wayFile, self.numWaysInDisk, Const.WAY_FILE_NAME, reconType)
            
        if (not wayDict):
            self.Logger.debug( "NO WAY FOUND, wayId=%s", ref)
            return nodeXml;
        
        '''
        try:
            wayDict = self.cachedWays[ref]
        except Exception:
            self.Logger.warn( "Not found nested element id=%s in cache 'cachedWays[%s]'." %(ref, ref))
            if (Const.IS_DISK_SWITCH):
                wayDict = self._getElementFromDiskById (ref, self.numWaysInDisk, self.wayFile, Const.WAY_FILE_NAME)
        
        #TODO get wayDict from Internet
        if (Const.FETCH_ONE_IF_NOT_EXISTS):
            self.Logger.debug( "fetching from Internet")
            
        if (not wayDict):
            self.Logger.error( "No way found for wayId=", ref)
            return nodeXml;
        '''
        
        #skip way's attr, tags
        
        #fetchData = self.GetFetchDataList(reconType, self.cachedRelations)
        
        i = -1
        nodeList = wayDict.get(u"nd", [])
        size = len(nodeList)
        for nodeId in  nodeList:
            
            i += 1
            if ((needDataFlag == 2) and (i != size-1)): #need the last one only
                continue
            
            if ((needDataFlag == 3) and (i != 0)): #need the first one only
                continue
            
            nodeXml += self._buildNestedNodeElement(str(nodeId), role, 2, rootId, reconType)

        
        return nodeXml
     
    def _buildNestedRelationElement(self, refId, role, level, rootId="", reconType=None, needDataFlag=1):
        nodeXml = u""
        
        relationDict = self.getElementFromCache(refId, self.numCachedRelation, self.cachedRelations, self.relationFile, self.numRelationsInDisk, Const.RELATION_FILE_NAME, reconType)
            
        if (not relationDict):
            self.Logger.debug( "NO RELATION FOUND, relationId=%s", refId)
            return nodeXml;
        
        '''  
        try:
            relationDict = self.cachedRelations[ref]
        except Exception:
            self.Logger.warn( "Not found nested element id=%s in cache, 'cachedRelations[%s]'" %(ref, ref))
            if (Const.IS_DISK_SWITCH):
                relationDict = self._getElementFromDiskById (ref, self.numRelationsInDisk, self.relationFile, Const.RELATION_FILE_NAME)
                
        
        #TODO get relationDict from Internet
        if (Const.FETCH_ONE_IF_NOT_EXISTS):
            self.Logger.debug( "fetching from Internet")
            
        if (not relationDict):
            self.Logger.error( "No relation found for relationId=", ref)
            return nodeXml;
        '''
        
        #skip way's attr, tags
        
        '''fetchDataList = []
        if  ((level == 2) and (reconType == Const.RECON_TYPE_NATIONAL_BORDER)):
            self.GetFetchDataList(reconType, relationDict)
        '''    

        # element member
        i = -1
        memberList = relationDict.get(u"member", [])
        size = len(memberList)
        for member in memberList:
            #xml += u"    <member type=\""+member[u"type"]+"\" ref=\""+str(member[u"ref"])+u"\" role=\""+self._XmlEncode(member[u"role"])+"\"/>\n"
            memberType = member[u"type"]
            ref = str(member[u"ref"])
            role = member[u"role"]
            i += 1
            self.Logger.debug("rootid=%s, current_id=%s, member_type=%s, ref=%s, role=%s" %(rootId, refId, memberType, ref, role))
            
            if ((needDataFlag == 2) and (i != size-1)): #need the last one only
                continue
            
            if ((needDataFlag == 3) and (i != 0)): #need the first one only
                continue
            
                
            if (memberType == Const.TAG_WAY):
                nodeXml += self._buildNestedWayElement(ref, role, level+1, rootId, reconType, needDataFlag)
            elif (memberType == Const.TAG_RELATION):
                nodeXml += self._buildNestedRelationElement(ref, role, level+1, rootId, reconType, needDataFlag)
            elif (memberType == Const.TAG_NODE):
                nodeXml += self._buildNestedNodeElement(ref, role, level+1, rootId, reconType)
            else:
                self.Logger.warn( "Unkown memberElement, rootid=%s, member_type=%s, ref=%s, role=%s" %(rootId, memberType, ref, role))
                
        return nodeXml
  
    def isWritePartialObjs(self, ElementType, reconType, ElementData):
#         if (ElementType != Const.TAG_RELATION):
#             return False
        if (reconType == Const.RECON_TYPE_NATIONAL_BORDER or reconType == Const.RECON_TYPE_WATERWAY):
            return True
        if ((reconType !=Const.RECON_TYPE_WOOD) and (reconType !=Const.RECON_TYPE_WATER) and (reconType != Const.RECON_TYPE_PARK)):
            return False
        
        memberList = ElementData.get(u"member", [])
        size = len(memberList)
        if (size > Const.THRESHOLD_PARTCAL_RELATION):
            return True
        for member in memberList:
            memberType = member[u"type"]
            if (memberType == Const.TAG_RELATION):
                return True
        
        return False
        
    def isLineTypeObj(self, reconType):
        if ((reconType == Const.RECON_TYPE_NATIONAL_BORDER) or (reconType == Const.RECON_TYPE_WATERWAY)):
            return True
        
        return False
        
    # Generate a list to set flag whether or not we will put its child objects into recon map data. 
    # 0 - Not need its child objects; 1 - need all child objects; 2 - need the last child object (node); 3 - need the first child object (node)
    def GetNeedDataList(self, reconType, ElementData):
        fetchDataLst = []
        previousIsCached = False
        currIsCached = False
        #ids = []
        
        i = 0
        for member in ElementData.get(u"member", []):
            memberType = member[u"type"]
            ref = str(member[u"ref"])
            #self.Logger.debug("id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
            #ids.append(ref)

            if (memberType == Const.TAG_WAY):
                currIsCached = self.IsCached(ref, self.numCachedWay, self.cachedWays, self.wayFile)
            elif (memberType == Const.TAG_RELATION):
                currIsCached = self.IsCached(ref, self.numCachedRelation, self.cachedRelations, self.relationFile)
            elif (memberType == Const.TAG_NODE):
                currIsCached = self.IsCached(ref, self.numCachedNode, self.cachedNodes, self.nodeFile)
                
            if (currIsCached):
                fetchDataLst.append(1)
                if ((not previousIsCached) and (i > 0)):
                    fetchDataLst[i-1] = 2
            else:
                if (previousIsCached):
                    fetchDataLst.append(3)
                else:
                    fetchDataLst.append(0)
                    
            previousIsCached = currIsCached
            i += 1
            
        self.Logger.debug("GetNeedDataList numItems=%i, GetNeedDataList=%s", i, fetchDataLst)
        #print("list=%s", str(ids).translate(None, "'"))
        return fetchDataLst
    
    
    def _XmlEncode(self, text):
        #return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<","&lt;").replace(">","&gt;").replace(":","&colon;")
        return text.replace(":","_").replace("&"," and ")
      
    #def writeElement(self, xmlFile, elementType, cachedElements, numElementsInDisk, elementFile, elementFileName):
        
    def _getElementFromDiskById (self, elementId, numElementsInDisk, elementFile, elementFileName):
        element = {}
        if (Const.IS_DISK_SWITCH and numElementsInDisk[0] > 0):
            self.Logger.debug( "numNodeInDIsk=", numElementsInDisk[0])
            
            position = elementFile.tell()
            self.Logger.debug( "curr_position=%d" %( position))
            elementFile.seek(0, 0)
            position = elementFile.tell()
            #self.Logger.debug( "curr_position=%d" %( position))
             
            fileSize = os.stat(elementFileName).st_size
            self.Logger.debug( "node_file_Size=%s" %fileSize)
            
            for fieldList in iter(lambda: elementFile.read(Const.PROCESSING_SIZE), ""):
                #print len(fieldList)
                fields = fieldList.spilt("\t")
                self.Logger.debug( len(fields))
                if (len(fields) > 2):
                    self.Logger.warn( "Wrong parse, length=%d, source=%s" %(len(fields), fieldList))
                self.Logger.debug( "id=%s, element=%s" % (fields[0], fields[1]))
                if (elementId != fields[0]):
                    continue
                element = fields[1]
                break
                
        return element
    
    def Verificate(self, elementType, cachedElements, needList):
        lat = u""
        lon = u""
        isPass = True
        
        for elementId in needList:
            elementDict = cachedElements[elementId]
        #for elementId, elementDict in cachedElements.items():
            
            self.Logger.debug("id=%s, type=%s, element=%s" % (elementId, elementType, str(elementDict)))
            if (not elementDict):
                continue
            if (elementDict[Const.KEY_IS_IN_OUTPUT] != True):
                continue

            for k, v in elementDict.get(u"attr", {}).items():
                if k== "lat":
                    lat = v
                elif k == "lon":
                    lon = v
            if (lat != u"" and lon != u""):
                isPass = self.IsInsideOfTile(lat, lon)
            if (not isPass):
                self.Logger.error("Point (lat=%s, lon=%s) is not belong to this tile (minlat=%s, maxlat=%s, minlon=%s, maxlon=%s.", lat, lon, self.respBounds[u'minlat'], self.respBounds[u'maxlat'], self.respBounds[u'minlon'], self.respBounds[u'maxlon'])       
                self.Logger.error("Verificate(), Failure!!!" )
                        
        return
        
    def IsInsideOfTile(self, lat, lng):
        if (lat >= self.respBounds[Const.MINLAT] and lat <= self.respBounds[Const.MAXLAT] and lng >= self.respBounds[Const.MINLON] and lng <= self.respBounds[Const.MAXLON]):
            return True
        else:
            return False
    
    def IsInsideOfTileByValue(self, lat, lng):
        #print lat, lng
        #print self.reqBoundsInFloat
        if ((lat >= self.reqBoundsInFloat[Const.MINLAT]) and (lat <= self.reqBoundsInFloat[Const.MAXLAT]) and (lng >= self.reqBoundsInFloat[Const.MINLON]) and (lng <= self.reqBoundsInFloat[Const.MAXLON])):
            return True
        else:
            return False
    
    def isStartNewId(self, reconType, role):
        if (self.numNodesWrote == 0):
            return False
        if ((reconType == Const.RECON_TYPE_WATERWAY) and ( role == Const.ROLE_SIDE_STREAM or role == Const.ROLE_TRIBUTARY)):
            return True
        return False
    
    def setBoundary(self, minlat, minlon, maxlat, maxlon):
        bounds = {}
        bounds[Const.MINLAT] = str(minlat)
        bounds[Const.MINLON] = str(minlon)
        bounds[Const.MAXLAT] = str(maxlat)
        bounds[Const.MAXLON] = str(maxlon)
        self.reqBoundsInStr = bounds
        
        bounds1 = {}
        bounds1[Const.MINLAT] = minlat
        bounds1[Const.MINLON] = minlon
        bounds1[Const.MAXLAT] = maxlat
        bounds1[Const.MAXLON] = maxlon
        self.reqBoundsInFloat = bounds1
    
    def setMostLeftTile(self, isMostLeftTile):
        self.isMostLeftTile = isMostLeftTile
        
    #chop objects into the tile, throw away polygon outside.
    def proceedAction(self, tileId):
        self.Logger.debug("proceedAction")
        #Proceed Ocean Action 
        actionCoastLineList = self.parser.reconUtils.getItemFromDict(self.actionDict, Const.ACTION_OCEAN_GEO_REGION)
        oceanActionSize = len(actionCoastLineList)
        if (oceanActionSize > 0):
            self.proceedOceanAction(tileId, actionCoastLineList)

        #Proceed other Actions, forest, lake, park, and wetland
        for actionType in Const.ACTION_LIST_FOR_POLYGON_OBJECTS:
            actionList = self.parser.reconUtils.getItemFromDict(self.actionDict, actionType)


            actionSize = len(actionList)
            if (actionSize > 0):
                self.proceedPolygonActions(tileId, actionType, actionList)
            

        
    def proceedPolygonActions(self, tileId, actionType, actionList):
        self.Logger.debug("proceedPolygonActions, actionType=%s", actionType)
        innerActionTypePolygonList = []
        innerOppositeActionTypePolygonList = []
        for action in actionList:
            self.proceedPolygonAction(tileId, actionType, action, innerActionTypePolygonList, innerOppositeActionTypePolygonList)
        #eof for
        
        #draw inner objects
        if (Const.IS_DRAW):
            if (len(innerActionTypePolygonList) > 0):
                self.Logger.debug("Num_inner_land_Polygon=%d", (len(innerActionTypePolygonList)))
                self.geometric.drawTileAndMutilPolygon(None, self.reqBoundsInFloat, innerActionTypePolygonList, tileId + "_" + actionType + "_inner")
                
            if (len(innerOppositeActionTypePolygonList) > 0):
                self.Logger.debug("Num_inner_land_Polygon=%d", (len(innerOppositeActionTypePolygonList)))
                self.geometric.drawTileAndMutilPolygon(None, self.reqBoundsInFloat, innerOppositeActionTypePolygonList, tileId + "_"+ "land" + "_inner")
                
        #clean up
        innerActionTypePolygonList = []
        innerOppositeActionTypePolygonList = []
        
        
    def proceedPolygonAction(self, tileId, actionType, action, innerActionTypePolygonList, innerOppositeActionTypePolygonList):
        [elementId, element_type, action_type, elementDict, ndList, attr] = action
        elementDict[Const.KEY_IS_IN_OUTPUT] = False
        #self.getNeedList.remove(elementId)
        self.removeFromNeedList(element_type, elementId)
        
        reconType = elementDict[Const.KEY_RECON_TYPE]
        ndList = []
        isInsideOfTile = [False,]
        sz = self.parser.reconUtils._retriveElements(element_type, elementDict, reconType, ndList, True, isInsideOfTile)
        size = len(ndList)
        if (sz != size):
            print "ERROR SIZE not equal, retucode_sz=" + str(sz) +", num_node_size="+str(size)
            
        
        if ((not isInsideOfTile[0]) or (size == 0)):
            self.Logger.warn("proceedPolygonAction, tileId=%s, elementId=%s, elementType=%s, size=%d, isInsideOfTile=%r", tileId, elementId, element_type, size, isInsideOfTile[0])
            return
        
        
        i = 0
        j = 0
        start = ndList[0]
        end = ndList[0]
        outerList = []
        innerActionTypeList = []
        innerOppositeActionTypeList = []
        oppositeReconType = Const.RECON_TYPE_LAND
        outerSize = 0
        innerSize = 0
        self.Logger.debug("proceedPolygonAction, size=%d", size)
        
        
        while (i < size):
            start = ndList[i]
            isOneInside = start[2]
            isAllInside = start[2]
            j = i + 1

            while (j < size):
                end = ndList[j]
                if (len(end)<4):
                    print "error, end[2]"
                    print "end=" + str(end)
                if ((not isOneInside) and (end[2])):
                    isOneInside = True
                if ((isAllInside) and (not end[2])):
                    isAllInside = False
                    
                if ((start[0] != end[0]) or (start[1] != end[1])):
                    j += 1
                    continue
                
                if (not isOneInside):
                    j += 1
                    continue
                
                #find a polygon    
                role = start[3]
                polygon = ndList[i:j+1]
                if (isAllInside):
                    self.Logger.debug("IsAllInside=true")
                    if (role==Const.ROLE_INNER):
                        self.correctClockwise(polygon, False)
                        innerOppositeActionTypeList.append(polygon)
                    else:#role=="" or role == ROLE_OUTTER
                        self.correctClockwise(polygon, True)
                        innerActionTypeList.append(polygon)
                    innerSize += 1
                else:
                    self.Logger.debug("isOneInside=true")
                    if (role == Const.ROLE_INNER):
                        self.correctClockwise(polygon, False)
                        outerList.append(polygon)
                    else: #role == Const.ROLE_OUTER or role == ""
                        self.correctClockwise(polygon, True)
                        outerList.append(polygon)
                    outerSize += 1
                
                i = j
                break;
                
            i += 1
        #eof while
        
        #process outers
        segmentList = []
        for ndList in outerList:
            if (ndList[0] == ndList[(len(ndList)-1)]):
                #print "is closed."
                ndList.pop() #start from outside
                while (ndList[0][2]):
                    itm = ndList.pop()
                    ndList.insert(0, itm)
                ndList.append(ndList[0])
                
            
            segmentList +=self.addIntersectionPoints(elementId, element_type, elementDict, tileId, ndList)
        
        if (segmentList):
            self.generateGeoRegion(elementId, elementDict, tileId, segmentList, ndList, element_type)
            
            
        #process inner objects
        for ndList in innerActionTypeList:
            self.addToDictInList(element_type, reconType, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
        innerActionTypePolygonList.extend(innerActionTypeList)
        
        for ndList in innerOppositeActionTypeList:
            self.addToDictInList(element_type, oppositeReconType, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
        innerOppositeActionTypePolygonList.extend(innerOppositeActionTypeList)

        
        #clean up
        outerList = []
        innerActionTypeList = []
        innerOppositeActionTypeList = []
        
                    
        #end of proceedPolygonAction
        
    def proceedOceanAction(self, tileId, actionList):
        newRelationActionList = self.RetrieveSegmentList(tileId, actionList)
        self.Logger.warn("num_new_action=%i", len(newRelationActionList))
        
        self.joinCoastLineSegement(tileId, actionList)

        if newRelationActionList:
            #print len(actionList)
            actionList +=newRelationActionList
            #print len(actionList)
        
        segmentList = []
        for elementId, element_type, action_type, elementDict, ndList, attr in (actionList):
            if ((action_type == Const.ACTION_OCEAN_GEO_REGION or action_type == Const.ACTION_RIVER_BANK) and Const.IS_CALCULATE_OCEAN_GEO_REGION):
                #print elementId
                elementDict[Const.KEY_IS_IN_OUTPUT] = False
                #self.getNeedList(element_type).remove(elementId)
                self.removeFromNeedList(element_type, elementId)
                
                if (not ndList):
                    continue
                if (self.isInsidePolygon(ndList)):
                    if (attr == Const.ROLE_INNER):
                        #print "poly_inner"
                        self.addToDictInList(element_type, Const.RECON_TYPE_LAND, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
                        if (Const.IS_DRAW):
                            self.geometric.drawTileAndNodeList(self.reqBoundsInFloat, ndList, tileId + "-" + elementId + "-land")
                        continue
                    elif (attr == Const.ROLE_OUTER):
                        #print "poly_outer"
                        self.addToDictInList(element_type, Const.RECON_TYPE_OCEAN, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
                        if (Const.IS_DRAW):
                            self.geometric.drawTileAndNodeList(self.reqBoundsInFloat, ndList, tileId + "-" + elementId + "-ocean")
                        continue
                    else:
                        leng  = len(ndList)
                        if (leng < 2 or ndList[0] != ndList[leng-1]):
                            continue

                        clockwise = self.geometric.isClockwise(ndList[0:leng-1])
                        if (clockwise):
                            self.addToDictInList(element_type, Const.RECON_TYPE_OCEAN, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
                            if (Const.IS_DRAW):
                                self.geometric.drawTileAndNodeList(self.reqBoundsInFloat, ndList, tileId + "-" + elementId + "-ocean")
                        else:
                            self.addToDictInList(element_type, Const.RECON_TYPE_LAND, elementDict, tileId, Const.TAG_ACTION_POINT_LIST, ndList)
                            if (Const.IS_DRAW):
                                self.geometric.drawTileAndNodeList(self.reqBoundsInFloat, ndList, tileId + "-" + elementId + "-land")
                        continue
                        #print "poly_", attr
                    
                segmentList +=self.addIntersectionPoints(elementId, element_type, elementDict, tileId, ndList)
                
                
        if (segmentList):
            self.generateGeoRegion(elementId, elementDict, tileId, segmentList, ndList, element_type)
                
            

    def retrivePointsFromWay(self, elementId, elementDict, ndList):
        isInsideTile = False
        if (not elementDict):
            return isInsideTile
        

        #[elementId, element_type, action_type, elementDict, nodedList] = action
        self.Logger.debug("retrivePointsFromWay, wayId=%s." , elementId)
        
        #ndList = []
        #u'nd': [290748892, 478224037, 59203273, 478224051, 337947337, 452378432, 478224000, 478224086, 2400037711, 127417822, 127417824]
        for nodeId in elementDict.get(u"nd", []):
            
            #print nodeId
            '''
            try:
                nodeDict = self.cachedNodes[str(nodeId)]
            except Exception:
                self.Logger.error( "retrivePointsFromWay, Not found nested node id=%s in cache 'cachedNodes', try Disk." %(nodeId))
                #todo resquest new one.
                #for testing add exception for this case.
                continue
        
            if (not nodeDict):
                self.Logger.error("NO NODE FOUND, nodeId=", nodeId)
                continue
            '''
             
            nodeDict = self.getElementFromCache(str(nodeId), self.numCachedNode, self.cachedNodes, self.nodeFile, self.numNodesInDisk, Const.NODE_FILE_NAME, None)
             
             
            nodeAttrDict = nodeDict.get(u"attr", {})
            #print nodeAttrDict
            
            if not nodeAttrDict:
                self.Logger.warn( "retrivePointsFromWay, elementId=%s, nodeId=%s, not attribute." %(elementId, nodeId))
                continue;
        
            lat = float(nodeAttrDict[u'lat'])
            lng = float(nodeAttrDict[u'lon'])
            point = (lng, lat)
            isInside = self.geometric.isInside(point, self.reqBoundsInFloat)
            #self.Logger.debug ("id=%12s, isInside=%s, minlng=%.10f, lng=%.10f, maxlng=%.10f, minlat=%.10f, lat=%.10f, maxlat=%.10f.", nodeId, isInside, self.reqBoundsInFloat[Const.MINLON], 
            #       lng, self.reqBoundsInFloat[Const.MAXLON], self.reqBoundsInFloat[Const.MINLAT], lat, self.reqBoundsInFloat[Const.MAXLAT])
            
            if ((not isInsideTile) and (isInside)):
                isInsideTile = True
            
            ndList.append((lng, lat, isInside))
                
            
        return isInsideTile
            
    def getRiverbankWaySegmentList(self, action, tileId):

        retuNewActionList = []
        
        [elementId, element_type, action_type, elementDict, nodedList, attr] = action
        
        #resend request for relation object to get all
        if (Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_REMOTE_SERVER):
            self.parser.AddRelationFullById(elementId)
        
        outerList = []
        innerList = []
        
        for member in elementDict.get(u"member", []):
            memberType = member[u"type"]
            ref = str(member[u"ref"])
            role = member[u"role"]
            self.Logger.debug("id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
            
            if (memberType == Const.TAG_WAY):
                try:
                    wayDict = self.cachedWays[ref]
                except Exception:
                    self.Logger.warn( "getRiverbankWaySegmentList, Not found nested way id=%s in cache 'cachedWays'." %(ref))
                    #TODO Add WayFullByID
                    self.parser.AddWayFullById(ref)
                    try:
                        wayDict = self.cachedWays[ref]
                    except Exception:
                        #for debug
                        wayDict[1]
                        continue       

                
                ndList = []
                isInsideTile = self.retrivePointsFromWay(ref, wayDict, ndList)
                waySegment = [ref, ndList, False, isInsideTile]
                
                if (role == u"outer"):
                    outerList.append(waySegment)
                elif (role == u"inner"):
                    innerList.append(waySegment)
                else:
                    outerList.append(waySegment) #default, it is outer
                    
                '''if (isInsideTile):
                    print waySegment
                    waySegment[0] = elementId + u"_"+ref
                    waySegment[1] = element_type
                    waySegment[3] = elementDict
                    outerList.append(waySegment)'''
                waySegment = []
    
            elif (memberType == Const.TAG_RELATION or memberType == Const.TAG_NODE):
                self.Logger.warn( "getRiverbankWaySegmentList, id=%s, not processing neted relation and node for relation type, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
                continue
        #eof for
        
        bounds = self.reqBoundsInFloat
        #self.geometric.drawTileAndSegmentList(bounds, outerList, tileId +"_1"  )
        '''if outerList:
            print "outer=", len(outerList)
        if innerList:
            print "inner=", len(innerList)'''
        outerList = self.joinToPolygon(tileId, outerList, True)
        innerList = self.joinToPolygon(tileId, innerList, False)  
        '''if outerList:       
            print "outer=", len(outerList)
        if innerList:
            print "inner=", len(innerList)'''
        
        if (Const.IS_DRAW):  
            self.geometric.drawTileAndSegmentList(bounds, outerList, tileId +"_" + elementId + "_1"  )
            self.geometric.drawTileAndSegmentList(bounds, innerList, tileId +"_" + elementId + "_2"  )
         
        
        counter = 0
        if outerList:
            for wid, polygon, isInside in outerList:
                if ((not polygon) or (not isInside)):
                    continue
                size = len(polygon)
                if (size == 0):
                    continue
                #print polygon
                #if (not self.isClockwise(polygon[4])):
                #    polygon[4].reverse()
                
                newAction = [elementId+u"_"+wid, action[1], action[2], action[3], polygon, Const.ROLE_OUTER]
                retuNewActionList.append(newAction)
                counter += 1
        self.Logger.info( "getRiverbankWaySegmentList, outer_geoplogon=%i", counter)
            
        counter = 0
        if innerList:
            for wid, polygon, isInside in innerList:
                if ((not polygon) or (not isInside)):
                    continue
                size = len(polygon)
                if (size == 0):
                    continue
                #print polygon
                #if (self.isClockwise(polygon[4])):
                #    polygon[4].reverse()
                newAction = [elementId+u"_"+wid, element_type, action_type, elementDict, polygon, Const.ROLE_INNER]
                retuNewActionList.append(newAction)
                counter += 1
            self.Logger.info( "getRiverbankWaySegmentList, inner_geoplogon=%i", counter)
            
            
            
        return retuNewActionList
                    
    def RetrieveSegmentList(self, tileId, actionList):
        newActionList = []

        for idx, action in enumerate(actionList):
            self.Logger.info("RetrieveSegmentList, actionList_len=%i.", len(actionList))
            [elementId, element_type, action_type, elementDict, nodedList, attr] = action
            elementDict[Const.KEY_IS_IN_OUTPUT] = False
            #self.getNeedList(element_type).remove(elementId)
            self.removeFromNeedList(element_type, elementId)
            
            if (action_type != Const.ACTION_OCEAN_GEO_REGION and action_type != Const.ACTION_RIVER_BANK):
                continue
            
            self.Logger.debug("RetrieveSegmentList, tileId=%s, idx=%i, elementId=%s." , tileId, idx, elementId)
            if (element_type == Const.TAG_NODE):
                action[2] = Const.ACTION_NONE
                self.Logger.error ("RetrieveSegmentListCoastline object is type=%d.", element_type)
                continue
            elif (element_type == Const.TAG_WAY):
                if (not Const.isAddRelativeOsmObjectOn):
                    self.parser.AddWayFullById(elementId)
                action[4] = []
                isInsideOfTile = self.retrivePointsFromWay(elementId, elementDict, action[4])
                if (not isInsideOfTile):
                    action[2] = Const.ACTION_NONE
                    action[4] = []
                    continue
                       
                if action_type == Const.ACTION_RIVER_BANK:
                    self.correctClockwise(action[4], True)
                    
                if (Const.IS_DRAW):
                    self.geometric.drawTileAndNodeList(self.reqBoundsInFloat, action[4], tileId + "_" + elementId + "_3")
                #newActionList.append(action)
                continue
            elif (element_type == Const.TAG_RELATION ):
                if (not Const.isAddRelativeOsmObjectOn):
                    #retrive full relation
                    self.parser.AddRelationFullById(elementId)
                if (action_type == Const.ACTION_RIVER_BANK):
                    polygonPointList = self.getRiverbankWaySegmentList(action, tileId)
                    newActionList += polygonPointList
                    action[2] = Const.ACTION_NONE
                    #print len(newActionList)
                    
        return newActionList
        
        #return retuNewActionList
                     
    def correctClockwise(self, ndList, clockwiseReq):
        if (not ndList):
            return
        leng  = len(ndList)
        if (leng < 2 or ndList[0] != ndList[leng-1]):
            return

        clockwise = self.geometric.isClockwise(ndList[0:leng-1])
        if (clockwiseReq != clockwise):
            #print ndList
            ndList.reverse()
            #print ndList
            #print "end reverse"
                
    def joinToPolygon(self, tileId, waySegmentList, clockwiseReq):
        #size = len(waySegmentList)
        #for idx, elementId, element_type, action_type, elementDict, ndList in enumerate(waySegmentList):
        if (not waySegmentList):
            return
        
        returnList = []
        length = len(waySegmentList)
        
        #segmentList = enumerate(waySegmentList)
        #elementId, ndList, isInsideTile, isProcessed
        for idx in range (0, length):
            segment = waySegmentList[idx]
            [elementId, ndList, isProcessed, isInside] = segment
            
            #print "id1=", elementId
            if (not ndList or isProcessed):
                continue
            leng = len(ndList)
            if ((leng < 2)):
                continue
            
            if ((ndList[0] == ndList[leng-1])):
                segment[2] = True
                clockwise = self.geometric.isClockwise(ndList[0:leng-1])
                if (clockwiseReq != clockwise):
                    segment[1].reverse()
                returnList.append([elementId, segment[1], segment[3]])
                continue
            
            point1 = ndList[leng-1]
            isClosed = False
            j = 0
            while ((not isClosed) and (j < length)):
                #j=0
                #for idx2 in range (0, length):
                segment2 = waySegmentList[j]
                [elementId2, ndList2, isProcessed2, isInside2] = segment2
                j += 1
                if ((elementId == elementId2) or (not ndList2) or isProcessed2):
                    continue
                
                leng2 = len(ndList2)
                if(leng2 < 1):
                    continue
                
                isEqual = False
                    
                if (point1 == ndList2[0]):
                    isEqual = True
                elif (point1 == ndList2[leng2-1]):
                    segment2[1].reverse()
                    isEqual = True
                    
                    #for testr
                    #if (point1 == segment2[1][0]):
                    #    print "right"
                    #else:
                    #    print "wrong"
                    
                if (not isEqual):
                    continue
                
                    
                #print "id2=", elementId2
                segment[1] += segment2[1][1:leng2]
                leng += (leng2 -1)
                segment2[1] =[]
                segment2[2] = True
                segment[3] = (segment[3] or segment2[3])
                
                if (segment[1][0] == segment[1][leng-1]):
                    segment[2] = True
                    
                    clockwise = self.geometric.isClockwise(segment[1][0:leng-1])
                    if (clockwiseReq != clockwise):
                        segment[1].reverse()
                    returnList.append([elementId, segment[1], segment[3]])
                    isClosed = True
                    
                    break
                else:
                    point1 = segment[1][leng -1]
                    j = 0
                
            #eof while
            if ((not isClosed)):
                #print j
                self.Logger.debug("joinToPolygon, tileId=%s, elementId=%s is not ploygon." , tileId, elementId)
            
        return returnList
           
                
        self.Logger.debug("joinToPolygon Done, tileId=%s." , tileId)
        
    
    def joinCoastLineSegement(self, tileId, actionList):

        length = len(actionList)
        isProcessed = [False for x in range(length)]
        
        for idx in range (0, length):
            action = actionList[idx]
            [elementId, element_type, action_type, elementDict, ndList, attr ] = action
            
        
            #print "id1=", elementId
            if ((not ndList) or isProcessed[idx]):
                continue

            leng = len(ndList)
            if ((ndList[0] == ndList[leng-1])):
                continue
            
            point1 = ndList[leng-1]

            isClosed = False
            j = 0
            while ((not isClosed) and (j < length)):
                
            
                action2 = actionList[j]
                [elementId2, element_type2, action_type2, elementDict2, ndList2, attr] = action2
                
                j += 1
                if ((elementId == elementId2) or (not ndList2) or isProcessed[j-1]):
                    continue
                
                leng2 = len(ndList2)
                if(leng2 < 1):
                    continue
                
                point2 = ndList2[0]
                if (point1 != point2):
                    continue
                
                #print "id2=", elementId2
                action[4] += action2[4][1:leng2]
                leng += (leng2 -1)
                action2[4] =[]
                isProcessed[j-1] = True

                if (action[4][0] == action[4][leng-1]):
                    isProcessed[idx] = True
                    isClosed = True
                    break
                else:
                    point1 = action[4][leng -1]
                    j = 0
                    
                    
            #eof while
           
        #eof for
        
        #for testing
        for idx in range (0, length):
            action = actionList[idx]
            [elementId, element_type, action_type, elementDict, ndList, attr ] = action
            #if (ndList):
            #    print ("idx=%i, ndlist_len=%i" %(idx, len(ndList)))
        self.Logger.debug("joinCoastLineSegement Done, tileId=%s." , tileId)
                    
       

          
    def addIntersectionPoints(self, elementId, element_type, elementDict, tileId, ndList):
        self.Logger.debug("addIntersectionPoints, tileId=%s, wayId=%s." , tileId, elementId)
        segmentList = []
        segment=[]
        segmentSize = 0
    
    
        self.Logger.debug ("ndList=%s", str(ndList))
        size = len(ndList)
        if (size < 3):
            #todo clear up the nd list
            self.Logger.warn("addIntersectionPoints, no enough points (size=%i) to calculate geoRegion, tileId=%s, wayId=%s.", size, tileId, elementId)
            elementDict[Const.KEY_IS_IN_OUTPUT] = False
            #self.getNeedList(element_type).remove(elementId)
            self.removeFromNeedList(element_type, elementId)
            
            return segmentList
    
        #For the special case of closed polygon, it need start from outside
        if (ndList[0] == ndList[size-1] and ndList[0][2]):
            p = ndList.pop()
            while (ndList[0][2]):
                p = ndList.pop(0)
                ndList.append(p)
            ndList.append(ndList[0])

        
        out1 = 0
        out2 = 0
        in1 = 0
        in2 = 0
        
        STATE_PRE_IN = 0
        STATE_IN = 1
        STATE_OUT = 3
        state = STATE_PRE_IN
        bounds = self.reqBoundsInFloat
        
        i = 0
        while (i < size):
            if (state == STATE_PRE_IN):
                if (not ndList[i][2]):
                    i += 1
                    continue

                if ((i == 0)):
                    if (ndList[1][2]):
                        in1 = 0
                        in2 = 1       
                        state = STATE_IN
                        poi = self.geometric.getBackJoinPoint(ndList[in1], ndList[in2], bounds)
                        segment = [poi, ndList[in1]]
                        
                    i += 1
                    continue
                else:
                    in2 = i
                    in1 = in2 - 1
                    state = STATE_IN
                    poi = self.geometric.getCrossingPoint(ndList[in1], ndList[in2], bounds)
                    segment = [poi, ndList[in2]]
                    i += 1
                    continue
            
            elif state == STATE_IN:
                if (ndList[i][2]):
                    segment.append(ndList[i])
                else:
                    out2 = i
                    out1 = out2 -1
                    state = STATE_OUT
                    poi = self.geometric.getCrossingPoint(ndList[out1], ndList[out2], bounds)
                    segment.append(poi)
                    segmentList.append(segment)
                    segmentSize += 1
                    segment = []
                    
                i += 1
                continue
            
            elif state == STATE_OUT:
                if (ndList[i][2]):
                    in2 = i
                    in1 = in2 - 1
                    state = STATE_IN
                    poi = self.geometric.getCrossingPoint(ndList[in1], ndList[in2], bounds)
                    segment = [poi, ndList[in2]]
                    
                i += 1
                continue
                 
            #eof of while

        if (state == STATE_IN and ndList[size-1][2] and ndList[size-2][2]):
            out1 = size -2
            out2 = size -1
            poi = self.geometric.getJoinPoint(ndList[out1], ndList[out2], bounds)
            segment.append(poi)
            segmentList.append(segment)
            segment = []
            segmentSize += 1
                                    
        if ((state == STATE_PRE_IN)  and (i >= size)):
            self.Logger.debug("addIntersectionPoints, size=%d, all nodes are outside of bounds.")
            elementDict[Const.KEY_IS_IN_OUTPUT] = False
            #self.getNeedList(element_type).remove(elementId)
            self.removeFromNeedList(element_type, elementId)
            return segmentList
        
        self.Logger.info("addIntersectionPoints, id_elemId=%s, segment_len=%i", elementId +"_" + tileId, segmentSize)
        return segmentList
        #eof  addIntersectionPoints
    
        
    def generateGeoRegion(self, elementId, elementDict, tileId, segmentList, ndList, element_type):
         
        
        allNdList = []
        polygonList = []
        polygon = []
        segSize = len(segmentList)
        segmentDone = [False for x in range(segSize)]
        bounds = self.reqBoundsInFloat
        processingSegments = []
        closedSegments = []
        
        i = 0
        while (i < segSize):
            if (segmentDone[i]):
                i += 1
                continue
            
            seg = segmentList[i]
            size = len(seg)
            if (size < 3):
                #TODO LOG
                continue
      
            processingSegments.append(i)
            in1 = seg[0]
            out1 = seg[size-1]
            
            polygon.append(in1)
            allNdList.append(in1)
            
            #for testng
            #print ("in_from_edge=%i", self.geometric.getEdgeNumber(in1, bounds))
            #print ("out_from_edge=%i", self.geometric.getEdgeNumber(out1, bounds))
            
            for j in range (1, (size-1)):
                polygon.append(seg[j])
                allNdList.append(seg[j])

            inPointList = []
            for k in range (0, segSize):
                if (segmentDone[k]):
                    inPointList.append(None)
                else:
                    inPointList.append(segmentList[k][0])

                
            nextSegmentIdx = self.geometric.findNextPointInClockwise(out1, inPointList)
            
            if (nextSegmentIdx == -1):
                #TODO LOG
                i += 1
                continue
            
            if (nextSegmentIdx not in closedSegments):
                closedSegments.append(nextSegmentIdx)
                
                segment2 = segmentList[nextSegmentIdx]
          
                in2 = segment2[0]
                lst = self.geometric.getTileEdgeByPoints(out1, in2, bounds)
                #print lst
                polygon += lst
                allNdList += lst
               
                #for testng
                #print ("in_from_edge=%i", self.geometric.getEdgeNumber(in2, bounds))
                
            #segmentList[processingSegments[0]][0] == in2 or 
            if ((nextSegmentIdx in processingSegments)):
                if (polygon and (polygon[0] != segmentList[processingSegments[0]][0])):
                    polygon.append(segmentList[processingSegments[0]][0])

                if (self.parser.reconUtils.isPolygon(polygon)):              
                    polygonList.append(polygon)
                polygon = []
                for m in processingSegments:
                    segmentDone[m] = True
                processingSegments = []
                i = 0
                continue
                
            else:
                i = nextSegmentIdx
            continue
        
        reconType = elementDict[Const.KEY_RECON_TYPE]
        if (Const.IS_DRAW and (len(polygonList) > 0)):
            #print ("Num_Polygon=%d" %(len(polygonList)))
            self.geometric.drawTileAndMutilPolygon(None, self.reqBoundsInFloat, polygonList, tileId + "-" + elementId+"_"+ reconType)
            #self.geometric.drawTileAndMutilPolygon(None, self.reqBoundsInFloat, segmentList, tileId + "_" + elementId+"_" + reconType + "_seg", True)
            


        self.addToDictInListList(element_type, reconType, elementDict, Const.TAG_ACTION_POINT_LIST, polygonList,  tileId)
                
    
        #eof generateGeoRegion

    def addToDictInList(self, element_type, recon_type, dictio, tileId, tag, newone):
        if ((not dictio) or (not tag) or (not newone) or (not recon_type)):
            return

        dicti = copy.deepcopy(dictio)
        newId = dicti[u'attr'][u'id'] + "_" + tileId + "_" + str(self.oceanLandSN)
        dicti[u'attr'][u'id'] = newId
        self.oceanLandSN += 1
        dicti[Const.KEY_IS_IN_OUTPUT] = True
        self.getNeedList(element_type).append(newId)
        
        dicti[Const.KEY_RECON_TYPE] = recon_type
        dicti[tag] = [newone]
        
        if element_type == Const.TAG_WAY:
            self.cacheElement(dicti, self.numCachedWay, self.cachedWays, self.wayFile, self.numWaysInDisk)
        elif element_type == Const.TAG_RELATION:
            self.cacheElement(dicti, self.numCachedRelation, self.cachedRelations, self.relationFile, self.numRelationsInDisk)
        else:
            self.Logger.error("addToDictInList, not support type=%d yet.", element_type)
        
        if (recon_type == Const.RECON_TYPE_OCEAN or recon_type == Const.RECON_TYPE_LAND):
            self.cachedOceanList.append([tileId, recon_type, dicti])
        
        if (recon_type == Const.RECON_TYPE_WOOD or recon_type == Const.RECON_TYPE_PARK):
            self.forestList.append([tileId, recon_type, dicti])
            
            
        
    
        return 

    def addToDictInListList(self, element_type, recon_type, dicti, tag, newList, tileId):
        if ((not dicti) or (not tag) or (not newList) or (not recon_type)):
            return
    
        for one in newList:
            self.addToDictInList(element_type, recon_type, dicti, tileId, tag, one)
            
        return 
    
    def isInsidePolygon(self, pointList):
        if (not pointList):
            return True
        
        size = len(pointList)
        if (pointList[0] != pointList[size-1]):
            return False
        
        for p in pointList:
            if (not p[2]):
                return False
            
        return True
    
    
    def getNeedList(self, elementType):
    
        if (elementType == Const.TAG_WAY):
            return self.needWaysList
        elif (elementType == Const.TAG_RELATION):
            return self.needRelationsList
        else:
            return self.needNodesList
    
        
    def removeFromNeedList(self, elementType, elementId):
        needList = []
        if (elementType == Const.TAG_WAY):
            needList = self.needWaysList
        elif (elementType == Const.TAG_RELATION):
            needList = self.needRelationsList
        else:
            needList = self.needNodesList
            
        try:
            needList.remove(elementId)
        except Exception:
            self.Logger.warn("elementId=%s not exists in needList.", elementId)
          
        

    '''
    def testGeometric(self):
        #geometric = ReconGeometric(self.Logger)
        #geometric.testInteractions()
        #pointlist1 = [(0,0), (1, 0), (1,1), (0,1)]
        #pointlist2 = [(0,0), (2, 0), (2,2), (0,2)]
        #intersect = geometric.IntersectGeo(pointlist1, pointlist2)
        #diff = geometric.DifferenceGeo(pointlist1, pointlist2)
        bounds = {}
        bounds[Const.MINLAT] = 0.0
        bounds[Const.MINLON] = 0.0
        bounds[Const.MAXLAT] = 10.0
        bounds[Const.MAXLON] = 10.0
            
        #bounds = (0,0), (1, 0), (1,1), (0,1)]
        #tart = (2, 3)
        #print geometric.getEdgeNumber(start, bounds)
        
        #stop = (3, 1)
        #print geometric.getEdgeNumber(stop, bounds)
        
        #lst = geometric.getTileEdgeByPoints(start, stop, bounds)
        #print lst
        
        #start = (5, 5)
        #stop =  (7, 14)
        
        #lst = self.geometric.getJoinPoint(start, stop, bounds)
        #print lst
        
        route =[(1.0, 1.0), (2.0, 3.0),(4.0, 6.0)]
        #bounds =[(1.0, 1.0), (10.0, 10.0),(4.0, 6.0)]
        regionPoints =[(1.0, 1.0), (1.0, 2.0),(2.0, 2.0),(2.0, 1.0),(1.0, 1.0)]
        #print 
        #self.geometric.drawLines(lst)
        self.geometric.drawTile(route, bounds, regionPoints)
        

    def getFromDictInList(self, dicti, tag):
        if ((not dicti) or (not tag) ):
            return
        lst = dicti[tag]
        i =0
        
        print ('**********')
        for v in lst:
            print ("i=", i, " v=", v)
            i +=1
        print ("---------")
        return lst      
    def getNodesFromCrossing(self, q1, q2, q3, q4, bounds):
        
        startEdge = self.geometric.getCrossingPoint(q1, q2, self.reqBoundsInFloat)
        stopEdge  = self.geometric.getCrossingPoint(q3, q4, self.reqBoundsInFloat)
        lst =  self.geometric.getTileEdgeByPoints(stopEdge, startEdge, self.reqBoundsInFloat)
        print str(lst)
        return lst
    
    def getEdgeNodes(self, p1, p2, p3, p4, startPointsInside, endPointsInside, bounds):
        
        startEdge = []
        stopEdge  = []
        if startPointsInside:
            startEdge = self.geometric.getBackJoinPoint(p1, p2, bounds)
        else :
            startEdge = self.geometric.getCrossingPoint(p1, p2, bounds)
        
        if endPointsInside:
            stopEdge = self.geometric.getJoinPoint(p3, p4, bounds)
        else :    
            stopEdge  = self.geometric.getCrossingPoint(p3, p4, bounds)
            
        lst =  self.geometric.getTileEdgeByPoints(stopEdge, startEdge, bounds)
        print "getEdgeNodes=", str(lst)
        return lst
    
    def addPloygonNodes(self, n1, n2, n3, n4, startPointsInside, endPointsInside, bounds, ndList, allNdList, polygonList):
            newPloygon = []
            lastInside = 0
            if (endPointsInside):
                newPloygon.append(ndList[n4])
                lastInside = n4
            else:
                newPloygon.append(ndList[n3])
                lastInside = n3
            
            newPloygon += self.getEdgeNodes(ndList[n1], ndList[n2], ndList[n3], ndList[n4], startPointsInside, endPointsInside, self.reqBoundsInFloat)
            for j in range (n2, (lastInside + 1)):
                print j
                if ndList[j][2]:
                    newPloygon.append(ndList[j])
                    
            polygonList.append(newPloygon)
            allNdList += newPloygon
                        
        
    
    def getDiskNodeById (self, nodeId):
        node = {}
        if (Const.IS_DISK_SWITCH and self.numNodesInDisk > 0):
            print "numNodeInDIsk=", self.numNodesInDisk
            
            position = self.nodeFile.tell()    <point lat="49.2717728" lon="-123.1072916"/>
    <point lat="49.2717754" lon="-123.1073348"/>
    <point lat="49.2717783" lon="-123.1077535"/>
    <point lat="49.2723799" lon="-123.1077261"/>
    <point lat="49.2723912" lon="-123.1078627"/>
    <point lat="49.2723328" lon="-123.1078639"/>
    <point lat="49.2723501" lon="-123.1088508"/>
    <point lat="49.2724233" lon="-123.1091765"/>
    <point lat="49.2724028" lon="-123.1094757"/>
            print "curr_position=%d" %( position)
            self.nodeFile.seek(0, 0)
            position = self.nodeFile.tell()
            print "curr_position=%d" %( position)
             
            fileSize = os.stat(Const.NODE_FILE_NAME).st_size
            print "node_file_Size=%s" %fileSize
            
            for fieldList in iter(lambda: self.wayFile.read(Const.PROCESSING_SIZE), ""):
                #print len(fieldList)
                fields = fieldList.spilt("\t")
                print len(fields)
                if (len(fields) > 2):
                    print "Wrong parse, length=%d, source=%s" %(len(fields), fieldList)
                print "id=%s, node=%s" % (fields[0], fields[1])
                if (nodeId != fields[0]):
                    continue
                node = fields[1]
                break
                
        return node
    
    def getDiskWayById (self, wayId):
        way = {}
        return way
    
    def getDiskRelationById (self, relationId):
        relationDict = {}
        return relationDict
    
    
    def findNodefromList(self, nodeId):
        result = ""
        if (self.numCachedNode < 1):
            return result
        result = self.cachedNodes[nodeId]
            
        
        print "Don't Found the Node from List"
        return result
    '''
  

###########################################################################
## End of RimWriter class                                                ##
###########################################################################
        
    

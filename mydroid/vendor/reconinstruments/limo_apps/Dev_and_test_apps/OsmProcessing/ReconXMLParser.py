import logging.handlers
from ReconConst import Const
import lxml.etree as ET
#import textwrap
import time



        
class XMLParser:
    
    def __init__(self, parser = None, writer = None, logger=None):
        self.Logger = logger
        self.parser = parser
        self.writer= writer

        

    
    def ParseOsmByLxml(self, xmlFile, isAddToOutput=True):
        if (True):
            return
        numNode = 0
        numWay = 0
        numRelation = 0
        self.Logger.info(  "***ParseOsmByLxml")
        start_time = time.time()
        context = ET.iterparse(xmlFile, events=('end',), tag='osm')
        #self.osm_fast_iter(context, lambda elem: None)
        
        reconType = []
        for event, element in context:
        
            for elem in element.xpath('node|way|relation|bounds'):
                reconType = []
                self.parser.counter += 1
                #print (self.parser.counter)
                #print elem.tag
                
                if elem.tag == Const.TAG_NODE:
                    nodeDict = self.LxmlParseNode(elem)
                    #if (self.debug):
                    #    self.Logger.debug(  "---Parsing Node_id=%s" % nodeDict[u'attr'][u'id'])
                    if (isAddToOutput):
                        isInOutput = self.parser.IsNeed(nodeDict, Const.TAG_NODE, self.parser.node_features, self.parser.node_types, None, reconType, self.writer.needNodesList)
                        nodeDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                    else:
                        nodeDict[Const.KEY_IS_IN_OUTPUT] = False
                    if (reconType):
                        nodeDict[Const.KEY_RECON_TYPE] = reconType[0]
                    #self.writer.cacheNode(nodeDict)
                    self.writer.cacheElement(nodeDict, self.writer.numCachedNode, self.writer.cachedNodes, self.writer.nodeFile, self.writer.numNodesInDisk)
                    numNode += 1
                    
                elif elem.tag == Const.TAG_WAY:
                    wayDict = self.LxmlParseWay(elem)
                    #self.Logger.debug(  "---Parsing WAY_id=%s" % wayDict[u'attr'][u'id'])
                    #if ("109276905" == wayDict[u'attr'][u'id']):
                    #    print "debug"
                    if (isAddToOutput):
                        isInOutput = self.parser.IsNeed(wayDict, Const.TAG_WAY, self.parser.way_features, self.parser.way_types, self.parser.way_actions, reconType, self.writer.needWaysList)
                        wayDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                    else:
                        wayDict[Const.KEY_IS_IN_OUTPUT] = False
                    if (reconType):
                        wayDict[Const.KEY_RECON_TYPE] = reconType[0]
                    #self.writer.cacheWay(wayDict, reconType)
                    self.writer.cacheElement(wayDict, self.writer.numCachedWay, self.writer.cachedWays, self.writer.wayFile, self.writer.numWaysInDisk)
                    numWay += 1
                        
                elif elem.tag == Const.TAG_RELATION:
                    relationDict = self.LxmlParseRelation(elem)
                    #self.Logger.debug(  "---Parsing RELATION_id=%s" % relationDict[u'attr'][u'id'])
                    
                    if (isAddToOutput):
                        isInOutput = self.parser.IsNeed(relationDict, Const.TAG_RELATION, self.parser.relation_features, self.parser.relation_types, self.parser.relation_actions, reconType, self.writer.needRelationsList)
                        relationDict[Const.KEY_IS_IN_OUTPUT] = isInOutput
                    else:
                        relationDict[Const.KEY_IS_IN_OUTPUT] = False
                    if (reconType):
                        relationDict[Const.KEY_RECON_TYPE] = reconType[0]
                    #self.writer.cacheRelation(relationDict, reconType);
                    self.writer.cacheElement(relationDict, self.writer.numCachedRelation, self.writer.cachedRelations, self.writer.relationFile, self.writer.numRelationsInDisk)
                    numRelation += 1
                elif ((elem.tag == Const.TAG_BOUNDS) and (isAddToOutput)):
                    print str(self.LxmlParseBounds(elem))
                    self.writer.respBounds = self.LxmlParseBounds(elem)
                    #print ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (self.writer.respBounds[u'minlon'], self.writer.respBounds[u'minlat'], self.writer.respBounds[u'maxlon'], self.writer.respBounds[u'maxlat']))
                    #print ("bounds minlon=%s, minlat=%s, maxlon=%s, maxlat=%s." % (self.writer.reqBoundsInStr[u'minlon'], self.writer.reqBoundsInStr[u'minlat'], self.writer.reqBoundsInStr[u'maxlon'], self.writer.reqBoundsInStr[u'maxlat']))
                    
    
                #print('Deleting {p}'.format(p=(elem.tag)))
                del elem
            element.clear()
            del element
            del event
        del context
        self.Logger.warn('processed (numNode=%d, numWay=%d, numRelation=%d); total cachedNode=%d, cachedWay=%d, cachedRelation=%d' %(numNode, numWay, numRelation, self.writer.numCachedNode[0], self.writer.numCachedWay[0], self.writer.numCachedRelation[0] ) )
        #self.Logger.info('caches:, cachedNode=%s, cachedWay=%s, cachedRelation=%s' %(self.writer.cachedNodes, self.writer.cachedWays, self.writer.cachedRelations ))
        elapsed_time = (time.time() - start_time)
        self.Logger.warn("---End parse xml dom, elapsed_time=%.2f secs.", elapsed_time)
        self.parser._UsingMemory("ParseOsmByLxml")
        
        
    def LxmlParseNode(self, LxmlElement):
        """ Returns NodeData for the node. """
        result = {}
        result[u"attr"] = self._LxmlGetAttributes(LxmlElement)
        result[u"tag"] = self._LxmlGetTag(LxmlElement)
        return result

    def LxmlParseWay(self, LxmlElement):
        """ Returns WayData for the way. """
        result = {}
        result[u"attr"] = self._LxmlGetAttributes(LxmlElement)
        result[u"tag"] = self._LxmlGetTag(LxmlElement)
        result[u"nd"]  = self._LxmlGetNd(LxmlElement)        
        return result
    
    def LxmlParseRelation(self, LxmlElement):
        """ Returns RelationData for the relation. """
        result = {}
        result[u"attr"] = self._LxmlGetAttributes(LxmlElement)
        result[u"tag"]    = self._LxmlGetTag(LxmlElement)
        result[u"member"] = self._LxmlGetMember(LxmlElement)
        return result
      
    def LxmlParseBounds(self, LxmlElement):
        """ Returns RelationData for the relation. """
        result = self._LxmlGetAttributes(LxmlElement)
        return result
    
    def _LxmlGetAttributes(self, LxmlElement):
        """ Returns a formated dictionnary of attributes of a LxmlElement. """
        result = {}
        for k, v in LxmlElement.attrib.items():
            result[k] = v
        return result
 
    def _LxmlGetTag(self, LxmlElement):
        """ Returns the dictionnary of tags of a LxmlElement. """
        result = {}
        for tag in LxmlElement.xpath('tag'):
            k = tag.attrib["k"]
            v = tag.attrib["v"]
            result[k] = v
        return result
    
    def _LxmlGetNd(self, LxmlElement):
        """ Returns the list of nodes of a LxmlElement. """
        result = []
        for nd in LxmlElement.xpath('nd'):
            ref=int(nd.attrib["ref"])
            result.append(ref)
        return result 
    
    def _LxmlGetMember(self, LxmlElement):
        """ Returns a list of relation members. """
        result = []
        for m in LxmlElement.xpath("member"):
            result.append(self._LxmlGetAttributes(m))
        return result
    
    def osm_fast_iter(self, context, func, *args, **kwargs):
        print "mod_fast_iter"
        for event, elem in context:
            print('-' * 80)
            #print('Processing {e}'.format(e=ET.tostring(elem)))
            #func(elem, *args, **kwargs)
            # It's safe to call clear() here because no descendants will be
            # accessed
            #print('Clearing {e}'.format(e=ET.tostring(elem)))
            #elem.clear()
            # Also eliminate now-empty references from the root node to elem
            for osmElem in elem.xpath('node|way|relation'):
                print('-' * 10)
                print('Checking tag: {a}'.format(a=osmElem.tag))
                if (osmElem is None):
                    continue
                
                if (osmElem.tag == 'node'):
                    result = self.LxmlParseNode(osmElem)
                    print result
                elif (osmElem.tag == 'way'):
                    result = self.LxmlParseWay(osmElem)
                    print result
                    
                elif (osmElem.tag == 'relation'):
                    result = self.LxmlParseRelation(osmElem)
                    print result
                elif (elem.tag == Const.TAG_BOUNDS):
                    print str(self.LxmlParseBounds(osmElem))
                    
#                 id = osmElem.attrib['id']
#                 version = osmElem.attrib['version']
#                 print('id={a}'.format(a=id))
#                 print version
#                 attrs = osmElem.attrib
#                 print str(attrs)
#                 
#  
#                 for tag in osmElem.xpath('tag'):
#                     kv=tag.attrib
#                     print(kv)
#                     
#                 for nd in osmElem.xpath('nd'):
#                     ndAttrs=nd.attrib
#                     print(ndAttrs)
#                     
#                 for member in osmElem.xpath('member'):
#                     self.LxmlParseRelation(member)
#                     #memberAttrs=member.attrib
#                     #print(memberAttrs)
                
                #fields='{p}'.format(p=(osmElem.getparent()[0]).tag)
                #values='{v}'.format(v=(osmElem.values))
                #print(fields +"=" + values) 
                print('Deleting {p}'.format(p=(osmElem.tag)))
                del osmElem
            elem.clear()
            del elem
            del event
        del context
        

        

         
if __name__ == "__main__":
    
    '''Setup the logging'''
    LOG_PATH = "logs/"
    LOG_FILE_NAME = "osmtest"
    LOG_TO_CONSOLE = True
    LOG_DEFAULT_LEVEL = logging.DEBUG
    #LOG_DEFAULT_LEVEL = logging.INFO
    #LOG_DEFAULT_LEVEL = logging.WARN
    LOG_FILE_MAX_BYTES = 1*1024*1024
    LOG_BACKUP_COUNT = 2
    Logger = logging.getLogger()
    Logger.setLevel(LOG_DEFAULT_LEVEL)
    fileHandler = logging.handlers.RotatingFileHandler("{0}/{1}.log".format(LOG_PATH, LOG_FILE_NAME), maxBytes=LOG_FILE_MAX_BYTES, backupCount=LOG_BACKUP_COUNT)
    logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
    fileHandler.setFormatter(logFormatter)
    Logger.addHandler(fileHandler)
    #Logger = None
    
    xmlParser = XMLParser(Logger)

    print('-' * 80)
    infile = u"map_osm/o.xml"
    context = ET.iterparse(infile, events=('end',), tag='osm')
    xmlParser.osm_fast_iter(context, lambda elem: None)    
    #xmlParser.ParseOsmByLxml(infile, True)
    

    
    
    print "done."
    
   

    
    

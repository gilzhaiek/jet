import logging

from ReconConst import Const

from ReconFetcher import ReconFetcher
#from ReconProcessor import OsmParser, RimWriter
#from ReconGlobalSweep import GlobalSweep
import sys
import math


class ReconUtils:
    
    def __init__(self, parser, writer, logger):
        self.parser = parser
        self.writer = writer
        self.Logger = logger

        self.ID = 55666207
        self.TILE_HEIGHT_IN_DEGREE = (0.025)
        self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR = (0.024958105) #which value is same as the one in MapApp
        self.PRECISION_ROUND = 0.0000000001
        self.OVERLAP_IN_DEGREE = 0.000001

    
    def __del__(self):
        self.Logger("ReconUtils.__del__()")
    
    def retriveElement(self, ndList, elementType, cachedElements):
        numElementRetrived = 0
        self.Logger.info(">>>writting type=%s", elementType)
        
        #Write ways from cachedElements
        for elementId, elementDict in cachedElements.items():
            self.Logger.debug("id=%s, type=%s, element=%s" % (elementId, elementType, str(elementDict)))
            if (not elementDict):
                continue
            if (elementDict[Const.KEY_IS_IN_OUTPUT] != True):
                continue

                
            
            numElementRetrived = self._retriveElements(ndList, elementType, elementDict, elementDict[Const.KEY_RECON_TYPE], ndList, False, [False,])        
      
            self.Logger.debug("writting id=%s, type=%s, numNodeRetrived=%s, isInsideBound=%r", elementId, elementType, numElementRetrived)
            
        return numElementRetrived
    
       
    def _retriveElements(self, ElementType, ElementData, reconType, ndList, isClosed, insideOfTile):
        
        lat = u""
        lon = u""
        numNode = 0
        startPoint = 0
        elementId = ""

        if ElementType not in [Const.TAG_NODE, Const.TAG_WAY, Const.TAG_RELATION]:
            return numNode


        # element attr id
        attrDict = ElementData.get(u"attr", {})
        
        if (attrDict):
            k = Const.KEY_ID
            elementId = attrDict[k]
             
        # element attr - othes
        for k, v in ElementData.get(u"attr", {}).items():
            if k== "lat":
                lat = v
            elif k == "lon":
                lon = v
        
        role = ""
        try:
            role = attrDict[Const.TAG_ROLE]
        except Exception:
            role =""
        

        if (ElementType == Const.TAG_NODE):
            latFloat = float(lat)
            lonFloat = float(lon)
            isInside = self.writer.IsInsideOfTileByValue(latFloat, lonFloat)
            point = (lonFloat, latFloat, isInside, role)
            ndList.append(point);
            numNode += 1
            if ((not insideOfTile[0]) and (isInside)):
                insideOfTile[0] = True
        
        elif (ElementType == Const.TAG_WAY):
            if (not Const.isAddRelativeOsmObjectOn):
                self.parser.AddWayFullById(elementId)
            for nodeId in  ElementData.get(u"nd", []):
                numNode += self._retriveNestedNodeElement(str(nodeId), role, 2, elementId, reconType, ndList, insideOfTile)
                
        else: # element member
            if (not Const.isAddRelativeOsmObjectOn):
                self.parser.AddRelationFullById(elementId)
            cachedList = []
            for member in ElementData.get(u"member", []):
                #xml += u"    <member type=\""+member[u"type"]+"\" ref=\""+str(member[u"ref"])+u"\" role=\""+self._XmlEncode(member[u"role"])+"\"/>\n"
                memberType = member[u"type"]
                ref = str(member[u"ref"])
                role = member[u"role"]
                self.Logger.debug("id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))


                currNdList = []
                num = 0

                if (memberType == Const.TAG_NODE):
                    numNode += self._retriveNestedNodeElement(ref, role, 2, elementId, reconType, ndList, insideOfTile)
                    continue
                elif (memberType == Const.TAG_WAY):
                    num += self._retriveNestedWayElement(ref, role, 2, elementId, reconType, currNdList, insideOfTile)
                elif (memberType == Const.TAG_RELATION):
                    num += self._retriveNestedRelationElement(ref, role, 2, elementId, reconType, currNdList, insideOfTile, isClosed)
                else:
                    self.Logger.warn( "Unkown memberElement, id=%s, member_type=%s, ref=%s, role=%s" %(elementId, memberType, ref, role))
                
                
                currLeng = len(currNdList)
                if (currLeng == 0):
                    continue
            
            
                cachedList.append([ref, currNdList])
                continue;
                #remainListSize = len(cachedList)
            
                '''
                #numNode += num
                length = len(ndList)
                   
                
                if (length == startPoint):#start
                    ndList.extend(currNdList)
                    numNode += num
                    if (ndList[startPoint] == ndList[(len(ndList)-1)]):
                        startPoint = len(ndList) #print "It is closed polygon."
                    continue
                
                cachedList.append([ref, currNdList])
                remainListSize = len(cachedList)
                #if (remainListSize > 1):
                    #print remainListSize
                    
                i = 0
                isJoin = False
                while (i < remainListSize):
                    curCachedItem = cachedList[i]
                    curNdList = curCachedItem[1]
                    curLength = len(curNdList)
                    if (self.joinTwoSegments(ndList, startPoint, len(ndList), curNdList, curLength)):
                        numNode += (curLength - 1)
                        #print "1:" + str(len(cachedList))
                        cachedList.remove(curCachedItem)
                        del curNdList[:]
                        del curCachedItem[:]
                        #print "2:" + str(len(cachedList))
                        remainListSize -= 1
                        i = 0
                        isJoin = True
                    else:
                        i += 1
                        #print i
                    continue
                
                #if (not isJoin):
                #    self.Logger.warn("_retriveElements cached, eid=%s, cid=%s.", elementId, ref)
                    
                    
      
                if (ndList[startPoint] == ndList[(len(ndList)-1)]):
                    startPoint= len(ndList) #print "It is closed polygon."
            #eof for
            
            #joint of cachedList
            for curCachedItem in cachedList:
                curNdList = curCachedItem[1]
                currSize = len(curNdList)
                if ((currSize > 0) and (curNdList[0] != curNdList[currSize-1])):
                    self.Logger.warn("_retriveElements, eid=%s, cid=%s.", elementId, curCachedItem[0])
                
                ndList.extend(curNdList)
                numNode += currSize
     
                #print"currNodeLen=" +str(numNode)
                #del curNdList [:]
                #del curCachedItem[:]
                
            '''

            #eof for    
            numNode += self.joinMultiSegments(elementId, ndList, cachedList)    
            cachedList = []
            self.Logger.warn("retriveElements, complete")
            
        #end if            

        #print ("totoalNode=%i"% (numNode))
        return numNode;
    
    
    def joinMultiSegments(self, elementId, baseNdList, cachedList):
        
        numNodeAdd = 0
        #move all closed ndList into baseNdList
        closedNdList = []
        closedNdListSize = 0
        inputLen = 0
        
        for curCachedItem in cachedList[:]:
            (cid, curNdList) = curCachedItem 
            curSize = len(curNdList)
            inputLen += curSize
            if (curSize == 0):
                cachedList.remove(curCachedItem)
                continue
            
            if (curNdList[0] == curNdList[curSize-1]):
                
                closedNdList.extend(curNdList)
                closedNdListSize +=curSize
                cachedList.remove(curCachedItem)
                continue
            

        #find other segment to join together       
        i=0
        segment = []
        remainListSize = len(cachedList)
        numNode = 0
        while (i < remainListSize):
            curCachedItem1 = cachedList[i]
            cid = curCachedItem1[0]
            segment = curCachedItem1[1]
            curSize = len(segment)
            
            cachedList.remove(curCachedItem1)
            remainListSize -= 1
            i = 0
            
            j = 0
            numNode = curSize
            startPoint = 0
            isFoundClose = False
            while (j < remainListSize):
                curCachedItem = cachedList[j]
                cid2 = curCachedItem[0]
                curNdList = curCachedItem[1]
                curLength = len(curNdList)
                    
                if (self.joinTwoSegments(segment, startPoint, curSize, curNdList, curLength)):
                    numNode += (curLength - 1)
                    #print "1:" + str(len(cachedList))
                    cachedList.remove(curCachedItem)
                    del curNdList[:]
                    del curCachedItem[:]
                    remainListSize -= 1
                    j = 0
                    if (segment[startPoint] == segment[(len(segment)-1)]):
                        isFoundClose = True
                        break
                else:
                    j += 1
                    #print i
            
                continue
            
            
            #end of while
            baseNdList.extend(segment)
            numNodeAdd +=numNode
            if (not isFoundClose):
                #segemtn is not closed
                self.Logger.warn("_retriveElements, eid=%s, cid=%s.", elementId, cid)
        
        #end of while
        baseNdList.extend(closedNdList)
        numNodeAdd +=closedNdListSize
        #print "inputLen=" +str(inputLen)
        #print "OutputLen=" +str(numNodeAdd)
        #print "ndList_len=" + str(len(baseNdList))
        return numNodeAdd
        
        
    # eof joinMultiSegments

        
    def joinTwoSegments(self, aList, aStartPoint, aLength, bList, bLength):
        retuCode = True
        
        if ((aLength == 0) or (bLength ==0)):
            return False
        
        if (aList[aLength-1] == bList[0]):#join a+b   
            aList.pop() 
            aList.extend(bList)
            return retuCode
    
    
        elif (aList[aStartPoint] == bList[bLength-1]): #join b+a
            bList.pop()
            self.insertIntoList(aList, aStartPoint, bList)
            return retuCode
            
        elif (aList[aLength-1] == bList[bLength-1]):#join a+reverse(b)
            bList.pop()
            bList.reverse()
            aList.extend(bList)
            return retuCode
        
        elif (aList[aStartPoint] == bList[0]):#join reverse(a) + b
            aList.reverse()
            aList.pop()
            aList.extend(bList)
            return retuCode
        
        return False
            
                    
    def _retriveNestedNodeElement(self, nodeId, role, level, rootId, reconType, ndList, isInsideOfTile):
        numNode  = 0

        
        nodeDict = self.writer.getElementFromCache(nodeId, self.writer.numCachedNode, self.writer.cachedNodes, self.writer.nodeFile, self.writer.numNodesInDisk, Const.NODE_FILE_NAME, reconType)
            
        if (not nodeDict):
            self.Logger.error("NO NODE FOUND, nodeId=%s", nodeId)
            return numNode;


        nodeAttrDict = nodeDict.get(u"attr", {})
        
        if nodeAttrDict:
            k = u"lat"
            v = nodeAttrDict[k]
            lat = float(v)
            k = u"lon"
            v = nodeAttrDict[k]
            lon = float(v)
            #if (nodeId == u"412714708"):
            #    print ("lon=%.8f, lat=%.8f" % (lon,lat))
            isInside = self.writer.IsInsideOfTileByValue(lat, lon)
            point = (lon, lat, isInside, role)
            ndList.append(point)
            numNode += 1
            if ((not isInsideOfTile[0]) and (isInside)):
                isInsideOfTile[0] = True
                
        return numNode
    
    
    def _retriveNestedWayElement(self, ref, role, level, rootId, reconType, ndList, isInsideOfTile):
        numNode = 0
        
        wayDict = self.writer.getElementFromCache(ref, self.writer.numCachedWay, self.writer.cachedWays, self.writer.wayFile, self.writer.numWaysInDisk, Const.WAY_FILE_NAME, reconType)
            
        if (not wayDict):
            self.Logger.debug( "NO WAY FOUND, wayId=%s", ref)
            return numNode;
        
                
        nodeList = wayDict.get(u"nd", [])
        for nodeId in  nodeList:
            numNode += self._retriveNestedNodeElement(str(nodeId), role, level+1, rootId, reconType, ndList, isInsideOfTile)

        return numNode

     
    def _retriveNestedRelationElement(self, refId, role, level, rootId, reconType, ndList, isInsideOfTile, isClosed):
        numNode = 0
        startPoint = 0
        
        relationDict = self.writer.getFullElementFromCache(refId, self.writer.numCachedRelation, self.writer.cachedRelations, self.writer.relationFile, self.writer.numRelationsInDisk, Const.RELATION_FILE_NAME, reconType)
            
        if (not relationDict):
            self.Logger.debug( "NO RELATION FOUND, relationId=%s", refId)
            return numNode;
        
        cachedList = []
        memberList = relationDict.get(u"member", [])
        for member in memberList:
            memberType = member[u"type"]
            ref = str(member[u"ref"])
            role = member[u"role"]
            self.Logger.debug("rootid=%s, current_id=%s, member_type=%s, ref=%s, role=%s" %(rootId, refId, memberType, ref, role))
                
            currNdList = []
            num = 0
            
            if (memberType == Const.TAG_NODE):
                numNode += self._retriveNestedNodeElement(ref, role, level+1, rootId, reconType, ndList, isInsideOfTile)
                continue
            elif (memberType == Const.TAG_WAY):
                num += self._retriveNestedWayElement(ref, role, level+1, rootId, reconType, currNdList, isInsideOfTile)
            elif (memberType == Const.TAG_RELATION):
                num += self._retriveNestedRelationElement(ref, role, level+1, rootId, reconType, currNdList, isInsideOfTile, isClosed)
            else:
                self.Logger.warn( "Unkown memberElement, rootid=%s, member_type=%s, ref=%s, role=%s" %(rootId, memberType, ref, role))
                
                
                
            currLeng = len(currNdList)
            if (currLeng == 0):
                continue
        
            cachedList.append([ref, currNdList])
            continue;
            
            '''
            length = len(ndList)
               
            
            if (length == startPoint):#start
                ndList.extend(currNdList)
                numNode += num
                if (ndList[startPoint] == ndList[(len(ndList)-1)]):
                    startPoint = len(ndList) #print "It is closed polygon."
                continue
  
  
            cachedList.append([ref, currNdList])
            remainListSize = len(cachedList)
            #if (remainListSize > 1):
            #    print remainListSize
                
            i = 0
            isJoin = False
            while (i < remainListSize):
                curCachedItem = cachedList[i]
                curNdList = curCachedItem[1]
                curLength = len(curNdList)
                if (self.joinTwoSegments(ndList, startPoint, len(ndList), curNdList, curLength)):
                    numNode += (curLength - 1)
                    #print "1:" + str(len(cachedList))
                    cachedList.remove(curCachedItem)
                    del curNdList[:]
                    del curCachedItem[:]
                    #print "2:" + str(len(cachedList))
                    remainListSize -= 1
                    i = 0
                    isJoin = True
                else:
                    i += 1
                    #print i
                continue
            
            if (not isJoin):
                self.Logger.warn("_retriveElements cached, rootid=%s, eid=%s, cid=%s.", rootId, refId, ref)
                
                
  
            if (ndList[startPoint] == ndList[(len(ndList)-1)]):
                startPoint= len(ndList) #print "It is closed polygon."
        #eof for
        
        #joint of cachedList
        for curCachedItem in cachedList:
            curNdList = curCachedItem[1]
            ndList.extend(curNdList)
            numNode += len(curNdList)
            self.Logger.warn("_retriveElements, rootid=%s, eid=%s, cid=%s.", rootId, refId, curCachedItem[0])
            #del curNdList [:]
            #del curCachedItem[:]
        '''
        #end while
        numNode += self.joinMultiSegments(refId, ndList, cachedList)
        
        self.Logger.warn("retriveElements, complete")
        cachedList = []
            
        return numNode
    

    def insertIntoList(self, baseList, position, insertList ):
        length = len(baseList)
        itInserted = False
        if (position < length):
            itInserted = True
        i = position
        for itm in insertList:
            if (itInserted):
                baseList.insert(i, itm)
                i +=1
            else:
                baseList.append(itm)
   
    #end of insertIntoList

    def testRetriveObjects(self, Logger):
    
        tileId = "55426548"
        inputXML = "./map_osm/o" + tileId + ".xml"
        outputXML = "./map_osm/" + tileId + ".xml"
        
        #osmParser.ParseAndWriteOneTile("../../projects_py/os/map_osm/o" + tileId + ".xml", tileId, "map_recon0/" + tileId + ".xml")
        dom = osmParser.OpenOsmXml(inputXML)
        if (not dom):
            Logger.warn("ParseAndWriteOneTile, dom=null")
            sys.exit(0)
        
        
        osmParser.ParseOsmByDom(dom)
        rimWriter.setBoundary(float(rimWriter.respBounds[Const.MINLAT]), float(rimWriter.respBounds[Const.MINLON]), float(rimWriter.respBounds[Const.MAXLAT]), float(rimWriter.respBounds[Const.MAXLON]))
         
        #rimWriter.reqBoundsInFloat = rimWriter.respBounds
        #rimWriter.setBoundary(rimminlat, minlon, maxlat, maxlon)
        #self.writer.setBoundary(min_lat, min_lon, max_lat, max_lon)
        
        '''
        ref = "83421090"
        reconType = "water"
        ElementData = rimWriter.getElementFromCache(ref, rimWriter.numCachedNode, rimWriter.cachedNodes, rimWriter.nodeFile, rimWriter.numNodesInDisk, Const.NODE_FILE_NAME, reconType)
        
        ref = "119104136"
        reconType = "highway-primary"
        ElementData = rimWriter.getElementFromCache(ref, rimWriter.numCachedWay, rimWriter.cachedWays, rimWriter.wayFile, rimWriter.numWaysInDisk, Const.WAY_FILE_NAME, reconType)
           
        '''   
        
        ref ="1298019"
        reconType = "highway-primary"
        ElementData = rimWriter.getElementFromCache(ref, rimWriter.numCachedRelation, rimWriter.cachedRelations, rimWriter.relationFile, rimWriter.numRelationsInDisk, Const.RELATION_FILE_NAME, reconType)
    
        
        ndList = []
    
        num = reconUtils._retriveElements(Const.TAG_RELATION, ElementData, reconType, ndList, False)
        print ndList
        Logger.error("NUM_Point=" + str(num))
        print ("NUM_Point=" + str(num))
    
    def isPolygon(self, polygon):
        if (len(polygon) < 3):
            return False
        #it is polygon if it is a horizontal line
        isSame = True
        value = polygon[0][0]
        for point in polygon:
            if (value <> point[0]):
                isSame = False
                break;
        if (isSame):
            return False
        isSame = True
        value = polygon[0][1]
        for point in polygon:
            if (value <> point[1]):
                isSame = False
                break;
        if (isSame):
            return False
        return True
    
    def addItemIntoDict(self, dicti, key, value):
        try:
            v = dicti[key]
        except:
            v = []
            dicti[key] = v 
        v.append(value)
        
    def getItemFromDict(self, dicti, key):
        try:
            v = dicti[key]
        except:
            v = []
        return v
    
    def emptyDict(self, dicti):
        dicti = {}
        
    def printItems(self, dicti):
        print dicti
            
    def testDict(self):
        dicti = {}
        self.addItemIntoDict(dicti, "1", "111")
        print dicti
        v = self.getItemFromDict(dicti, "2")
        print v
        
        self.addItemIntoDict(dicti, "2", "222")
        print dicti
        v = self.getItemFromDict(dicti, "2")
        print v
        
        self.addItemIntoDict(dicti, "3", "333")
        print dicti
        
        #self.emptyDict(dicti)
        dicti = {}
        print dicti
        
    def GetBoundaryByTileId(self,Id):

        latIndex = int(Id/100000 + self.PRECISION_ROUND)
        lngIndex = Id % 100000
        
        dLat = self.TILE_HEIGHT_IN_DEGREE
        latmin = (dLat * latIndex) - 90.0
        if (latmin == 90):
            latmin -= self.OVERLAP_IN_DEGREE
        elif (latmin == -90):
            latmin += self.OVERLAP_IN_DEGREE
            
        latmax = (latmin + dLat)
        
        dLng = self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR/ (math.cos((latmin) * math.pi /180.0))
        lngmin = (self.TILE_WIDTH_IN_DEGREES_AT_EQUATOR * lngIndex)/(math.cos((latmin * math.pi) /180.0))
        if (lngmin>180):
            lngmin -= 360
        lngmax = lngmin +dLng
           
        result = (("%.7f" % lngmin), ("%.7f" % latmin), ("%.7f" % lngmax), ("%.7f" % latmax))
        #result = (left, bottom, right, top)
        #result = (-122.9350, 49.1956, -122.5903, 49.2)
        print ("tielId=%i, bounds=%s" %(Id, result))
        
        return result

    

if __name__ == "__main__":
    print("================================START MAIN ========================================.")
    print("ReconUtils.main")
    
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

    #Initialize the objects
    print("Start ReconFetcher.")
    reconFetcher = ReconFetcher(-122.7843, 49.1848, Logger, True)
    print("Start Writer.")
    rimWriter = None
    #rimWriter = RimWriter(reconFetcher, Logger,Const.DEBUG)
    print("Start Parser.")
    osmParser = None
    #osmParser = OsmParser(reconFetcher, rimWriter, Logger, Const.DEBUG)
    #rimWriter.setParser(osmParser)
    reconUtils = ReconUtils(osmParser, rimWriter, Logger)
        
    #reconUtils.testRetriveObjects(Logger)
    reconUtils.testDict()
    
        
    
    
    print("================================END MAIN==========================================.")
    sys.exit(0)
    
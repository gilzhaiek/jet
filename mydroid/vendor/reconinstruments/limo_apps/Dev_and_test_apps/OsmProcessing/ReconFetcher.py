'''
Created on Nov 29, 2013
Create ReconFetcher (Recon Instruments Map) class to get and process open street map resrouce.
@author: simonwang
'''

import base64, xml.dom.minidom, time

import codecs
import math
from ReconOSMSource import OSMSource
import os
from ReconConst import Const
if(Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_REMOTE_SERVER):
    import httplib
from ReconCompressor import ReconCompressor
#from ReconHelper import ReconHelper
#from ReconProcessor import Const
#from ReconMemory import ReconMemory
import gc


class ApiError(Exception):
        
    def __init__(self, status, reason, payload):
        self.status  = status
        self.reason  = reason
        self.payload = payload

    
    def __str__(self):
        return "Request failed: " + str(self.status) + " - " + self.reason + " - " + self.payload
    
class ReconFetcher:
    def __init__(self, lat, lon, logger, debug):
        self.lat = lat
        self.lon = lon
        self.size = Const.FETCH_DEFAULT_SIZE
        self.Logger = logger
        self.debug = debug
        self.version = '0.2.19'
        self._api = Const.API
        self._debug = Const.DEBUG
        self._created_by = Const.CREATED_BY
        self.Parser = None
        self.Writer = None
        #self.reconHelper = ReconHelper(self.Logger)
        self.compressor = ReconCompressor(self.Logger)
        #self.osmApi = OsmApi.OsmApi(None ,None, None, "" , "PythonOsmApi/" + self.version, "www.openstreetmap.org", False, {}, 500, 1, debug)
        if (debug):
            self._debug = debug
        
        if (Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DB):
            self.source = OSMSource(self.Logger)
            
        #self.mem = ReconMemory(self.Logger)
            
    def __del__(self):
        return [];
    def setParser(self, parser):
        self.Parser = parser
        
    def setWriter(self, writer):
        self.Writer = writer
        
    def Capabilities(self):
        """ Returns ApiCapabilities. """
        uri = "/api/capabilities"
        data = self._get(uri)
        data = xml.dom.minidom.parseString(data)
        self.Logger.debug( data.getElementsByTagName("osm"))
        data = data.getElementsByTagName("osm")[0].getElementsByTagName("api")[0]
        result = {}
        for elem in data.childNodes:
            if elem.nodeType != elem.ELEMENT_NODE:
                continue
            result[elem.nodeName] = {}
            #self.Logger.debug( elem.nodeName)
            for k, v in elem.attributes.items():
                try:
                    result[elem.nodeName][k] = float(v)
                except:
                    result[elem.nodeName][k] = v
        self.Logger.debug( "Capablities=", result)
        return result
    
    def GetOneMap(self, min_lon, min_lat, max_lon, max_lat):
        self.Logger.debug('get one map:')
        #return self.osmApi.Map(min_lon, min_lat, max_lon, max_lat)
        return self._Map(min_lon, min_lat, max_lon, max_lat)
    
    def SaveOneRawTileToFileByHttp(self, min_lon, min_lat, max_lon, max_lat, outputfile):
        self.Logger.debug('save one map (left=%f,botton=%f,right=%f,top=%f) to file=%s'% (min_lon, min_lat, max_lon, max_lat, outputfile))
        uri = "/api/0.6/map?bbox=%f,%f,%f,%f"%(min_lon, min_lat, max_lon, max_lat)
        data = self._get(uri)
        of = codecs.open(outputfile,"w","utf-8")
        if data:
            of.write(data)
        of.flush()
        of.close()
        
    def ProcessOneTile(self, min_lon, min_lat, max_lon, max_lat, osmFile, reconId, reconFileName, isMostLeftTile, isSourceLocalFolder):
        self.Logger.warn('+++++Start a new Tile=%s, (left=%f,botton=%f,right=%f,top=%f) to file=%s, isMostLeftTile=%r, isSourceLocalFolder=%r'% (reconId, min_lon, min_lat, max_lon, max_lat, osmFile, isMostLeftTile, isSourceLocalFolder))
        if (not isSourceLocalFolder):
            if (Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_REMOTE_SERVER):
                uri = "/api/0.6/map?bbox=%f,%f,%f,%f"%(min_lon, min_lat, max_lon, max_lat)
                data = self._get(uri).decode("utf-8")
                of = codecs.open(osmFile,"w","utf-8")
                if data:
                    of.write(data)
                of.flush()
                of.close()
            elif (Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DB): #tiler is using local database.
                self.source.queryRawTile(min_lon, min_lat, max_lon, max_lat, osmFile, False)
            elif (Const.OSM_SOURCE_DEFAULT == Const.OSM_SOURCE_LOCAL_OSM_DUMP):
                self.Logger.debug( "OSM_SOURCE_LOCAL_OSM_DUMP")
            
           
        #process the raw tile.
        self.Writer.setBoundary(min_lat, min_lon, max_lat, max_lon)
        self.Writer.setMostLeftTile(isMostLeftTile)
        self.Parser.ParseAndWriteOneTile(osmFile, reconId, reconFileName, min_lat, min_lon, max_lat, max_lon) 
        if (self.Writer.exitTile):
            self.Logger.warn("ProcessOneTile, Skipping the tile=" + reconId)
            self.Parser.clearCacheDisk()
            #gc.collect()
            return
        
        srcFolder = os.path.dirname(reconFileName)
        #COMPRESS: rgz file
        self.compressor.compressTile(srcFolder,Const.RGZ_PATH)
        #UPLOAD:to s3
        self.compressor.whereTileSaveto(Const.RGZ_PATH,str(reconId)+".rgz")
        
        #DELETEtempFile:
        self.compressor.deleteTempFiles(osmFile, srcFolder, Const.RGZ_PATH)
        
        result =  os.system("echo " + str(reconId) +" >> generatedTile.txt")
        #result = self.reconHelper.launchNewProcess("echo " + str(reconId) +" >> generatedTile.txt")
        if (result):
            raise Exception("echo " + reconId +" >> generatedTile.txt shell runtime Exception !")
        
        self.Parser.clearCacheDisk()
        #gc.collect()
        #self.mem.printMemorySummary()
    
        
        
    def SaveBigMapToFiles(self, left, bottom, right, top, size, outputFileIndex):
        self.Logger.debug( "SaveBigMapToFiles(), (left=%f,bottom=%f,right=%f,top=%f, size=%f) to fileIndex=%s" % (left, bottom, right, top, size, outputFileIndex))
        if (left >right or bottom > top or outputFileIndex < 0):
            self.Logger.error( "SaveBigMapToFiles(), invalid inputs, (left=%f,bottom=%f,right=%f,top=%f) to fileIndex=%d" % (left, bottom, right, top, outputFileIndex))
            return;
        
        self.Logger.debug((right-left))
        self.Logger.debug((right-left)/size)
        self.Logger.debug((top-bottom))
        self.Logger.debug((top-bottom)/size)
        
        self.Logger.debug( "SaveBigMapToFiles(), the number of %d will be saved.", (math.ceil((right-left)/size) * math.ceil((top - bottom)/size)))

        l = 1
        
        current_min_lon = left
        current_min_lat = bottom
        current_max_lon = left
        current_max_lat = bottom
        
        outputfile = ""
        
        while (current_min_lat < top):
            current_min_lon = left
            while (current_min_lon < right):
                current_max_lon = current_min_lon + size
                if (current_max_lon > right):
                    current_max_lon = right
                    
                current_max_lat = current_min_lat + size
                if (current_max_lat > top):
                    current_max_lat = top
                outputfile = Const.OSM_MAPS_PATH + "osm%d.xml" %l
                self.Logger.debug("Get [%d] (left=%f,bottom=%f,right=%f,top=%f) into file=%s,"%(l, current_min_lon, current_min_lat, current_max_lon, current_max_lat, outputfile))
                self.SaveOneRawTileToFileByHttp(current_min_lon, current_min_lat, current_max_lon, current_max_lat, outputfile)
                reconfile = Const.OUTPUT_PATH + "%d.xml" %l
                self.Parser.ParseAndWriteOneTile(outputfile, str(l), reconfile)
                #self.Writer.writeIntoDB(l, current_min_lon, current_min_lat, current_max_lon, current_max_lat)
                l += 1
                current_min_lon = current_max_lon

            current_min_lat = current_max_lat
                        
        self.Logger.debug("Save Number of maps into file done.")


    
    def SaveNumMapsToFile(self, lon, lat, height, max_num_map):
        self.Logger.debug( "Save Number of maps into file")
        adjust = 0.000001
        sqrt = math.sqrt(max_num_map)
        x_num = math.ceil(sqrt)
        x_num_half = x_num/2.0
        min_lon = (lon - (((math.floor(x_num_half)   ) * height) + 0.5 * height))
        max_lon = (lon + (((math.ceil(x_num_half ) -1) * height) + 0.5 * height)) - adjust
        min_lat = (lat - (((math.floor(x_num_half)   ) * height) + 0.5 * height))
        max_lat = (lat + (((math.ceil(x_num_half ) -1) * height) + 0.5 * height)) - adjust
        
        if (min_lon < -180):
            min_lon = -180
        if (max_lon > 180):
            max_lon = 180
        if (min_lat < -90):
            min_lat = -90
        if (max_lat > 90):
            max_lat = 90

        i = 1
        current_min_lon = min_lon
        current_min_lat = min_lat
        
        while (current_min_lat < max_lat):
            while (current_min_lon < max_lon):
                current_max_lon = current_min_lon + height
                current_max_lat = current_min_lat + height
                outputfile = Const.OSM_MAPS_PATH + "ReconFetcher%d.xml"%i
                self.Logger.debug("Get [%i] OSMap=%s,"%(i, outputfile))
                self.SaveOneRawTileToFileByHttp(current_min_lon, current_min_lat, current_max_lon, current_max_lat, outputfile)
                i += 1
                current_min_lon = current_max_lon
            current_min_lon = min_lon
            current_min_lat = current_max_lat            
        self.Logger.debug("Save Number of maps into file done.")
        
    def NodeGet(self, NodeId, NodeVersion = -1):
        """ Returns NodeData for node #NodeId. """
        uri = "/api/0.6/node/"+str(NodeId)
        if NodeVersion != -1: uri += "/"+str(NodeVersion)
        data = self._get(uri)
        #if not data: return data
        #data = xml.dom.minidom.parseString(data)
        #data = data.getElementsByTagName("osm")[0].getElementsByTagName("node")[0]
        #return self._DomParseNode(data)
        return data
    
    def WayFullGet(self, WayId):
        if (not WayId):
            return
        uri = "/api/0.6/way/"+str(WayId)+"/full"
        data = self._get(uri)
        return data
    
    def RelationFullGet(self, RelationId):
        """ Return full data for relation RelationId as list of {type: node|way|relation, data: {}}. """
        uri = "/api/0.6/relation/"+str(RelationId)+"/full"
        data = self._get(uri)
        return data
    
    
    def _changesetautoflush(self, force = False):
        while (len(self._changesetautodata) >= self._changesetautosize) or (force and self._changesetautodata):
            if self._changesetautocpt == 0:
                self.ChangesetCreate(self._changesetautotags)
            self.ChangesetUpload(self._changesetautodata[:self._changesetautosize])
            self._changesetautodata = self._changesetautodata[self._changesetautosize:]
            self._changesetautocpt += 1
            if self._changesetautocpt == self._changesetautomulti:
                self.ChangesetClose()
                self._changesetautocpt = 0
        if self._changesetautocpt and force:
            self.ChangesetClose()
            self._changesetautocpt = 0
        return None
        
    def HttpRequest(self, cmd, path, auth, send):
        if self._debug:
            path2 = path
            if len(path2) > 250:
                path2 = path2[:250]+"[...]"
            self.Logger.debug("1-%s %s %s"%(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2))
        self._conn.putrequest(cmd, path)
        self._conn.putheader('User-Agent', self._created_by)
        if auth:
            self._conn.putheader('Authorization', 'Basic ' + base64.encodestring(self._username + ':' + self._password).strip())
        if send != None:
            self._conn.putheader('Content-Length', len(send))
        self._conn.endheaders()
        if send:
            self._conn.send(send)
        response = self._conn.getresponse()
        if response.status != 200:
            payload = response.read().strip()
            if response.status == 410:
                return None
            raise ApiError(response.status, response.reason, payload)
        if self._debug:
            self.Logger.debug("2-%s %s %s done"%(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2))
        result = response.read()
        self._conn.close()
        return result
    
    def _http(self, cmd, path, auth, send):
        i = 0
        while True:
            i += 1
            try:
                return self.HttpRequest(cmd, path, auth, send)
                '''
                except ApiError:
                if ApiError.status >= 500:
                    if i == 5: raise
                    if i != 1: time.sleep(5)
                    self._conn = httplib.HTTPConnection(self._api, 80)
                else: raise'''
            except Exception:
                if i == 5: raise
                if i != 1: time.sleep(5)
                self._conn = httplib.HTTPConnection(self._api, 80)
    
    def _get(self, path):
        return self._http('GET', path, False, None)

    def _put(self, path, data):
        return self._http('PUT', path, True, data)
    
    def _delete(self, path, data):
        return self._http('DELETE', path, True, data)
    
    def _Map(self, min_lon, min_lat, max_lon, max_lat):
        """ Download data in bounding box. Returns list of dict {type: node|way|relation, data: {}}. """
        uri = "/api/0.6/map?bbox=%f,%f,%f,%f"%(min_lon, min_lat, max_lon, max_lat)
        data = self._get(uri)
        print (data)
        return self._ParseOsm(data)
    
    def _ParseOsm(self, data):
        """ Parse osm data. Returns list of dict {type: node|way|relation, data: {}}. """
        data = xml.dom.minidom.parseString(data)
        data = data.getElementsByTagName("osm")[0]
        result = []
        for elem in data.childNodes:
            if elem.nodeName == "node":
                result.append({"type": elem.nodeName, "data": self._DomParseNode(elem)})
            elif elem.nodeName == "way":
                result.append({"type": elem.nodeName, "data": self._DomParseWay(elem)})                        
            elif elem.nodeName == "relation":
                result.append({"type": elem.nodeName, "data": self._DomParseRelation(elem)})
        return result  
    
    def _DomParseNode(self, DomElement):
        """ Returns NodeData for the node. """
        result = self._DomGetAttributes(DomElement)
        result["tag"] = self._DomGetTag(DomElement)
        return result

    def _DomParseWay(self, DomElement):
        """ Returns WayData for the way. """
        result = self._DomGetAttributes(DomElement)
        result["tag"] = self._DomGetTag(DomElement)
        result["nd"]  = self._DomGetNd(DomElement)        
        return result
    
    def _DomParseRelation(self, DomElement):
        """ Returns RelationData for the relation. """
        result = self._DomGetAttributes(DomElement)
        result["tag"]    = self._DomGetTag(DomElement)
        result["member"] = self._DomGetMember(DomElement)
        return result
    def _DomGetAttributes(self, DomElement):
        """ Returns a formated dictionnary of attributes of a DomElement. """
        result = {}
        for k, v in DomElement.attributes.items():
            if k == "uid"         : v = int(v)
            elif k == "changeset" : v = int(v)
            elif k == "version"   : v = int(v)
            elif k == "id"        : v = int(v)
            elif k == "lat"       : v = float(v)
            elif k == "lon"       : v = float(v)
            elif k == "open"      : v = v=="true"
            elif k == "visible"   : v = v=="true"
            elif k == "ref"       : v = int(v)
            result[k] = v
        return result            
        
    def _DomGetTag(self, DomElement):
        """ Returns the dictionnary of tags of a DomElement. """
        result = {}
        for t in DomElement.getElementsByTagName("tag"):
            k = t.attributes["k"].value
            v = t.attributes["v"].value
            result[k] = v
        return result

    def _DomGetNd(self, DomElement):
        """ Returns the list of nodes of a DomElement. """
        result = []
        for t in DomElement.getElementsByTagName("nd"):
            result.append(int(int(t.attributes["ref"].value)))
        return result            

    def _DomGetMember(self, DomElement):
        """ Returns a list of relation members. """
        result = []
        for m in DomElement.getElementsByTagName("member"):
            result.append(self._DomGetAttributes(m))
        return result
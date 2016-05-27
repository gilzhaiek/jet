#import logging
from ReconConst import Const
import os
import subprocess
import time
        
#from subprocess import call
import logging.handlers
from xml.dom.minidom import parse
#from ReconHelper import ReconHelper
        
class OSMSource:
    OSMOSIS_CMD_PATH = "osmosis/bin/osmosis"
    #DUMP_OSM_DATA_PATH = "osmosis/osmdump/planet-140317.osm"
    DUMP_OSM_DATA_PATH = "Downloads/changesets-140327.osm"
    DB_HOST = "localhost"
    DB_NAME = "osm"
    DB_USER = "postgres"
    DB_PWD = "postgres"
    IS_COMPLETE_RELATIONs = "yes"
    
    def __init__(self, logger=None):
        self.Logger = logger
        self.HOME_PATH = os.getenv("HOME")
        if (Const.IS_HOST_VM):
            self.HOME_PATH = "/home/simon/"
        #self.HOME_PATH = "/home/simon/"
        self.baseCmd = self.HOME_PATH + "/" + self.OSMOSIS_CMD_PATH + " --read-pgsql "
        self.baseCmd += "host=\"" + self.DB_HOST + "\" "
        self.baseCmd += "database=\"" + self.DB_NAME + "\" "
        self.baseCmd += "user=\"" + self.DB_USER + "\" "
        self.baseCmd += "password=\"" + self.DB_PWD + "\" "
        self.baseCmd += "validateSchemaVersion=\"yes\" allowIncorrectSchemaVersion=\"no\" "
        
     
        
    def queryRawTile(self, left, bottom, right, top, osmFile, isCompleteRelations):
        self.Logger.debug("queryRawTile")
        '''./osmosis --read-pgsql host="localhost" database="bc" user="postgres" password="postgres" validateSchemaVersion="yes" 
           allowIncorrectSchemaVersion="no" --dbb top=49.275 left=-122.982794 bottom=49.25 right=-122.944559 completeWays=yes 
           --buffer --write-xml file="/home/simonwang/osmosis/bin/o43767748.osm"
           ./osmosis --read-pgsql host="localhost" database="bc" user="postgres" password="postgres" validateSchemaVersion="yes" 
           allowIncorrectSchemaVersion="no" --dbb top=49.275 left=-122.982794 bottom=49.25 right=-122.944559 completeWays=yes 
            --write-xml file="/home/simonwang/osmosis/bin/o43767748.osm"
        '''
        
        cmd = self.baseCmd
        cmd += "--dataset-bounding-box "
        cmd += "left=" + str(left) + " right=" + str(right) + " bottom=" + str(bottom) + " top=" +str(top)
        cmd += " completeWays="
        
        if (isCompleteRelations):
            cmd += "yes "
        else:
            cmd += "no "
        cmd + "--buffer "
        cmd += "--write-xml file=" + osmFile
               
        #print cmd
         
        #startTimeStr = time.strftime("%c")
        start_time = time.time()
        #Logger.warn("---------Start Process MAP with boundary of " + boundry[0] +" at time %s--------" + time.strftime("%c"))
        #print("---------Start Process MAP at time %s--------" + time.strftime("%c"))
        
        result = os.system(cmd) #15 seconds
        #result = self.reconHelper.launchNewProcess(cmd)
        if (result):
            raise Exception("Query postgres db Exception!")
        
        #output = os.popen(cmd) #16seconds:
        '''
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True) #,stderr=subprocess.PIPE) 15seconds
        output, errors = p.communicate()
        try:
            p.stdout.close()
            p.kill()
        except OSError:
            print "exception kill Popen"
            # can't kill a dead proc
            pass
        if (errors):
            print errors
        '''
        
        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---End query bbox=%s, elapsed_time=%d secs.", osmFile, elapsed_time)
        
    def queryRawTileRelatives(self, left, bottom, right, top, osmFile):
        self.Logger.debug("queryRawTile")
        '''./osmosis --read-pgsql host="localhost" database="bc" user="postgres" password="postgres" validateSchemaVersion="yes" 
           allowIncorrectSchemaVersion="no" --dbb2 top=49.275 left=-122.982794 bottom=49.25 right=-122.944559 completeWays=yes 
            --write-xml file="/home/simonwang/osmosis/bin/o43767748.osm"
        '''
        
        cmd = self.baseCmd
        cmd += "--dataset-bounding-box2 "
        cmd += "left=" + str(left) + " right=" + str(right) + " bottom=" + str(bottom) + " top=" +str(top)
        cmd += " completeWays=yes "
        cmd + "--buffer "
        cmd += "--write-xml file=" + osmFile
               
        start_time = time.time()
        
        result = os.system(cmd) #15 seconds
        #result = self.reconHelper.launchNewProcess(cmd)
        
        if (result):
            #retry if sth wrong.
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True) #,stderr=subprocess.PIPE) 15seconds
            output, errors = p.communicate()
            try:
                p.stdout.close()
            except OSError:
                print "exception kill Popen"
                p.kill()
                pass
            if (errors):
                print errors
                print output
                self.Logger.error("queryRawTileRelatives() (from db) failed with cmd="+ cmd)
                raise Exception("Query postgres DB queryRawTileRelatives runtime Exception !")
            

        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---queryRawTileRelatives,end query bbox=%s, elapsed_time=%d secs.", osmFile, elapsed_time)
        
    def queryNodeById(self, nid, osmFile):
        self.Logger.debug("queryNodeById="+ nid)
        '''
        ./osmosis --read-pgsql host="localhost" database="osm" user="postgres" password="postgres" validateSchemaVersion="yes" allowIncorrectSchemaVersion="no" 
        --dataset-node nid=55418561 --buffer --write-xml file="node.xml"
        '''
        
        cmd = self.baseCmd
        cmd += "--dataset-node "
        cmd += "nid=" + str(nid)
         
        cmd + "--buffer "
        cmd += " --write-xml file=" + osmFile
        
        self.Logger.debug(cmd)
        print cmd
         
        #startTimeStr = time.strftime("%c")
        start_time = time.time()
        self.Logger.warn("---Start to query node id=%s at time %s ---", nid, time.strftime("%c"))
        
        output = os.system(cmd) #15 seconds
        #output = self.reconHelper.launchNewProcess(cmd)
        #output = os.popen(cmd) #16seconds:
        #print output
        if (output):
            self.Logger.error("queryNodeById() (from db) failed with NodeById="+ nid)
            raise Exception("Query postgres db queryNodeById shell runtime Exception !")
        
        
        elapsed_time = (time.time() - start_time)
        self.Logger.warn("---End query node id=%s, elapsed_time=%d secs.---", nid, elapsed_time)
                
        
    def queryWayById(self, nid, osmFile, isCompleteWays=True):
        self.Logger.debug("queryWayById=" + nid)
        '''
        ./osmosis --read-pgsql host="localhost" database="osm" user="postgres" password="postgres" validateSchemaVersion="yes" allowIncorrectSchemaVersion="no" 
        --dataset-way nid=207814 --buffer --write-xml file="relation.xml"
        '''
        
        cmd = self.baseCmd
        cmd += "--dataset-way "
        cmd += "nid=" + str(nid)
        
        '''
        if (isCompleteWays):
            cmd += " completeWays=yes "
        '''
        cmd + "--buffer "
        cmd += " --write-xml file=" + osmFile
        
        self.Logger.debug(cmd)
        print cmd
         
        #startTimeStr = time.strftime("%c")
        start_time = time.time()
        #self.Logger.warn("---Start query way id=%s at time %s---", nid, time.strftime("%c"))
        
        output = os.system(cmd) #15 seconds
        #output = os.popen(cmd) #16seconds:
        #output = self.reconHelper.launchNewProcess(cmd)
        
        if (output):
            #os.remove(osmFile)
            self.Logger.error("queryWayById() (from db) failed with WayById="+ nid)
            raise Exception("Query postgres db queryWayById shell runtime Exception !")
        
        elapsed_time = (time.time() - start_time)
        self.Logger.warn("---End query way id=%s, elapsed_time=%d secs.---", nid, elapsed_time)
        
        
    def queryRelationById(self, nid, osmFile, isCompleteWays=True, isCompleteRelations=True):
        self.Logger.debug("queryRelationById=" + nid)
        '''
        ./osmosis --read-pgsql host="localhost" database="osm" user="postgres" password="postgres" validateSchemaVersion="yes" allowIncorrectSchemaVersion="no" 
        --dataset-relation nid=207814 --buffer --write-xml file="relation.xml"
        '''
        
        cmd = self.baseCmd
        cmd += "--dataset-relation "
        cmd += "nid=" + str(nid)
        
        '''
        if (isCompleteRelations):
            cmd += " completeRelations=yes"
        elif (isCompleteWays):
            cmd += " completeWays=yes "
        '''
        cmd + "--buffer "
        cmd += " --write-xml file=" + osmFile
        
        self.Logger.debug(cmd)
        print cmd
         
        start_time = time.time()
        #self.Logger.warn("---Start query relation id=%s at time %s--------", nid, time.strftime("%c"))
        
        output = os.system(cmd) #15 seconds
        #output = os.popen(cmd) #16seconds:
        #output = self.reconHelper.launchNewProcess(cmd)
        
        if (output):
            #retry if sth wrong.
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True) #,stderr=subprocess.PIPE) 15seconds
            output, errors = p.communicate()
            try:
                p.stdout.close()
            except OSError:
                print "exception kill Popen"
                p.kill()
                pass
            if (errors):
                print errors
                print output
                self.Logger.error("queryRelationById() (from db) failed with RelationById="+ nid)
                raise Exception("Query postgres db queryRelationById shell runtime Exception !")
        
        elapsed_time = (time.time() - start_time)
        self.Logger.warn("---End query relation id=%s, elapsed_time=%d secs.", nid, elapsed_time)
        
        
         
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
    
    
    source = OSMSource(Logger)
    
    
    '''Test QueryRawTile'''
    '''
    reconId = 43767748
    top=49.275
    left=-122.982794
    bottom=49.25
    right=-122.944559
    osmFile = Const.OSM_MAPS_PATH +"o%s_1.xml" %reconId
    source.queryRawTile(left, bottom, right, top, osmFile, True)
    '''
    
    '''Test QureyRelationById'''
    '''
    nid = '207814'
    osmFile = Const.OSM_MAPS_PATH +"o_relation_%s.xml" %nid
    source.queryRelationById(nid, osmFile)
    '''
    
    '''Test QureyWayById'''
    '''
    nid = '5256967'
    osmFile = Const.OSM_MAPS_PATH +"o_way_%s.xml" %nid
    source.queryWayById(nid, osmFile)
    '''
    
    '''Test QureyNodeById'''
    nid = '55418561'
    osmFile = Const.OSM_MAPS_PATH +"o_node_%s.xml" %nid
    source.queryNodeById(nid, osmFile)
    dom = parse(osmFile)
    print dom
    
    print "done."
    
   

    
    

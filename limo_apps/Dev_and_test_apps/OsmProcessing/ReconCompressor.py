#import logging
from ReconConst import Const
import os
import httplib
from urlparse import urlparse
import subprocess
#from subprocess import call
#from ReconHelper import ReconHelper


import time        
import logging.handlers
        
class ReconCompressor:
    PATH_TO_COMPRESSOR_JAR="java -jar " + os.getenv("HOME")  + "/projects_workspace/tileCompressor_limo/tileCompressor_bin/tileCompressor.jar"
    if Const.IS_HOST_VM:
        PATH_TO_COMPRESSOR_JAR="java -jar /home/simon/projects_workspace/tileCompressor_limo/tileCompressor_bin/tileCompressor.jar"
    
    PATH_TO_S3_PUT_CMD=os.getenv("HOME")  + "/programs/s3cmd-1.5.0/s3cmd put --acl-public "
    if Const.IS_HOST_VM:
        PATH_TO_S3_PUT_CMD="/home/simon/programs/s3cmd-1.5.0/s3cmd put --acl-public "
    
    PATH_TO_S3_GET_CMD=os.getenv("HOME")  + "/programs/s3cmd-1.5.0/s3cmd --force get "
    if Const.IS_HOST_VM:
        PATH_TO_S3_GET_CMD="/home/simon/programs/s3cmd-1.5.0/s3cmd get "
    
    PATH_TO_S3_BUCKET="s3://geotilebucket/1/base/"
    

    def __init__(self, logger=None):
        self.Logger = logger
        #self.reconHelper = reconHelper
        
        if (not os.path.isdir(Const.RGZ_PATH)):
            os.mkdir(Const.RGZ_PATH)

        if((Const.WHERE_KEEP_TILE & Const.TILE_DOWNLOAD_MODE == Const.TILE_DOWNLOAD_MODE) or (Const.IS_DOWNLOAD_TILE_FROM_S3_TO_LOCAL_WHEN_EXIST)):
            if (not os.path.isdir(Const.RECON_RGZ_PATH)):
                os.mkdir(Const.RECON_RGZ_PATH)
   
        if (not Const.IS_DEL_TEMP_RECON_XML_FILE):
            if (not os.path.isdir(Const.RECON_XML_PATH)):
                os.mkdir(Const.RECON_XML_PATH)
        
        
    def compressTile(self, srcFolder, dstFolder):
        self.Logger.debug("compressTile")
        
        if (srcFolder==None):
            self.Logger.debug("srcFolder==None")
            #exit(1)
            raise Exception("compressTile srcFolder not existed !")                
                
        if (dstFolder==None):
            self.Logger.debug("dstFolder==None")
            #exit(2)
            raise Exception("compressTile dstFolder not existed !") 
            
            
        cmd = self.PATH_TO_COMPRESSOR_JAR + " "+srcFolder+" "+dstFolder


               
        start_time = time.time()
        
        #result = self.reconHelper.launchNewProcess(cmd)
        result=os.system(cmd)
        if (result): #Something has not worked
            print cmd
            errmsg = "compressTile shell runtime Exception !, errno=" + str(result)
            print errmsg
            
            #retry 
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True) #,stderr=subprocess.PIPE) 15seconds
            output, errors = p.communicate()
            try:
                p.stdout.close()
            except OSError:
                p.kill()
                print "exception kill Popen"
                pass
                if (errors):
                    print errors
                    print output
                    raise Exception(errmsg)
        
        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---End query bbox=%s, elapsed_time=%d secs.", srcFolder, elapsed_time)
        return result
        
    
    def checkTileExisted(self,tilegroup,tileid):
        if ( (Const.IS_WHERE_CHECK_EXIST & Const.NOT_CHECK_TILE_EXIST) == Const.NOT_CHECK_TILE_EXIST ):
            return False
        elif ( (Const.IS_WHERE_CHECK_EXIST & Const.CHECK_TILE_EXIST_LOCAL) == Const.CHECK_TILE_EXIST_LOCAL):
            if (self.checkGeneratedFile(tileid)):
                return True
            fileFullName = Const.RECON_RGZ_PATH + tileid +".rgz"
            retu = os.path.exists(fileFullName)
            if (retu):
                return True;
                
        if ( (Const.IS_WHERE_CHECK_EXIST & Const.CHECK_TILE_EXIST_IN_S3 ) == Const.CHECK_TILE_EXIST_IN_S3 ):
            #url = "https://s3.amazonaws.com/geotilebucket/1/"+tileid+".rgz"
            url = "https://s3.amazonaws.com/geotilebucket/1/"+tilegroup + tileid+".rgz"
            time.sleep(0.1)
            result =  self.checkUrl(url)
            if (result and Const.IS_DOWNLOAD_TILE_FROM_S3_TO_LOCAL_WHEN_EXIST):
                self.downloadFileFromS3(tileid+".rgz")
            return result;
                            

        return False
    
    
    def checkGeneratedFile(self, tileid):
        try:
            datafile = file('generatedTile.txt')
            tileIdStr = str(tileid) 
            for line in datafile:
                if tileIdStr in line:
                    return True
        except Exception:
            return False
        return False

    def checkUrl(self, url):
        p = urlparse(url)
        conn = httplib.HTTPConnection(p.netloc)
        conn.request('HEAD', p.path)
        resp = conn.getresponse()
        result = resp.status < 400
        conn.close()
        return result

    def deleteTempFiles(self, osmFile, reconFileFolder,rgzFileFolder):
        if (Const.IS_DEL_TEMP_OSM_FILE):
            result = os.system("rm -rf "+osmFile)
            #result = self.reconHelper.launchNewProcess("rm -rf "+osmFile)
            if (result):
                raise Exception("deleteTempFiles osmFile Exception !") 
    
        #if (Const.IS_DEL_TEMP_RECON_XML_FILE):
        result = os.system("rm -rf "+reconFileFolder+"/*")
        #result = self.reconHelper.launchNewProcess("rm -rf "+reconFileFolder+"/*")
        if (result):
            raise Exception("deleteTempFiles srcFolder Exception !") 

        #if (Const.IS_DEL_TEM_RECON_RGZ_FILE):
        result = os.system("rm -rf "+rgzFileFolder+"/*")
        #result = self.reconHelper.launchNewProcess("rm -rf "+rgzFileFolder+"/*")
        if (result):
            raise Exception("deleteTempFiles outputFolder Exception !") 
        

        
        
        
        
    def whereTileSaveto(self, path, fileName):
        if (path==None):
            self.Logger.debug("path==None")
            raise Exception( "whereTileSaveto.path=null")           
                
        if (fileName==None):
            self.Logger.debug("fileName==None")
            raise Exception( "whereTileSaveto.fileName=null") 
            
        if (Const.WHERE_KEEP_TILE & Const.TILE_UPLOAD_S3_MODE == Const.TILE_UPLOAD_S3_MODE):
            print "s3 upload mode"
            self.uploadFileToS3(path, fileName)
        
        if (Const.WHERE_KEEP_TILE & Const.TILE_DOWNLOAD_MODE == Const.TILE_DOWNLOAD_MODE):
            print "down mode"
            self.moveFileToLocalFoler(path, fileName, Const.RECON_RGZ_PATH)
            
        if (not Const.IS_DEL_TEMP_RECON_XML_FILE):
            xmlFileName = fileName.replace("rgz", "xml")
            self.moveFileToLocalFoler(Const.OUTPUT_PATH, xmlFileName, Const.RECON_XML_PATH)
                
            
    def uploadFileToS3(self, path, fileName):
        self.Logger.debug("uploadToS3")

        fromFile = path +"/" +fileName
        toFile = self.PATH_TO_S3_BUCKET +fileName
        cmd = self.PATH_TO_S3_PUT_CMD + " "+fromFile+" "+toFile
        print(cmd)


        start_time = time.time()
        
        result=os.system(cmd)
        #result = self.reconHelper.launchNewProcess(cmd)
        if (result != 0): #Something has not worked
            #exit(3)
            raise Exception("uploadFileToS3 shell Exception !")
            
        
        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---upload tile file=%s, elapsed_time=%d secs.", fileName, elapsed_time)
        return result
    
    def downloadFileFromS3(self, fileName):
        print fileName
        #self.Logger.debug("downloadFileFromS3, file=" + fileName)

        fromFile = self.PATH_TO_S3_BUCKET +fileName
        toFile = Const.RECON_RGZ_PATH +fileName
        cmd = self.PATH_TO_S3_GET_CMD + " "+fromFile+" "+toFile
        print(cmd)


        start_time = time.time()
        
        result=os.system(cmd)
        #result = self.reconHelper.launchNewProcess(cmd)
        if (result != 0): #Something has not worked
            raise Exception("downloadFileFromS3 shell Exception !")
            
        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---download tile file=%s, elapsed_time=%d secs.", fileName, elapsed_time)
        return result
        
    def moveFileToLocalFoler(self, path, fileName, dstFolder):
        print "moveFileToLocalFoler, file=" + path + fileName + ", to Dst=" + dstFolder
        
        fromFile = path +fileName
        toFile = dstFolder +fileName
        cmd = "mv "+fromFile+" "+toFile
        print(cmd)
        start_time = time.time()
        
        result=os.system(cmd)
        #result = self.reconHelper.launchNewProcess(cmd)
        if (result != 0): #Something has not worked
            #exit(3)
            raise Exception("moveFileToLocalFoler shell Exception !")
            
        
        elapsed_time = (time.time() - start_time)
        #Logger.warn("----------End Process MAP Boundary: start=%s, end=%s, elapsed_time=%.2f hours.", startTimeStr, time.strftime("%c"), elapsed_time)
        self.Logger.warn("---moveFileToLocalFoler file=%s, elapsed_time=%d secs.", fileName, elapsed_time)
        return result
                
         
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
    
    

    
    compressor = ReconCompressor(Logger)
    
#     srcfolder = "/home/simonwang/projects_workspace/tileCompressor_limo/tileCompressor_bin/test"
#     dstfolder = srcfolder + "/../output"
#     result=compressor.compressTile(srcfolder,dstfolder)
#     if (result == 0):
#         print "sucess"
#     else:
#         print "failed"
    
#     path="/home/simonwang/programs/s3cmd-1.5.0/"
#     fileName="test.rgz"
#     result = compressor.uploadFileToS3(path, fileName)
#     if (result == 0):
#         print "sucess"
#     else:
#         print "failed"


    if (Const.WHERE_KEEP_TILE & Const.TILE_UPLOAD_S3_MODE  == Const.TILE_UPLOAD_S3_MODE):
        print "s3 mode"
    else:
        print "not s3 mode"
    
    if (Const.WHERE_KEEP_TILE & Const.TILE_DOWNLOAD_MODE  == Const.TILE_DOWNLOAD_MODE):
        print "download mode"
    else:
        print "not download mode"


            
    print "done."
    
   

    
    


from ReconTileFetcherByName import ReconTileFetcherByName

import datetime
import os.path
import shutil
import time
import sys

class ReconTileSeacher():
    
    SRC_ROOT = "/media/passport/os/map_recon_Canada/map_recon3/"
    DST_ROOT = "/media/passport/os/"
    
    DEFAULT_CITY_NAME = "Whistler, BC, Canada"
    cityName = ""
       
    def SearchTiles(self):
        argv = sys.argv
        if (argv and len(argv) > 1):
            self.cityName = argv[1]
        else:
            self.cityName = self.DEFAULT_CITY_NAME

        
        self.SearchTilesByName(self.cityName)
        
    def SearchTilesByName(self, name):
        
        
        fetcher = ReconTileFetcherByName()
        tileIdList = fetcher.GetTileIdListByCityName(name)
        if (not tileIdList or len(tileIdList) < 1):
            print "Not file to copy."
            return;
        
        print tileIdList
        tileIdList.sort();
        print tileIdList
        
        newTileList = []
        newFolderList = []
        for tileId in tileIdList:
            if (not tileId):
                continue
            newTileList.append([str(tileId), False])
            folder = tileId[:4]
            if (folder not in newFolderList):
                newFolderList.append(folder)


        now = datetime.datetime.now().strftime("%Y-%m-%d_%I_%M")
        dstbase = os.path.join(self.DST_ROOT, now)
        print dstbase
        if (not os.path.isdir(dstbase)):
            os.mkdir(dstbase)

        numCopied = 0

        startTimeStr = time.strftime("%c")
        start_time = time.time()
        print("---------Start to search tile files at time %s--------" + time.strftime("%c"))
        
        
        for path, subdirs, files in os.walk(self.SRC_ROOT):
            '''
            print "============="
            print "path="+ path
            print subdirs
            print files
            '''
            
            currFolderName=os.path.split(path)
            currFolder = currFolderName[1]
            print "search folder =" + currFolder
            if (currFolder not in newFolderList):
                continue;
            
            isAllCopied = True
            for tile in newTileList:
                [tileId1, isCopied1] = tile
                if (isCopied1):
                    continue
                fileName =tileId1 +".xml"
                src = os.path.join(path, fileName)
                if (os.path.isfile(src)):
                    dst = os.path.join(dstbase, fileName)
                    print "copying src=" + src + " to dst=" +dst
                    shutil.copy2(src, dst)
                    tile[1] = True #set isCopied1= True
                    numCopied += 1
                    
                else:
                    isAllCopied = False
                    
            #for end
            
            if (isAllCopied):
                print "Done, copied all files."
                break;
                    
                
        #for

        for tileId2, isCopied2 in newTileList:
            if (isCopied2):
                continue
            print "Not found file tileId=" + tileId
            


        elapsed_time = (time.time() - start_time)/(60.00)
        print "Searched the city=" + self.cityName
        print(("----------End to search tile files: the_number_copied_files=%i, start=%s, end=%s, elapsed_time=%.2f minues.") % (numCopied, startTimeStr, time.strftime("%c"), elapsed_time))
        
                    
                    
#print("----------------------------------------Main------------------------------------------------")    
if __name__ == "__main__":
    
    seacher = ReconTileSeacher()
        
    seacher.SearchTiles()
    #seacher.LocateFile("55995655.xml", seacher.SRC_ROOT)

#print("----------------------------------------Main------------------------------------------------")    


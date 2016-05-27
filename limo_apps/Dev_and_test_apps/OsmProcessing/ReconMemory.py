from pympler import muppy
from pympler import summary

#import logging
import logging.handlers
import os.path
import types
from pympler import tracker
        
class ReconMemory:

    
    def __init__(self, logger=None):
        self.Logger = logger
        memoryFileStr = "memory.txt"
        if (os.path.exists(memoryFileStr)):
            os.remove(memoryFileStr)
        self.memoryFile = open(memoryFileStr, "w+")
        self.sumPrev = None
        self.tr = tracker.SummaryTracker()
        
    def __close__(self):
        self.memoryFile.close();
        self.memoryFile = None
        self.Logger = None
        self.sumPrev = None
        
                    
    def write(self, out):
        print "MEMORY: " + out;
        self.memoryFile.write(out)
        self.Logger.warn("MEMORY:" + out)
        
    def printMemorySummary(self):
        all_objects = muppy.get_objects()
        out = "Total_NumObjects=" + str(len(all_objects)) + "\n"
        self.write(out)
        
        #filter out certain types of objects
        out =   None
        types1 = muppy.filter(all_objects, Type= types.ClassType)
        out = "Num_Type=" + str(len(types1)) +"\n"
        for t in types1:
            out += str(t)
            out += "\n"
            
        self.write(out)
        out = None

        #comppare summery of memory
#         sumCurr = summary.summarize(all_objects)
#         if (self.sumPrev):
#             diff = summary.get_diff(sumCurr, self.sumPrev)
#             summary.print_(diff)
#             #self.write(str(diff))
#         self.sumPrev = sumCurr
#         

        self.tr.print_diff() 

        print "memory.summary.done"
        
        
            
        
        
                        
         
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
    
    

    
    memory = ReconMemory(Logger)
    memory.printMemorySummary()
    memory.printMemorySummary()
    


            
    print "done."
    
   

    
    

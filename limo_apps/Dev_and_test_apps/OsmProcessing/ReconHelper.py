# from ReconConst import Const
import subprocess
#import os



class ReconHelper:
    
    def __init__(self, logger):
        self.Logger = logger

    
    def __del__(self):
        print("ReconHelper.__del__()")
        
    def launchNewProcess(self, cmd):
        #result=os.system(cmd)
        #result = subprocess.call(cmd, shell=True)
        #print "launchNewProcess, return_code" + str(result)
        result = 1
        if (result):
            print "launchNewProcess, retry"
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)  # ,stderr=subprocess.PIPE) 15seconds
            output, errors = p.communicate()
            try:
                p.stdout.close()
                #p.kill()
                result = 0
            except OSError:
                print "exception kill Popen"
                p.kill()
                pass
            if (errors):
                print errors
                print output
                if (self.Logger):
                    self.Logger.error("launchNewProcess() failed running cmd=" + cmd)
                raise Exception("launchNewProcess() runtime Excepton, failed running cmd=" + cmd)
        return result;
        
            
    

if __name__ == "__main__":
    print("================================START MAIN ========================================.")
    print("ReconHelper.main()")
    
    reconHelper = ReconHelper(None)
    
    cmd = "cat logs/recon.log"
    reconHelper.launchNewProcesscd(cmd)
    
    
        
    
    
    print("================================END MAIN==========================================.")

    

from geocoder import Geocoder
#from recon_tiles import ReconTiles
from ReconGlobalSweep import GlobalSweep

import sys

class TileNameFetcher():
    
    DISPLAY_NAME = 'display_name'
    BOUNDINGBOX = 'boundingbox'
    ADDRESS = 'address'
    CITY_NAME = 'Vancouver, BC, Canada'
    
    def geocode_city(self, city_state_country):
        client = Geocoder("http://open.mapquestapi.com/nominatim/v1/search?format=json")
        response = client.geocode(city_state_country)
        
        if (not response):
            print ("no response for city=%s" % (city_state_country))
            return None
        
        
        #print (response)
        #print response[0][self.DISPLAY_NAME]
        #print response[0][self.BOUNDINGBOX]
        #print response[0][self.ADDRESS]
        
        '''return boundary of city in format [bottom, top, left, right]'''
        return response[0][self.BOUNDINGBOX]



#print("----------------------------------------Main------------------------------------------------")    
if __name__ == "__main__":
    
    fetcher = TileNameFetcher()
    globalSweep = GlobalSweep(None, None)
    
    '''Get city name from passing paramater'''
    argv = sys.argv
    if (argv and len(argv) > 1):
        #print("city=%s" % argv[1])
        city = argv[1]
    else:
        city = fetcher.CITY_NAME
    
    
    '''Get Boundary of city'''
    bound = fetcher.geocode_city(city)
    print bound
    if (not bound):
        exit(0)
    
    '''Get Tile name List'''
    globalSweep.GetTileListByBound(float(bound[2]), float(bound[0]), float(bound[3]), float(bound[1]))
#print("----------------------------------------Main------------------------------------------------")    


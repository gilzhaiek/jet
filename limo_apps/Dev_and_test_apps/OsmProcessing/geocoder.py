import urllib
import urllib2
import simplejson

class GeocoderError(Exception): pass
class GeocoderResultError(Exception): pass

class Geocoder(object):
    def __init__(self, base_url = "http://open.mapquestapi.com/nominatim/v1/search?format=json"):
        self.base_url = base_url + "&%s"

    def geocode(self, q, addressdetails=False, limit=None,
                 countrycodes='', viewbox=(), exclude_place_ids=[],
                 bounded=False, routewidth=None, osm_type='', osm_id=None):

        params = { 'q': q }
        params['addressdetails'] = addressdetails

        if limit is not None:
            params['limit'] = limit

        if countrycodes:
            params['countrycodes'] = countrycodes

        if routewidth:
            params['routewidth'] = routewidth

        if osm_type:
            params['osm_type'] = osm_type

        if osm_id:
            params['osm_id'] = osm_id


        url = self.base_url % urllib.urlencode(params)
        data = urllib2.urlopen(url)
        response = data.read()
        
        return self.parse_json(response)


    def parse_json(self, data):
        try:
            data = simplejson.loads(data)
        except simplejson.JSONDecodeError:
            data = []
        
        return data


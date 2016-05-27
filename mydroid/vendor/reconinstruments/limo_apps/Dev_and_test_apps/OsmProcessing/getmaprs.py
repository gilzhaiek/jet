'''
Created on Nov 28, 2013

@author: simonwang
'''

from xml.dom.minidom import parse
from urllib2 import Request, urlopen, URLError, HTTPError
import urllib
import re
import sys
import codecs
#import OsmApi



def read_xml_from_url(xmlurl):
    try:
        zdom = parse(urllib.urlopen(xmlurl))
        return zdom
    except:
        return []
    return zdom

def read_html_from_url(htmlurl):
    thing = urllib.urlopen(htmlurl).read()
    return thing


def get_latlong_from_html_source(hs):
    latlong = ','
    try:
        latlong = (re.search("LatLng\((.*?)\);",hs)).group(1)
        latlong = re.sub(" ","",latlong)
    except:
        pass
    return latlong

def get_country_from_html_source(hs):
    country = u''

    try:
        country = re.findall("Regions(.*?)>(.*?)<",hs)[2][1]
    except:
        pass

    return country
    
def downloadOneResource(baseurl):
    baseurl="http://api.openstreetmap.org/api/0.6/map?"
    values = {'bbox' : "-123.4238,49.095,-122.7846,49.4137"}
    try:
        data = urllib.urlencode(values)
        req = Request(baseurl, data)
        response = urlopen(req)
        page = response.read()
    except HTTPError as e:
        print 'The server couldn\'t fulfill the request.'
        print 'Error code: ', e.code
        return []
    except URLError as e:
        if hasattr(e, 'reason'):
            print 'failed to reach to server ', baseurl
            print 'Reason: ', e.reason
            return []
        #elif hasattr(e, 'code'):
        #    print 'The server couldn\'t fulfill the request.'
        #    print 'Error Code: ', e.code
    else:
        #everything is fine
        print 'response=', response, "="
        print 'page=', page
        return page;
    

def getOneResource(baseurl):
    xmlurl = baseurl+".xml"
    zdom = read_xml_from_url(xmlurl)
    hs = read_html_from_url(baseurl)

    # initialize values to effective null
    area_id = u''
    resort_name = u''
    region = u''
    region_id = u''
    website = u''
    operatingStatus = u''

    if zdom:
        try:
            area_id = zdom.getElementsByTagName("skiArea")[0].getAttribute("id")
        except:
            pass

        try:
            resort_name = zdom.getElementsByTagName("name")[0].childNodes[0].data
        except:
            pass

        try:
            region = zdom.getElementsByTagName("region")[0].childNodes[0].data
        except:
            pass

        try:
            region_id = zdom.getElementsByTagName("region")[0].getAttribute("id")
        except:
            pass

        try:
            website = zdom.getElementsByTagName("officialWebsite")[0].childNodes[0].data
        except:
            pass

        try:
            operatingStatus = zdom.getElementsByTagName("operatingStatus")[0].childNodes[0].data
        except:
            pass

        latlong = get_latlong_from_html_source(hs)
        country = get_country_from_html_source(hs)

        if (country == 'Canada' or country == 'United States'):
            return area_id+u',"'+resort_name+u'",'+region_id+u',"'+country+"/"+region+u'",'+latlong+u',"'+operatingStatus+u'","'+website+u'"\n'
        else:
            return area_id+u',"'+resort_name+u'",'+region_id+u',"'+country+u'",'+latlong+u',"'+operatingStatus+u'","'+website+u'"\n'
    else:                       # no resort
        return ''
    



def getResources(base_base_url,startnumber, endnumber,outputfile):
    of = codecs.open(outputfile,"w","utf-8")
    #for i in range(startnumber,endnumber+1):
    baseurl = base_base_url
    #of.write(getOneResource(baseurl))
    resp = downloadOneResource(baseurl)
    if resp !="":
        of.write(resp)

        
argv = sys.argv
print "getmaprs started with ", len(argv), "paramaters=", argv

#url="http://www.openstreetmap.org/export#map=17/49.23735/-123.09083"
url="http://api.openstreetmap.org/api/0.6/map?bbox=-123.4238,49.095,-122.7846,49.4137"

if (len(argv) < 3):
    print "Usage: python get_resource_map.py http://www.skimap.org/SkiAreas/view/ <start: usually 1> <end:usually 5000> <outputfile>"
else:
    getResources (url ,int(argv[1]),int(argv[2]),argv[3])
   
print "getmaprs end."


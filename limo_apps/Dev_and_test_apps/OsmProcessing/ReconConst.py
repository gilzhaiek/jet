import logging

class Const:
    
    ENCODE = "utf-8"
    OSM_MAPS_PATH = u"map_osm/"
    OUTPUT_PATH = u"map_recon/"
    #OSM_MAPS_PATH = u"/media/passport/map_osm/"
    #OUTPUT_PATH = u"/media/passport/map_recon/"
    RGZ_PATH = u"map_rgz/"
    RECON_RGZ_PATH = u"map_reconrgz/"
    RECON_XML_PATH = u"map_reconxml/"
    LOG_PATH = u"logs/"
    LOG_FILE_NAME = u"recon"
    PICS_FILES_PATH = u"pics/"
    LOG_TO_CONSOLE = True
    #LOG_DEFAULT_LEVEL = logging.DEBUG
    #LOG_DEFAULT_LEVEL = logging.INFO
    LOG_DEFAULT_LEVEL = logging.WARN
    LOG_FILE_MAX_BYTES = 5*1024*1024
    LOG_BACKUP_COUNT = 10
    
    TAG_NODE = u"node"
    TAG_WAY = u"way"
    TAG_RELATION = u"relation"
    TAG_BOUNDS = u"bounds"
    TAG_RMO = u"rmo"
    TAG_AREA = u"area"
    TAG_LINE = u"line"
    TAG_POINT = u"point"
    TAG_ND = u"nd"
    TAG_ACTION_POINT_LIST = u"actNdList"
    TAG_ACTION_POINT_EXCEPTION_LIST = u"actNdExceptionList"
    IS_BUILD_FROM_POINT_LIST = u"isBuildFromPointList"
    TAG_ROLE = u"role"
    DISK_CACHE_PATH = u"tmp/"
    NODE_FILE_NAME = DISK_CACHE_PATH + u"tempNodes"
    WAY_FILE_NAME = DISK_CACHE_PATH + u"tempWays"
    RELATION_FILE_NAME = DISK_CACHE_PATH + u"tempRelations"
    RECON_MAP_DB_PATH = "db/"
    RECON_MAP_DB = RECON_MAP_DB_PATH + u"recondb.db"
    PROCESSING_SIZE = (1024)
    CONFIG_NODE = "NODE"
    CONFIG_WAY = "WAY"
    CONFIG_RELATION = "RELATION"
    CONFIG_NODE_DISP = 'NODE_DISP_TAGS'
    CONFIG_WAY_DISP = 'WAY_DISP_TAGS'
    CONFIG_RELATION_DISP = 'RELATION_DISP_TAGS'
    
    INPUT_GEO_REGION_FILE = "InputGeoRegion.txt"
    
    SKIP_TILES_FILE = "InputGeoRegionsSkipedTile.txt"
    PROCESSED_BBX_FILE = "ProcessedBBX.txt"
    TILE_IDS_FILE = "TileIDsList.txt"
    
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegion.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegions.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsFromMD.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsCities.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsEuropeanCities.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsUsaCities.txt"
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsCities1.txt"
    INPUT_GEO_REGIONS_FILE = "InputGeoRegionsBetaCities.txt"
    #INPUT_GEO_REGIONS_FILE = SKIP_TILES_FILE
    #INPUT_GEO_REGIONS_FILE = "InputGeoRegionsFromMDWithoutExtension.txt"
    

    IS_DISK_SWITCH = False
    MAX_NUM_CACHED_ELEMENT_SIZE = 1 
    MAX_NUM_CACHED_WAY_SIZE =  1
    KEY_IS_IN_OUTPUT = u"isInterested"
    KEY_RECON_TYPE = u"reconType"
    KEY_ID = u"id"
    ACTION_OCEAN_GEO_REGION = u"oceanGeo"
    ACTION_RIVER_BANK = u"riverbank"
    ACTION_WOOD = u"wood"
    ACTION_LAKE = u"water"
    ACTION_PARK = u"park"
    ACTION_WETLAND = u"wetland"
    ACTION_GRASSLAND = u"green"
    ACTION_LIST_FOR_POLYGON_OBJECTS = [ACTION_WOOD, ACTION_LAKE, ACTION_PARK, ACTION_WETLAND]
    ACTION_LIST_FOR_PRE_PROCESS = [ACTION_OCEAN_GEO_REGION, ACTION_RIVER_BANK]
    ACTION_LIST_FOR_PRE_PROCESS += ACTION_LIST_FOR_POLYGON_OBJECTS
    ACTION_NONE = u"none"
    
    RECON_TYPE_OCEAN = u"ocean"
    RECON_TYPE_LAND = u"land"
    RECON_TYPE_NATIONAL_BORDER = "nationalborder"
    RECON_TYPE_WOOD = "wood"
    RECON_TYPE_PARK = "park"
    RECON_TYPE_WATER = "water"
    RECON_TYPE_WETLAND = "wetland"
    RECON_TYPE_WATERWAY = "waterway"
    #RECON_TYPE_PRE_PROCESS_LIST = [RECON_TYPE_OCEAN, RECON_TYPE_WOOD, RECON_TYPE_PARK, RECON_TYPE_WATER, RECON_TYPE_WETLAND]

    ROLE_MAIN_STREAM = "main_stream"
    ROLE_SIDE_STREAM = "side_stream"
    ROLE_TRIBUTARY = "tributary"
    
    IS_CALCULATE_OCEAN_GEO_REGION = True
    FETCH_ONE_IF_NOT_EXISTS = True
    DEBUG = True
    API = "www.openstreetmap.org"
    FETCH_DEFAULT_SIZE = 1
    OSM_VERSION = "0.2.19"
    CREATED_BY = "PythonOsmApi/" + OSM_VERSION
    KM_TO_DEGREE =  0.0009009
    #MAP_BOX_LENGTH_IN_DEGREE = 5*KM_TO_DEGREE
    THRESHOLD_PARTCAL_RELATION = 30
    
    KM_PER_ONE_LAT_DEGREE = 110.54
    KM_CONST_PER_ONE_LNG_DEGREE = 111.320 #KM__PER_LNG_DEGREE=111.320*(math.cos(lat))
    NORTH_HEMISPERE_ONLY = True
    VERFICATION_MODE_OFF = True
    IS_DEL_TEMP_OSM_FILE = True
    IS_DEL_TEM_RECON_RGZ_FILE = True
    IS_DEL_TEMP_RECON_XML_FILE = True
    
    ROLE_INNER = u"inner"
    ROLE_OUTER = u"outer"
    
    ATTR_ROLE_OUTER = 0
    ATTR_ROLE_INNER = 1 
    
    MINLAT = u'minlat'
    MAXLAT = u'maxlat'
    MINLON = u"minlon"
    MAXLON = u"maxlon"
    
    IS_MOST_LEFT_TILE = "isMostLeftTile"
    
    TYPE_UNKNOWN = 0
    TYPE_OCEAN = 1
    TYPE_LAND = 2
    TYPE_FOREST = 3
    TYPE_INVALID =4
    POLYGON_TYPE_LIST = ["unknown", "ocean", "land", "forest", "invalid"]
    
    #TILE_RIGHT_TYPE = POLYGON_TYPE_LIST[TYPE_LAND]
    #TILE_RIGHT_TYPE = POLYGON_TYPE_LIST[TYPE_OCEAN]
    TILE_RIGHT_TYPE = POLYGON_TYPE_LIST[TYPE_UNKNOWN]
    TILE_TOP_TYPE = TILE_RIGHT_TYPE
    
    OSM_SOURCE_REMOTE_SERVER = 1
    OSM_SOURCE_LOCAL_OSM_DUMP = 2
    OSM_SOURCE_LOCAL_OSM_DB = 3
    
    #OSM_SOURCE_DEFAULT = OSM_SOURCE_REMOTE_SERVER
    #OSM_SOURCE_DEFAULT = OSM_SOURCE_LOCAL_OSM_DUMP
    OSM_SOURCE_DEFAULT = OSM_SOURCE_LOCAL_OSM_DB
    
    IS_HOST_VM = False
    
    ADD_RELATIVE_OSM_OBJECTS_ON = True
    #ADD_RELATIVE_OSM_OBJECTS_ON = False
    isAddRelativeOsmObjectOn = False #not change
    
    MAX_SIZE_XML_FILES_PROCESSED = (37000000) #37 MB for virtual machine
    #MAX_SIZE_RELATIVE_FILE = (3145728) #3MB
    MAX_SIZE_RELATIVE_FILE = (22971520) #20MB
    #MAX_SIZE_RELATIVE_FILE =  (10485760) #10mb
    
    
    #IS_RESUME_FROM_LAST_END = True  #True- it will not re-generate tiles alread had; False - re-generate all tiles 
    NOT_CHECK_TILE_EXIST = 1<<0   #0- it will re-generate tiles alread had; False - re-generate all tiles 
    CHECK_TILE_EXIST_IN_S3 = 1<<1
    CHECK_TILE_EXIST_LOCAL = 1<<2
    IS_WHERE_CHECK_EXIST = NOT_CHECK_TILE_EXIST
    #IS_WHERE_CHECK_EXIST = CHECK_TILE_EXIST_IN_S3
    #IS_WHERE_CHECK_EXIST = CHECK_TILE_EXIST_LOCAL
    #IS_WHERE_CHECK_EXIST = ( CHECK_TILE_EXIST_LOCAL | CHECK_TILE_EXIST_IN_S3 )

    #IS_DOWNLOAD_TILE_FROM_S3_TO_LOCAL_WHEN_EXIST =  False
    IS_DOWNLOAD_TILE_FROM_S3_TO_LOCAL_WHEN_EXIST =  True
    
    TILE_UPLOAD_S3_MODE = 1<<0
    TILE_DOWNLOAD_MODE  = 1<<1
    #WHERE_KEEP_TILE = (TILE_UPLOAD_S3_MODE | TILE_DOWNLOAD_MODE)
    WHERE_KEEP_TILE = (TILE_UPLOAD_S3_MODE)
    #WHERE_KEEP_TILE = (TILE_DOWNLOAD_MODE)
    


    IS_DRAW = False   #True - draw the riverbank and ocean inside tile; False - not Draw to speed up.

    
    

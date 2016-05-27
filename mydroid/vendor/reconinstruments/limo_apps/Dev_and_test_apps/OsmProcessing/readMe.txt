
The OSM (Open Street Map) processing project, is that fetch Open Street Map, and covert into Recon Map format. The project is wrote by Python script language. The reason selected Python language, this is because:


There are the diversity of tools readily available and freedom of choice.

Open Source Roots: Python is rooted in an open source solution. This means that the support for it through the community is massive. There are tonnes of resources in the form of books, web pages, and articles about how to accomplish almost any coding problem, and because it had it’s roots in open source there is a huge library of third party applications already developed, and more being developed everyday. This makes Python extensible to an almost infinite degree. This also means there is a pre-existing library of code, so if you want to make python perform a specific action, you can find it, and integrate into own program.

Object Oriented: Much like powerhouses C, C++, and others, Python has very strong object oriented features. With Python’s language, the syntax is clear and straight forward, so it is effective in implementing an environment based on object oriented programming methods.

Development: Due in part to it’s popularity, and in part to it’s continuous development, Python is one of the most mature languages available. It allows a programmer to rapidly develop and prototype a program, and Python has cross platform portability so a programmer can code once and deploy to multiple environments. This is a huge advantage of Python as a programming language. Python is also available on Windows, Linux, and Mac OS X, so almost any user can install and use it.

Modules : Finally, one of the great advantages of Python is that you can save any work you do as a module. You can then reuse that module in another program, or make it freely available for other programmers. Vice Vera, because it is such a popular program, finding modules, or getting custom programming done is easy.

The cycle of writing, compiling, testing, recompiling is extremely slow and can make even the most patient programmer using the C and Java languages tear out their hair. Python is powerful enough to handle any task. Any programmer who wants to quit the grind of standard application development would find a useful companion in Python.







The Recon Map Processing project includes below several modules:

 1. Recon Main modules:
 2. Recon Configuration Module
 3. Recon OSM Map fetcher Module
 4. Recon OSM XML parser Module
 5. Recon Map writer Module
 6. Recon Global Sweeper Module




 1. Recon Main modules:
     In this module, it sets up logging environment, including console log interface and file log interface. For the file logging, we set over-writable logging file, it could has maximum 10 files, and maximum size of each file is 512 MB, all of them are configurable.
     
      In this module, it creates and initializes all objects need. 

      It also kicks off the process of processing Map data. It can go to either global processing model or region processing model.
      
      The region processing model is process a Geo region. which chops a Geo Region map into small Geo Regions, for example, it is 5KM Width and 5KM height Map Box. And it processes each of Small Geo Regions.
      
      The global processing model is processing the global map, which chops the global map into small Geo Regions, for example, it is 5KM Width and 5KM height Map Box. And it processes each of small Geo Regions.
           
      
      
 

 2. Recon Configuration Module

     In this module, it configures the mapping from OSM type (key-value pair) into Recon type. The OSM type is being to identify what the object of Map is; Recon type is to identify the Recon Map Object. This Mapping is Many to one mapping.

   It also configures the map object filters, what kind of OSM map objects will go to Recon Map objects.

   It also configures the attribute filters, what kind of attributes of OSM map objects will go to Recon Map objects.

   The reason we made the map object filters and map attribute filters are configurable, because we frequently change the  map objects and attributes of map, we might add more feature/capability of the map. We made it dynamic and configurable, once they are being changed, we don't need change the code.

 


 3. Recon OSM Map fetcher Module

   In this module, it uses RESTful protocol to talk with OSM server to fetch the OSM maps. 

   This can request a Geo region map, or request single Map Object, point/way/relation. 
   
   This sections depends on the capability of OSM Server provided. 
   
   
   


 4. Recon OSM XML parser Module

  In this module, it parses the OSM map format into python objects. 

Because OSM is using reference mode, it means that one Map object can refer another Map object, it keeps only the ID info to reduce the data redundancy. In Recon Map format, we don't use reference mode, we want to Map-app can quickly render the Map View and reduce the logic and processing time in the Map-app, Recon Map object includes all information need to render a map, it doesn't need  any calculation, all this project will care about it.

   

 5. Recon Map writer Module


In this module, it writes python objects into Recon Map format (XML file). The big challenge was that when it writes to Recon Map format, the referenced Map objects were not available, it need call Recon Fetcher class to fetch the referenced Map objects over the air. The referenced Map objects can be recursive, it will more time/memory to process those recursive.



 6. Recon Global Sweeper Module

  In this module, it splits global Map or region Map into Map tiles.

  It provides APIs for get Map Tile ID by GPS coordinate. 

  
  




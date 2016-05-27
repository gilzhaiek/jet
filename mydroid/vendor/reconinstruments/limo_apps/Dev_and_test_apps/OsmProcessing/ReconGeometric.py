# intersect.py
# Demonstrate how Shapely can be used to analyze and plot the intersection of
# a trajectory and regions in space.

from ReconConst import Const
from math import acos, sqrt
if Const.IS_DRAW : 
    import matplotlib.pyplot as plt


'''
from functools import partial
import random
from shapely.geometry import LineString, Point, MultiLineString
from shapely.geometry.polygon import Polygon
from shapely.ops import cascaded_union
import sys
import numpy as np

'''


class ReconGeometric:

    IGNORE_VALUE = 0.000001
    
    def __init__(self, logger):
        self.Logger = logger

        
            
    def __del__(self):
        return [];
    
        
    def drawTile(self, route, bounds, regionPoints, fileName=None):
        
        #draw boundary
        shift = 0.0
        plt.figure()
        x0 = [bounds[Const.MINLON], bounds[Const.MINLON], bounds[Const.MAXLON], bounds[Const.MAXLON], bounds[Const.MINLON]]
        y0 = [bounds[Const.MINLAT], bounds[Const.MAXLAT],  bounds[Const.MAXLAT], bounds[Const.MINLAT], bounds[Const.MINLAT]]
        polygon = plt.plot(x0, y0, 'g')
        plt.setp(polygon, linewidth=1.0)
        
        #draw route
        size = len(route)
        i = 0
        x1 = []
        y1 = []
        while (i < size):
            x1.append(route[i][0])
            y1.append(route[i][1])
            i += 1
            
        lines1 = plt.plot(x1, y1, 'bo')
        plt.setp(lines1, linewidth=2)
        lines2 = plt.plot(x1, y1, 'b')
        plt.setp(lines2, linewidth=1)
        
        lines3 = plt.plot([route[0][0]], [route[0][1]], 'ro')
        plt.setp(lines3, linewidth=1)
        plt.text(route[0][0] - shift, route[0][1], r'start')
        
        
        #draw geo region
        if (regionPoints):
            size = len(regionPoints)
            i = 0
            x2 = []
            y2 = []
            while (i < size):
                x2.append(regionPoints[i][0])
                y2.append(regionPoints[i][1])
                i += 1
                
            lines = plt.plot(x2, y2, 'r')
            plt.setp(lines,  linewidth=2.0)
        
        
        #plt.axis([40, 160, 0, 0.03])
        if (fileName):
            plt.savefig(Const.PICS_FILES_PATH + fileName + ".png")
            plt.clf()
        else:
            plt.show()

        
    def drawTileAndMutilPolygon(self, route, bounds, polygonList, fileName=None, multiColor=False):
        
        #draw boundary
        shift = 0.0
        plt.figure()
        x0 = [bounds[Const.MINLON], bounds[Const.MINLON], bounds[Const.MAXLON], bounds[Const.MAXLON], bounds[Const.MINLON]]
        y0 = [bounds[Const.MINLAT], bounds[Const.MAXLAT],  bounds[Const.MAXLAT], bounds[Const.MINLAT], bounds[Const.MINLAT]]
        polygon = plt.plot(x0, y0, 'g')
        plt.setp(polygon, linewidth=1.0)
        
        #draw route
        if (route):
            size = len(route)
            if (size > 1 ):
                i = 0
                x1 = []
                y1 = []
                while (i < size):
                    x1.append(route[i][0])
                    y1.append(route[i][1])
                    i += 1
                    
                lines1 = plt.plot(x1, y1, 'bo')
                plt.setp(lines1, linewidth=2)
                lines2 = plt.plot(x1, y1, 'b')
                plt.setp(lines2, linewidth=1)
                
                lines3 = plt.plot([route[0][0]], [route[0][1]], 'bo')
                plt.setp(lines3, linewidth=1)
                plt.text(route[0][0] - shift, route[0][1], r'start')
        
                    
        for polygonRegion in polygonList:
            if (polygonRegion):
                size = len(polygonRegion)
                if (size < 3):
                    continue
                i = 0
                x2 = []
                y2 = []
                while (i < size):
                    x2.append(polygonRegion[i][0])
                    y2.append(polygonRegion[i][1])
                    i += 1
                
                if (multiColor):
                    if i%5 ==0:
                        lines = plt.plot(x2, y2, 'yellow')
                    elif i%5 ==1:
                        lines = plt.plot(x2, y2, 'r')
                    elif i%5 ==2:
                        lines = plt.plot(x2, y2, 'b')
                    elif i%5 ==3:
                        lines = plt.plot(x2, y2, 'g')
                    else:
                        lines = plt.plot(x2, y2, 'black')
                    
                    lable = str(i)
                    plt.text(polygonRegion[0][0] - shift, polygonRegion[0][1], lable)
                
                else:
                    lines = plt.plot(x2, y2, 'r')
                plt.setp(lines,  linewidth=2.0)
        
        
        
        #plt.axis([40, 160, 0, 0.03])
        if (fileName):
            plt.savefig(Const.PICS_FILES_PATH + fileName + ".png")
            plt.clf()
        else:
            plt.show()
    
    def drawTileAndSegmentList(self, bounds, segmentList, fileName=u'1'):
        
        #draw boundary
        shift = 0.0
        plt.figure()
        x0 = [bounds[Const.MINLON], bounds[Const.MINLON], bounds[Const.MAXLON], bounds[Const.MAXLON], bounds[Const.MINLON]]
        y0 = [bounds[Const.MINLAT], bounds[Const.MAXLAT],  bounds[Const.MAXLAT], bounds[Const.MINLAT], bounds[Const.MINLAT]]
        polygon = plt.plot(x0, y0, 'g')
        plt.setp(polygon, linewidth=1.0)

        k = 0
        for action in segmentList:
            if (action):
                pointList = action[1]
                k += 1
                size = len(pointList)
                if (size == 0):
                    continue
                #print "idx=", k, "id=", action[0], "points=", pointList
                i = 0
                x2 = []
                y2 = []
                while (i < size):
                    x2.append(pointList[i][0])
                    y2.append(pointList[i][1])
                    i += 1
                if k%5 ==0:
                    lines = plt.plot(x2, y2, 'yellow')
                elif k%5 ==1:
                    lines = plt.plot(x2, y2, 'r')
                elif k%5 ==2:
                    lines = plt.plot(x2, y2, 'b')
                elif k%5 ==3:
                    lines = plt.plot(x2, y2, 'g')
                elif k%5 ==4:
                    lines = plt.plot(x2, y2, 'black')
                    
                plt.setp(lines,  linewidth=2.0)
                lable = str(k)
                plt.text(pointList[0][0] - shift, pointList[0][1], lable)
        
        
        
        #plt.axis([40, 160, 0, 0.03])
        if (fileName):
            plt.savefig(Const.PICS_FILES_PATH + fileName + ".png")
            plt.clf()
        else:
            plt.show()
            
    def drawTileAndNodeList(self, bounds, ndList, fileName=u'1'):
        
        #draw boundary
        shift = 0.0
        plt.figure()
        x0 = [bounds[Const.MINLON], bounds[Const.MINLON], bounds[Const.MAXLON], bounds[Const.MAXLON], bounds[Const.MINLON]]
        y0 = [bounds[Const.MINLAT], bounds[Const.MAXLAT],  bounds[Const.MAXLAT], bounds[Const.MINLAT], bounds[Const.MINLAT]]
        polygon = plt.plot(x0, y0, 'g')
        plt.setp(polygon, linewidth=1.0)

        size = len(ndList)
        if (size < 2):
            return
        
        i = 0
        x2 = []
        y2 = []
        while (i < size):
            x2.append(ndList[i][0])
            y2.append(ndList[i][1])
            i += 1
        lines = plt.plot(x2, y2, 'b')
            
        plt.setp(lines,  linewidth=2.0)
        lable = '1'
        plt.text(ndList[0][0] - shift, ndList[0][1], lable)
        
        #plt.axis([40, 160, 0, 0.03])
        if (fileName):
            plt.savefig(Const.PICS_FILES_PATH + fileName + ".png")
            plt.clf()
        else:
            plt.show()
            
            
    def getEdgeNumber(self, point, bounds):
        if (point[1] == bounds[Const.MINLAT]):
            return 0
        elif (point[0] == bounds[Const.MINLON]):
            return 1
        elif (point[1] == bounds[Const.MAXLAT]):
            return 2
        elif (point[0] == bounds[Const.MAXLON]):
            return 3
        return -1
          
    def isGreater(self, point1, point2):
        if (point1[0] == point2[0]):
            if (point1[1] > point2[1]):
                return True
            else:
                return False
        elif point1[1] == point2[1]:
            if (point1[0] > point2[0]):
                return True
            else:
                return False
        else:
            if (point1[0]+point1[1] > point2[0]+point2[1]):
                return True
            else:
                return False
        
            
    def getTileEdgeByPoints(self, start, stop, bounds):
        
        if (start == None or stop == None or bounds == None or start == stop):
            return []
        p = [(bounds[Const.MINLON], bounds[Const.MINLAT]), (bounds[Const.MINLON], bounds[Const.MAXLAT]), (bounds[Const.MAXLON], bounds[Const.MAXLAT]), (bounds[Const.MAXLON], bounds[Const.MINLAT])]
        #print str(p)
        #TODO CHECK THE NULL POINT
        startEdge = self.getEdgeNumber(start, bounds)
        stopEdge = self.getEdgeNumber(stop, bounds)
        #print "num_startEdge=", startEdge, "num_stopEdge=", stopEdge
        
        retuList = [start,]
        i = startEdge
        if (startEdge == stopEdge):
            isStartGreatStop = self.isGreater(start, stop)
            isLeftOrTop = self.isLeftOrTop(start, bounds)
            if ((isStartGreatStop and isLeftOrTop) or ((not isStartGreatStop) and (not isLeftOrTop))):
                retuList.append(p[i])
                i = (i + 1) % 4
            else:
                retuList.append(stop)
                return retuList

        while (i != stopEdge):
            retuList.append(p[i])
            i = (i + 1) % 4
            #print ("point=%d.", i)
        
        retuList.append(stop)
        
        return retuList
            

    def isInside (self, point, bounds):
        if (point[0] < bounds[Const.MINLON]):
            return False
        elif (point[0] > bounds[Const.MAXLON]):
            return False
        elif (point[1] < bounds[Const.MINLAT]):
            return False
        elif (point[1] > bounds[Const.MAXLAT]):
            return False
        else:
            return True

    def isLeftOrTop(self, point, bounds):
        return ((point[0] == bounds[Const.MINLON]) or (point[1] == bounds[Const.MAXLAT]))
    
    def isRightOrBottom(self, point, bounds):
        return ((point[0] == bounds[Const.MAXLON]) or (point[1] == bounds[Const.MINLAT]))

   
    def findNextPointInClockwise(self, fromPoint, toPointList):
        if ((not fromPoint) or (not toPointList)):
            return -1
    
        edgeList =[fromPoint[3],]
        i = (fromPoint[3] + 1) % 4
        while (i != fromPoint[3]):
            edgeList.append(i)
            i = (i + 1) % 4
        
        edgeSize = len(edgeList)    
        length = len(toPointList)
        dist = [0.0 for x in range(length)]
        isPrcoessed = [False for x in range(length)]
        DISTANCE = [360.0, 180.0, 360.0, 180.0]
        TOTAL_DIST = 1080.0
    
        d =0.0
        dd = 0.0
        i = 0
        
        while (i < edgeSize):
            
            edgeNum = edgeList[i]
            
            for pointidx, toPoint in enumerate(toPointList):
    
                if (toPoint == None):
                    dist[pointidx] = TOTAL_DIST
                    continue
                
                if (isPrcoessed[pointidx]):
                    continue
                
                if (i == 0):
    
                    if (edgeNum == toPoint[3]):
                        
                        if (edgeNum == 0):
                            d = fromPoint[0] - toPoint[0]
                        elif (edgeNum == 1):
                            d = toPoint[1] - fromPoint[1]
                        elif (edgeNum ==2):
                            d = toPoint[0] - fromPoint[0]
                        else :
                            d = fromPoint[1] - toPoint[1]
                    
                        #if (d >= 0):
                        if (d > 0):
                            dist[pointidx] = d
                        else: 
                            dist[pointidx] = (1080.0 + d)
                        isPrcoessed[pointidx] = True
                        #print("d[%i]=%f", pointidx, dist[pointidx])
                            
                    else:
        
                        if (edgeNum == 0):
                            dd = 180.0 + fromPoint[0]
                        elif (edgeNum == 1):
                            dd = 90.0 - fromPoint[1]
                        elif (edgeNum ==2):
                            dd = 180.0 - fromPoint[0]
                        else :
                            dd = 90.0 + fromPoint[1]
                            
                        dist[pointidx] +=dd
                        
                elif (edgeNum == toPoint[3]):
                    if (edgeNum == 0):
                        dd = 180.0 - toPoint[0]
                    elif (edgeNum == 1):
                        dd = 90.0 + toPoint[1]
                    elif (edgeNum ==2):
                        dd = 180.0 + toPoint[0]
                    else :
                        dd = 90.0 - toPoint[1]
                        
                    dist[pointidx] += dd
                    isPrcoessed[pointidx] = True
                    #print("d[%i]=%f", pointidx, dist[pointidx])
                    
                else:
                    dist[pointidx] += DISTANCE[edgeNum]
            #eof for
            i +=1
                    
        #eof while
        minDist = TOTAL_DIST
        returnIdx = -1
        for pointidx, distan in enumerate(dist):      
            if (distan < minDist):
                minDist = distan
                returnIdx = pointidx
        
        #    
        return returnIdx
                
            
    def getJoinPoint(self, start, stop, bounds):
        
        if (stop[0] == start[0]):
            if (start[1] <= stop[1]):
                return (start[0], bounds[Const.MAXLAT], True, 2)
            else:
                return (start[0]. bounds[Const.MINLAT], True, 0)
            
        if (stop[1] == start[1]):
            if (start[0] <= stop[0]):
                return (bounds[Const.MAXLON], start[1], True, 3)
            else:
                return (bounds[Const.MINLON], start[1], True, 1)
            
        rate = (float(stop[1]) - start[1]) / (stop[0] - start[0])
        
        x0 = bounds[Const.MINLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction >= 0):
                return (x0, y0, True, 1)
            
        x0 = bounds[Const.MAXLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction >= 0):
                return (x0, y0, True, 3)
                
        y0 = bounds[Const.MINLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction >= 0):
                return (x0, y0, True, 0)
            
            
        y0 = bounds[Const.MAXLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction >= 0):
                return (x0, y0, True, 2)
            
        self.Logger.error("getJoinPoint, not dedge point found, start=%s, stop=%s, bounds=%s", str(start), str(stop), str(bounds))
        return ()
    
    def getBackJoinPoint(self, start, stop, bounds):
        
        if (stop[0] == start[0]):
            if (start[1] >= stop[1]):
                return (start[0], bounds[Const.MAXLAT], True, 2)
            else:
                return (start[0], bounds[Const.MINLAT], True, 0)
            
        if (stop[1] == start[1]):
            if (start[0] >= stop[0]):
                return (bounds[Const.MAXLON], start[1], True, 3)
            else:
                return (bounds[Const.MINLON], start[1], True, 1)
            
        rate = (float(stop[1]) - start[1]) / (stop[0] - start[0])
        
        x0 = bounds[Const.MINLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 1)
            
        x0 = bounds[Const.MAXLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 3)
                
        y0 = bounds[Const.MINLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 0)
            
            
        y0 = bounds[Const.MAXLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (stop[1] - start[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 2)
            
        return ()
        
    def getCrossingPoint(self, start, stop, bounds):
        
        if (stop[0] == start[0]):
            if (((start[1] >= stop[1]) and stop[2]) or ((start[1] < stop[1]) and start[2])):
                return (start[0], bounds[Const.MAXLAT], True, 2)
            else:
                return (start[0], bounds[Const.MINLAT], True, 0)
            
        if (stop[1] == start[1]):
            if (((start[0] >= stop[0]) and stop[2]) or ((start[0] < stop[0]) and start[2])):
                return (bounds[Const.MAXLON], start[1], True, 3)
            else:
                return (bounds[Const.MINLON], start[1], True, 1)
            
        rate = (float(stop[1]) - start[1]) / (stop[0] - start[0])
        
        x0 = bounds[Const.MINLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (y0 - stop[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 1)
            
        x0 = bounds[Const.MAXLON]
        y0 = rate * (x0 - start[0]) + start[1]
        if (y0 >= bounds[Const.MINLAT] and y0 <= bounds[Const.MAXLAT]):
            direction = (y0 - stop[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 3)
                
        y0 = bounds[Const.MINLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (y0 - stop[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 0)
            
            
        y0 = bounds[Const.MAXLAT]
        x0 = (y0 - start[1]) / rate + start[0]
        if (x0 >= bounds[Const.MINLON] and x0 <= bounds[Const.MAXLON]):
            direction = (y0 - stop[1]) * (y0 - start[1])
            if (direction <= 0):
                return (x0, y0, True, 2)
            
        self.Logger.error("getJoinPoint, not dedge point found, start=%s, stop=%s, bounds=%s", str(start), str(stop), str(bounds))
        return ()
    
    
    # Code to calculate polygon direction to know if it is clockwise or counter clockwise
    def modulus(self, x,y):
        return sqrt(x*x + y*y)
    
    def dot_prod (self, x1,y1,x2,y2):
        return x1*x2 + y1*y2
    
    def cross_prod (self, x1,y1,x2,y2):
        return x1*y2 - x2*y1
    
    def sign_of(self, x):
        if (x >= 0):
            return 1
        else:
            return -1
        
    def delta_calc(self, p0,p1,p2):
        if ((p0 == p1) or (p1 == p2)):
            return 0
        
        x1 = p1[0] - p0[0]
        x2 = p2[0] - p1[0]
        y1 = p1[1] - p0[1]
        y2 = p2[1] - p1[1]

        '''Add to fix the issue that p1 and p2 is same point, but p1[3]=inner and p2[3]=outer'''
        if (x2==0 and y2==0):
            return 0
        
        if (x1==0 and y1==0):
            return 0
        
        signtheta = 0.0
        abstheta = 0.0
        
        try:
            r = self.dot_prod(x1,y1,x2,y2)/ (self.modulus(x1,y1)*self.modulus(x2,y2))
            #print r
            if (r >= 1.0):
                r = 1
            if (r <= -1.0):
                r = -1
            abstheta = acos(r)
            signtheta = self.sign_of(self.cross_prod(x1,y1,x2,y2))
        except Exception:
            self.Logger.error("delta_calc.ERROR, x1=" + str(x1) + ", y1=" + str(y1) + ", x2=" + str(x2) + ", y2=" + str(y2))
            dot_prod = self.dot_prod(x1,y1,x2,y2)
            #print 'prod=', dot_prod
            m1 = self.modulus(x1,y1)
            m2= self.modulus(x2,y2)
            #print 'm1=', m1, 'm2=', m2
            #print dot_prod/(m1 *m2)
            abstheta = acos(self.dot_prod(x1,y1,x2,y2)/ (self.modulus(x1,y1)*self.modulus(x2,y2)))
            signtheta = self.sign_of(self.cross_prod(x1,y1,x2,y2))
            
        return signtheta*abstheta
    
    def isClockwise (self, ps):
        ps.append(ps[0])
        ps.append(ps[1])
        mydelta = 0
        for i in range(len(ps)-2):
            p0 = ps[i]
            p1 = ps[i+1]
            p2 = ps[i+2]
            mydelta += self.delta_calc(p0,p1,p2)        
        ps.pop()
        ps.pop()
        return (mydelta < 0)


    #the polygon is truncated polygon, which has at lease two matches.
    def getEdgeType(self, polygon, edge, isRight):
        
        preValue = 0.0
        currValue = 0.0
        isMatch = False
        numMatch = 0
        preIncrease = 0 # 0-unknow 1-increase 2-decrease

        
        for point in polygon:
            
            if isRight:
                diff  = abs(edge - point[0])
                currValue = point[1]
            else:
                diff = abs (edge - point[1])
                currValue = point[0]
                
            if (diff > self.IGNORE_VALUE): #not match
                if (numMatch == 1):
                    preValue = 0.0
                    isMatch = False
                    numMatch = 0
                    preIncrease = 0
                elif (numMatch >1):
                    break;
                continue
            
            numMatch += 1
            
            if (not isMatch):#first time match
                isMatch = True
                preValue = currValue
                continue
            else: #many time match
                if (currValue >= preValue):
                    increase = 1
                else:
                    increase = 2
                
                if (preIncrease == 0):
                    preIncrease = increase
                elif (preIncrease != increase):
                    return Const.TYPE_UNKNOWN
                
                    
                preValue = currValue
                continue
        #eof for
        
        if (numMatch < 2):
            return Const.TYPE_UNKNOWN
        

        if (isRight):
            if (preIncrease == 1): #increase
                return Const.TYPE_LAND
            elif (preIncrease ==2):#decrease
                return Const.TYPE_OCEAN
        else:
            if (preIncrease == 1): #increase 
                return Const.TYPE_OCEAN
            elif (preIncrease ==2): #decrease
                return Const.TYPE_LAND
    
        return Const.TYPE_UNKNOWN
  
  
    def isEdgeTypeForest(self, polygon, point1, point2, isRight): #if two points contains in the polygon, the edge is forest
        
        prevPoint = None
        for pnt in polygon:
            if (prevPoint == None):
                prevPoint = pnt
                continue
            
            if isRight:
                diff1  = abs(point1[0] - pnt[0])
                diff2  = abs(point1[0] - prevPoint[0])
            else:
                diff1 =abs(point1[1] - pnt[1])
                diff2= abs(point1[1] - prevPoint[1])
                
            if ((diff1 > self.IGNORE_VALUE) or (diff2 > self.IGNORE_VALUE)): #at least one not match
                prevPoint = pnt
                continue
            
            if isRight:
                diff1  = abs(point1[1] - pnt[1])
                diff2  = abs(point2[1] - prevPoint[1])
                diff3  = abs(point1[1] - prevPoint[1])
                diff4  = abs(point2[1] - pnt[1])
            else:
                diff1 =abs(point1[0] - pnt[0])
                diff2= abs(point2[0] - prevPoint[0])
                diff3 =abs(point1[0] - prevPoint[0])
                diff4= abs(point2[0] - pnt[0])
                
            counter =0
            if ((diff1 <= self.IGNORE_VALUE)): 
                counter +=1
            if ((diff2 <= self.IGNORE_VALUE)): 
                counter +=1
            if ((diff3 <= self.IGNORE_VALUE)): 
                counter +=1
            if ((diff4 <= self.IGNORE_VALUE)): 
                counter +=1   
            if (counter != 2):
                continue
            
            return Const.TYPE_FOREST
            
        #eof for
            
        return Const.TYPE_UNKNOWN
    
    
    def getEdgeTypeByForest(self, polygon, edge, isRight):
        
        preValue = 0.0
        currValue = 0.0
        isMatch = False
        numMatch = 0
        preIncrease = 0 # 0-unknow 1-increase 2-decrease

        
        for point in polygon:
            
            if isRight:
                diff  = abs(edge - point[0])
                currValue = point[1]
            else:
                diff = abs (edge - point[1])
                currValue = point[0]
                
            if (diff > self.IGNORE_VALUE): #not match
                if (numMatch == 1):
                    preValue = 0.0
                    isMatch = False
                    numMatch = 0
                    preIncrease = 0
                elif (numMatch >1):
                    break;
                continue
            
            numMatch += 1
            
            if (not isMatch):#first time match
                isMatch = True
                preValue = currValue
                continue
            else: #many time match
                if (currValue >= preValue):
                    increase = 1
                else:
                    increase = 2
                
                if (preIncrease == 0):
                    preIncrease = increase
                elif (preIncrease != increase):
                    return Const.TYPE_UNKNOWN
                
                    
                preValue = currValue
                continue
        #eof for
        
        if (numMatch < 2):
            return Const.TYPE_UNKNOWN
        

        if (isRight):
            if (preIncrease == 1): #increase
                #return Const.TYPE_LAND
                return Const.TYPE_UNKNOWN
            elif (preIncrease ==2):#decrease
                return Const.TYPE_FOREST
        else:
            if (preIncrease == 1): #increase 
                return Const.TYPE_FOREST
            elif (preIncrease ==2): #decrease
                #return Const.TYPE_LAND
                return Const.TYPE_UNKNOWN
    
        return Const.TYPE_UNKNOWN
    
                
                
                
                
    '''
    def drawline(self):
        plt.plot([1,2,3,4])
        plt.ylabel('some numbers')
        plt.show()
    '''        
    '''def getDistance(self, fromPoint, toPoint):
        
        if ((toPoint[3] > fromPoint[3]) or  ((toPoint[3] == fromPoint[3]) and self.geometric.isGreater(toPoint, fromPoint))):
            return (toPoint[3] - fromPoint[3])
        else:
            return (toPoint[3] + 4 - fromPoint[3])'''
    
    '''def findNextPointInClockwise(self, fromPoint, toPointList):

        edgeList =[fromPoint[3],]
        i = (fromPoint[3] + 1) % 4
        while (i != fromPoint[3]):
            edgeList.append(i)
            i = (i + 1) % 4

        roundIdx = -1
        for edgeNum in edgeList:
            distance = (9999.0)
            foundIdx = -1
            for idx, toPoint in enumerate(toPointList):
    
                if ((toPoint == None) or (edgeNum != toPoint[3])):
                    continue
                
                if (edgeNum == 0):
                    d = fromPoint[0] - toPoint[0]
                elif (edgeNum == 1):
                    d = toPoint[1] - fromPoint[1]
                elif (edgeNum ==2):
                    d = toPoint[0] - fromPoint[0]
                else :
                    d = fromPoint[1] - toPoint[1]
                    
                if d < 0:
                    d = (1080.0 + d)
                    
                if (d < distance):
                    foundIdx = idx
                    distance = d
                    roundIdx = idx
                    
            if ((foundIdx >=0) and (distance <361)):
                return foundIdx
        
        if (roundIdx >= 0):
            return roundIdx
        
        return -1'''
        
            
        
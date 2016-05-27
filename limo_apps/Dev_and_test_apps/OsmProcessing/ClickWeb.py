import sys
import webbrowser
import httplib, base64
#from httplib import HTTPConnection
import time, threading, datetime

import logging.handlers


class ApiError(Exception):
        
    def __init__(self, status, reason, payload):
        self.status  = status
        self.reason  = reason
        self.payload = payload
    
    def __str__(self):
        return "Request failed: " + str(self.status) + " - " + self.reason + " - " + self.payload
    

SLEEP_SHORT = (5*60) #5 mins
SLEEP_LONG = (60*60) #1 hour
LOG_DEFAULT_LEVEL = logging.DEBUG
#LOG_DEFAULT_LEVEL = logging.INFO
#LOG_DEFAULT_LEVEL = logging.WARN
LOG_PATH = "logs/"
LOG_FILE_NAME = "web"
LOG_TO_CONSOLE = True    
LOG_FILE_MAX_BYTES = 1*1024*1024
LOG_BACKUP_COUNT = 3
ID = 1
NEW = 2
HOST = "www.canlanding.com"


urls = {"https://www.google.com/?hl=add&gl=url.html#hl=add&q=canlanding+%E5%8A%A0%E8%BE%BE%E7%A7%BB%E6%B0%91",
        "http://canlanding.com/index.html",
        "http://canlanding.com/contactUs_en.html",
        "http://canlanding.com/AboutUs_cn",
        "https://www.google.com/?hl=add&gl=url.html#hl=add&q=canlanding.com",
        }

MAX_RETRY = 5 *len(urls)
retrycounter = [0]

def SendURLs():
    Logger.debug ("start to send -----------------")
    for url in urls:
        SendURLReq(url)
        time.sleep(SLEEP_SHORT)
    Logger.debug ("end of send ===================")
    print datetime.datetime.now()
    threading.Timer(SLEEP_LONG, SendURLs).start()

def SendURLReq(url):
    cmd = 'GET'
    path = url
    auth = False
    send = None

    conn = httplib.HTTPConnection(HOST, 80)
    i = 0
    while True:
        i += 1
        try:
            return HttpRequest(conn, cmd, path, auth, send)
        except Exception:
            retrycounter[0] += 1
            Logger.error("SendURLReq, exception #%i for url=%s. retrycounter=%i" % (i, url, retrycounter[0])) 
            if (retrycounter[0] >= MAX_RETRY):
                retrycounter[0] = 0
                SendEmail()
                Logger.error ("Reach max retry, and shout down.")
                exit(0)
                
            if i == 5: break
            if i != 1: time.sleep(5)
            conn = httplib.HTTPConnection(HOST, 80)
    conn = None
    
    
def HttpRequest(conn, cmd, path, auth, send, username = None, password= None):

    path2 = path
    if len(path2) > 250:
        path2 = path2[:250]+"[...]"
    Logger.debug("1-%s %s %s"%(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2))
    conn.putrequest(cmd, path)
    #conn.putheader('User-Agent', user_agent)
    if auth:
        conn.putheader('Authorization', 'Basic ' + base64.encodestring(username + ':' + password).strip())
    if send != None:
        conn.putheader('Content-Length', len(send))
    conn.endheaders()
    if send:
        conn.send(send)
    response = conn.getresponse()
    if response.status != 200:
        payload = response.read().strip()
        if response.status == 410:
            return None
        Logger.debug("HttpRequest-%s %s %s failed with response code=%i, reason=%s, payload=%s" %(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2, response.status, response.reason, payload))
        raise ApiError(response.status, response.reason, payload)
    Logger.debug("2-%s %s %s done sccessfully" %(time.strftime("%Y-%m-%d %H:%M:%S"),cmd,path2))
    result = response.read()
    conn.close()
    return result
    
import smtplib
from email.mime.text import MIMEText

def SendEmail():
      
    fromaddr = 'sunnyskyhf3@gmail.com'  
    toaddrs  = 'sunnyskyhf3@gmail.com'
    SUBJECT = "canlanding.com"
    TEXT = 'website was down!!!'
    

    msg = MIMEText(TEXT)
    msg['Subject'] = SUBJECT
    msg['From'] = fromaddr
    msg['To'] = toaddrs
      
      
    # Credentials (if needed)  
    username = 'sunnyskyhf3'  
    password = '19660824'  
      
    # The actual mail send  
    server = smtplib.SMTP('smtp.gmail.com:587')  
    server.starttls()  
    server.login(username,password)  
    #server.sendmail(fromaddr, toaddrs, msg)
    server.sendmail(fromaddr, [toaddrs], msg.as_string())
    server.quit()

def SendURLToBrowser(url, NEW):
    webbrowser.open(url, new=NEW)

    

print("----------------------------------------Main------------------------------------------------")

argv = sys.argv
if (argv and len(argv) > 1):
    print("TileId=%s" % argv[1])
    Id = int(argv[1])
else:
    Id = ID
    
#Setup Logger
Logger = logging.getLogger()
Logger.setLevel(LOG_DEFAULT_LEVEL)


fileHandler = logging.handlers.RotatingFileHandler("{0}/{1}.log".format(LOG_PATH, LOG_FILE_NAME), maxBytes=LOG_FILE_MAX_BYTES, backupCount=LOG_BACKUP_COUNT)
logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
fileHandler.setFormatter(logFormatter)
Logger.addHandler(fileHandler)

if(LOG_TO_CONSOLE):

    consoleHandler = logging.StreamHandler()
    consoleHandler.setFormatter(logFormatter)
    Logger.addHandler(consoleHandler)
    
    
SendURLs()
print("================================END MAIN==========================================.")
exit(0)






------How to create a vnc connection with mod live as the server (view on phone/tablet)------

-Root both phone and mod live
-enable bluetooth on both devices and pair them from bluetooth settings

-shell to modlive
#pand --listen
#hcitool dev
this outputs the bluetooth mac address you will use to connect, will output nothing if bluetooth is turned off

-shell to client device (make sure you get a # prompt, you might have to enter 'su')

#pand --connect #modlivemacaddress
#pand -l 
pand -l should show a connection starting with 'bnep0' if connect was successful (bnep1 etc for subsequent connections)
(if nothing shows up, wait a few seconds)


#ifconfig bnep0 10.0.0.2

-shell to modlive again

#ifconfig bnep0 10.0.0.1

-to confirm the network interface is up on either side, use
#netcfg
you should get something like
lo       UP    127.0.0.1       255.0.0.0       0x00000049
bnep0    UP    10.0.0.1        255.0.0.0       0x00001043
if its down it will say
bnep0    DOWN  0.0.0.0         0.0.0.0         0x00001002

make sure the connection is up on both sides

-on client device, open vnc viewer app and connect to 10.0.0.1 port 5901 (10.0.0.1:5901)


To use mod live as the viewer, run the phonevncserver.apk application on your phone and modlivevncviewer on mod live after establishing the bnep interface


Folder Contents:
android-vnc-server: modified open source project used as a vnc server on mod live
android-vnc-viewer: modified open source project used as a vnc viewer on mod live
modlivevncserver: compiled binary of android-vnc-server
modlivevncviewer.apk: compiled application of android-vnc-viewer
phonevncserver.apk: android vnc server application
phonevncviewer.apk: android vnc viewer application
sqlitegen_eclipse_site_0.1.18.jar: required to build android-vnc-viewer


MOD Live VNC server:
modlivevncserver is a binary application to allow mod live to be viewed over an ip connection.
pand and bnep are required to create an ip connection over bluetooth
it is based on the open source project android-vnc-server
http://code.google.com/p/android-vnc-server/ (revision 3)
it seems to be a dead project
the only changes made were to comment out lines 492-495 in fbvncserver.c to disable keyboard and touch, because they prevented it from running on mod live


MOD Live VNC viewer:
based on android-vnc-viewer 
http://code.google.com/p/android-vnc-viewer/ (revision 201)

modified to be rotated 90 degrees and scaled.
It also has the ip address entered as an existing connection so the user doesn't have to type it in
This was necessary because rotation is not supported, and we wanted to fill the whole screen. It was only used with the Galaxy S so it probably won't work with other screen sizes.

the transformation code that was changed is in FullBufferBitmapData.draw(Canvas canvas)


Android VNC Viewer:
same code as mod live vnc server but got the apk from google play
most vnc viewers work on phone side, I used this one which is also open source
https://play.google.com/store/apps/details?id=android.androidVNC&hl=en
in case it has been changed and no longer works, you can use phonevncviewer.apk

Android VNC server:
used on android phone (Galaxy S) to stream to mod live
http://www.onaips.com/wordpress/?page_id=60
https://play.google.com/store/apps/details?id=org.onaips.vnc&feature=search_result
newer version on google play doesn't seem to work but the apk phonevncserver.apk is an older version that does work.


Future areas to look at
LibVNCServer:
android-vnc-server is based on LibVNCServer
http://libvncserver.sourceforge.net/
it seems like since the version used in android-vnc-server it has added support for android directly so it may be easier/possible to use libvncserver directly now. or just drop the new version directly into android-vnc-server

the newer LibVNCServer supports WebSockets, if we wrap a bluetooth socket with a WebSocket it could be possible to have a vnc viewer in application level code, ie. without using pand or bnep, and therefore not requiring root access on the client device.

It could also be possible to use websockets and libvncserver on android and use modlive as a client, using something other than the framebuffer (eg. application window) to have a vnc server in an application without requiring root access.
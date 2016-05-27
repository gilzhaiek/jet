#!/usr/bin/env python
import subprocess
import cgi
import shlex
form = cgi.FieldStorage()
address = form.getvalue('address')
padding = form.getvalue('padding')

bashCommand ='bash check_tiles.bash "'+address+'" '+padding
splitted = shlex.split(bashCommand)
splitted[2] = '"' + splitted[2]+'"'
#bashCommand ="ls -l"
process = subprocess.Popen(splitted, stdout=subprocess.PIPE)
output = process.communicate()[0]

print 'Content-Type: text/html\n'
print "<pre>"
print output
print "</pre>"






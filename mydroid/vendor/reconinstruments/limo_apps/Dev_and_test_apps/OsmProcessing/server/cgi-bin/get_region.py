#!/usr/bin/env python
import subprocess
import cgi
import shlex
form = cgi.FieldStorage()
address = form.getvalue('address')
padding = form.getvalue('padding')


# bashCommand ='bash check_tiles.bash "'+address+'" '+padding
# splitted = shlex.split(bashCommand)
# splitted[2] = '"' + splitted[2]+'"'
# #bashCommand ="ls -l"
# process = subprocess.Popen(splitted, stdout=subprocess.PIPE)
# output = process.communicate()[0]

bashCommand_2 ='bash create_task.bash "'+address+'" '+padding
splitted_2 = shlex.split(bashCommand_2)
splitted_2[2] = '"' + splitted_2[2]+'"'
#bashCommand ="ls -l"
process_2 = subprocess.Popen(splitted_2, stdout=subprocess.PIPE)
output_2 = process_2.communicate()[0]

print 'Content-Type: text/html\n'
print "<pre>"
print "Your region corresponds to:"
print output_2
print "</pre>"







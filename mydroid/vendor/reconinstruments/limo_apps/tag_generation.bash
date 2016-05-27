find . -name *.java | awk '{sub(/.\//,"",$0); print $0}' | etags -



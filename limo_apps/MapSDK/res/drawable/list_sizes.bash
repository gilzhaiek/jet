file="$@"
if [ "$file" = "" ] ; then
	echo "no file specified.. do nothing";
	exit;
fi
for f in $file
do
	width=`identify $f | sed -n -e "s/.* \([0-9]*\)x\([0-9]*\) [0-9]*x[0-9+]*.*/\1/p"`
	height=`identify $f | sed -n -e "s/.* \([0-9]*\)x\([0-9]*\) [0-9]*x[0-9+]*.*/\2/p"`
	echo -e "$f size: ${width}x${height}";
done

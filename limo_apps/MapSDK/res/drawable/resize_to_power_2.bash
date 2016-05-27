file="$@"
if [ "$file" = "" ] ; then
	echo "no file specified.. do nothing";
	exit;
fi

for f in $file
do
	width=`identify $f | sed -n -e "s/.* \([0-9]*\)x\([0-9]*\) [0-9]*x[0-9+]*.*/\1/p"`
	height=`identify $f | sed -n -e "s/.* \([0-9]*\)x\([0-9]*\) [0-9]*x[0-9+]*.*/\2/p"`

	echo -en "resizing $f to .......";

	if [ "$width" -gt "$height" ]; then
		bigger_size=$width;
	else
		bigger_size=$height;
	fi

	if [ "$bigger_size" -le 16 ] && [ "$bigger_size" -gt 0 ]; then
		#mogrify -resize 16x16\! $f;
		convert -resize 16x16 $f -background none -gravity center -extent 16x16 output;
		mv -f output $f;
		echo -e "16x16";
	elif [ "$bigger_size" -le 32 ] && [ "$bigger_size" -gt 16 ]; then
		#mogrify -resize 32x32\! $f;
		convert -resize 32x32 $f -background none -gravity center -extent 32x32 output;
		mv -f output $f;
		echo -e "32x32";
	elif [ "$bigger_size" -le 64 ] && [ "$bigger_size" -gt 32 ]; then
		#mogrify -resize 64x64\! $f;
		convert -resize 64x64 $f -background none -gravity center -extent 64x64 output;
		mv -f output $f;
		echo -e "64x64";
	elif [ "$bigger_size" -le 128 ] && [ "$bigger_size" -gt 64 ]; then
		#mogrify -resize 128x128\! $f;
		convert -resize 128x128 $f -background none -gravity center -extent 128x128 output;
		mv -f output $f;
		echo -e "128x128";
	else
		echo -e "nothing. Size unchanged."
	fi

done

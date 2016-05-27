curfmode=$(grep "This is the OpenGL implementation" src/com/reconinstruments/mapsdk/mapview/MapView.java);
echo -e
if [ "$curfmode" = "" ]; then
curmode="Canvas";
else
curmode="OpenGL";
fi

desiredmode=$1;

if [ "$desiredmode" = "canvas" ]; then
    if [ "$curmode" = "Canvas" ]; then
		if cmp -s "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.Canvas" 
		then
			echo -e "Current version of MapView.java is already the Canvas version...  nothing changed";
		else
			if [ "src/com/reconinstruments/mapsdk/mapview/MapView.java" -nt "src/com/reconinstruments/mapsdk/mapview/MapView.Canvas" ]; then
				echo -e "Current version of MapView.java is already Canvas but it has been changed !!";
			else
				cp src/com/reconinstruments/mapsdk/mapview/MapView.Canvas src/com/reconinstruments/mapsdk/mapview/MapView.java 
				echo -e "MapView.java updated to newer version of Canvas template";
			fi
		fi
    	echo -e
	else
		if ! ( cmp -s "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.GL" )
		then
   			cp "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.GL";
   			echo -e "Modified version of OpenGL MapView.java overwriting OpenGL template...";
	    	echo -e
		fi
		cp src/com/reconinstruments/mapsdk/mapview/MapView.Canvas src/com/reconinstruments/mapsdk/mapview/MapView.java 
		echo -e "MapSDK rendering mode changed to Android Canvas ...";
		echo -e 
	fi
elif [ "$desiredmode" = "opengl" ]; then
    if [ "$curmode" = "OpenGL" ]; then
		if cmp -s "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.GL"
		then
			echo -e "Current version of MapView.java is already the OpenGL version... nothing changed";
		else
			if [ "src/com/reconinstruments/mapsdk/mapview/MapView.java" -nt "src/com/reconinstruments/mapsdk/mapview/MapView.GL" ]; then
				echo -e "Current version of MapView.java is already OpenGL but it has been changed !!";
			else
				cp src/com/reconinstruments/mapsdk/mapview/MapView.GL src/com/reconinstruments/mapsdk/mapview/MapView.java 
				echo -e "MapView.java updated to newer version of OpenGL template";
			fi
		fi
    	echo -e
	else
		if ! ( cmp -s "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.Canvas" )
		then
   			cp "src/com/reconinstruments/mapsdk/mapview/MapView.java" "src/com/reconinstruments/mapsdk/mapview/MapView.Canvas";
   			echo -e "Modified version of Canvas MapView.java overwriting Canvas template...";
	    	echo -e
		fi
		cp src/com/reconinstruments/mapsdk/mapview/MapView.GL src/com/reconinstruments/mapsdk/mapview/MapView.java 
		echo -e "MapSDK rendering mode changed to OpenGL ...";
		echo -e 
	fi
else
	echo -e "Unrecognized rendering mode - nothing done ";
	echo -e 
	echo -e "usage: bash switchRenderingMethod [opengl | canvas]"
	echo -e ;
fi



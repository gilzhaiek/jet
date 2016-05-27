# javah -jni -classpath src/ -o jni/ReconBLE.h com.reconinstruments.reconble.ReconBLE
# javah -jni -classpath src/ -o jni/BLENative.h com.reconinstruments.reconble.BLENative
javah -jni -classpath bin/classes -o jni/ReconBLE.h  com.reconinstruments.reconble.ReconBLE
javah -jni -classpath bin/classes -o jni/BLENative.h com.reconinstruments.reconble.BLENative
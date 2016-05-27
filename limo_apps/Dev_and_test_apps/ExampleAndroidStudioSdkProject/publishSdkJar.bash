#~/bin/bash

# The 'compileRelease' task must be explicity run first due to limitations in the Gradle API
./gradlew clean javadocJar compileRelease bundleJar

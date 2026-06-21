#!/bin/sh
# Gradle wrapper script for building the project
exec 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' \
    -Xmx2048m \
    -Dorg.gradle.wrapper.download.checksums=sha256 \
    -Dfile.encoding=UTF-8 \
    -Duser.country=CN \
    -Duser.language=zh \
    -classpath 'C:\Users\zywds\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\lib\gradle-launcher-8.7.jar' \
    org.gradle.launcher.GradleMain "$@"

#!/bin/bash

# IMPORTANT: To connect to your GitHub Maven Repository, copy your "settings.xml" file from into the "~/.m2" directory.  

mvn deploy:deploy-file \
  -DgroupId=brotenet \
  -DartifactId=json-toolkit \
  -Dversion=0.0.1 \
  -Dpackaging=jar \
  -Dfile=lib/json-toolkit-0.0.1.jar \
  -DrepositoryId=github \
  -Durl=https://maven.pkg.github.com/brotenet/JsonToolKit

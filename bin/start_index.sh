#!/bin/bash
java -cp ./src -classpath ./src:./libs/jsoup-1.7.2.jar:./libs/gson-2.2.3.jar -Xmx512m edu.nyu.cs.cs2580.SearchEngine \
-mode=index --port=25804 --options=conf/engine.conf
exit 0

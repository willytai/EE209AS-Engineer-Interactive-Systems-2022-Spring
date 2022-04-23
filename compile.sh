#!/bin/bash 
javac -classpath "$PWD/bin:$PWD/lib/core.jar:$PWD/lib/weka.jar:$PWD/lib/sound.jar:$PWD/lib/jsyn-20171016.jar:$PWD/lib/javamp3-1.0.4.jar" src/ClassifyVibration.java -d bin/

#!/bin/bash

srcFile=$1
prog=$2
res=$3

time java -Xms8G -Xmx12G -jar saxon9he.jar -s:$srcFile -xsl:$prog -o:$res

echo "#########################################################"
echo "src file:"
ls -lh $srcFile

echo "#########################################################"
echo "number of generate rows:"
wc -l $res

#echo "diff reult:"
#diff ../../benchmarks/XML/benchmark$expNum/res.csv ../../benchmarks/XML/benchmark$expNum/out.csv 



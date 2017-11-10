#!/bin/bash

srcFile=$1
prog=$2
res=$3

time java -Xms8G -Xmx12G -jar saxon9he.jar -s:../../benchmarks/Full-DB/dblp/$srcFile -xsl:../../benchmarks/Full-DB/dblp/$prog -o:../../benchmarks/Full-DB/dblp/$res

echo "#########################################################"
echo "src file:"
ls -lh ../../benchmarks/Full-DB/dblp/$srcFile

echo "#########################################################"
echo "number of generate rows:"
wc -l ../../benchmarks/Full-DB/dblp/$res

#echo "diff reult:"
#diff ../../benchmarks/XML/benchmark$expNum/res.csv ../../benchmarks/XML/benchmark$expNum/out.csv 



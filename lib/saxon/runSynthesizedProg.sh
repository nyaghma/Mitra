#!/bin/bash

expNum=$1

time java -Xms8G -Xmx12G -jar saxon9he.jar -s:../../benchmarks/XML/benchmark$expNum/instance1.xml -xsl:../../benchmarks/XML/benchmark$expNum/program.xsl -o:../../benchmarks/XML/benchmark$expNum/res-512m.csv 


#echo "#########################################################"
#echo "src file:"
#ls -lh ../../benchmarks/XML/benchmark$expNum/in.xml

echo "#########################################################"
echo "number of generate rows:"
wc -l ../../benchmarks/XML/benchmark$expNum/res-512m.csv

#echo "#########################################################"
#echo "diff res:"
#diff ../../benchmarks/XML/benchmark$expNum/out.csv ../../benchmarks/XML/benchmark$expNum/up_res.csv

#echo "diff reult:"
#diff ../../benchmarks/XML/benchmark$expNum/res.csv ../../benchmarks/XML/benchmark$expNum/out.csv 



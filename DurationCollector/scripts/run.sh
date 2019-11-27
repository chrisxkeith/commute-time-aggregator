set -x
cd ~/Documents/Github/commute-time-aggregator/DurationCollector/	; if [ $? -ne 0 ] ; then exit -6 ; fi
cat ~/Documents/routeInfo.txt										; if [ $? -ne 0 ] ; then exit -6 ; fi

mvn clean install exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector" -Dmaven.test.skip=true | \
 tee -a ~/Documents/route-data/routeInfo.log

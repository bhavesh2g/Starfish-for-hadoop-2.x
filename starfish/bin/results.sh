cd ..
cd results
mkdir application_$1
scp -r hadoop@sandking01:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/ 
scp -r hadoop@sandking02:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/
scp -r hadoop@sandking03:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/
cd ..
ant clean
ant init
ant compile-profile-classes
cp lib/hadoop-0.20.2-core.jar ./build/
cp lib/commons-logging-1.0.4.jar ./build/
cd build
java -cp ${CLASSPATH}:hadoop-0.20.2-core.jar:commons-logging-1.0.4.jar ExecuteProfile $1

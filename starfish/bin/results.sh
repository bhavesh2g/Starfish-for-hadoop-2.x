cd ..
cd results
mkdir application_$1
scp -r hadoop@sandking01:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/ 
scp -r hadoop@sandking02:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/
scp -r hadoop@sandking03:~/hadoop-2.2.0/logs/userlogs/application_$1/* ./application_$1/


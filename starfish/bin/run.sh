(cd ..; ant clean; ant init; ant compile-btrace-classes;)
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
hdfs dfs -rm -r /user/output
hadoop jar ~/wangyu/test.jar wordcount /user/input /user/output


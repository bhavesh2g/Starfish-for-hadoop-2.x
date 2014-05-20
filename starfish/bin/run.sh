(cd ..; ant clean; ant init; ant compile-btrace-classes;)
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
hdfs dfs -rm -r /tmp/HiBench/Sort/Output
hadoop jar ~/wangyu/test.jar sort -outKey org.apache.hadoop.io.Text -outValue org.apache.hadoop.io.Text -r 48 /tmp/HiBench/Sort/Input /tmp/HiBench/Sort/Output
cd ~/starfish/starfish/bin 

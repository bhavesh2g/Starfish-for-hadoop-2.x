(cd ..; ant clean; ant init; ant compile-btrace-classes;)
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.jar sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking01:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking02:/home/hadoop/starfish/starfish/hadoop-btrace/
scp ~/starfish/starfish/hadoop-btrace/*.class sandking03:/home/hadoop/starfish/starfish/hadoop-btrace/
hdfs dfs -rm -r /tmp/HiBench/Wordcount/Output
hadoop jar ./test.jar wordcount -D mapreduce.inputformat.class=org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat -D mapreduce.outputformat.class=org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat /tmp/HiBench/Wordcount/Input /tmp/HiBench/Wordcount/Output

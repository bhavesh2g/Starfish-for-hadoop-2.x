###Starfish2
#####Project    ：Starfish-for-hadoop-2.x
#####Author : WangYu @Tsinghua University
#####Date : June 1st, 2014

###Discription
Starfish is a self-tuning system for big data analytics built on hadoop. Visit the Duke University website for details. http://www.cs.duke.edu/starfish
The old Starfish only works well on hadoop 1.x such as hadoop v0.20.x and v1.0 3. We are doing work to make Starfish works well with hadoop 2.x includs v0.23 and versions after v2.2.0 which is called Starfish2

###About Starfish
Please read the README file in /starfish/starfish/README to learn about the details of the original Starfish.

###Requirements
1. Starfish2 has been tested to work well on hadoop 2.2.0. It should also work on all the hadoop2.x version
2. The MapReduce programs must be written using the new Hadoop API.

###Usage
#####Just make it work
1.  Use "ant compile" to compile the Starfish2 source code
2.  Copy the Starfish2 project to the "~/" directory for every nodes in hadoop cluster
3.  Run the "bin2/run.sh" to run hadoop(You shall modify run.sh to change the BenchMark you want to run)
4.  Run the "bin2/result.sh XXXXX_XX" to gather and generate result profiles (The results will be in "starfish/starfish/results")

#####Understand it step by step
0.  You can always refer to bin2/run.sh bin2/results.sh as an example to help you understand
1.  Use "ant compile" to compile the Starfish2 source code
2.  Copy the *.jar and *.class files in /starfish/starfish/hadoop-btrace to all the nodes.
3.  Modify hadoop configuration file "mapred-site.xml" to set the "mapred.child.java.opts" (You can refer to the example file in starfish/starfish/bin2, you may need to change the path of btrace files)
4.  Copy the configuration file to all the nodes
5.  Run hadoop application
6.  Gather the stdouts found in userlogs to one node
7.  Run the ExecuteProfile.class which can be found in /starfish/starfish/build to get the profiles from stdouts, give the job id as parameter. (That is , if the application is called job_XXXXX_XX, then you should run the java class as "ExecuteProfile.class XXXXX_XX")
8.  You shall get the results in /starfish/starfish/results

###New Directories
* -bin2   :   Useful shell scripts for run Starfish2 and example for hadoop configuration
* -results    :   Will keep the outputs of hadoop tasks and the generated profile
Notes   :   The java classes i've changed in src/profile are always renamed as OLD_CLASS_NAME2

###Schedule
#####Description
In Profiler Component, there are three steps:
*   Gather and analyze the history of hadoop application
*   Use dynamic instrumentation tool BTrace to learn the profile of every single task
*   Get the data transfers if requested

#####Done
1.  In Profiler Component, Use dynamic instrumentation tool BTrace to learn the profile of every single task

#####TO DO
1.  In Profiler Component, analyze the history of hadoop application
2.  Complete the other three Components "What-if Engine", "Cost-based Optimizer", "Visualizer"

###BUGs Until now
1.  Can't profile the Combine parts, because the related data is in the history files
2.  Can't trace the two Daemon Thread which do merges during Shuffle phase. So we can't get the merge time of "do in memory " and "do on disk" respectively.  All we can get is the total merge time.

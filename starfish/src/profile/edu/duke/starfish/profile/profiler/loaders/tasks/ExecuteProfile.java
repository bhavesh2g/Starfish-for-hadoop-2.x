// package edu.duke.starfish.profile.profiler.loaders.tasks;

import org.apache.hadoop.conf.Configuration;
import edu.duke.starfish.profile.profileinfo.execution.profile.*;
import edu.duke.starfish.profile.profiler.loaders.tasks.*;

import java.io.*;
import java.util.Scanner;

/**
 * Read the stdouts generated by containers;
 * Generate task profiles;
 * @author WangYu
 */

public class ExecuteProfile {

	private class myFileFilter implements FileFilter{
		public boolean accept(File pathname) {
			if (pathname.getName().startsWith("container")) {
				return true;
			}
			return false;
		}
	}

	private String appId = "";
	private String filePath = "/home/hadoop/starfish/starfish/results/";
	private Configuration conf;

	public ExecuteProfile(String appid) {
		appId = appid;
		String seperator = System.getProperty("file.seperator");
		filePath = filePath + "application_" + appId;
	}

	public void setFilePath(String path) {
		filePath = path;
	}

	public void loadProfile() {
		Configuration conf = new Configuration();
		conf.addResource("/home/hadoop/hadoop-2.2.0/etc/hadoop/mapred-site.xml");
		File files = new File(filePath);
		File[] containers = files.listFiles(new myFileFilter());
		int nMap = 0, nReduce = 0;
		for (File container : containers) {
			File oneFile = new File(container, "stdout");
			System.out.println(oneFile.getAbsolutePath());
			boolean isMap = false, isReduce = false;
			try {
				Scanner fin = new Scanner(oneFile);
				String line;
				while (fin.hasNextLine()) {
					line = fin.nextLine();
					if (line.startsWith("REDUCE")) {
						isReduce = true;
						break;
					}
					if (line.startsWith("MAP")) {
						isMap = true;
						break;
					}
				}
				fin.close();
			}
			catch(Exception e) {
				System.out.println("Exception:" + e.getMessage());
			}
			MRMapProfile mapProfile;
			MRMapProfileLoader2 mapLoader;
			MRReduceProfile reduceProfile;
			MRReduceProfileLoader2 reduceLoader;
			MRTaskProfile taskProfile;
			PrintStream ps;
			if (isMap) {
				System.out.println("isMap");
				mapProfile = new MRMapProfile(oneFile.getAbsolutePath());
				mapLoader = new MRMapProfileLoader2(mapProfile, conf, oneFile.getAbsolutePath());
				try {
					taskProfile = (MRTaskProfile)mapLoader.getProfile();
					if (mapLoader.loadExecutionProfile(taskProfile)) {
						File f = new File(files, "map" + nMap);
						System.out.println(nMap);
						if (!f.exists()) {
							f.createNewFile();
						}
						ps = new PrintStream(new FileOutputStream(f));
						nMap ++;
						taskProfile.printProfile(ps);
						ps.close();
					}
				}
				catch (Exception e) {
					System.out.println("Error:" + e.getMessage() + " " + oneFile.getAbsolutePath());
				}
			}
			if (isReduce) {
				System.out.println("isReduce");
				reduceProfile = new MRReduceProfile(oneFile.getAbsolutePath());
				reduceLoader = new MRReduceProfileLoader2(reduceProfile, conf, oneFile.getAbsolutePath());
				try {
					taskProfile = (MRTaskProfile)reduceLoader.getProfile();
					if (reduceLoader.loadExecutionProfile(taskProfile)) {
						File f = new File(files, "reduce" + nReduce);
						System.out.println(nReduce);
						if (!f.exists()) {
							f.createNewFile();
						}
						ps = new PrintStream(new FileOutputStream(f));
						nReduce ++;
						taskProfile.printProfile(ps);
						ps.close();
					}
				}
				catch (Exception e) {
					System.out.println("Error:" + e.getMessage() + " " + oneFile.getAbsolutePath());
				}
			}
		}
	}

	public static void main(String[] args) {
		ExecuteProfile executeProfile = new ExecuteProfile(args[0]);
		executeProfile.loadProfile();
	}

}

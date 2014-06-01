import static com.sun.btrace.BTraceUtils.println;
import static com.sun.btrace.BTraceUtils.str;
import static com.sun.btrace.BTraceUtils.strcat;
import static com.sun.btrace.BTraceUtils.timeNanos;
import static com.sun.btrace.BTraceUtils.used;
import static com.sun.btrace.BTraceUtils.heapUsage;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import com.sun.btrace.AnyType;
import com.sun.btrace.annotations.*;

/**
 * Profile the execution of a Map-Reduce job on a hadoop cluster.
 * 
 * This class is logically split into three sections:
 * (a) Profile common task parts
 * (b) Profile map-related parts
 * (c) Profile reduce-related parts
 * 
 * It's not possible to split this class into multiple class because the
 * class cannot communicate with each other (i.e. common profiling cannot
 * be used by the map- or reduce-related parts). Also, writing to the same
 * file from multiple classes is not thread-safe.
 *  
 * @author hero
 */
@BTrace(unsafe=true)
public class HadoopBTrace {

	/*************************************************************************/
	/***************************** TEST        *******************************/
	/*************************************************************************/

	// @OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
	// 		method = "map", 
	// 		location = @Location(value = Kind.ENTRY))
	// public static void onJobClient_map_entry() {
	// 	println("---TEST MAPPER MAP");
	// }

	// @OnMethod(clazz = "org.apache.hadoop.mapreduce.JobSubmitter", 
	// 		method = "writeNewSplits", 
	// 		location = @Location(value = Kind.ENTRY))
	// public static void onJobSubmitter_writeNewSplits_entry(AnyType input) {
	// 	println("---TEST JOBSUBMITTER WRITENEWSPLITS");
	// }

	// @OnMethod(clazz = "org.apache.hadoop.mapred.gridmix.JobSubmitter", 
	// 		method = "run", 
	// 		location = @Location(value = Kind.ENTRY))
	// public static void onJobSubmitter_entry() {
	// 	println("---TEST JOBSUBMITTER RUN");
	// }
	
    /*************************************************************************/
	/***************************** TASK COMMON *******************************/
	/*************************************************************************/

// 	/* ***********************************************************
// 	 * TASK EXECUTION
// 	 * **********************************************************/
// 	@TLS private static long taskRunStartTime = 0l;

// 	@OnMethod(clazz = "org.apache.hadoop.mapred.Child", 
// 			method = "main", 
// 			location = @Location(where=Where.BEFORE, value = Kind.CALL, clazz="/.*/", method="run"))
// 	public static void onChild_main_Before_Call_run() {
// 		taskRunStartTime = timeNanos();
// 	}

// 	@OnMethod(clazz = "org.apache.hadoop.mapred.Child", 
// 			method = "main", 
// 			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="/.*/", method="run"))
// 	public static void onChild_main_After_Call_run() {
// 		println(strcat("TASK\tTOTAL_RUN\t", str(timeNanos() - taskRunStartTime)));
// 	}

// 	/* ***********************************************************
// 	 * HANDLE COMPRESSION
// 	 * **********************************************************/
//	@TLS private static long uncompressStartTime = 0l;
//	@TLS private static long compressStartTime = 0l;
	//They will always be zero;
	@TLS private static long uncompressDuration = 0l;
	@TLS private static long compressDuration = 0l;

// //	@OnMethod(clazz = "org.apache.hadoop.io.compress.DecompressorStream", 
// //			method = "decompress", 
// //			location = @Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="decompress"))
// //	public static void onDecompressorStream_Before_Call_uncompress() {
// //		uncompressStartTime = timeNanos();
// //	}
// //
// //	@OnMethod(clazz = "org.apache.hadoop.io.compress.DecompressorStream", 
// //			method = "decompress", 
// //			location = @Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="decompress"))
// //	public static void onDecompressorStream_After_Call_uncompress() {
// //		uncompressDuration += timeNanos() - uncompressStartTime;
// //	}
// //
// //	@OnMethod(clazz = "org.apache.hadoop.io.compress.CompressorStream", 
// //			method = "compress", 
// //			location = @Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="compress"))
// //	public static void onCompressorStream_Before_Call_uncompress() {
// //		compressStartTime = timeNanos();
// //	}
// //
// //	@OnMethod(clazz = "org.apache.hadoop.io.compress.CompressorStream", 
// //			method = "compress", 
// //			location = @Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="compress"))
// //	public static void onCompressorStream_After_Call_uncompress() {
// //		compressDuration += timeNanos() - compressStartTime;
// //	}

	
	/* ***********************************************************
OK	 * HANDLE MERGING
	 * **********************************************************/
	@TLS private static long mergerWriteFileDuration = 0l;
	@TLS private static int mergerWriteFileCount = 0;
	@TLS private static long mergerMergeDuration = 0l;
	
	// May not be executed, If this is only one file, we will execute sameVolRename and return (line 1807);
	@OnMethod(clazz = "org.apache.hadoop.mapred.Merger", 
			method = "writeFile",
			location = @Location(value = Kind.RETURN))
	public static void onMerger_writeFile_return(@Duration long duration) {
		mergerWriteFileDuration += duration;
		++mergerWriteFileCount;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.Merger",
			method = "merge",
			location = @Location(value = Kind.RETURN))
	public static void onMerger_merge_return(@Duration long duration) {
		mergerMergeDuration += duration;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "mergeParts", 
			location = @Location(value = Kind.ENTRY))
	public static void onMapOutputBuffer_mergeParts_entry() {
		mergerWriteFileDuration = 0l;
		mergerWriteFileCount = 0;
	}

	
	/* ***********************************************************
OK	 * PERFORM COMBINER
	 * **********************************************************/
	@TLS private static long combinerTotalDuration = 0l;
	@TLS private static long combinerWriteDuration = 0l;
	//Tem  add execution progressable.progress
	@OnMethod(clazz = "org.apache.hadoop.mapred.Task$CombineOutputCollector", 
			method = "collect", 
			location = @Location(value = Kind.RETURN))
	public static void onCombineOutputCollector_collect_return(@Duration long duration) {
		combinerWriteDuration += duration;
	}	

	@OnMethod(clazz = "org.apache.hadoop.mapred.Task$NewCombinerRunner", 
			method = "combine", 
			location = @Location(value = Kind.RETURN))
	public static void onNewCombinerRunner_combine_return(@Duration long duration) {
		combinerTotalDuration += duration;
	}	
	
    
	/*************************************************************************/
	/******************************* MAPPER **********************************/
	/*************************************************************************/

	
	/* ***********************************************************
OK	 * PERFORM MAPPER SETUP
	 * **********************************************************/
	@TLS private static long mapperSetupStartTime = 0l;

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Mapper", 
			  method="run", 
			  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="setup"))
	public static void onMapper_run_Before_Call_setup() {
		mapperSetupStartTime = timeNanos();
		println(strcat("MAP\tSTARTUP_MEM\t", str(used(heapUsage()))));
	}

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Mapper", 
			  method="run", 
			  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="setup"))
	public static void onMapper_run_After_Call_setup() {
		println(strcat("MAP\tSETUP\t", str(timeNanos() - mapperSetupStartTime)));
		println(strcat("MAP\tSETUP_MEM\t", str(used(heapUsage()))));
	}

	
	/* ***********************************************************
Tem	 * READ MAP INPUT
	 * **********************************************************/
	@TLS private static long mapInputDuration = 0l;

 
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(value = Kind.ENTRY))
	public static void onMapper_run_Entry(Mapper<?,?,?,?>.Context context) {
		// InputSplit split = context.getInputSplit();
		// if (split instanceof FileSplit) {
		// 	// println("MAP\t" + ((FileSplit) split).getPath() + "\t0");
		// 	println("MAP\t" + "LEGAL PATH" + "\t0");
		// } else {
		// 	println("MAP\tNOT_FILE_SPLIT\t0");
		// }
		println("MAP\tLEGAL PATH\t2");
	}
     
		
	
	@TLS private static long tempStartTime = 0l;

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "nextKeyValue"))
	public static void onMapper_run_Before_Call_nextKeyValue() {
		tempStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "nextKeyValue"))
	public static void onMapper_run_After_Call_nextKeyValue() {
		mapInputDuration += timeNanos() - tempStartTime;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "getCurrentKey"))
	public static void onMapper_run_Before_Call_getCurrentKey() {
		tempStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "getCurrentKey"))
	public static void onMapper_run_After_Call_getCurrentKey() {
		mapInputDuration += timeNanos() - tempStartTime;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "getCurrentValue"))
	public static void onMapper_run_Before_Call_getCurrentValue() {
		tempStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Mapper$Context", method = "getCurrentValue"))
	public static void onMapper_run_After_Call_getCurrentValue() {
		mapInputDuration += timeNanos() - tempStartTime;
	}


	
	/* ***********************************************************
OK	 * PERFORM MAP PROCESSING
	 * **********************************************************/
	@TLS private static long mapProcessingDuration = 0l;
	@TLS private static long mapProcessingStartTime = 0l;
	@TLS private static long mapInputKByteCount = 0l;
	@TLS private static long mapInputVByteCount = 0l;
	@TLS private static long mapInputPairCount = 0l;

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where=Where.BEFORE, value = Kind.CALL, clazz="/.*/", method="map"))
	public static void onMapper_run_Before_Call_map() {
		mapProcessingStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="/.*/", method="map"))
         
	public static void onMapper_run_After_Call_map(AnyType k, AnyType v, AnyType c) {
		try {
			if (k != null)
				mapInputKByteCount += k.toString().getBytes("UTF-8").length;
			if (v != null)
				mapInputVByteCount += v.toString().getBytes("UTF-8").length;
		}catch(Exception e) {}
		mapInputPairCount += 1l;
		mapProcessingDuration += timeNanos() - mapProcessingStartTime;
	}
        

	
	/* ***********************************************************
OK	 * WRITE INTERMEDIATE MAP OUTPUT
	 * **********************************************************/
	@TLS private static long mapCollectorWriteDuration = 0l;
	@TLS private static long mapBufferCollectStartTime = 0l;
	@TLS private static long mapBufferCollectDuration = 0l;
	@TLS private static long mapPartitionStartTime = 0l;
	@TLS private static long mapPartitionDuration = 0l;
	@TLS private static long mapOutputKByteCount = 0l;
	@TLS private static long mapOutputVByteCount = 0l;
	@TLS private static long mapOutputPairCount = 0l;


	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$NewOutputCollector", 
			method = "write", 
			location = @Location(value = Kind.RETURN))
	public static void onNewOutputCollector_write_return(@Duration long duration, AnyType k, AnyType v) {
		mapCollectorWriteDuration += duration;
		try {
			mapOutputPairCount += 1l;
			if (k != null)
				mapOutputKByteCount += k.toString().getBytes("UTF-8").length;
			if (v != null)
				mapOutputVByteCount += v.toString().getBytes("UTF-8").length;
		} catch (Exception e) {}
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$NewOutputCollector", 
			method = "write", 
			location = @Location(where=Where.BEFORE, value = Kind.CALL, clazz="org.apache.hadoop.mapreduce.Partitioner", method="getPartition"))
	public static void onNewOutputCollector_write_Before_Call_getPartition() {
		mapPartitionStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$NewOutputCollector", 
			method = "write", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="org.apache.hadoop.mapreduce.Partitioner", method="getPartition"))
	public static void onNewOutputCollector_write_After_Call_getPartition() {
		mapPartitionDuration += timeNanos() - mapPartitionStartTime;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "collect", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="/.*/", method="checkSpillException"))
	public static void onMapOutputBuffer_collect_after_await() {
		mapBufferCollectStartTime = timeNanos();
	}

	// This spillLock.unlock() method may not be called, that's why we trace checkSpillException() method above;
	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "collect", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="java.util.concurrent.locks.ReentrantLock", method="unlock"))
	public static void bonMapOutputBuffer_collect_after_await() {
		mapBufferCollectStartTime = timeNanos();
	}
	
	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "collect", 
			location = @Location(value = Kind.RETURN))
	public static void conMapOutputBuffer_collect_return() {
		mapBufferCollectDuration += timeNanos() - mapBufferCollectStartTime;
	}

	
	/* ***********************************************************
OK	 * WRITE DIRECT MAP OUTPUT
	 * **********************************************************/
	@TLS private static long mapDirectOutputDuration = 0l;


	// We execute NewOutputCollector above or NewDirectOutputCollector if reduceTasks == 0
	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$NewDirectOutputCollector", 
			method = "write", 
			location = @Location(value = Kind.RETURN))
	public static void onNewDirectOutputCollector_write_return(@Duration long duration, AnyType k, AnyType v) {
		mapCollectorWriteDuration += duration;
 		try {
 			mapOutputPairCount += 1l;
			if (k != null)
				mapOutputKByteCount += k.toString().getBytes("UTF-8").length;
			if (v != null)
				mapOutputVByteCount += v.toString().getBytes("UTF-8").length;
		} catch (Exception e) {}
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$NewDirectOutputCollector", 
			method = "close", 
			location = @Location(value = Kind.RETURN))
	public static void onNewDirectOutputCollector_close_return(@Duration long duration) {
		mapDirectOutputDuration = duration;
	}
	
	/* ***********************************************************
OK	 * PERFORM MAPPER CLEANUP
	 * **********************************************************/
	@TLS private static long mapCleanupStartTime = 0l;

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Mapper", 
			  method="run", 
			  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="cleanup"))
	public static void onMapper_run_Before_Call_cleanup() {
		mapCleanupStartTime = timeNanos();
	}

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Mapper", 
			  method="run", 
			  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="cleanup"))
	public static void onMapper_run_After_Call_cleanup() {
		println(strcat("MAP\tCLEANUP\t", str(timeNanos() - mapCleanupStartTime)));
		println(strcat("MAP\tCLEANUP_MEM\t", str(used(heapUsage()))));
	}

	
	/* ***********************************************************
OK	 * PERFORM MAP SPILL
	 * **********************************************************/
	@TLS private static long sortDuration = 0l;
	@TLS private static int sortNumRecs = 0;
	@TLS private static long spillRawByteCount = 0l;
	@TLS private static long spillCompressedByteCount = 0l;

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "sortAndSpill", 
			location = @Location(value = Kind.ENTRY))
	public static void onMapOutputBuffer_sortAndSpill_entry() {
		combinerTotalDuration = 0l;
		combinerWriteDuration = 0l;
		compressDuration = 0l;
		spillRawByteCount = 0l;
		spillCompressedByteCount = 0l;
	}
	
	@OnMethod(clazz = "org.apache.hadoop.util.QuickSort", 
			method = "sort", 
			location = @Location(value = Kind.RETURN))
	public static void onQuickSort_sort_return(@Duration long duration, 
			AnyType s, int l, int r, AnyType rep) {
		sortDuration = duration;
		sortNumRecs = r-l;
	}
	
	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "sortAndSpill", 
		    location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="getRawLength"))
	public static void onMapOutputBuffer_sortAndSpill_getRawLength(@Return long length) {
		spillRawByteCount += length;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "sortAndSpill", 
		    location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="getCompressedLength"))
	public static void onMapOutputBuffer_sortAndSpill_getCompressedLength(@Return long length) {
		spillCompressedByteCount += length;
	}
	
	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "sortAndSpill", 
			location = @Location(value = Kind.RETURN))
	public static void onMapOutputBuffer_sortAndSpill_return(@Duration long duration) {
		println(strcat("SPILL\tSORT_AND_SPILL\t", str(duration)));
		println(strcat("SPILL\tQUICK_SORT\t", str(sortDuration)));
		println(strcat("SPILL\tSORT_COUNT\t", str(sortNumRecs)));
		println(strcat("SPILL\tCOMBINE\t", str(combinerTotalDuration)));
		if (combinerTotalDuration == 0)
			println(strcat("SPILL\tWRITE\t", str(duration - sortDuration)));
		else
			println(strcat("SPILL\tWRITE\t", str(combinerWriteDuration)));
		println(strcat("SPILL\tCOMPRESS\t", str(compressDuration)));
		println(strcat("SPILL\tUNCOMPRESS_BYTE_COUNT\t", str(spillRawByteCount)));
		println(strcat("SPILL\tCOMPRESS_BYTE_COUNT\t", str(spillCompressedByteCount)));

		combinerTotalDuration = 0l;
		combinerWriteDuration = 0l;
		compressDuration = 0l;
	}	

	
	/* ***********************************************************
OK	 * PERFORM MERGE OF INTERMEDIATE OUTPUT
	 * **********************************************************/

	@OnMethod(clazz = "org.apache.hadoop.mapred.MapTask$MapOutputBuffer", 
			method = "mergeParts", 
			location = @Location(value = Kind.RETURN))
	public static void onMapOutputBuffer_mergeParts_return(@Duration long duration) {
		println(strcat("MERGE\tTOTAL_MERGE\t", str(duration)));
		println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
		println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
		println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
		println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));

		uncompressDuration = 0l;
		compressDuration = 0l;
	}


	/* ***********************************************************
OK	 * DONE WITH MAPPER EXECUTION
	 * **********************************************************/
	@TLS private static boolean onMapper = false;

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Mapper", 
			method = "run", 
			location = @Location(value = Kind.RETURN))
	public static void onMapper_run_return(@Duration long duration) {
		// Print out the map statistics
		println(strcat("MAP\tTOTAL_RUN\t", str(duration)));
		println(strcat("MAP\tREAD\t", str(mapInputDuration)));
		println(strcat("MAP\tUNCOMPRESS\t", str(uncompressDuration)));
		println(strcat("MAP\tKEY_BYTE_COUNT\t", str(mapInputKByteCount)));
		println(strcat("MAP\tVALUE_BYTE_COUNT\t", str(mapInputVByteCount)));
		println(strcat("MAP\tINPUT_PAIR_COUNT\t", str(mapInputPairCount)));
		println(strcat("MAP\tMAP\t", str(mapProcessingDuration)));
		println(strcat("MAP\tWRITE\t", str(mapCollectorWriteDuration)));
		println(strcat("MAP\tCOMPRESS\t", str(compressDuration)));
		println(strcat("MAP\tPARTITION_OUTPUT\t", str(mapPartitionDuration)));
		println(strcat("MAP\tSERIALIZE_OUTPUT\t", str(mapBufferCollectDuration)));
		println(strcat("MAP\tMAP_MEM\t", str(used(heapUsage()))));

		onMapper = true;
		uncompressDuration = 0l;
		compressDuration = 0l;
	}
	
	// If reduceTasks != 0, mapDirectOutputDuration,mapOutputKByteCount,mapOutputVByteCount = 0;
	@OnMethod(clazz = "org.apache.hadoop.mapred.Task", 
			method = "done", 
			location = @Location(value = Kind.ENTRY))
	public static void onMapTask_runNewMapper_return() {
		if (onMapper) {
			println(strcat("MAP\tWRITE\t", str(mapDirectOutputDuration)));
			println(strcat("MAP\tCOMPRESS\t", str(compressDuration)));
			println(strcat("MAP\tKEY_BYTE_COUNT\t", str(mapOutputKByteCount)));
			println(strcat("MAP\tVALUE_BYTE_COUNT\t", str(mapOutputVByteCount)));
			println(strcat("MAP\tOUTPUT_PAIR_COUNT\t", str(mapOutputPairCount)));
		}
	}
	
 /*************************************************************************/
	/******************************* REDUCER *********************************/
	/*************************************************************************/
	
	// We can only model Shuffle phase that uses default ShuffleConsumerPlugin and AuxiliaryService
	// that is org.apache.hadoop.task.reduce.Shuffle and org.apache.hadoop.mapred.ShuffleHandler
	/* ***********************************************************
My	 * SHUFFLE MAP OUTPUT TO REDUCER
	 * **********************************************************/
	@TLS private static boolean onReducer = false;
	@TLS private static long shuffleUncomprByteCount = 0l;
	@TLS private static long shuffleComprByteCount = 0l;
	@TLS private static long copyDataDuration = 0l;
	@TLS private static long tempCopyStartTime = 0l;
	// @TLS private static long doMergeStartTime = 0l;
	@TLS private static long doInMemMergeDuration = 0l;
	@TLS private static long doOnDiskMergeDuration = 0l;

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier", 
	// 		method = "fetchOutputs", 
	// 		location = @Location(value = Kind.RETURN))
	// public static void onReduceCopier_fetchOutputs_return(@Duration long duration) {
	// 	onReducer = true;
	// 	uncompressDuration = 0l;
	// 	compressDuration = 0l;
	// }
	// Only normal reduce task will execute initCodec
	// this is how we distinguish normal reduce task to jobcleanup,setup,taskcleanup task
	@OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask", 
			  method="run", 
			  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="initCodec"))
	public static void onReducerTask_run_Before_Call_initCodec() {
		onReducer = true;
		uncompressDuration = 0l;
		compressDuration = 0l;
		mergerWriteFileCount = 0;
		mergerWriteFileDuration = 0l;
		combinerTotalDuration = 0l;
		combinerWriteDuration = 0l;
	}

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$MapOutputCopier", 
	// 		  method="getMapOutput", 
	// 		  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="java.lang.Long", method="parseLong"))
	// public static void onReducerCopier_getMapOutput_Before_Call_parseLong(String s) {
	// 	if (toggleByteCount == 0)
	// 		shuffleUncomprByteCount = s;
	// 	else
	// 		shuffleComprByteCount = s;
	// 	toggleByteCount = 1 - toggleByteCount;
	// }

	//copyDataDuration is the time to copy data from other nodes to get MapOutput
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.Fetcher", 
		  method="copyMapOutput", 
		  location=@Location(value=Kind.ENTRY))
	public static void onFetcher_copyMapOutput_entry() {
		tempCopyStartTime = timeNanos();
	}


	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.Fetcher", 
			  method="copyMapOutput", 
			  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="shuffle"))
	public static void onFetcher_copyMapOutput_After_Call_shuffle(AnyType a, AnyType b, long c, long d, AnyType e, AnyType f) {
		// a = compressedLength, b = decompressedLength
		shuffleUncomprByteCount += c;
		shuffleComprByteCount += d;
		copyDataDuration += timeNanos() - tempCopyStartTime;
	}

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask", 
	// 		  method="getMapOutput", 
	// 		  location=@Location(value = Kind.RETURN))
	// public static void onReducerCopier_getMapOutput_return(@Duration long duration) {
	// 	String out = strcat("SHUFFLE\tUNCOMPRESS_BYTE_COUNT\t", shuffleUncomprByteCount);
	// 	out += strcat("\nSHUFFLE\tCOMPRESS_BYTE_COUNT\t", shuffleComprByteCount);
	// 	out += strcat("\nSHUFFLE\tCOPY_MAP_DATA\t", str(duration));
	// 	out += strcat("\nSHUFFLE\tUNCOMPRESS\t", str(uncompressDuration));
	// 	println(out);
		
	// 	uncompressDuration = 0l;
	// }
	// @OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.Fetcher", 
	// 		  method="run", 
	// 		  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="copyFromHost"))
	// public static void onFetcher_run_Before_Call_copyFromHost() {
	// 	copyDataDuration = timeNanos();
	// }

	//When we finish copyFromHost , output 
	// @OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.ShuffleSchedulerImpl", 
	// 		  method="freeHost", 
	// 		  location=@Location(value=Kind.ENTRY))
	// public static void onShuffleSchedulerImpl_freeHost_entry() throws InterruptedException{
	// 	try {
	// 	String out = strcat("SHUFFLE\tUNCOMPRESS_BYTE_COUNT\t", str(shuffleUncomprByteCount));
	// 	out += strcat("\nSHUFFLE\tCOMPRESS_BYTE_COUNT\t", str(shuffleComprByteCount));
	// 	out += strcat("\nSHUFFLE\tCOPY_MAP_DATA\t", str(copyDataDuration));
	// 	out += strcat("\nSHUFFLE\tUNCOMPRESS\t", str(uncompressDuration));
	// 	println(out);
	// 	}
	// 	catch (InterruptedException e) {
	// 		throw(e);
	// 	}
	// 	uncompressDuration = 0l;
	// }
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.Fetcher", 
			  method="run", 
			  location=@Location(value=Kind.CALL, where=Where.AFTER, clazz="/.*/", method="copyFromHost"))
	public static void onShuffleSchedulerImpl_freeHost_entry(){
		// try {
			String out = strcat("SHUFFLE\tUNCOMPRESS_BYTE_COUNT\t", str(shuffleUncomprByteCount));
			out += strcat("\nSHUFFLE\tCOMPRESS_BYTE_COUNT\t", str(shuffleComprByteCount));
			out += strcat("\nSHUFFLE\tCOPY_MAP_DATA\t", str(copyDataDuration));
			out += strcat("\nSHUFFLE\tUNCOMPRESS\t", str(uncompressDuration));
			println(out);
		// }
		// catch (InterruptedException e) {
			// return;
		// }
		uncompressDuration = 0l;
	}


	/* ***********************************************************
WA	 * SORT/MERGE DURING SHUFFLING
	 * **********************************************************/

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$InMemFSMergeThread", 
	// 		method = "doInMemMerge", 
	// 		location = @Location(value = Kind.RETURN))
	// public static void onInMemFSMergeThread_doInMemMerge_return(@Duration long duration) {
	// 	doInMemMergeDuration += duration;
	// }
	
	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$InMemFSMergeThread", 
	// 		method = "run", 
	// 		location = @Location(value = Kind.RETURN))
	// public static void onInMemFSMergeThread_run_return() {
	// 	if (doInMemMergeDuration != 0l) {
	// 		println(strcat("MERGE\tMERGE_IN_MEMORY\t", str(doInMemMergeDuration)));
	// 		println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
	// 		println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
	// 		println(strcat("MERGE\tCOMBINE\t", str(combinerTotalDuration)));
	// 		println(strcat("MERGE\tWRITE\t", str(combinerWriteDuration)));
	// 		println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
	// 		println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));
	// 	}
	// }

	// can not find class InMemoryMerger
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl$InMemoryMerger",
			method = "merge",
			location = @Location(value=Kind.RETURN))
	public static void onInMemoryMerger_merge_return(@Duration long duration) {
		doInMemMergeDuration += duration;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl$InMemoryMerger",
			method = "close",
			location = @Location(value=Kind.RETURN))
	public static void onInMemoryMerger_close_return(){
		// if (doInMemMergeDuration != 0l) {
			println(strcat("MERGE\tMERGE_IN_MEMORY\t", str(doInMemMergeDuration)));
			println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
			println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
			println(strcat("MERGE\tCOMBINE\t", str(combinerTotalDuration)));
			println(strcat("MERGE\tWRITE\t", str(combinerWriteDuration)));
			println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
			println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));
		// }
		mergerWriteFileDuration = 0l;
		mergerWriteFileCount = 0;
		combinerTotalDuration = 0l;
		combinerWriteDuration = 0l;
		uncompressDuration = 0l;
		compressDuration = 0l;
	}


	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$LocalFSMerger", 
	// 		method = "run", 
	// 		  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="getLocalPathForWrite"))
	// public static void onLocalFSMerger_run_Before_Merge() {
	// 	doOnDiskMergeStartTime = timeNanos();
	// }

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$LocalFSMerger", 
	// 		method = "run", 
	// 		  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="addToMapOutputFilesOnDisk"))
	// public static void onLocalFSMerger_run_After_Merge() {
	// 	doOnDiskMergeDuration += timeNanos() - doOnDiskMergeStartTime;
	// }

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier$LocalFSMerger", 
	// 		method = "run", 
	// 		location = @Location(value = Kind.RETURN))
	// public static void onLocalFSMerger_run_return() {
	// 	if (doOnDiskMergeDuration != 0l) {
	// 		println(strcat("MERGE\tMERGE_TO_DISK\t", str(doOnDiskMergeDuration)));
	// 		println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
	// 		println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
	// 		println(strcat("MERGE\tCOMBINE\t", str(combinerTotalDuration)));
	// 		println(strcat("MERGE\tWRITE\t", str(combinerWriteDuration)));
	// 		println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
	// 		println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));
	// 	}
	// }

	// can't trace yet, In debug output, btrace indeed instruments OnDiskMerger, but never trace any of its methods;
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl$OnDiskMerger", 
				method = "merge", 
				  location=@Location(value=Kind.RETURN))
	public static void onOnDiskMerger_merge_return(@Duration long duration) {
		doOnDiskMergeDuration += duration;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl$OnDiskMerger",
				method = "close",
				location = @Location(value=Kind.RETURN))
	public static void onOnDiskMerger_close_return(){
		// if (doOnDiskMergeDuration != 0l) {
			println(strcat("MERGE\tMERGE_TO_DISK\t", str(doOnDiskMergeDuration)));
			println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
			println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
			println(strcat("MERGE\tCOMBINE\t", str(combinerTotalDuration)));
			println(strcat("MERGE\tWRITE\t", str(combinerWriteDuration)));
			println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
			println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));
		// }
		mergerWriteFileDuration = 0l;
		mergerWriteFileCount = 0;
		combinerTotalDuration = 0l;
		combinerWriteDuration = 0l;
		uncompressDuration = 0l;
		compressDuration = 0l;
	}

	
	/* ***********************************************************
OK	 * SORT/MERGE MAP OUTPUT DATA
	 * **********************************************************/

	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier", 
	// 		method = "createKVIterator", 
	// 		location = @Location(value = Kind.ENTRY))
	// public static void onReduceCopier_createKVIterator_entry() {
	// 	if (onReducer) {
	// 		mergerWriteFileCount = 0;
	// 		mergerWriteFileDuration = 0;
	// 	}
	// }

 	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl", 
			method = "finalMerge", 
			location = @Location(value = Kind.ENTRY))
	public static void onMergeManagerImpl_finalMerge_entry() {
		if (onReducer) {
			println(strcat("MERGE\tMERGE_IN_MEMORY\t", str(mergerMergeDuration)));
			println(strcat("MERGE\tREAD_WRITE\t", str(mergerWriteFileDuration)));
			println(strcat("MERGE\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
			println(strcat("MERGE\tCOMBINE\t", str(combinerTotalDuration)));
			println(strcat("MERGE\tWRITE\t", str(combinerWriteDuration)));
			println(strcat("MERGE\tUNCOMPRESS\t", str(uncompressDuration)));
			println(strcat("MERGE\tCOMPRESS\t", str(compressDuration)));

			mergerMergeDuration = 0l;
			combinerTotalDuration = 0l;
			combinerWriteDuration = 0l;
			uncompressDuration = 0l;
			compressDuration = 0l;
			mergerWriteFileCount = 0;
			mergerWriteFileDuration = 0l;
		}
	}
	
	// @OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$ReduceCopier", 
	// 		method = "createKVIterator", 
	// 		location = @Location(value = Kind.RETURN))
	// public static void onReduceCopier_createKVIterator_return(@Duration long duration) {
	// 	if (onReducer) {
	// 		println(strcat("SORT\tMERGE_MAP_DATA\t", str(duration)));
	// 		println(strcat("SORT\tREAD_WRITE\t", str(mergerWriteFileDuration)));
	// 		println(strcat("SORT\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
	// 		println(strcat("SORT\tUNCOMPRESS\t", str(uncompressDuration)));
	// 		println(strcat("SORT\tCOMPRESS\t", str(compressDuration)));

	// 		uncompressDuration = 0l;
	// 		compressDuration = 0l;
	// 	}
	// }
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.task.reduce.MergeManagerImpl", 
				method = "finalMerge", 
				location = @Location(value = Kind.RETURN))
	public static void onMergeManagerImpl_finalMerge_return(@Duration long duration) {
		if (onReducer) {
			println(strcat("SORT\tMERGE_MAP_DATA\t", str(duration)));
			println(strcat("SORT\tREAD_WRITE\t", str(mergerWriteFileDuration)));
			println(strcat("SORT\tREAD_WRITE_COUNT\t", str(mergerWriteFileCount)));
			println(strcat("SORT\tUNCOMPRESS\t", str(uncompressDuration)));
			println(strcat("SORT\tCOMPRESS\t", str(compressDuration)));

			mergerWriteFileDuration = 0l;
			mergerWriteFileCount = 0;
			uncompressDuration = 0l;
			compressDuration = 0l;
		}
	}
	
	
	/* ***********************************************************
OK	 * PERFORM REDUCER SETUP
	 * **********************************************************/
	@TLS private static long reducerSetupStartTime = 0l;

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Reducer", 
			  method="run", 
			  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="setup"))
	public static void onReducer_run_Before_Call_setup() {
		if (onReducer) {
			reducerSetupStartTime = timeNanos();
			println(strcat("REDUCE\tSTARTUP_MEM\t", str(used(heapUsage()))));
		}
	}

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Reducer", 
			  method="run", 
			  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="setup"))
	public static void onReducer_run_After_Call_setup() {
		if (onReducer) {
			println(strcat("REDUCE\tSETUP\t", 
					str(timeNanos() - reducerSetupStartTime)));
			println(strcat("REDUCE\tSETUP_MEM\t", str(used(heapUsage()))));
		}
	}
	
	
	/* ***********************************************************
OK	 * READ REDUCER INPUT
	 * **********************************************************/
	@TLS private static long reduceInputStartTime = 0l;
	@TLS private static long reduceInputDuration = 0l;

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "nextKey"))
	public static void onReducer_run_Before_Call_nextKeyValue() {
		reduceInputStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "nextKey"))
	public static void onReducer_run_After_Call_nextKeyValue() {
		reduceInputDuration += timeNanos() - reduceInputStartTime;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "getCurrentKey"))
	public static void onReducer_run_Before_Call_getCurrentKey() {
		reduceInputStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "getCurrentKey"))
	public static void onReducer_run_After_Call_getCurrentKey() {
		reduceInputDuration += timeNanos() - reduceInputStartTime;
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.BEFORE, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "getValues"))
	public static void onReducer_run_Before_Call_getValues() {
		reduceInputStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where = Where.AFTER, value = Kind.CALL, clazz = "org.apache.hadoop.mapreduce.Reducer$Context", method = "getValues"))
	public static void onReducer_run_After_Call_getValues() {
		reduceInputDuration += timeNanos() - reduceInputStartTime;
	}

	
	/* ***********************************************************
OK	 * PERFORM REDUCE PROCESSING
	 * **********************************************************/
	@TLS private static long reduceProcessingDuration = 0l;
	@TLS private static long reduceProcessingStartTime = 0l;

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where=Where.BEFORE, value = Kind.CALL, clazz="/.*/", method="reduce"))
	public static void onReducer_run_Before_Call_reduce() {
		if (onReducer)
			reduceProcessingStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="/.*/", method="reduce"))
	public static void onReducer_run_After_Call_reduce() {
		if (onReducer)
			reduceProcessingDuration += timeNanos() - reduceProcessingStartTime;
	}

	
	/* ***********************************************************
OK	 * WRITE REDUCER OUTPUT
	 * **********************************************************/
	@TLS private static long reduceWriteDuration = 0l;
	@TLS private static long reduceWriterCloseStartTime = 0l;
	@TLS private static long reduceWriteKByteCount = 0l;
	@TLS private static long reduceWriteVByteCount = 0l;
	
     
	@OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask$NewTrackingRecordWriter", 
			method = "write", 
			location = @Location(value = Kind.RETURN))
	public static void onNewTrackingRecordWriter_write_return(@Duration long duration, AnyType k, AnyType v) {
		if (onReducer) {
			reduceWriteDuration += duration;
			try {
				if (k != null)
					reduceWriteKByteCount += k.toString().getBytes("UTF-8").length;
				if (v != null)
					reduceWriteVByteCount += v.toString().getBytes("UTF-8").length;
			} catch (Exception e) {}
		}
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask", 
			method = "runNewReducer", 
			location = @Location(where=Where.BEFORE, value = Kind.CALL, clazz="/.*/", method="close"))
	public static void onReduceTask_runNewReducer_Before_Call_close() {
		if (onReducer)
			reduceWriterCloseStartTime = timeNanos();
	}

	@OnMethod(clazz = "org.apache.hadoop.mapred.ReduceTask", 
			method = "runNewReducer", 
			location = @Location(where=Where.AFTER, value = Kind.CALL, clazz="/.*/", method="close"))
	public static void onReduceTask_runNewReducer_After_Call_close() {
		if (onReducer) {
			println(strcat("REDUCE\tWRITE\t", str(timeNanos() - reduceWriterCloseStartTime)));
			println(strcat("REDUCE\tCOMPRESS\t", str(compressDuration)));
			compressDuration = 0l;
		}
	}


	/* ***********************************************************
OK	 * PERFORM REDUCER CLEANUP
	 * **********************************************************/
	@TLS private static long reducerCleanupStartTime = 0l;

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Reducer", 
			  method="run", 
			  location=@Location(where=Where.BEFORE, value=Kind.CALL, clazz="/.*/", method="cleanup"))
	public static void onReducer_run_Before_Call_cleanup() {
		if (onReducer)
			reducerCleanupStartTime = timeNanos();
	}

	@OnMethod(clazz="org.apache.hadoop.mapreduce.Reducer", 
			  method="run", 
			  location=@Location(where=Where.AFTER, value=Kind.CALL, clazz="/.*/", method="cleanup"))
	public static void onReducer_run_After_Call_cleanup() {
		if (onReducer) {
			println(strcat("REDUCE\tCLEANUP\t", str(timeNanos() - reducerCleanupStartTime)));
			println(strcat("REDUCE\tCLEANUP_MEM\t", str(used(heapUsage()))));
		}
	}

	
	/* ***********************************************************
OK	 * DONE WITH REDUCER EXECUTION
	 * **********************************************************/
	
	@OnMethod(clazz = "org.apache.hadoop.mapreduce.Reducer", 
			method = "run", 
			location = @Location(value = Kind.RETURN))
	public static void onReducer_run_return(@Duration long duration) {
		// Print out the reducer statistics
		if (onReducer) {
			println(strcat("REDUCE\tTOTAL_RUN\t", str(duration)));
			println(strcat("REDUCE\tREAD\t", str(reduceInputDuration)));
			println(strcat("REDUCE\tUNCOMPRESS\t", str(uncompressDuration)));
			println(strcat("REDUCE\tREDUCE\t", str(reduceProcessingDuration)));
			println(strcat("REDUCE\tWRITE\t", str(reduceWriteDuration)));
			println(strcat("REDUCE\tCOMPRESS\t", str(compressDuration)));
			println(strcat("REDUCE\tKEY_BYTE_COUNT\t", str(reduceWriteKByteCount)));
			println(strcat("REDUCE\tVALUE_BYTE_COUNT\t", str(reduceWriteVByteCount)));
			println(strcat("REDUCE\tREDUCE_MEM\t", str(used(heapUsage()))));

			uncompressDuration = 0l;
			compressDuration = 0l;
		}
	}

}


package handist.moldyn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Timer {
	
	private static long time_ns;
	private static boolean isVerbose = false;

	public static enum Phase {
		domove,
		interForces,
		reduceForces,
		reduceParams,
		scaleForces,
		averVelocity,
		tempScale
	}
	private transient static final HashMap<Phase, Long> results = new HashMap<>();

	
	/**
	 * Create file and write header
	 * 
	 * iter,domove,interForces,reduceForces,reduceParams,scaleForces,averVelocity, tempScale
	 * */
	public static void fileSetup(String filePath) {
		if(!isVerbose) return;
		try {
			File file = new File(filePath);
			if(file.exists()){
				return;
			}
			file.createNewFile();
			
			FileWriter fw = new FileWriter(filePath, true);
			fw.write("iter");
			for(Phase p : Phase.values()) {
				fw.write("," + p.toString());
			}
			fw.write("\n");
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void turnVerboseMode(boolean flag) {
		isVerbose = flag;
	}

	/**
	 * Start time_ns measurement.
	 * */
	public static void start() {
		time_ns = System.nanoTime();
	}
	
	/**
	 * Finish time_ns measurement. Result is written file when {@link Timer#write} is called.
	 * 
	 * */
	public static void finish(Phase phase) {
		if(!isVerbose) return;
		results.put(phase, (System.nanoTime() - time_ns));
		time_ns = -1;
	}
	
	/**
	 * 
	 * */
	public static void write(String filePath, int iter) {
		if(!isVerbose) return;
		try {
			FileWriter fw = new FileWriter(filePath, true);
			fw.write(String.format("%d", iter + 1));
			for(Phase tag : Phase.values()) {
				Long t = results.get(tag);
				if(t == null)
					t = -2l;
				fw.write("," + t);
			}
			fw.write("\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			results.clear();
		}
	}
	
	public static void clear() {
		results.clear();
		time_ns = -1;
	}
	
}

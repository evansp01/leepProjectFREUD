package consoleThings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A Class to keep track of the settings of the current project
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class Settings {
    public final int days;
    public final int blocks;
    public final boolean largeConstraint;
    public final boolean facultyConstraint;
    public final boolean[][] backToBack;
    public static final int MAX_BACK_TO_BACK = 1, LARGE = 50;
    public static final int MAX_EXAM_PER_DAY = 2;

    /**
     * creates a settings file which stores all project settings
     * 
     * @param days
     * @param blocks
     * @param largeConstraint
     * @param facultyConstraint
     * @param backToBack
     */
    public Settings(int days, int blocks, boolean largeConstraint, boolean facultyConstraint, boolean[][] backToBack) {
	this.days = days;
	this.blocks = blocks;
	this.largeConstraint = largeConstraint;
	this.facultyConstraint = facultyConstraint;
	this.backToBack = backToBack;
    }

    public static boolean isBackToBack(int block1, int block2, Settings s) {
	return s.backToBack[block1][block2];
    }

    /**
     * Parses a settings file
     * 
     * @param settingsFile
     * @return Object -- an error string on failure, or a Settings class on
     *         success
     */
    public static Object parseSettings(File settingsFile) {
	BufferedReader settingsReader = null;
	int days = -1;
	int blocks = -1;
	int facConstraint = -1;
	int largeConstraint = -1;
	ArrayList<int[]> backToBack = new ArrayList<>();
	boolean[][] backToBackArray = null;

	String line = null;
	int lineNum = 0;
	try {
	    settingsReader = new BufferedReader(new FileReader(settingsFile));
	    while ((line = settingsReader.readLine()) != null) {
		lineNum++;
		line = line.replaceAll("\\s+", ""); //remove all whitespace
		if (line.contains("#"))
		    line = line.substring(0, line.indexOf("#"));
		if (line.length() == 0)
		    continue;
		//at this point we have what should be a valid instruction
		String[] parts = line.split("=");
		if (parts.length != 2)
		    return "too many equals characters on line " + lineNum;
		String label = parts[0].toLowerCase();
		String value = parts[1];
		switch (label) {
		case "examdays":
		    days = Integer.parseInt(value);
		    break;
		case "examblocks":
		    blocks = Integer.parseInt(value);
		    break;
		case "backtoback":
		    String[] backToBackBlocks = value.split(":");
		    if (backToBackBlocks.length != 2)
			return "unexpected BACKTOBACKBLOCK input on line " + lineNum;
		    int[] pair = { Integer.parseInt(backToBackBlocks[0]), Integer.parseInt(backToBackBlocks[1]) };
		    backToBack.add(pair);
		    break;
		case "facultyconstraint":
		    if ("TRUE".equalsIgnoreCase(value))
			facConstraint = 1;
		    else if ("FALSE".equalsIgnoreCase(value))
			facConstraint = 0;
		    else
			return "unexpected value for FACULTYCONSTRAINT on line " + lineNum;
		    break;
		case "largeexamconstraint":
		    if ("TRUE".equalsIgnoreCase(value))
			largeConstraint = 1;
		    else if ("FALSE".equalsIgnoreCase(value))
			largeConstraint = 0;
		    else
			return "unexpected value for LARGEEXAMCONSTRAINT on line " + lineNum;
		    break;
		default:
		    return "unexpected label on line " + lineNum;
		}

	    }

	} catch (FileNotFoundException e) {
	    return "Could not open settings file";
	} catch (IOException e) {
	    return "error while reading from settings file";
	} catch (NumberFormatException e) {
	    return "unexpected value on line " + lineNum;
	} finally {
	    if (settingsReader != null)
		try {
		    settingsReader.close();
		} catch (IOException e) {
		}
	}
	if (days <= 0)
	    return "EXAMDAYS must be defined as a positive integer";
	if (blocks <= 0)
	    return "EXAMBLOCKS must be defined as a positive integer";
	if (!(facConstraint == 0 || facConstraint == 1))
	    return "FACULTYCONSTRAINT must be defined as TRUE or FALSE";
	if (!(largeConstraint == 0 || largeConstraint == 1))
	    return "LARGEEXAMCONSTRAINT must be defined as TRUE or FALSE";

	backToBackArray = new boolean[blocks][blocks];

	for (int[] btb : backToBack) {
	    if (btb[0] < 1 || btb[0] > blocks)
		return "BACKTOBACK values must be between 1 and EXAMBLOCKS";
	    backToBackArray[btb[0] - 1][btb[1] - 1] = true;
	    backToBackArray[btb[1] - 1][btb[0] - 1] = true;
	}
	Settings settings = new Settings(days, blocks, largeConstraint == 1, facConstraint == 1, backToBackArray);
	return settings;
    }

}

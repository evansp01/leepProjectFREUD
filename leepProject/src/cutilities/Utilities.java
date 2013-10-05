package cutilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.h2.store.fs.FileUtils;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class Utilities {

    /**
     * Prints one day of the current finals schedule in pretty formatting
     * 
     * @param dayRS
     * @param day
     * @param crossList
     * @return
     * @throws SQLException
     */
    public static int printDay(ResultSet[] dayRS, int day, int crossList) throws SQLException {
	int blocks = dayRS.length;
	@SuppressWarnings("unchecked")
	//an array of queues to put elements from the resultSets into
	Queue<String>[] crns = new LinkedList[blocks];

	//iterates over the result sets and puts them into the queues. Also breaks up cross listed courses 
	//from hyphenated format to single crn with a cross list number
	for (int list = 0; list < blocks; list++) {
	    crns[list] = new LinkedList<String>();
	    while (dayRS[list].next()) {
		//split on hypen
		String[] crn = dayRS[list].getString(1).split("-", -1);
		if (crn.length > 1) {
		    //make new crns
		    for (int i = 0; i < crn.length; i++)
			crn[i] = "(" + crossList + ")" + crn[i];
		    crossList++;
		}
		crns[list].addAll(Arrays.asList(crn));
	    }
	}
	//builds the formatted string to print with
	StringBuilder headers = new StringBuilder("|%-3s |");
	String[] values = new String[blocks + 1];
	values[0] = "Day";
	for (int i = 0; i < blocks; i++) {
	    headers.append("%9s|");
	    values[i + 1] = "Block " + (i + 1);
	}
	headers.append("%n");
	String rowTemplate = headers.toString();
	boolean needMoreRows = true;
	boolean first = true;
	//while there are things in any queue print a new row
	do {
	    needMoreRows = false;
	    System.out.format(rowTemplate, (Object[]) values);
	    for (int i = 1; i < blocks + 1; i++) {
		if (!crns[i - 1].isEmpty()) {
		    values[i] = crns[i - 1].poll();
		    needMoreRows = true;
		} else {
		    values[i] = "";
		}
	    }
	    if (first) {
		values[0] = "" + (day + 1);
		first = false;
	    }
	} while (needMoreRows);
	return crossList;
    }

    /**
     * Copies the source file to the destination
     * 
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
	if (!destFile.exists()) {
	    destFile.createNewFile();
	}

	FileChannel source = null;
	FileChannel destination = null;

	try {
	    source = new FileInputStream(sourceFile).getChannel();
	    destination = new FileOutputStream(destFile).getChannel();
	    destination.transferFrom(source, 0, source.size());
	} finally {
	    if (source != null) {
		source.close();
	    }
	    if (destination != null) {
		destination.close();
	    }
	}
    }

    /**
     * A recursive method to delete an existing directory
     * 
     * @param rootDir
     */
    public static void deleteDir(File rootDir) {
	File[] childDirs = rootDir.listFiles();
	for (int i = 0; i < childDirs.length; i++) {
	    if (childDirs[i].isFile()) {
		childDirs[i].delete();
	    } else {
		deleteDir(childDirs[i]);
		childDirs[i].delete();
	    }
	}
	rootDir.delete();

    }

}

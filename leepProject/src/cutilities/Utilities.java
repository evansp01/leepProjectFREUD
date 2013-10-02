package cutilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Utilities {

    public static void printDay(ResultSet[] dayRS, int day) throws SQLException {
	int blocks = dayRS.length;
	@SuppressWarnings("unchecked")
	Queue<String>[] crns = new LinkedList[blocks];

	//put the result set into list format and
	//change cross listed crns to have parenthesis 
	//so that they can't mess up column formatting
	for (int list = 0; list < blocks; list++) {
	    int crossList = 1;
	    crns[list] = new LinkedList<String>();
	    while (dayRS[list].next()) {
		String[] crn = dayRS[list].getString(1).split("-", -1);
		if (crn.length > 1) {
		    for (int i = 0; i < crn.length; i++)
			crn[i] = "(" + crossList + ")" + crn[i];
		    crossList++;
		}
		crns[list].addAll(Arrays.asList(crn));
	    }
	}
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
    }

    public static void print(ResultSet rs) throws SQLException {
	ResultSetMetaData rsmd = rs.getMetaData();

	PrintColumnTypes.printColTypes(rsmd);
	System.out.println("");

	int numberOfColumns = rsmd.getColumnCount();

	for (int i = 1; i <= numberOfColumns; i++) {
	    if (i > 1)
		System.out.print(",  ");
	    String columnName = rsmd.getColumnName(i);
	    System.out.print(columnName);
	}
	System.out.println("");

	int j = 0;
	while (rs.next()) {
	    for (int i = 1; i <= numberOfColumns; i++) {
		if (i > 1)
		    System.out.print(",  ");
		String columnValue = rs.getString(i);
		System.out.print(columnValue);
	    }
	    j++;
	    System.out.println("");
	}
	System.out.println(j);

    }
    
    public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}



    public static void prl() {
	prl("");
    }

    public static void prl(Object s) {

	System.out.println(s);
    }

    public static void pr(Object s) {

	System.out.print(s);
    }

}

class PrintColumnTypes {

    public static void printColTypes(ResultSetMetaData rsmd) throws SQLException {
	int columns = rsmd.getColumnCount();
	for (int i = 1; i <= columns; i++) {
	    int jdbcType = rsmd.getColumnType(i);
	    String name = rsmd.getColumnTypeName(i);
	    System.out.print("Column " + i + " is JDBC type " + jdbcType);
	    System.out.println(", which the DBMS calls " + name);
	}
    }
}
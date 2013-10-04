package cStatistics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;

import consoleThings.Settings;
import cutilities.StatsPrinter;
import cutilities.Utilities;
import databaseForMainProject.DatabaseConnection;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class SchedulerChecking {

    /**
     * 
     * @param conn
     *            database connection
     * @param tableName
     *            name of table
     * @param sett
     *            settings file
     * @throws SQLException
     */
    public static void printSchedule(DatabaseConnection conn, String tableName, Settings sett) throws SQLException {
	int day = sett.days, block = sett.blocks;
	Statement sts[] = new Statement[block];
	ResultSet[] dayRS = new ResultSet[block];
	for (int i = 0; i < block; i++)
	    sts[i] = conn.getStatement();

	int crossList = 1;

	System.out.println("This is a printout of the schedule.\nThe numbers "
		+ "in parenthesis signify that\nthe crns are cross listed (within blocks)\n");
	for (int i = 0; i < day; i++) {
	    for (int j = 0; j < block; j++) {
		String query = "SELECT Distinct CourseCRN FROM " + tableName + " WHERE FinalDay = " + i
			+ " AND FinalBlock = " + j;
		dayRS[j] = sts[j].executeQuery(query);
	    }
	    crossList = Utilities.printDay(dayRS, i, crossList);
	    System.out.println();

	}
	for (int i = 0; i < block; i++)
	    sts[i].close();

    }

    /**
     * does statistics on the current schedule and prints them
     * 
     * @param conn
     * @param tableName
     * @param sett
     * @throws SQLException
     */
    public static void stats(DatabaseConnection conn, String tableName, Settings sett) throws SQLException {
	StatsPrinter sp = new StatsPrinter();
	examDistribution(conn, tableName, sett, sp);
	studentDistribution(conn, tableName, sett, sp);
    }

    /**
     * Does the statistics related to only the exam distribution and not the
     * individual students
     * 
     * @param connect
     * @param tableName
     * @param sett
     * @param sp
     * @throws SQLException
     */
    public static void examDistribution(DatabaseConnection connect, String tableName, Settings sett, StatsPrinter sp)
	    throws SQLException {

	int days = sett.days;
	int blocks = sett.blocks;
	int large = Settings.LARGE;

	//things that will eventually be printed
	int[][] examsPerDayBlock = new int[days][blocks];
	int[][] largeExamsPerDayBlock = new int[days][blocks];

	Statement st = connect.getStatement();

	String query1 = "SELECT DISTINCT CourseCRN, FinalDay, FinalBlock, ActualEnroll FROM " + tableName
		+ " WHERE FinalDay != -1";
	ResultSet rs1 = st.executeQuery(query1);

	while (rs1.next()) {
	    int fday = rs1.getInt(2);
	    int fblock = rs1.getInt(3);
	    int enroll = rs1.getInt(4);
	    if (fday != -1 && fblock != -1) {
		examsPerDayBlock[fday][fblock]++;
		if (enroll >= large)
		    largeExamsPerDayBlock[fday][fblock]++;
	    }

	}
	sp.printSectionHeader("Number of exams in each day and block");
	sp.printDayBlock2DIntArray(examsPerDayBlock, days, blocks);
	sp.printSectionHeader("Number of large exams in each day and block");
	sp.printDayBlock2DIntArray(largeExamsPerDayBlock, days, blocks);

    }

    /**
     * Does the statistics related to students and exam distribution
     * 
     * @param connect
     * @param swf
     * @param sett
     * @param sp
     * @throws SQLException
     */
    public static void studentDistribution(DatabaseConnection connect, String swf, Settings sett, StatsPrinter sp)
	    throws SQLException {
	int days = sett.days;
	int blocks = sett.blocks;

	//things that will eventually be printed

	int[][] studentsWithNExamsPerDay = new int[days][blocks + 1];
	int[][] studentsWithExamsInDayBlock = new int[days][blocks];
	@SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] NExamsInMDays = new HashMap[days - 1];
	for (int i = 0; i < days - 1; i++)
	    NExamsInMDays[i] = new HashMap<>();

	HashMap<Integer, Integer> backToBackPerStudent = new HashMap<Integer, Integer>();
	StringBuilder listOf2Conflicts = new StringBuilder();
	StringBuilder listOf3Conflicts = new StringBuilder();
	int[] conflicts = new int[2];
	int[][] backToBackByDayBlock = new int[days][blocks];

	Statement st = connect.getStatement();
	Statement st2 = connect.getStatement();

	String query1 = "SELECT DISTINCT StudentIDNo FROM " + swf;
	ResultSet rs1 = st.executeQuery(query1);

	while (rs1.next()) {
	    String id = rs1.getString(1);
	    String query2 = "SELECT DISTINCT CourseCRN, FinalDay, FinalBlock FROM " + swf + " WHERE StudentIDNo = '"
		    + id + "' AND FinalDay != '-1'";
	    ResultSet rs2 = st2.executeQuery(query2);
	    String[][] exams = new String[days][blocks];
	    while (rs2.next()) {
		String crn = rs2.getString(1);
		int fday = rs2.getInt(2);
		int fblock = rs2.getInt(3);
		if (fday != -1)
		    exams[fday][fblock] = crn;
	    }
	    rs2.close();

	    {

		int[] perDay = new int[days];
		for (int i = 0; i < days; i++) {
		    for (int j = 0; j < blocks; j++)
			if (exams[i][j] != null)
			    perDay[i]++;
		}

		//fill NExamsPerDay
		for (int i = 0; i < days; i++)
		    studentsWithNExamsPerDay[i][perDay[i]]++;
		//fill examsInDayBlock
		for (int i = 0; i < days; i++)
		    for (int j = 0; j < days; j++)
			if (exams[i][j] != null)
			    studentsWithExamsInDayBlock[i][j] += 1;
		//fill NExamsInMDays

		for (int N = 2; N < NExamsInMDays.length + 2; N++) {
		    int maxExamNum = 0;
		    for (int j = 0; j <= days - N; j++) {
			int examsNum = 0;
			for (int k = j; k < j + N; k++) {
			    examsNum += perDay[k];
			}
			if (examsNum > maxExamNum)
			    maxExamNum = examsNum;
		    }
		    if (NExamsInMDays[N - 2].containsKey(maxExamNum))
			NExamsInMDays[N - 2].put(maxExamNum, NExamsInMDays[N - 2].get(maxExamNum) + 1);
		    else
			NExamsInMDays[N - 2].put(maxExamNum, 1);

		}

		//populate back to back
		LinkedList<String[]> backToBack = new LinkedList<String[]>();

		for (int i = 0; i < days; i++) {
		    for (int j = 1; j < blocks; j++) {
			if (exams[i][j - 1] != null && exams[i][j] != null)
			    if (Settings.isBackToBack(j - 1, j, sett)) {
				String[] b2b = { exams[i][j - 1], exams[i][j] };
				backToBack.add(b2b);
				backToBackByDayBlock[i][j - 1]++;
				backToBackByDayBlock[i][j]++;
			    }
		    }
		}
		//populate triple
		LinkedList<String[]> triple = new LinkedList<String[]>();
		for (int i = 0; i < days; i++) {
		    for (int j = 2; j < blocks; j++) {
			if (exams[i][j - 1] != null && exams[i][j] != null && exams[i][j] != null)
			    if (Settings.isBackToBack(j - 2, j - 1, sett) && Settings.isBackToBack(j - 1, j, sett)) {
				String[] b2b = { exams[i][j - 2], exams[i][j - 1], exams[i][j] };
				triple.add(b2b);
			    }
		    }
		}
		//populate conflicts
		conflicts[0] += backToBack.size();
		conflicts[1] += triple.size();
		//create detailed lists
		for (String[] s : backToBack)
		    listOf2Conflicts.append("Student " + id + " has a back to back conflict with " + s[0] + " and "
			    + s[1] + "\n");
		for (String[] s : triple)
		    listOf3Conflicts.append("Student " + id + " has a three in a row conflict with " + s[0] + ", "
			    + s[1] + " and " + s[2] + "\n");
		//back to back per student
		int size = backToBack.size();
		if (backToBackPerStudent.containsKey(size)) {
		    backToBackPerStudent.put(size, backToBackPerStudent.get(size) + 1);
		} else {
		    backToBackPerStudent.put(size, 1);
		}
	    }

	}
	rs1.close();
	//end of large while loop
	//printing a large number of things	
	sp.printSectionHeader("Number of students with exams in each day and block");
	sp.printDayBlock2DIntArray(studentsWithExamsInDayBlock, days, blocks);
	sp.printSectionHeader("Students with N exams in a day");
	sp.printDayExams2DIntArray(studentsWithNExamsPerDay, days, blocks + 1);
	sp.printSectionHeader("Students with at most N exams over any M consecutive days ");
	sp.printNExamsInMDays(NExamsInMDays);
	sp.printSectionHeader("The number of back to back and three in a row cases");
	sp.printConflicts(conflicts);
	sp.printSectionHeader("Number of students with N pairs of back to back exams");
	sp.printB2BPerStudent(backToBackPerStudent);
	sp.printSectionHeader("Occurance of back to back exams by day and block");
	sp.printDayBlock2DIntArray(backToBackByDayBlock, days, blocks);
	sp.printSectionHeader("Comprehensive list of three in a row cases");
	sp.printList(listOf3Conflicts.toString(), 3);
	sp.printSectionHeader("Comprehensive list of back to back cases");
	sp.printList(listOf2Conflicts.toString(), 2);

    }
}

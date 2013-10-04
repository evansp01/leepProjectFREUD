package consoleThings;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import cStatistics.SchedulerChecking;
import cutilities.Exporter;
import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Pair;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.DatabaseConnection;

//TODO in general these method need testing

public class APIProject {
    private static CurrentProject currentProject;
    private static boolean TESTING = false;

    public static void setProject(CurrentProject cp) {
	currentProject = cp;
    }

    /**
     * return the name of the current project
     * 
     * @return
     */
    public static String getWorkingProjectName() {
	if (currentProject != null && currentProject.name != null)
	    return currentProject.name;
	else
	    return "No Current Project";
    }

    /**
     * close the current project
     */
    public static void closeProject() {
	if (currentProject.connection != null) {
	    currentProject.connection.close();
	}
	currentProject = null;

    }

    //add entries to database then run scheduler
    public static String scheduleNewFinals(String file) {
	file = "/home/evan/file.txt";
	String tempTable = "FREUDtoAdd";
	StringBuilder sb = new StringBuilder();
	int result = currentProject.connection.loadFinalTable(file, tempTable);
	if (result != DatabaseConnection.SUCCESS)
	    return currentProject.connection.getErrorString();

	String query1 = "SELECT t1.CourseCRN FROM " + tempTable + " AS t1, " + CurrentProject.finals
		+ " AS t2 WHERE t1.CourseCRN = t2.CourseCRN";
	String query2 = "SELECT t1.CourseCRN FROM " + tempTable + " AS t1 WHERE NOT EXISTS (SELECT t2.CourseCRN FROM "
		+ CurrentProject.courses + " AS t2 WHERE t1.CourseCRN = t2.CourseCRN)";
	String query3 = "MERGE INTO " + CurrentProject.finals + " SELECT t2.CourseCRN FROM " + tempTable + " AS t1, "
		+ CurrentProject.courses + " AS t2 WHERE t1.CourseCRN=t2.CourseCRN";
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    ResultSet rs1 = st.executeQuery(query1);
	    while (rs1.next()) {
		sb.append(rs1.getString(1) + " was already scheduled");
		sb.append(System.getProperty("line.separator"));
	    }
	    ResultSet rs2 = st.executeQuery(query2);
	    while (rs2.next()) {
		sb.append(rs2.getString(1) + " was not added because it is not in the master course list");
		sb.append(System.getProperty("line.separator"));
	    }
	    st.executeUpdate(query3);
	    st.executeUpdate("DROP TABLE " + tempTable);
	} catch (SQLException e) {
	    if (TESTING)
		System.out.println(e.getMessage());
	    return "error accessing database to add finals: " + e.getMessage();
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	GraphCreation gc = null;
	try {
	    gc = new GraphCreation(currentProject);
	} catch (SQLException e) {
	    return "Error during graph creation: this shouldn't happen";
	}
	Scheduler scheduler = new Scheduler(gc, currentProject);
	if (scheduler.schedule() == Scheduler.FAILURE)
	    return "Could not find a valid schedule for this project";
	if (sb.toString().length() > 0)
	    return sb.toString();
	return null;

    }

    /**
     * Prints the current schedule
     * 
     * @return null on no error, error string on success
     */
    public static String printCurrent() {
	Settings sett = currentProject.settings;
	if (!SchedulerChecking.printSchedule(currentProject.connection, CurrentProject.studentsWithInfo, sett))
	    return "Error while printing schedule: ";
	return null;

    }

    /**
     * 
     * @return
     */
    public static String printStatistics() {
	Settings sett = currentProject.settings;

	if (!SchedulerChecking.stats(currentProject.connection, CurrentProject.studentsWithInfo, sett))
	    return "Error while printing statistics: ";
	return null;

    }

    /**
     * 
     * @param file
     * @return
     */
    public static String exportToFile(String file) {
	Exporter ex = null;
	try {
	    if (file == null)
		ex = new Exporter();
	    else {
		File f = new File(file);
		if (f.exists())
		    return "file could not be opened";
		ex = new Exporter(f);
	    }

	    if (!ex.export(currentProject.connection, CurrentProject.studentsWithInfo, currentProject.settings))
		return "error with export";
	} catch (IOException e) {
	    return "error while exporting to file";
	} finally {
	    if (ex != null)
		ex.close();
	}
	return null;

    }

    //a bunch of calls to unscheduleFinal and some parsing
    public static String unscheduleFinals(String file) {
	StringBuilder sb = new StringBuilder();
	if (TESTING)
	    file = "/home/evan/file.txt";
	String tempTable = "FREUDtoUnschedule";
	int result = currentProject.connection.loadFinalTable(file, tempTable);
	if (result != DatabaseConnection.SUCCESS)
	    return currentProject.connection.getErrorString();
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    ResultSet rs = st.executeQuery("SELECT CourseCRN FROM " + tempTable);
	    while (rs.next()) {
		String crn = rs.getString(1);
		if (!unscheduleFinal(crn))
		    sb.append("Unscheduling failed for crn " + crn);
	    }
	    st.executeUpdate("DROP TABLE " + tempTable);
	} catch (SQLException e) {
	    if (TESTING)
		System.out.println(e.getMessage());
	    return "error while accessing database to unschedule courses: " + e.getMessage();
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	String failed = sb.toString();
	if (failed.length() != 0)
	    return failed;
	else
	    return null;

    }

    //just an sql update query, with the possibility of another query
    public static boolean unscheduleFinal(String name) {
	String dbname = CurrentProject.studentsWithInfo;
	String queryFin = "UPDATE " + dbname + " SET FinalDay = '-1', "
		+ "FinalBlock = '-1' WHERE CHARINDEX(CourseCRN,'" + name + "')>0";
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    st.executeUpdate(queryFin);
	} catch (SQLException e) {
	    return false;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return true;

    }

    //create the graph and list possible times
    public static boolean[][] listPossibleTimes(String name) {
	//wont work with cross listed courses
	//update equals to do that thing
	String query1 = "SELECT COUNT(CourseCRN), CourseCRN FROM " + CurrentProject.finals
		+ " WHERE CHARINDEX(CourseCRN,'" + name + "')>0 GROUP BY CourseCRN";
	String query2 = "MERGE INTO " + CurrentProject.finals + " (CourseCRN) VALUES(?)";
	String query3 = "DELETE FROM " + CurrentProject.finals + " WHERE CourseCRN = '" + name + "'";
	ArrayList<Pair> al = null;
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    ResultSet rs1 = st.executeQuery(query1);
	    rs1.next();
	    int rs1Int = rs1.getInt(1);
	    name = rs1.getString(2);
	    st.executeUpdate(query2);

	    GraphCreation gc = null;
	    gc = new GraphCreation(currentProject);
	    Scheduler scheduler = new Scheduler(gc, currentProject);
	    al = scheduler.findAvailableSlots(scheduler.getCourseMap().get(name));
	    if (rs1Int == 1)
		st.executeUpdate(query3);
	} catch (SQLException e) {
	    System.out.println(e.getMessage());
	    return null;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	if (al == null)
	    return null;
	boolean[][] results = new boolean[currentProject.settings.days][currentProject.settings.blocks];
	for (int i = 0; i < al.size(); i++) {
	    Pair p = al.get(i);
	    results[p.day()][p.block()] = true;
	}
	return results;
    }

    //just an sql update query
    public static String scheduleFinalForTime(String name, int day, int time) {
	String dbname = CurrentProject.studentsWithInfo;
	String queryFin = "UPDATE " + dbname + " SET FinalDay = '" + day + "', " + "FinalBlock = '" + time
		+ "' WHERE CHARINDEX(CourseCRN,'" + name + "')>0";
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    st.executeUpdate(queryFin);
	} catch (SQLException e) {
	    return "error while attempting to schedule final";
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return null;

    }

    public static int getDays() {
	return currentProject.settings.days;

    }

    public static int getBlocks() {
	return currentProject.settings.blocks;

    }

    public static boolean crnInDB(String crn) {
	String query = "SELECT Count(CourseCRN) FROM " + CurrentProject.courses + " WHERE CourseCRN = '" + crn
		+ " GROUP BY CourseCRN";
	Statement st = null;
	int count = 0;
	try {
	    st = currentProject.connection.getStatement();

	    ResultSet rs = st.executeQuery(query);
	    rs.next();
	    count = rs.getInt(1);

	} catch (SQLException e) {
	    return false;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return count > 0;
    }

    public static boolean crnInFinals(String name) {
	String query = "SELECT Count(CourseCRN) FROM " + CurrentProject.finals + " WHERE CourseCRN = '" + name
		+ " GROUP BY CourseCRN";
	Statement st = null;
	int count = 0;
	try {
	    st = currentProject.connection.getStatement();

	    ResultSet rs = st.executeQuery(query);
	    rs.next();
	    count = rs.getInt(1);

	} catch (SQLException e) {
	    return false;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return count > 0;
    }

}

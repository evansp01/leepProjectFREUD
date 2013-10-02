package consoleThings;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cStatistics.SchedulerChecking;
import cutilities.Exporter;
import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.CreateFinalTable;
import databaseForMainProject.DatabaseConnection;

public class API {

    //	TODO all of this

    //    switch day with most large exams to first day

    private static CurrentProject currentProject = null;

    public static final boolean TESTING = true;

    //TODO make it so this works when false

    /**
     * determines if the project in question exists and attempts to open it null
     * indicates success, other is an error string
     * 
     * @param project
     * @return
     */
    //TODO test this dumb thing
    public static String projectExists(String project) {
	File f = new File(pathToDocuments() + File.separator + project);
	if (!f.exists())
	    return "Could not find project named: " + project;
	String notValid = project + " is not a valid project";
	if (!f.isDirectory())
	    return notValid;
	File[] contents = f.listFiles();
	boolean found = false;
	for (int i = 0; i < contents.length; i++) {
	    if ("project.h2.db".equals(contents[i].getName()))
		found = true;
	}
	if (!found)
	    return notValid;
	String settingsFileName = f.getAbsolutePath() + File.separator + CurrentProject.settingsFile;
	File settingsFile = new File(settingsFileName);
	if (!settingsFile.exists())
	    return notValid;

	String url = CurrentProject.urlStart + f.getAbsolutePath() + CurrentProject.dbFile;
	Object result = Settings.parseSettings(settingsFile);
	if (result instanceof String)
	    return project + "is not valid due to settings error: " + result;

	DatabaseConnection connection = new DatabaseConnection(url, CurrentProject.user, CurrentProject.password);
	CurrentProject cp = new CurrentProject(project, (Settings) result, connection);
	currentProject = cp;
	return null;
    }

    /**
     * determines if this is a valid project name
     * 
     * @param name
     * @return
     */
    public static boolean isValidName(String name) {
	File f = new File(pathToDocuments() + name);
	if (f.exists())
	    return false;
	if (f.getParentFile().exists())
	    return true;
	return false;
    }

    /**
     * creates a new project with the given name from the given folder null
     * return indicates successful creation, any other string is an error string
     * 
     * @param name
     * @param project
     * @return
     */

    public static final int CONFIG = 0, COURSES = 1, FINALS = 2, STUDENTS = 3;
    public static final String[] REQS = { "config.txt", "courses.csv", "finals.csv", "students.csv" };

    public static String createProjectFromFolder(String name, String folder) {
	File[] reqs = new File[REQS.length];
	File dir = null;
	File[] contents = null;
	try { //try to open the 
	    dir = new File(folder);
	    if (!dir.exists())
		return "" + folder + " does not exist";
	    if (!dir.isDirectory()) {
		return "" + folder + " is not a directory";
	    }
	    contents = dir.listFiles();
	} catch (Exception e) {
	    return "Could not open directory at " + folder;
	}
	//list all files in the directory
	for (File f : contents) {
	    if (f.exists()) {
		String nameOfFile = f.getName();
		for (int i = 0; i < REQS.length; i++) {
		    if (REQS[i].equals(nameOfFile)) {
			reqs[i] = f;
		    }
		}
	    }
	}
	for (int i = 0; i < REQS.length; i++) {
	    if (reqs[i] == null)
		return "Required file " + (REQS[i]) + " is missing";
	}
	//returns either the settings object or a string containing an error message
	Object o = Settings.parseSettings(reqs[CONFIG]);
	Settings settings = null;
	if (o instanceof Settings)
	    settings = (Settings) o;
	else if (o instanceof String)
	    return (String) o;
	String url;
	//TODO test to make sure the relative path stuff works
	if (TESTING) {
	    url = CurrentProject.urlStart + "~/test";
	    System.out.println(url);
	} else
	    url = "jdbc:h2:" + pathToDocuments() + currentProject.name + File.pathSeparator + CurrentProject.dbFile;

	String user = CurrentProject.user;
	String password = CurrentProject.password;
	DatabaseConnection connection = null;
	try {
	    connection = new DatabaseConnection(url, user, password);
	    connection.connect();
	    if (connection.loadCourseOfferings(reqs[COURSES].getAbsolutePath()) != 0)
		return connection.getErrorString();
	    if (connection.loadFinalExams(reqs[FINALS].getAbsolutePath()) != 0)
		return connection.getErrorString();
	    if (connection.loadStudentScheudle(reqs[COURSES].getAbsolutePath()) != 0)
		return connection.getErrorString();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println(e.getMessage());
	    return "error connecting to database";
	} finally {
	    //	    if (connection != null)
	    //		connection.close();
	}
	currentProject = new CurrentProject(name, settings, connection);
	CreateFinalTable.maintainTables(currentProject.connection);
	GraphCreation gc = null;
	try {
	    gc = new GraphCreation(currentProject);
	} catch (SQLException e) {
	    return "Error during graph creation: this shouldn't happen";
	}
	Scheduler scheduler = new Scheduler(gc, currentProject);
	if (!scheduler.schedule())
	    return "Could not find a valid schedule for this project";
	return null;

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
	if (currentProject.connection != null)
	    currentProject.connection.close();
	currentProject = null;

    }

    //TODO in general these methods do not belong where they are currently

    //add entries to database then run scheduler
    //TODO test

    public static String scheduleNewFinals(String file) {
	file = "/home/evan/file.txt";
	String tempTable = "FREUDtoAdd";
	StringBuilder sb = new StringBuilder();
	currentProject.connection.loadFinalTable(file, tempTable);

	String query1 = "SELECT t1.CourseCRN FROM " + tempTable + ", " + CurrentProject.finals
		+ " AS t2 WHERE t1.CourseCRN = t2.CourseCRN";
	String query2 = "SELECT t1.CourseCRN FROM " + tempTable + "AS t1 WHERE NOT EXISTS SELECT CourseCRN FROM "
		+ CurrentProject.courses + "AS t2 WHERE t1.CourseCRN = t2.CourseCRN";
	String query3 = "MERGE INTO " + CurrentProject.finals + " SELECT t2.CourseCRN FROM " + tempTable + " AS t1, "
		+ CurrentProject.courses + " AS t2 WHERE t1.CourseCRN=t2.CourseCRN";
	Statement st = null;
	try {
	    st = currentProject.connection.getStatement();
	    ResultSet rs1 = st.executeQuery(query1);
	    while (rs1.next())
		sb.append(rs1.getString(1) + " was already scheduled\n");
	    ResultSet rs2 = st.executeQuery(query2);
	    while (rs2.next())
		sb.append(rs2.getString(1) + " was not added because it is not in the master course list\n");
	    st.executeUpdate(query3);
	    st.executeUpdate("DROP TABLE " + tempTable);
	} catch (SQLException e) {
	    return "error accessing database to add finals";
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
	if (!scheduler.schedule())
	    return "Could not find a valid schedule for this project";
	return null;

    }

    public static String printCurrent() {
	Settings sett = currentProject.settings;
	try {
	    SchedulerChecking.printSchedule(currentProject.connection, CurrentProject.studentsWithInfo, sett);
	} catch (SQLException e) {
	    return "Error while printing schedule";
	}
	return null;

    }

    public static String printStatistics() {
	Settings sett = currentProject.settings;
	try {
	    SchedulerChecking.stats(currentProject.connection, CurrentProject.studentsWithInfo, sett);
	} catch (SQLException e) {
	    return "Error while printing statistics";
	}
	return null;

    }

    public static String exportToFile(String file) {
	Exporter ex;
	try {
	    if (file == null)
		ex = new Exporter();
	    else {
		File f = new File(file);
		if (f.exists())
		    return "file could not be opened";
		ex = new Exporter(f);
	    }

	    ex.export(currentProject.connection, CurrentProject.studentsWithInfo, currentProject.settings);
	} catch (SQLException e) {
	    return "error with export";
	} catch (IOException e) {
	    return "error while exporting to file";
	}
	return null;

    }

    //a bunch of calls to unscheduleFinal and some parsing
    public static String unscheduleFinals(String file) {
	StringBuilder sb = new StringBuilder();
	if (TESTING)
	    file = "/home/evan/file.txt";
	String tempTable = "FREUDtoUnschedule";
	currentProject.connection.loadFinalTable(file, tempTable);
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
	    return "error while accessing database to unschedule courses";
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
	try {
	    currentProject.connection.getStatement().executeUpdate(queryFin);
	} catch (SQLException e) {
	    return false;
	}
	return true;

    }

    //create the graph and list possible times
    //TODO implement
    public static String[] listPossibleTimes(String name) {
	//check if in the list of finals
	//if not in, add to the list of finals
	//make the graph and scheduler
	//get the blocks that this course is available
	//remove this course from the list of finals if it wasn't there previously
	//	Scheduler s = new Scheduler(null, null);
	//	s.findAvailableSlots(null);

	return null;
    }

    //just an sql update query
    public static String scheduleFinalForTime(String name, int day, int time) {
	String dbname = CurrentProject.studentsWithInfo;
	String queryFin = "UPDATE " + dbname + " SET FinalDay = '" + day + "', " + "FinalBlock = '" + time
		+ "' WHERE CHARINDEX(CourseCRN,'" + name + "')>0";
	try {
	    currentProject.connection.getStatement().executeUpdate(queryFin);
	} catch (SQLException e) {
	    return "error while attempting to schedule final";
	}
	return null;

    }

    public static String pathToDocuments() {
	if (TESTING)
	    return "/home/evan/documentsTesting";
	return "not implemented";
    }

}

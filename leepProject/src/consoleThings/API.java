package consoleThings;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import cStatistics.SchedulerChecking;
import cutilities.Exporter;
import cutilities.Utilities;
import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Pair;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.CreateFinalTable;
import databaseForMainProject.DatabaseConnection;

public class API {

    //	TODO all of this

    //    switch day with most large exams to first day

    private static CurrentProject currentProject = null;

    public static final boolean TESTING = false;

    /**
     * determines if the project in question exists and attempts to open it null
     * indicates success, other is an error string
     * 
     * @param project
     * @return
     */
    public static String projectExists(String project, String path) {
	File f = new File(path + File.separator + project);
	if (!f.exists())
	    return "Could not find project named: " + project;
	String notValid = project + " is not a valid project";
	if (!f.isDirectory())
	    return notValid + " no matching directory found";
	File[] contents = f.listFiles();
	boolean found = false;
	for (int i = 0; i < contents.length; i++) {
	    if ("project.h2.db".equals(contents[i].getName()))
		found = true;
	}
	if (!found)
	    return notValid + " no database file found";
	String settingsFileName = f.getAbsolutePath() + File.separator + CurrentProject.settingsFile;
	File settingsFile = new File(settingsFileName);
	if (!settingsFile.exists())
	    return notValid + " no settings file found";

	String url = CurrentProject.urlStart + f.getAbsolutePath() + File.separator + CurrentProject.dbFileName;
	System.out.println(url);
	Object result = Settings.parseSettings(settingsFile);
	if (result instanceof String)
	    return project + "is not valid due to settings error: " + result;

	DatabaseConnection connection = new DatabaseConnection(url, CurrentProject.user, CurrentProject.password);
	if (!connection.connect())
	    return notValid + " error loading database";
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
    public static String isValidName(String name, String path) {
	//project name cannot have whitespace characters
	if (!name.equals(name.replaceAll("\\s+", "")))
	    return "Project names cannot contain whitespace";
	File f = new File(path + File.separator + name);
	if (f.exists())
	    return "This project already exists";
	if (f.getParentFile().exists())
	    return null;
	return "cannot find the documents file";
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

    public static String createProjectFromFolder(String name, String folder, String path) {
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
	//create directory
	if (!(new File(path + File.separator + name)).mkdirs())
	    return "could not create directory in documents folder";
	try {
	    Utilities.copyFile(reqs[CONFIG], new File(path + File.separator + name + File.separator
		    + CurrentProject.settingsFile));
	} catch (IOException e1) {
	    return "error copying settings file to documents file";
	}

	String url;
	if (TESTING) {
	    url = CurrentProject.urlStart + "~/test";
	} else {
	    url = "jdbc:h2:" + path + File.separator + name + File.separator + CurrentProject.dbFileName;
	}

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
	    if (TESTING)
		System.out.println(e.getMessage());
	    return "error connecting to database: " + e.getMessage();
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
	if (currentProject.connection != null) {
	    currentProject.connection.close();
	}
	currentProject = null;

    }

    //add entries to database then run scheduler
    //TODO test

    public static String scheduleNewFinals(String file) {
	file = "/home/evan/file.txt";
	String tempTable = "FREUDtoAdd";
	StringBuilder sb = new StringBuilder();
	currentProject.connection.loadFinalTable(file, tempTable);

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
	    while (rs1.next())
		sb.append(rs1.getString(1) + " was already scheduled\n");
	    ResultSet rs2 = st.executeQuery(query2);
	    while (rs2.next())
		sb.append(rs2.getString(1) + " was not added because it is not in the master course list\n");
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
	if (!scheduler.schedule())
	    return "Could not find a valid schedule for this project";
	if (sb.toString().length() > 0)
	    return sb.toString();
	return null;

    }

    public static String printCurrent() {
	Settings sett = currentProject.settings;
	try {
	    SchedulerChecking.printSchedule(currentProject.connection, CurrentProject.studentsWithInfo, sett);
	} catch (Exception e) {
	    return "Error while printing schedule: " + e.getMessage();
	}
	return null;

    }

    public static String printStatistics() {
	Settings sett = currentProject.settings;
	try {
	    SchedulerChecking.stats(currentProject.connection, CurrentProject.studentsWithInfo, sett);
	} catch (Exception e) {
	    return "Error while printing statistics: " + e.getMessage();
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
    //TODO check
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
    //TODO check
    public static Pair[] listPossibleTimes(String name) {
	//wont work with cross listed courses
	//update equals to do that thing
	String query1 = "SELECT COUNT(CourseCRN), CourseCRN FROM " + CurrentProject.finals
		+ " WHERE CHARINDEX(CourseCRN,'" + name + "')>0";
	String query2 = "MERGE INTO " + CurrentProject.finals + " (CourseCRN) VALUES(?)";
	String query3 = "DELETE FROM " + CurrentProject.finals + " WHERE CourseCRN = '" + name + "'";
	ArrayList<Pair> al = null;
	try {
	    Statement st = currentProject.connection.getStatement();
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
	    return null;
	}
	if (al == null)
	    return null;
	return (Pair[]) al.toArray();
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

}

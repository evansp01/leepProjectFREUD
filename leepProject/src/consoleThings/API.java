package consoleThings;

import java.io.File;
import java.sql.SQLException;

import cStatistics.SchedulerChecking;
import cutilities.Exporter;
import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.CreateFinalTable;
import databaseForMainProject.DatabaseConnection;

public class API {

    private static CurrentProject currentProject = null;

    public static final boolean TESTING = true;

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
	if (TESTING)
	    url = CurrentProject.urlStart + "~/test";
	else
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
	    if (connection != null)
		connection.close();
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

    //add entries to database then run scheduler
    public static String scheduleNewFinals(String filename) {
	return "not implemented";
	// TODO Auto-generated method stub

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
	//TODO make so this prints to a file
	Exporter ex = new Exporter();
	try {
	    ex.export(currentProject.connection, CurrentProject.studentsWithInfo, currentProject.settings);
	} catch (SQLException e) {
	    return "error with export";
	}
	return null;

    }

    //a bunch of calls to unscheduleFinal and some parsing
    public static String unscheduleFinals(String file) {
	return "not implemented";
	// TODO Auto-generated method stub

    }

    //just an sql update query, with the possibility of another query
    public static boolean unscheduleFinal(String name) {
	return false;
	// TODO Auto-generated method stub

    }

    //create the graph and list possible times
    public static String[] listPossibleTimes(String name) {
	//	Scheduler s = new Scheduler(null, null);
	//	s.findAvailableSlots(null);
	// TODO Auto-generated method stub
	return null;
    }

    //just an sql update query
    public static void scheduleFinalForTime(String name) {
	// TODO Auto-generated method stub

    }

    public static String pathToDocuments() {
	if (TESTING)
	    return "/home/evan/documentsTesting";
	return "not implemented";
    }

}

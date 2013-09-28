package consoleThings;

import java.io.File;
import java.sql.SQLException;

import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.CreateFinalTable;
import databaseForMainProject.DatabaseConnection;

public class API {

    private static CurrentProject currentProject = null;

    /**
     * determines if the project in question exists and attempts to open it null
     * indicates success, other is an error string
     * 
     * @param project
     * @return
     */
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
	String urla = "jdbc:h2:" + pathToDocuments() + currentProject.name + File.pathSeparator + CurrentProject.dbFile;
	String url = CurrentProject.urlStart + "~/test";
	String user = "javauser";
	String password = "derp";
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

    public static final boolean TESTING = true;

    public static String pathToDocuments() {
	if (TESTING)
	    return "/home/evan/documentsTesting";
	return "not implemented";
    }

    public static void scheduleNewFinals(String filename) {
	// TODO Auto-generated method stub

    }

    public static void printCurrent() {
	// TODO Auto-generated method stub

    }

    public static void printStatistics() {
	// TODO Auto-generated method stub

    }

    public static void exportToFile(String file) {
	// TODO Auto-generated method stub

    }

    public static void unscheduleFinals(String file) {
	// TODO Auto-generated method stub

    }

    public static void unscheduleFinal(String name) {
	// TODO Auto-generated method stub

    }

    public static String[] listPossibleTimes(String name) {
	// TODO Auto-generated method stub
	return null;
    }

    public static void scheduleFinalForTime(String name) {
	// TODO Auto-generated method stub

    }

}

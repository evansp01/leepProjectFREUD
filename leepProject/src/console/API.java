package console;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import scheduling.GraphCreation;
import scheduling.Scheduler;
import utilities.Utilities;

import database.CreateFinalTable;
import database.DatabaseConnection;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class API {

    //	TODO all of this

    //    switch day with most large exams to first day

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
	String notValid = project + " is not a valid project or is already open: ";
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
	Object result = Settings.parseSettings(settingsFile);
	if (result instanceof String)
	    return project + "is not valid due to settings error: " + result;

	DatabaseConnection connection = new DatabaseConnection(url, CurrentProject.user, CurrentProject.password);
	if (!connection.connect()) {
	    if (connection != null)
		connection.close();
	    return notValid + " error loading database";
	}
	CurrentProject cp = new CurrentProject(project, (Settings) result, connection);
	APIProject.setProject(cp);
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

	File createdDirectory = (new File(path + File.separator + name));
	String result = createProject(name, folder, path, reqs, settings, createdDirectory);
	//delete folder that was created if things screw up
	if (result != null) {
	    if (createdDirectory.exists())
		Utilities.deleteDir(createdDirectory);
	}
	return result;
    }

    //the part of the method that creates the folder -- if this fails, the folder should be deleted
    private static String createProject(String name, String folder, String path, File[] reqs, Settings settings,
	    File dir) {

	//create directory
	if (!dir.mkdirs())
	    return "could not create directory in documents folder";
	try {
	    Utilities.copyFile(reqs[CONFIG], new File(path + File.separator + name + File.separator
		    + CurrentProject.settingsFile));
	} catch (IOException e) {
	    return "error copying settings file to documents file";
	}

	String url;

	url = "jdbc:h2:" + path + File.separator + name + File.separator + CurrentProject.dbFileName;

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
	    if (connection.loadStudentScheudle(reqs[STUDENTS].getAbsolutePath()) != 0)
		return connection.getErrorString();
	} catch (Exception e) {
	    if (connection != null)
		connection.close();
	    return "error connecting to database: " + e.getMessage();
	}
	CurrentProject cp = new CurrentProject(name, settings, connection);

	CreateFinalTable.maintainTables(connection);
	GraphCreation gc = null;
	try {
	    gc = new GraphCreation(cp);
	} catch (SQLException e) {
	    if (connection != null)
		connection.close();
	    return "Error during graph creation: this shouldn't happen";
	}
	Scheduler scheduler = new Scheduler(gc, cp);
	if (scheduler.schedule(settings.retries) == Scheduler.FAILURE) {
	    if (connection != null)
		connection.close();
	    return "Could not find a valid schedule for this project";
	}
	CreateFinalTable.updateExams(connection, scheduler.getCourseMap());

	APIProject.setProject(cp);

	return null;

    }

    //methods which are in the project api but can be accessed through this api
    public static boolean unscheduleFinal(String name) {
	return APIProject.unscheduleFinal(name);

    }

    public static boolean[][] listPossibleTimes(String name) {
	return APIProject.listPossibleTimes(name);
    }

    public static int getDays() {
	return APIProject.getDays();

    }

    public static int getBlocks() {
	return APIProject.getBlocks();
    }

    public static String getWorkingProjectName() {
	return APIProject.getWorkingProjectName();
    }

    public static String scheduleFinalForTime(String name, int day, int block) {
	return APIProject.scheduleFinalForTime(name, day, block);
    }

    public static void closeProject() {
	APIProject.closeProject();

    }

    public static String unscheduleFinals(String file) {
	return APIProject.unscheduleFinals(file);

    }

    public static String exportToFile(String file) {
	return APIProject.exportToFile(file);
    }

    public static String printStatistics(boolean withPauses) {
	return APIProject.printStatistics(withPauses);
    }

    public static String scheduleNewFinals(String file) {
	return APIProject.scheduleNewFinals(file);
    }

    public static String printCurrent(boolean withPauses) {
	return APIProject.printCurrent(withPauses);
    }

    public static boolean crnInDB(String name) {
	return APIProject.crnInDB(name);
    }

    public static boolean crnInFinals(String name) {
	return APIProject.crnInFinals(name);
    }

    public static int[] getFinalTime(String name) {
	return APIProject.getFinalTime(name);
    }

}

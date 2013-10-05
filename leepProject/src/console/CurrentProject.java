package console;

import database.DatabaseConnection;
/**
 * A class to keep track of information relevant to the current project.
 * Also has constants with the default names of various tables and files in the projectF
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class CurrentProject {
    public String name;
    public Settings settings;
    public DatabaseConnection connection;

    public static final String finals = "FREUDfinals", courses = "FREUDcourses", students = "FREUDstudents",
	    studentsWithInfo = "FREUDStudentsWithInfo";
    public static final String password = "", user = "user", dbFile = "project.h2.db", dbFileName = "project",
	    urlStart = "jdbc:h2:", settingsFile = "project_settings.txt";
/**
 * create a current project from a name, a settings file, and a database connectionF
 * @param name
 * @param settings
 * @param connection
 */
    public CurrentProject(String name, Settings settings, DatabaseConnection connection) {
	this.name = name;
	this.settings = settings;
	this.connection = connection;
    }

}

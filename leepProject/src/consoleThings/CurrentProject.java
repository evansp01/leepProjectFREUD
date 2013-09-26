package consoleThings;

import databaseForMainProject.DatabaseConnection;

public class CurrentProject {
    public String name;
    public Settings settings;
    public DatabaseConnection connection;

    public static final String finals = "FREUDfinals", courses = "FREUDcourses", students = "FREUDstudents",
	    studentsWithInfo = "FREUDStudentsWithInfo";
    public static final String password = "", user = "user", database = "project";

    public CurrentProject(String name, Settings settings, DatabaseConnection connection) {
	this.name = name;
	this.settings = settings;
	this.connection = connection;
    }

}

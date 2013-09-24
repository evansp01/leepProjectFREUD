package consoleThings;

import databaseForMainProject.DatabaseConnection;

public class CurrentProject {
    public String name;
    public Settings settings;
    public DatabaseConnection connection;

    public CurrentProject(String name, Settings settings, DatabaseConnection connection) {
	this.name = name;
	this.settings = settings;
	this.connection = connection;
    }

}

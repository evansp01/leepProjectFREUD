package consoleThings;

import java.util.Scanner;

public class Console {
    private static String path;
    private static Scanner scan;

    public static final int NO_CHOICE = 0;
    public static final String NO_STRING = null;
    public static final boolean SUCCESS = true, GO_BACK = false;

    public static void main(String[] args) {
	if (args.length < 1) {
	    prl("Console must be run with the path to the documents folder as an argument");
	    return;
	}
	path = args[0];
	System.out.println(args[0]);
	scan = new Scanner(System.in);
	//either open or create a project, both should have the same result
	while (true) {

	    int firstChoice = mainMenu();
	    switch (firstChoice) {
	    case CREATE_NEW:
		clearConsole();
		if (!createNewProject())
		    continue;
		break;
	    case LOAD_EXISTING:
		clearConsole();
		if (!loadExistingProject())
		    continue;
		break;
	    case QUIT:
		prl("Goodbye!");
		return;
	    }
	    clearConsole();
	    //at this point we should have a working project

	    int projectOption = NO_CHOICE;
	    while (true) {
		projectOption = projectOptions();
		boolean exit = false;
		switch (projectOption) {
		case NEW_FINAL:
		    addNewFinal();
		    break;
		case PRINT_CURRENT:
		    printCurrent();
		    break;
		case PRINT_STATS:
		    printStatistics();
		    break;
		case EXPORT:
		    exportToFile();
		    break;
		case MOVE_FINAL:
		    deleteFinal();
		    break;
		case DELETE_FINAL:
		    deleteFinal();
		    break;
		case EXIT_PROJECT:
		    exitProject();
		    exit = true;
		    break;
		}
		if (exit)
		    break;
	    }

	    clearConsole();

	}
    }

    //PROJECT SPECIFIC METHODS

    //    halt after output on display and stats
    //TODO
    private static void addNewFinal() {
	String finals = NO_STRING;

	prl("Enter the path of the file containing the finals you wish to add");
	pr("Path: ");
	try {
	    finals = scan.next();
	} catch (Exception e) {
	    finals = NO_STRING;
	}

	String result = API.scheduleNewFinals(finals);
	if (result == null) {
	    prl("Finals successfully scheduled");
	} else {
	    prl("Error: " + result);
	}

    }

    private static void printCurrent() {
	String result = API.printCurrent();
	if (result != null)
	    prl(result);

    }

    private static void printStatistics() {
	String result = API.printStatistics();
	if (result != null)
	    prl(result);

    }

    private static void exportToFile() {
	String finals = NO_STRING;
	while (true) {
	    prl("Enter the path to which you would like to export");
	    pr("Path: ");
	    try {
		finals = scan.nextLine();
	    } catch (Exception e) {
		finals = NO_STRING;
	    }

	    String result = API.exportToFile(finals);
	    if (result == null) {
		prl("Export Successful");
		break;
	    }

	    else
		prl("Error: " + result);
	    prl("File export failed: returning to main menu ");
	    prl();
	}

    }

    public static void deleteFinal() {
	String finals = NO_STRING;
	while (true) {
	    prl("Enter the path of the file with the CRNs of the clases you would like to unschedule");
	    pr("Path: ");
	    try {
		finals = scan.nextLine();
	    } catch (Exception e) {
		finals = NO_STRING;
	    }

	    String result = API.unscheduleFinals(finals);
	    if (result == null) {
		prl("Finals successfully unscheduled");
		break;
	    }

	    else
		prl("Error: " + result);
	    prl("Unscheduling Failed: Returning to main menu ");
	    prl();
	}
    }

    //needs implementation
    //TODO: implement this method
    public static void moveFinal() {
	String name = "";
	API.unscheduleFinal(name);
	Object[] possibilities = API.listPossibleTimes(name);
	String result = API.scheduleFinalForTime(name, -1, -1);

    }

    private static void exitProject() {
	API.closeProject();
	prl("Closing project " + API.getWorkingProjectName());

    }

    //END PROJECT SPECIFIC METHODS

    public static final int NEW_FINAL = 1, MOVE_FINAL = 2, DELETE_FINAL = 3, PRINT_CURRENT = 4, PRINT_STATS = 5,
	    EXPORT = 6, EXIT_PROJECT = 7;

    private static int projectOptions() {
	prl("Your working project is: " + API.getWorkingProjectName());
	prl("What would you like to do?");
	prl("\t" + NEW_FINAL + " -- Schedule finals for a group of unscheduled courses");
	prl("\t" + MOVE_FINAL + " -- Move a currently scheduled final to a new time manually");
	prl("\t" + DELETE_FINAL + " -- Unschedule a currently scheduled final");
	prl("\t" + PRINT_CURRENT + " -- Display the current schedule");
	prl("\t" + PRINT_STATS + " -- Display statistics about the current schedule");
	prl("\t" + EXPORT + " -- Export the schedule to a file");
	prl("\t" + EXIT_PROJECT + " -- Exit project and return to the main menu");
	int option = NO_CHOICE;
	while (true) {
	    pr("Enter choice here: ");
	    try {
		option = Integer.parseInt(scan.nextLine());
	    } catch (Exception e) {
		option = NO_CHOICE;
	    }
	    if (NEW_FINAL <= option && EXIT_PROJECT >= option)
		break;
	    prl("Please enter a number " + NEW_FINAL + "-" + EXIT_PROJECT);
	}
	return option;

    }

    private static boolean loadExistingProject() {
	String project = NO_STRING;
	while (true) {
	    prl("Enter the path of the project you wish to open");
	    pr("Path: ");
	    try {
		project = scan.nextLine();
	    } catch (Exception e) {
		project = NO_STRING;
	    }

	    String result = API.projectExists(project, path);
	    if (result == null) {
		prl("Project successfully opened");
		break;
	    }

	    else
		prl("Error: " + result);
	    prl("Sorry the project at " + project + " could not be opened");
	    prl();
	    if (!doYouWantToContinue())
		return GO_BACK;
	}
	return SUCCESS;
    }

    private static boolean createNewProject() {
	String name = NO_STRING;
	while (true) {
	    prl("Enter the name/path for your new project");
	    pr("Name: ");
	    try {
		name = scan.nextLine();
	    } catch (Exception e) {
		name = null;
		continue;
	    }
	    String result = API.isValidName(name, path);
	    if (result == null)
		break;
	    prl("Sorry, " + name + " is not a valid project name");
	    prl(result);
	    prl();
	    if (!doYouWantToContinue())
		return GO_BACK;
	}
	String project = NO_STRING;
	while (true) {
	    prl("Enter the path of the source folder to create the project from");
	    pr("Path: ");
	    try {
		project = scan.nextLine();
	    } catch (Exception e) {
		project = NO_STRING;
		continue;
	    }

	    prl("Attempting to create project " + name + " from folder " + project + "");
	    String result = API.createProjectFromFolder(name, project, path);
	    if (result == null) {
		prl("Project creation successful");
		break;
	    } else
		prl("Error: " + result);
	    prl("Sorry the folder at " + project + " is not a valid project folder");
	    prl();
	    if (!doYouWantToContinue())
		return GO_BACK;
	}

	return SUCCESS;

    }

    public static final int TRY_AGAIN = 1, EXIT = 2;

    private static boolean doYouWantToContinue() {
	int option = NO_CHOICE;
	prl("Enter \"" + TRY_AGAIN + "\" to try again or \"" + EXIT + "\" to exit:");
	while (true) {
	    pr("Enter choice here: ");
	    try {
		option = Integer.parseInt(scan.nextLine());
	    } catch (Exception e) {
		option = NO_CHOICE;
	    }
	    if (option == TRY_AGAIN || option == EXIT)
		break;
	    prl("Please enter \"" + TRY_AGAIN + "\" or \"" + EXIT + "\"");
	}
	return (option == TRY_AGAIN);

    }

    public static final int LOAD_EXISTING = 2, CREATE_NEW = 1, QUIT = 3;

    private static int mainMenu() {
	int option = NO_CHOICE;
	prl("Choose an option:");
	prl("\t" + CREATE_NEW + " - Create a new scheduling project");
	prl("\t" + LOAD_EXISTING + " - Open an existing project");
	prl("\t" + QUIT + " - to exit");
	while (true) {
	    pr("Enter choice here: ");
	    try {
		option = Integer.parseInt(scan.nextLine());
	    } catch (Exception e) {
		option = NO_CHOICE;
	    }
	    if (option == CREATE_NEW || option == LOAD_EXISTING || option == QUIT)
		break;
	    prl("Please enter \"" + CREATE_NEW + "\" or \"" + LOAD_EXISTING + "\" or \"" + QUIT + "\"");
	}
	return option;

    }

    public String path() {
	return path;
    }

    //METHODS TO MAKE PRINTING EASIER
    private static void prl(Object o) {
	System.out.println(o.toString());
    }

    private static void prl() {
	System.out.println();
    }

    private static void pr(Object o) {
	System.out.print(o.toString());
    }

    private static void clearConsole() {
	prl();
	prl("=======================================================");
	prl();
    }

}

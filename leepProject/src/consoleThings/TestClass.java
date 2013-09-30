package consoleThings;

import java.sql.SQLException;
import java.util.HashMap;

import cStatistics.SchedulerChecking;

import czexamSchedulingFinal.CourseVertex;
import czexamSchedulingFinal.GraphCreation;
import czexamSchedulingFinal.Scheduler;
import databaseForMainProject.CreateFinalTable;
import databaseForMainProject.DatabaseConnection;

public class TestClass {

    public static void main(String[] args) {
	int x = 0;
	String url = "jdbc:h2:~/test";
	String user = "javauser";
	String password = "";
	DatabaseConnection conn = new DatabaseConnection(url, user, password);
	conn.connect();
	System.out.println(x++);
	conn.loadCourseOfferings("/home/evan/Documents/regleep/realTest/courses201209.csv");
	conn.loadFinalExams("/home/evan/Documents/regleep/realTest/finals201209.csv");
	conn.loadStudentScheudle("/home/evan/Documents/regleep/realTest/students201209.csv");
	System.out.println(x++);
	boolean[][] array = new boolean[4][4];
	Settings sett = new Settings(4, 4, true, true, array);
	CurrentProject project = new CurrentProject("testProject", sett, conn);

	CreateFinalTable.maintainTables(project.connection);
	GraphCreation gc = null;
	System.out.println(x++);
	try {
	    gc = new GraphCreation(project);
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	Scheduler s = new Scheduler(gc, project);
	s.schedule();
	HashMap<String, CourseVertex> schedule = s.getCourseMap();
	for (CourseVertex cv : schedule.values())
	    System.out.println(cv.name() + "|" + cv.day() + "," + cv.block());
	CreateFinalTable.updateExams(conn, schedule);
	System.out.println(x++);
	System.out.println(gc.getGraph().edgeSet().size());
	System.out.println(gc.getGraph().vertexSet().size());
	System.out.println(gc.getAlreadyScheduled().size());
	System.out.println(schedule.size());
	try {
	    SchedulerChecking.printSchedule(conn, CurrentProject.studentsWithInfo);
	} catch (SQLException e) {
	    e.printStackTrace();
	}

    }
}

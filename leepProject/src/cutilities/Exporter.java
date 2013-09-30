package cutilities;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import consoleThings.CurrentProject;
import consoleThings.Settings;
import databaseForMainProject.DatabaseConnection;

public class Exporter {
    boolean toFile;

    public Exporter(String fname) {
	toFile = true;
	//TODO implement
    }

    public Exporter() {
	toFile = false;
    }

    public void export(DatabaseConnection conn, String swf, Settings sett) throws SQLException {
	Statement st = conn.getStatement();
	//TODO debug sql statement
	String select = "t1.CourseTitle, t1.CourseCRN, t2.FinalDay, t2.FinalBlock, t1.CatalogDeptCode";
	String query = "SELECT DISTINCT " + select + " FROM " + swf + " AS t2, " + CurrentProject.courses
		+ " AS t1 WHERE t2.FinalDay != '-1' AND CHARINDEX(t1.CourseCRN, t2.CourseCRN) > 0";
	ResultSet rs = st.executeQuery(query);
	String format = "FINAL_DAY%s\t%s (%s)\tFinal Exam\tBLOCK_%s_START\tBLOCK_%s_END\t%s%n";
	while (rs.next()) {
	    String title = rs.getString(1);
	    String crn = rs.getString(2);
	    int day = rs.getInt(3);
	    int block = rs.getInt(4);
	    String booker = rs.getString(5);
	    System.out.format(format, Integer.toString(day + 1), title, crn, Integer.toString(block + 1),
		    Integer.toString(block + 1), booker);
	}

    }
}

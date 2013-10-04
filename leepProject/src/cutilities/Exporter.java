package cutilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;

import consoleThings.CurrentProject;
import consoleThings.Settings;
import databaseForMainProject.DatabaseConnection;

/**
 * 
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class Exporter {
    boolean toFile;
    Formatter form;

    /**
     * Construct an exporter that will print to a file
     * 
     * @param File
     *            the file you wish to write to
     * @throws IOException
     *             if the file cannot be written to
     */
    public Exporter(File f) throws IOException {
	toFile = true;
	form = new Formatter(new BufferedWriter(new FileWriter(f)));

    }

    /**
     * closes the formatter if it is not closed
     */
    public void close() {
	if (form != null)
	    form.close();
    }

    /**
     * Construct an exporter that will print to System.out
     */
    public Exporter() {
	toFile = false;
    }

    /**
     * Export the current schedule
     * 
     * @param conn
     * @param swf
     * @param sett
     * @throws IOException
     */
    public boolean export(DatabaseConnection conn, String swf, Settings sett) throws IOException {
	Statement st = null;
	try {
	    st = conn.getStatement();

	    //query to get export information
	    String select = "t2.FinalDay, t2.FinalBlock, t1.Subject, t1.CrseNumb, t1.Sect, t1.CourseTitle,  "
		    + "t1.CatalogDeptCode, t1.FacFirstName, t2.FacLastName";
	    String query = "SELECT DISTINCT " + select + " FROM " + swf + " AS t2, " + CurrentProject.courses
		    + " AS t1 WHERE t2.FinalDay != '-1' AND CHARINDEX(t1.CourseCRN, t2.CourseCRN) > 0";
	    ResultSet rs = st.executeQuery(query);
	    //formatting string for the rows of export
	    String format = "FINAL_DAY%s\tBLOCK_%s_START\tBLOCK_%s_END\t%s %3s sect %2s - %s\t%s\t%s, %s%n";
	    //print the header
	    if (!toFile)
		System.out.format("FinalDay\tBlock_Start\tBlock_End\tCourseTitle\tCatalog Dept Code\tFaculty Name%n");
	    else {
		form.format("FinalDay\tBlock_Start\tBlock_End\tCourseTitle\tCatalog Dept Code\tFaculty Name%n");
	    }
	    while (rs.next()) {
		Object[] values = new Object[10];
		values[0] = rs.getInt(1) + 1; //final day
		values[1] = rs.getInt(2) + 1; //final block
		values[2] = rs.getInt(2) + 1; //final block
		values[3] = rs.getString(3); //department
		values[4] = pad(rs.getString(4), 3); //courseNum
		values[5] = pad(rs.getString(5), 2); //section
		values[6] = rs.getString(6); //title
		values[7] = rs.getString(7); //cat dept code
		values[8] = rs.getString(9); //fac last
		values[9] = rs.getString(8); //fac fist

		if (!toFile) {
		    System.out.format(format, values);
		} else {
		    form.format(format, values);
		}
	    }
	} catch (SQLException e) {
	    return false;
	} finally {
	    if (st != null)
		try {
		    st.close();
		} catch (SQLException e) {
		}
	}
	return true;

    }

    //pad the strings to look pretty
    private static String pad(String s, int length) {
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < (length - s.length()); i++)
	    sb.append("0");
	sb.append(s);
	return sb.toString();
    }
}

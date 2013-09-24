package statistics;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class Utilities {
    public static void print(ResultSet rs) throws SQLException {
	ResultSetMetaData rsmd = rs.getMetaData();

	PrintColumnTypes.printColTypes(rsmd);
	System.out.println("");

	int numberOfColumns = rsmd.getColumnCount();

	for (int i = 1; i <= numberOfColumns; i++) {
	    if (i > 1)
		System.out.print(",  ");
	    String columnName = rsmd.getColumnName(i);
	    System.out.print(columnName);
	}
	System.out.println("");

	int j = 0;
	while (rs.next()) {
	    for (int i = 1; i <= numberOfColumns; i++) {
		if (i > 1)
		    System.out.print(",  ");
		String columnValue = rs.getString(i);
		System.out.print(columnValue);
	    }
	    j++;
	    System.out.println("");
	}
	System.out.println(j);

    }

}

class PrintColumnTypes {

    public static void printColTypes(ResultSetMetaData rsmd) throws SQLException {
	int columns = rsmd.getColumnCount();
	for (int i = 1; i <= columns; i++) {
	    int jdbcType = rsmd.getColumnType(i);
	    String name = rsmd.getColumnTypeName(i);
	    System.out.print("Column " + i + " is JDBC type " + jdbcType);
	    System.out.println(", which the DBMS calls " + name);
	}
    }
}
package statistics;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import zdatabaseConnector.MySQLConnect;


public class SetUpTables {

	static final String[] Sems = { "201009", "201101", "201109", "201201",
			"201209", "201301", "201309" };
	static final String STUDS = "students", FINS = "finals",
			COURSES = "courses", SCHEDS = "finalSchedule", SWF = "studswfins",
			CWF = "courseswfins";
	static MySQLConnect conn = null;
	static Connection con = null;

	public static final int[] crnsM201101 = { 38588, 38636, 38764, 38765,
			38824, 38832, 38900, 39021 };
	public static final int[] timesM201101 = { 4, 4, -1, -1, 4, 1, 2, 4 };
	public static final int[] daysM201101 = { 2, 3, 1, 1, 3, 3, 4, 3 };
	public static final int[] crnsM201109 = { 20180, 20544, 21062, 21331,
			21489, 21602 };
	public static final int[] timesM201109 = { 4, 3, 4, 3, 2, 2 };
	public static final int[] daysM201109 = { 3, 3, 2, 1, 2, 4 };
	public static final int[] crnsM201201 = { 30272, 30319, 30482, 30554,
			31400, 31791 };
	public static final int[] timesM201201 = { 2, 4, 4, 2, 3, 3 };
	public static final int[] daysM201201 = { 4, 4, 1, 4, 4, 2 };
	public static final int[] crnsM201209 = { 20180, 20544, 21062, 21331, 22439 };
	public static final int[] timesM201209 = { 5, 1, 2, 4, 2 };
	public static final int[] daysM201209 = { 1, 4, 3, 3, 4 };
	public static final int[] crnsM201301 = { 30272, 30482, 30521, 30554,
			32313, 32357 };
	public static final int[] timesM201301 = { 5, 1, 3, 5, 2, 4 };
	public static final int[] daysM201301 = { 1, 2, 3, 1, 4, 3 };

	public static final int[] crnsO201101 = { 40209 };
	public static final int[] timesO201101 = { 5 };
	public static final int[] daysO201101 = { 2 };
	public static final int[] crnsO201109 = { 20167, 21298, 21331, 20409,
			20415, 21356, 21365 };
	public static final int[] timesO201109 = { 2, 2, 2, 2, 2, 2, 2, 2 };
	public static final int[] daysO201109 = { 1, 1, 1, 1, 1, 1, 1, 1 };
	public static final int[] crnsO201201 = { 30644, 30649, 30924 };
	public static final int[] timesO201201 = { 1, 1, 1 };
	public static final int[] daysO201201 = { 4, 4, 4 };
	public static final int[] crnsO201209 = { 20169, 20170, 21184, 20448 };
	public static final int[] timesO201209 = { 5, 5, 5, 5 };
	public static final int[] daysO201209 = { 2, 2, 2, 2 };
	public static final int[] crnsO201301 = { 30703, 31945, 32345 };
	public static final int[] timesO201301 = { 1, 1, 1 };
	public static final int[] daysO201301 = { 4, 4, 4 };

	public static void doThings() throws SQLException {
		createConnection();

		for (int i = 1; i < Sems.length - 1; i++) {
			createStudWFins(Sems[i]);
			createCourseWFins(Sems[i]);
			removeStrangeDayThings(Sems[i]);
			assignGeneralSFinals(Sems[i]);
			assignGeneralFinals(Sems[i]);
		}

		batchUpdateTimes("courseswfins201101", crnsM201101, daysM201101,
				timesM201101);
		batchUpdateTimes("courseswfins201109", crnsM201109, daysM201109,
				timesM201109);
		batchUpdateTimes("courseswfins201201", crnsM201201, daysM201201,
				timesM201201);
		batchUpdateTimes("courseswfins201209", crnsM201209, daysM201209,
				timesM201209);
		batchUpdateTimes("courseswfins201301", crnsM201301, daysM201301,
				timesM201301);

		batchUpdateTimes("courseswfins201101", crnsO201101, daysO201101,
				timesO201101);
		batchUpdateTimes("courseswfins201109", crnsO201109, daysO201109,
				timesO201109);
		batchUpdateTimes("courseswfins201201", crnsO201201, daysO201201,
				timesO201201);
		batchUpdateTimes("courseswfins201209", crnsO201209, daysO201209,
				timesO201209);
		batchUpdateTimes("courseswfins201301", crnsO201301, daysO201301,
				timesO201301);

		for (int i = 1; i < Sems.length - 1; i++)
			updateStudsWFins(Sems[i]);

		// create batch update for ids
		// update studentsList based on courseList

	}

	public static void updateStudsWFins(String semester) throws SQLException {
		Statement st = con.createStatement();
		Statement st2 = con.createStatement();
		String studs = "studswfins" + semester;
		String courses = "courseswfins" + semester;
		String getCRNS = "SELECT DISTINCT CourseCRN FROM " + courses + ";";
		ResultSet rs = st.executeQuery(getCRNS);
		while (rs.next()) {
			String crn = rs.getString(1);
			String query1 = "SELECT FinalDay, FinalTime FROM " + courses
					+ " WHERE CourseCRN = '" + crn + "';";
			prl(query1);
			ResultSet rs2 = st2.executeQuery(query1);
			String day = null, time = null;
			if (rs2.next()) {
				day = rs2.getString(1);
				time = rs2.getString(2);
				String query = "UPDATE " + studs + " SET FinalDay = '" + day
						+ "', FinalTime = '" + time + "' WHERE CourseCRN = '"
						+ crn + "';";
				st2.executeUpdate(query);
			}

		}

	}

	public static void batchUpdateTimes(String table, int[] crns, int[] days,
			int[] times) throws SQLException {
		Statement st = con.createStatement();
		for (int i = 0; i < crns.length; i++) {
			String query = "UPDATE " + table + " SET FinalDay = '" + days[i]
					+ "', FinalTime = '" + times[i] + "' WHERE CourseCRN = '"
					+ crns[i] + "';";
			prl(query);
			st.executeUpdate(query);
		}
		//

	}

	public static void assignGeneralSFinals(String semester)
			throws SQLException {
		Statement st = con.createStatement();
		Statement st2 = con.createStatement();
		String table = SWF + semester;
		String table1 = SCHEDS + semester;

		String getCRNs = "SELECT DISTINCT CourseCRN FROM " + table + ";";
		prl(getCRNs);
		ResultSet rs = st.executeQuery(getCRNs);
		while (rs.next()) {
			String crn = rs.getString(1);
			String getBlocks = "SELECT t1.block, t1.day FROM "
					+ table1
					+ " AS t1, "
					+ table
					+ " AS t2 WHERE REPLACE(t1.pattern,' ','') = REPLACE(t2.Days,' ','')"
					+ " AND t1.start = t2.MeetBeginTime AND t1.end=t2.MeetEndTime AND t2.CourseCRN = '"
					+ crn + "';";
			// prl(getBlocks);
			ResultSet rs2 = st2.executeQuery(getBlocks);
			int block = -1;
			int day = -1;
			if (rs2.next()) {
				block = rs2.getInt(1);
				day = rs2.getInt(2);
			}
			String query = "UPDATE " + table + " SET finalDay ='" + day
					+ "', finalTime ='" + block + "' WHERE CourseCRN = '" + crn
					+ "';";
			// prl(query);
			rs2.close();
			st2.executeUpdate(query);

		}

	}

	public static void assignGeneralFinals(String semester) throws SQLException {
		Statement st = con.createStatement();
		Statement st2 = con.createStatement();
		String table = CWF + semester;
		String table1 = SCHEDS + semester;

		String getCRNs = "SELECT DISTINCT CourseCRN FROM " + table
				+ " WHERE CourseCRN NOT IN (SELECT CourseCRN FROM " + table
				+ " GROUP BY CourseCRN HAVING COUNT(CourseCRN) > 1)";
		prl(getCRNs);
		ResultSet rs = st.executeQuery(getCRNs);
		while (rs.next()) {
			String crn = rs.getString(1);
			String getBlocks = "SELECT t1.block, t1.day FROM "
					+ table1
					+ " AS t1, "
					+ table
					+ " AS t2 WHERE REPLACE(t1.pattern,' ','') = REPLACE(t2.Days,' ','')"
					+ " AND t1.start = t2.MeetBeginTime AND t1.end=t2.MeetEndTime AND t2.CourseCRN = '"
					+ crn + "';";
			// prl(getBlocks);
			ResultSet rs2 = st2.executeQuery(getBlocks);
			int block = -1;
			int day = -1;
			if (rs2.next()) {
				block = rs2.getInt(1);
				day = rs2.getInt(2);
			}
			String query = "UPDATE " + table + " SET finalDay ='" + day
					+ "', finalTime ='" + block + "' WHERE CourseCRN = '" + crn
					+ "';";
			// prl(query);
			rs2.close();
			st2.executeUpdate(query);

		}

	}

	public static void removeStrangeDayThings(String semester)
			throws SQLException {
		if (true) {
			Statement st = con.createStatement();
			Statement st2 = con.createStatement();
			String table = CWF + semester;

			String select = "SELECT DISTINCT t1.CourseCRN FROM "
					+ table
					+ " AS t1 ,"
					+ table
					+ " AS t2 WHERE t1.CourseCRN IN (SELECT CourseCRN FROM "
					+ table
					+ " GROUP BY CourseCRN, MeetBeginTime, MeetEndTime HAVING COUNT(CourseCRN) =2)"
					+ " AND t1.CourseCRN = t2.CourseCRN AND t1.MeetBeginTime"
					+ " = t2.MeetBeginTime AND t1.MeetEndTime = t2.MeetEndTime;";
			ResultSet rs = st.executeQuery(select);
			while (rs.next()) {
				String crn = rs.getString(1);
				String getDays = "SELECT Days, " + table + ".* FROM " + table
						+ " WHERE CourseCRN = '" + crn + "';";
				ResultSet rs2 = st2.executeQuery(getDays);

				// printResultSet(rs2);
				// rs2.first();
				String days = "";
				String temp = "";
				while (rs2.next())
					temp += rs2.getString(1);
				String[] poss = { "M", "T", "W", "R", "F" };
				for (String s : poss) {
					if (temp.contains(s))
						days += s + " ";
				}
				days = days.substring(0, days.length() - 1);
				rs2.close();

				String query = "UPDATE " + table + " AS t SET Days = '" + days
						+ "' WHERE t.CourseCRN = '" + crn + "';";
				st2.executeUpdate(query);

				String query2 = "CREATE TABLE temp AS (SELECT DISTINCT * FROM "
						+ table + ");";
				String query3 = "RENAME TABLE " + table + " TO toDelete;";
				String query4 = "RENAME TABLE temp TO " + table + ";";
				String query5 = "DROP TABLE toDelete;";
				st2.executeUpdate(query2);
				st2.executeUpdate(query3);
				st2.executeUpdate(query4);
				st2.executeUpdate(query5);

			}
		}

	}

	public void displayStudentFinals(int index) throws SQLException {
		if (true) {
			Statement st = con.createStatement();
			Statement st2 = con.createStatement();

			String stu = STUDS + Sems[index];
			String fin = FINS + Sems[index];
			String cou = COURSES + Sems[index];
			String sch = SCHEDS + Sems[index];
			String swf = SWF + Sems[index];

			ResultSet students = st
					.executeQuery("SELECT DISTINCT StudentIDNo FROM " + swf
							+ ";");
			while (students.next()) {
				String id = students.getString(1);
				String coursesTimes = "SELECT " + join(swf, "CourseTitle")
						+ ", " + join(sch, "*") + " FROM " + swf + ", " + sch
						+ " WHERE ";
				String condition = cond(swf, "MeetBeginTime", sch, "start")
						+ "AND" + cond(swf, "MeetEndTime", sch, "end") + "AND "
						+ join(swf, "StudentIDNo") + " = '" + id
						+ "' AND REPLACE(" + join(swf, "Days")
						+ ",' ','') = REPLACE(" + join(sch, "pattern")
						+ ",' ','');";
				String query = coursesTimes + condition;
				prl(query);
				ResultSet rs = st2.executeQuery(query);
				printResultSet(rs);
				rs.close();
			}
			students.close();
			st.close();
		}

	}

	// for (int i = 0; i < Sems.length-1; i++) --was used to create all
	// studswfins tables
	// createStudWFins(Sems[i]);

	public static void createStudWFins(String semester) throws SQLException {
		Statement st = con.createStatement();
		String name = "studswfins";
		String drop = "DROP TABLE IF EXISTS " + name + semester + ";";
		String query = "CREATE TABLE " + name + semester + " AS (SELECT "
				+ STUDS + semester + ".* " + "FROM " + STUDS + semester + ", "
				+ FINS + semester + " WHERE " + STUDS + semester
				+ ".CourseCRN = " + FINS + semester + ".CourseCRN);";
		String query2 = "ALTER TABLE " + name + semester
				+ " ADD COLUMN finalDay INT NOT NULL DEFAULT '-1';";
		String query3 = "ALTER TABLE " + name + semester
				+ " ADD COLUMN finalTime INT NOT NULL DEFAULT '-1';";
		prl(query);
		st.executeUpdate(drop);
		st.executeUpdate(query);
		st.executeUpdate(query2);
		st.executeUpdate(query3);
		st.close();
	}

	// for (int i = 0; i < Sems.length - 1; i++)
	// //--was used to create all studswfins tables
	// createCourseWFins(Sems[i]);

	public static void createCourseWFins(String semester) throws SQLException {
		Statement st = con.createStatement();
		String name = "courseswfins";
		String drop = "DROP TABLE IF EXISTS " + name + semester + ";";
		String query = "CREATE TABLE " + name + semester + " AS (SELECT "
				+ COURSES + semester + ".* " + "FROM " + COURSES + semester
				+ ", " + FINS + semester + " WHERE " + COURSES + semester
				+ ".CourseCRN = " + FINS + semester + ".CourseCRN);";
		String query2 = "ALTER TABLE " + name + semester
				+ " ADD COLUMN finalDay INT NOT NULL DEFAULT '-1';";
		String query3 = "ALTER TABLE " + name + semester
				+ " ADD COLUMN finalTime INT NOT NULL DEFAULT '-1';";
		prl(query);
		st.executeUpdate(drop);
		st.executeUpdate(query);
		st.executeUpdate(query2);
		st.executeUpdate(query3);
		st.close();

	}

	public static String join(String course, String join) {
		return course + "." + join;
	}

	public static String cond(String c1, String j1, String c2, String j2) {
		return " " + join(c1, j1) + " = " + join(c2, j2) + " ";

	}

	public static void main(String[] args) {
		try {
			doThings();
		} catch (SQLException e) {
			prl(e.getMessage());
		}
	}

	public static void printResultSet(ResultSet rs) {
		try {
			ResultSetMetaData meta = rs.getMetaData();
			for (int i = 0; i < meta.getColumnCount(); ++i) {
				pr(meta.getColumnLabel(i + 1) + "    ");
			}
			prl("");
			while (rs.next()) {
				for (int i = 0; i < meta.getColumnCount(); ++i) {
					pr(rs.getString(i + 1) + "    ");
				}
				prl("");
			}
		} catch (SQLException e) {
			prl(e.getMessage());
		}
	}

	public static void createConnection() {
		String url = "jdbc:mysql://localhost:3306/leep";
		String user = "javauser";
		String password = "testpass";
		conn = new MySQLConnect(url, user, password);
		conn.connect();
		con = conn.getConnect();
	}

	public static void prl(String s) {
		System.out.println(s);
	}

	public static void pr(String s) {
		System.out.print(s);
	}

	public static void prl(boolean s) {
		System.out.println(s);
	}

	public static void pr(boolean s) {
		System.out.print(s);
	}

}

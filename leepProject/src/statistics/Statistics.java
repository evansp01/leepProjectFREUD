package statistics;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement; 

import java.util.*;

import databaseConnector.MySQLConnect;

public class Statistics {

	static final String[] Sems = { "201009", "201101", "201109", "201201",
			"201209", "201301", "201309" };
	static final String STUDS = "students", FINS = "finals",
			COURSES = "courses", SCHEDS = "finalSchedule", SWF = "studswfins";
	static MySQLConnect conn = null;
	static Connection con = null;

	// String cond = join(stu, "CourseCRN") + " = " + join(cou, "CourseCRN") +
	// " AND " + join(cou, "Days") + " = "
	// + join(sch, "pattern") + " AND " + join(cou, "MeetBeginTime") + " = " +
	// join(sch, "start") + " AND "
	// + join(stu, "StudentIDNo") + " = '" + id + "'";

	// SELECT studswfins201209.CourseTitle, finalSchedule201209.* FROM
	// studswfins201209, finalSchedule201209 WHERE
	// studswfins201209.MeetBeginTime = finalSchedule201209.start AND
	// studswfins201209.StudentIDNo = 'C70178809' AND
	// studswfins201209.MeetEndTime = finalSchedule201209.end AND
	// REPLACE(studswfins201209.Days,' ','') =
	// REPLACE(finalSchedule201209.pattern,' ','');

//	public static void doThings() throws SQLException {
//		createConnection();
//
//		Statement st = con.createStatement();
//		Statement st2 = con.createStatement();
//
//		int index = 4;
//		String stu = STUDS + Sems[index];
//		String fin = FINS + Sems[index];
//		String cou = COURSES + Sems[index];
//		String sch = SCHEDS + Sems[index];
//		String swf = SWF + Sems[index];
//
//		ResultSet students = st
//				.executeQuery("SELECT DISTINCT StudentIDNo FROM " + swf + ";");
//		while (students.next()) {
//
//			String id = students.getString(1);
//			String coursesTimes = "SELECT " + join(swf, "CourseTitle") + ", "
//					+ join(sch, "*") + " FROM " + swf + ", " + sch + " WHERE ";
//			String condition = cond(swf, "MeetBeginTime", sch, "start") + "AND"
//					+ cond(swf, "MeetEndTime", sch, "end") + "AND "
//					+ join(swf, "StudentIDNo") + " = '" + id + "' AND REPLACE("
//					+ join(swf, "Days") + ",' ','') = REPLACE("
//					+ join(sch, "pattern") + ",' ','');";
//			String query = coursesTimes + condition;
//			prl(query);
//			ResultSet rs = st2.executeQuery(query);
//			printResultSet(rs);
//			rs.close();
//
//		}
//		students.close();
//		st.close();
//
//	}

	public static void backToBack(int index) throws SQLException { 
		try {
			PrintStream out = new PrintStream(new FileOutputStream("BackToBack" + Sems[index] + ".txt")); 
			System.setOut(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		
		createConnection();

		Statement st = con.createStatement();
		Statement st2 = con.createStatement(); 
		Statement st3 = con.createStatement(); 
		Statement st4 = con.createStatement(); 

		int backToBackTotal = 0; 
		int threeExamsTotal = 0;

		String stu = STUDS + Sems[index];
		String fin = FINS + Sems[index];
		String cou = COURSES + Sems[index];
		String sch = SCHEDS + Sems[index];
		String swf = SWF + Sems[index];

		ResultSet students = st
				.executeQuery("SELECT DISTINCT StudentIDNo FROM " + swf + ";");

		while (students.next()) {

			String id = students.getString(1); 
			

			String query = "SELECT finalDay, finalTime, CourseTitle, MeetBeginTime, MeetEndTime, Days FROM "
					+ swf + " WHERE  " + "StudentIDNo = '" + id + "' GROUP BY CourseTitle;";

			ResultSet secondFilter = st2.executeQuery(query);

			boolean[][] dataBlocks = new boolean[4][5];

			while (secondFilter.next()) {
				
				int day = secondFilter.getInt(1);
				int block = secondFilter.getInt(2);
				String courseTitle = secondFilter.getString(3);
				int startTime = secondFilter.getInt(4);
				int endTime = secondFilter.getInt(5);   
				String meetDays = secondFilter.getString(6);
				
				if(block>=1){
				
				dataBlocks[day - 1][block - 1] = true;  
				ArrayList<Integer> dayBlocks = sequential(dataBlocks, day-1, block-1);   
				if (dayBlocks.size()>0)	{   
					System.out.println("Back to Back Courses for student: " + id );
					int day1=dayBlocks.get(0) + 1; 
					int block1=dayBlocks.get(1) + 1;  
					
					String query2="SELECT CourseTitle, MeetBeginTime, MeetEndTime, Days FROM " + swf +  
							" WHERE  " + "StudentIDNo = '" + id + "' AND finalDay= " + day1 + " AND finalTime= " + block1 + ";";  
					ResultSet thirdFilter = st3.executeQuery(query2);      
					
					thirdFilter.next();
					String courseTitleConf= thirdFilter.getString(1); 
					int startTimeConf = thirdFilter.getInt(2); 
					int endTimeConf = thirdFilter.getInt(3); 
					String meetDaysConf = thirdFilter.getString(4);   
					thirdFilter.close();
					 
					
					System.out.println("Course: " + courseTitleConf + " Final Exam Day and Block: " + day + ", " + block1 + " Meets: " + 
							meetDaysConf + " " + startTimeConf + "-" + endTimeConf + ";");  
					backToBackTotal++;
					
					if (dayBlocks.size()==4) {  
						int day2=dayBlocks.get(2)+1; 
						int block2=dayBlocks.get(3)+1;  
					 
						
						String query3="SELECT CourseTitle, MeetBeginTime, MeetEndTime, Days FROM " + swf +  
								" WHERE  " + "StudentIDNo = '" + id + "' AND finalDay= " + day1 + " AND finalTime= " + block2 + ";";   
						ResultSet fourthFilter = st4.executeQuery(query3); 
						
						fourthFilter.next();
						String courseTitleConf2= fourthFilter.getString(1); 
						int startTimeConf2 = fourthFilter.getInt(2); 
						int endTimeConf2 = fourthFilter.getInt(3); 
						String meetDaysConf2 = fourthFilter.getString(4);    
						
						
						System.out.print("Course: " + courseTitleConf2 + " Final Exam Day and Block: " + day1 + ", " + block2 + " Meets: " + 
								meetDaysConf2 + " " + startTimeConf2 + "-" + endTimeConf2);   
						
						System.out.println("<<------------Three in a row");
						
						fourthFilter.close(); 
						threeExamsTotal++;
					} 
					
					System.out.println("Course: " + courseTitle + " Final Exam Day and Block: " + day + ", " + block + " Meets: " + 
							meetDays + " " + startTime + "-" + endTime);  
					System.out.println("*******************");
				}
		
			} 
			}

			secondFilter.close();

		}

		students.close();   
		st4.close();
		st3.close(); 
		st2.close();
		st.close();

		System.out.println("Number of back to back exams: " + backToBackTotal); 
		System.out.println("Number of back to back to back (!!) exams: " + threeExamsTotal);

	}



	public static void main(String[] args) {
		try {   
			for (int i=3; i<=6; i++) {
				backToBack(i); 
			}  
			
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

	
	public static ArrayList<Integer> sequential(boolean [][] examSched, int day, int block){  
			ArrayList<Integer> dayBlock = new ArrayList<Integer>();   
		    if(block==0) {
		    	if (examSched[day][block+1]){ //first block, check only block after  
		    		dayBlock.add(day); 
		    		dayBlock.add(block+1); 
		    	
		    		if (examSched[day][block+2]) {
		    			dayBlock.add(day); 
		    			dayBlock.add(block+2); 
		    			return dayBlock;  
		    		}
		    		else 
		    			return dayBlock; 
		    	} 
		    	
		    	else 
		    		return dayBlock;
		    }	
		    else if(block==examSched[day].length-1) {
		    		if (examSched[day][block-1]) { 
		    			dayBlock.add(day); 
		    			dayBlock.add(block-1);  
		    			
		    			if (examSched[day][block-2]) {
			    			dayBlock.add(day); 
			    			dayBlock.add(block-2); 
			    			return dayBlock;  
			    		} 
		    			
		    			else 
		    				return dayBlock;
		    		}  
		    		
		    		else 
		    			return dayBlock;
		    }
		    		
		    else if (examSched[day][block-1] || examSched[day][block+1]) {//middle block, check before and after  
		    	if (examSched[day][block-1]) { 
		    		dayBlock.add(day); 
		    		dayBlock.add(block-1); 
		    		
		    		if (examSched[day][block+1]){ 
		    			dayBlock.add(day); 
		    			dayBlock.add(block+1); 
		    			return dayBlock;
		    		} 
		    		else 
		    			return dayBlock; 
		    			
		    	}    
		    	else {
		    		dayBlock.add(day); 
		    		dayBlock.add(block+1); 
		    		return dayBlock; 
		    	}
		    } 
		    else 
				return dayBlock;
		    	
		
		    	
		   }
	
	public static String printQuery(int day, int block, String id, String table){ 
		String query = "SELECT finalDay, finalTime, CourseTitle, MeetBeginTime, MeetEndTime, Days FROM "
				+ table + " WHERE  " + "StudentIDNo = '" + id + "' AND finalDay= " + day + " AND finalTime= " + block; 
		
		return query;
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
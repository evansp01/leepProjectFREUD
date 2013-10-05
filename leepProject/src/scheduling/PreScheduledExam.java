package scheduling;

/**
 * A class to hold the day,block,and time of an already scheduled exam
 * 
 * @author Evan Palmer and Dana Ferranti
 * 
 */
public class PreScheduledExam {
    String name;
    int day;
    int block;

    /**
     * construct a prescheduled exam
     * 
     * @param name
     *            crn of the class
     * @param d
     *            day of the exam
     * @param t
     *            block of the exam
     */
    public PreScheduledExam(String name, int d, int b) {
	this.name = name;
	day = d;
	block = b;
    }

}

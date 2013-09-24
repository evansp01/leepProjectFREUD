package consoleThings;

import java.util.Arrays;

/**
 * Student class holds additional information about students -- their student idF and
 * when they have exams
 * 
 * @author evan
 * 
 */

public class FStudent {

    private String name = null;
    private boolean[][] blocks;

    /**
     * 
     * @param name
     *            student id number
     * @param days
     *            the number of final days currently being used
     * @param blocksPerDay
     *            the number of blocks in each day
     */
    public FStudent(String name, int days, int blocksPerDay) {
	this.name = name;
	blocks = new boolean[days][blocksPerDay];
    }

    /**
     * 
     * @return returns the student id associated with this student
     */
    public String name() {
	return name;
    }

    /**
     * resets the occupancy of this student -- sets all blocks to unoccupied
     */
    public void resetOccupancy() {
	for (boolean[] i : blocks) {
	    Arrays.fill(i, false);
	}
    }

    /**
     * sets a block to occupied
     * 
     * @param day
     *            the day of the block
     * @param block
     *            the block number
     */
    public void occupy(int day, int block) {
	blocks[day][block] = true;
    }

    /**
     * 
     * @param day
     *            the day of the block to check
     * @param block
     *            the block number
     * @return returns true if the block is occupied, false otherwise
     */
    public boolean isOccupied(int day, int block) {
	return blocks[day][block];
    }

    /**
     * counts the number of exams in a day
     * 
     * @param day
     *            the day to count exams of
     * @return the number of exams in that day
     */
    public int examsInDay(int day) {
	int exams = 0;
	for (boolean b : blocks[day]) {
	    if (b)
		exams++;
	}
	return exams;
    }

    /**
     * returns true if there are N exams or more in the given day
     * 
     * @param N
     *            the threshold number of exams
     * @param day
     *            the day
     * @return returns true if examsInDay() >= N
     */
    public boolean gtNExamsInDay(int N, int day) {
	return examsInDay(day) >= N;
    }
}

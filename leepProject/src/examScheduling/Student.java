package examScheduling;

import java.util.Arrays;

public class Student {

    private String name = null;
    private boolean[][] blocks;

    public Student(String name, int days, int blocksPerDay) {
	this.name = name;
	blocks = new boolean[days][blocksPerDay];
    }

    public String name() {
	return name;
    }

    public void resetOccupancy() {
	for (boolean[] i : blocks) {
	    Arrays.fill(i, false);
	}
    }

    public void occupy(int day, int block) {
	blocks[day][block] = true;
    }

    public boolean isOccupied(int day, int block) {
	return blocks[day][block];
    }

    public int examsInDay(int day) {
	int exams = 0;
	for (boolean b : blocks[day]) {
	    if (b)
		exams++;
	}
	return exams;
    }

    public boolean gtNExamsInDay(int N, int day) {
	return examsInDay(day) >= N;
    }
}

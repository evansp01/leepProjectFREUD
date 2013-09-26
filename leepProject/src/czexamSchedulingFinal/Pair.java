package czexamSchedulingFinal;

public class Pair { 
	
	public int v1; 
	public int v2; 
	
	Pair (int v1, int v2) { 
		this.v1=v1; 
		this.v2=v2; 
		
	} 
	
	public int day() { 
		return v1;
	} 
	
	public int block() { 
		return v2;
	}

}

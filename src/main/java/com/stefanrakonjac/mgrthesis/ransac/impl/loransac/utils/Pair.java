/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.loransac.utils;

/**
 * @author Stefan.Rakonjac
 *
 */
public class Pair {

	private double x1;
	private double y1;
	private double z1 = 1.0;
	private double x2;
	private double y2;
	private double z2 = 1.0;
	
	public Pair(String line) {
		
		if(line == null)
			throw new IllegalArgumentException("line is null");
		
		final String[] split = line.split(",[\\s]*");
		
		if(split.length == 4) {

			this.setX1(new Double(split[0]))
				.setY1(new Double(split[1]))
				.setX2(new Double(split[2]))
				.setY2(new Double(split[3]));
			
		} else if(split.length == 6) {

			this.setX1(new Double(split[0]))
				.setY1(new Double(split[1]))
				.setZ1(new Double(split[2]))
				.setX2(new Double(split[3]))
				.setY2(new Double(split[4]))
				.setZ2(new Double(split[5]));
			
		} else {
			throw new IllegalArgumentException("bad format");
		}
	}
	
	public Pair(double[] pair) {
		
		if(pair == null)
			throw new IllegalArgumentException("pair");
		
		if(pair.length == 4) {

			this.setX1(new Double(pair[0]))
				.setY1(new Double(pair[1]))
				.setX2(new Double(pair[2]))
				.setY2(new Double(pair[3]));
			
		} else if(pair.length == 6) {

			this.setX1(new Double(pair[0]))
				.setY1(new Double(pair[1]))
				.setZ1(new Double(pair[2]))
				.setX2(new Double(pair[3]))
				.setY2(new Double(pair[4]))
				.setZ2(new Double(pair[5]));
			
		} else {
			throw new IllegalArgumentException("bad length");
		}
	}
	
	public double getX1() {
		return x1;
	}
	
	public Pair setX1(double x1) {
		this.x1 = x1;
		return this;
	}
	
	public double getY1() {
		return y1;
	}
	
	public Pair setY1(double y1) {
		this.y1 = y1;
		return this;
	}
	
	public double getZ1() {
		return z1;
	}
	
	public Pair setZ1(double z1) {
		this.z1 = z1;
		return this;
	}
	
	public double getX2() {
		return x2;
	}
	
	public Pair setX2(double x2) {
		this.x2 = x2;
		return this;
	}
	
	public double getY2() {
		return y2;
	}
	
	public Pair setY2(double y2) {
		this.y2 = y2;
		return this;
	}
	
	public double getZ2() {
		return z2;
	}
	
	public Pair setZ2(double z2) {
		this.z2 = z2;
		return this;
	}
	
	public double[] toArray(boolean fullFormat) {
		
		if(fullFormat) 
			return new double[] { x1, y1, z1, x2, y2, z2 };
		
		else 
			return new double[] { x1, y1, x2, y2 };
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(x2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (Double.doubleToLongBits(x1) != Double.doubleToLongBits(other.x1))
			return false;
		if (Double.doubleToLongBits(x2) != Double.doubleToLongBits(other.x2))
			return false;
		if (Double.doubleToLongBits(y1) != Double.doubleToLongBits(other.y1))
			return false;
		if (Double.doubleToLongBits(y2) != Double.doubleToLongBits(other.y2))
			return false;
		if (Double.doubleToLongBits(z1) != Double.doubleToLongBits(other.z1))
			return false;
		if (Double.doubleToLongBits(z2) != Double.doubleToLongBits(other.z2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Pair [x1=" + x1 + ", y1=" + y1 + ", z1=" + z1 + ", x2=" + x2 + ", y2=" + y2 + ", z2=" + z2 + "]";
	}
}

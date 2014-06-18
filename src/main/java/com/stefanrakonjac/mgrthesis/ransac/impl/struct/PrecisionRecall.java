/**
 * 
 */
package com.stefanrakonjac.mgrthesis.ransac.impl.struct;

/**
 * @author Stefan.Rakonjac
 *
 */
public class PrecisionRecall {
	
	private String imageName;
	private double precision;
	private double recall;
	
	public PrecisionRecall(String imageName, double precision, double recall) {
		this();
		this.imageName = imageName;
		this.precision = precision;
		this.recall = recall;
	}
	
	public PrecisionRecall() {
		super();
	}
	
	public String getImageName() {
		return imageName;
	}

	public PrecisionRecall setImageName(String imageName) {
		this.imageName = imageName;
		return this;
	}

	public double getPrecision() {
		return precision;
	}

	public PrecisionRecall setPrecision(double precision) {
		this.precision = precision;
		return this;
	}

	public double getRecall() {
		return recall;
	}

	public PrecisionRecall setRecall(double recall) {
		this.recall = recall;
		return this;
	}

	@Override
	public String toString() {
		return "PrecisionRecallScore [imageName=" + imageName + ", precision=" + precision + ", recall=" + recall + "]";
	}
}

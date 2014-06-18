package com.stefanrakonjac.mgrthesis.ransac.impl.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageScore implements Comparable<ImageScore> {
	
	private String imageName;
	private short imageID;
	private double score = 0d;
	
	public ImageScore(String imageName) {
		this.imageName = imageName;
	}
	
	public ImageScore(short imageID) {
		this.imageID = imageID;
	}
	
	public double getScore() {
		return score;
	}

	public ImageScore setScore(double score) {
		this.score = score;
		return this;
	}

	public void increaseScore(final double score) {
		this.score += score;
	}

	public String getImageName() {
		return imageName;
	}

	public ImageScore setImageName(String imageName) {
		this.imageName = imageName;
		return this;
	}

	public short getImageID() {
		return imageID;
	}

	@Override
	public int compareTo(ImageScore o) {
		return this.score - o.score < 0 ? -1 : this.score == o.score ? 0 : 1;
	}

	@Override
	public String toString() {
		return "Score [imageName=" + imageName + ", score=" + score + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((imageName == null) ? 0 : imageName.hashCode());
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
		ImageScore other = (ImageScore) obj;
		if (imageName == null) {
			if (other.imageName != null)
				return false;
		} else if (!imageName.equals(other.imageName))
			return false;
		return true;
	}

	public static List<ImageScore> createList(ArrayList<String> sortedSimilarImages) {
		
		if(sortedSimilarImages == null)
			throw new IllegalArgumentException("sortedSimilarImages");
		
		final List<ImageScore> retval = new ArrayList<>();
		
		int score = sortedSimilarImages.size();
		for(String sortedSimilarImage : sortedSimilarImages) {
			retval.add(new ImageScore(sortedSimilarImage).setScore(score--));
		}
		
		return retval;
	}

	public static List<ImageScore> createList(Map<String, Map<String, double[][]>> similarImages) {
		
		if(similarImages == null)
			throw new IllegalArgumentException("similarImages");
		
		final List<ImageScore> retval = new ArrayList<>();
		
		for(String sortedSimilarImage : similarImages.keySet()) {
			
			int pairsTotal = 0;
			for(double[][] pairs : similarImages.get(sortedSimilarImage).values()) {
				pairsTotal += pairs.length;
			}
			
			retval.add(new ImageScore(sortedSimilarImage).setScore(pairsTotal));
		}
		
		return retval;
	}
}

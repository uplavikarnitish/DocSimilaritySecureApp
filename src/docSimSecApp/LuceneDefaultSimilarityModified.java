package docSimSecApp;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * Created by nuplavikar on 7/12/15.
 */
public class LuceneDefaultSimilarityModified extends DefaultSimilarity
{
	@Override
	public float coord(int overlap, int maxOverlap)
	{
		//System.out.println("coord:: This is working!! overlap:"+overlap+" maxOverlap:"+maxOverlap);
		float coord = super.coord(overlap, maxOverlap);
		System.out.println("Coord() value returned = "+ coord);
		//new Exception("In coord()").printStackTrace();
		return coord;
	}

	@Override
	public float lengthNorm(FieldInvertState state)
	{
		return (float)1;
	}

	@Override
	public float sloppyFreq(int distance)
	{
		return 1;
	}

	@Override
	public float queryNorm(float sumOfSquaredWeights)
	{
		float queryNorm = super.queryNorm(sumOfSquaredWeights);
		return queryNorm;
	}

	@Override
	public float tf(float freq)
	{
		Exception e = new Exception();
		float temp = super.tf(freq);
		//System.out.println("Returning calculated frequency = "+temp);
		//e.printStackTrace();
		return temp;
	}


}

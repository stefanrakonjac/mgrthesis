package messif.objects.text.lucene;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Similarity;

/**
 * Data object that encapsulates objects stored in the Lucene indexing engine.
 * The distance function is a cosine distance on term weights defined by the Lucene.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class ObjectLuceneCosineDist extends LocalAbstractObject {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;
    /** Document term weights */
    private final Map<String, Double> termWeights;

    /**
     * Creates a Lucene document object for the given document ID.
     * @param documentId the Lucene internal document ID
     * @param reader the Lucene index reader to use to read the document
     * @param similarity the Lucene {@link Similarity} definition
     * @throws IOException if there was a problem reading the document by the reader
     */
    public ObjectLuceneCosineDist(int documentId, IndexReader reader, Similarity similarity) throws IOException {
        this.termWeights = LuceneAlgorithm.getDocumentTermWeights(documentId, reader, similarity);
    }

    /**
     * Returns the sum of weight squares of this object.
     * @return the sum of weight squares
     */
    protected final double getWeightsSqSum() {
        double weightsSqRootSum = 0;
        for (Double termWeight : termWeights.values())
            weightsSqRootSum += termWeight.doubleValue() * termWeight.doubleValue();
        return weightsSqRootSum;
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        ObjectLuceneCosineDist castObj = (ObjectLuceneCosineDist)obj;
        // Compute dot product of weights in both computers
        double dotProduct = 0;
        double weightsSqSum = 0;
        for (Map.Entry<String, Double> termWeight : termWeights.entrySet()) {
            double thisWeight = termWeight.getValue();
            Double otherWeight = castObj.termWeights.get(termWeight.getKey());
            if (otherWeight != null)
                dotProduct += thisWeight * otherWeight;
            weightsSqSum += thisWeight * thisWeight;
        }
        // Compute distance (dot product divided by the multiplication of the norms)
        return (float)(1 - dotProduct / Math.sqrt(weightsSqSum * castObj.getWeightsSqSum()));
    }

    @Override
    public int getSize() {
        return Integer.SIZE / 8;
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (obj instanceof ObjectLuceneCosineDist)
            return termWeights.equals(((ObjectLuceneCosineDist)obj).termWeights);
        else
            return false;
    }

    @Override
    public int dataHashCode() {
        return termWeights.hashCode();
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Cannot write object stored by the Lucene algorithm");
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.quantization.kmeans;

import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureByteL2;
import messif.objects.impl.ObjectFeatureFloatL2;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.AbstractObjectIterator;
import messif.quantization.AbstractVisualVocabulary;

/**
 * Serializable visual vocabulary implementation with k-means usage.
 * @author Marian Labuda
 */
public class KMeansVisualVocabulary extends AbstractVisualVocabulary {
    /** Serial ID for serialization */
    private static final long serialVersionUID = 1;
    
    private KMeansNode root = null;
    private int k;
    private int depth;
    
    //***********************************************
    //********* VOCABULARY IMPLEMENTATION ***********
    //***********************************************
    
    @Override
    public boolean createVocabulary(AbstractObjectIterator<ObjectFeature> objs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getVocabularySize() {
        return root.getChildNodesNumber();
    }

    @Override
    public ObjectFeatureQuantized convertToFeature(ObjectFeature feature) {
        ObjectFeatureQuantized quantizedFeature = new ObjectFeatureQuantized(feature.getX(),
                feature.getY(), feature.getOrientation(), feature.getScale(), 
                new long[] {getClusterNumber(feature, depth)});
        return quantizedFeature;
    }
    
    //***********************************************
    //********* K-MEANS IMPLEMENTATION **************
    //***********************************************

    protected long getClusterNumber(LocalAbstractObject o, int MaxLevel) {
        if (o instanceof ObjectFeatureByteL2) {
            ObjectFeatureByteL2 bf = (ObjectFeatureByteL2)o;
            // Convert it to ObjectFeatureFloatL2
            o = new ObjectFeatureFloatL2(bf.getX(), bf.getY(), bf.getOrientation(), bf.getScale(), shortArrayToFloatArray(bf.getVectorData()));
        }
        
        return root.getClusterID((ObjectFeatureFloatL2) o, 0, MaxLevel);
    }
    
    private float[] shortArrayToFloatArray(short[] shortVec) {
        float[] floatVec = new float[shortVec.length];
        for (int i = 0; i < shortVec.length; i++)
            floatVec[i] = shortVec[i];
        return floatVec;
    }

//    //***********************************************
//    //********** CONSTRUCTION ***********************
//    //***********************************************
//    
//    public KMeansVisualVocabulary(int k, int depth) {
//        this.k = k;
//        this.depth = depth;
//        root = new KMeansNode(k, depth);
//    }

    //***********************************************
    //********** LEGACY SERIALIZATION ***************
    //***********************************************
    
    /**
     * Reads the k-means visual vocabulary from a legacy text file, instantiates it and stores it using binary serialization.
     * 
     * @param directoryRoot directory with legacy text files
     * @param filePrefix prefix of name of the legacy text file
     * @param method method used to create the legacy text file
     * @param output output for binary serialization
     * @throws IOException on error 
     */
    public void importFromDirectory(String directoryRoot, String filePrefix, String method, BinaryOutput output) throws IOException {
        if (root.readFromTextFile(directoryRoot, filePrefix, 0, "", method) > 0) {
            binarySerialize(output, AbstractVisualVocabulary.getBinarySerializator(KMeansVisualVocabulary.class));
        }
    }

    
    //***********************************************
    //********** BINARY SERIALIZATION ***************
    //***********************************************
    
    /**
     * Constructor of k-means visual vocabulary. Read already created vocabulary.
     * @param input binary input of k-means vocabulary
     * @param serializator binary serializator of k-means vocabulary (k-means tree)
     */
    public KMeansVisualVocabulary(BinaryInput input, BinarySerializator serializator) throws IOException {
        this.k = serializator.readByte(input);
        this.depth = serializator.readByte(input);
        root = new KMeansNode(input, serializator);
    }
    
    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int written = serializator.write(output, (byte)k);
        written += serializator.write(output, (byte)depth);
        if (root != null) {
            written += root.binarySerialize(output, serializator);
        }
        return written;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize((byte)k) + serializator.getBinarySize((byte)depth) + 
               ((root == null) ? 0 : root.getBinarySize(serializator));
    }
}
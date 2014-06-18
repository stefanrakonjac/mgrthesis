package messif.quantization.kmeans;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFeatureByteL2;
import messif.objects.impl.ObjectFeatureFloatL2;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * @author xhomola
 * @deprecated Use {@link KMeansVisualVocabulary} instead.
 */
@Deprecated
public class KMeansTree implements Serializable{
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    private KMeansNode root = null;
    private int k;
    private int depth;
    
    public KMeansTree (int k, int depth) {
        this.k = k;
        this.depth = depth;
        root = new KMeansNode(k, depth);
    }

    public void importFromDirectory (String DirectoryRoot, String FilePrefix, String Method, BinaryOutput output, BinarySerializator serializator) {
        try {
            if (root.readFromTextFile(DirectoryRoot, FilePrefix, 0, "", Method) > 0) {
                binarySerialize(output, serializator);
            }
        }
        catch (Exception ex) { }
    }

    public KMeansTree (BinaryInput input, BinarySerializator serializator) throws IOException {
        this.k = serializator.readByte(input);
        this.depth = serializator.readByte(input);
        root = new KMeansNode (input, serializator);
    }

    public void binarySerialize (BinaryOutput output, BinarySerializator serializator) throws IOException {
        serializator.write(output, (int) k);
        serializator.write(output, (int) depth);
        if (root != null) {
            root.binarySerialize(output, serializator);
        }
    }
    
    public long getClusterNumber (LocalAbstractObject o, int MaxLevel) {
        if (o instanceof ObjectFeatureByteL2) {
            ObjectFeatureByteL2 bf = (ObjectFeatureByteL2)o;
            // Convert it to ObjectFeatureFloatL2
            o = new ObjectFeatureFloatL2(bf.getX(), bf.getY(), bf.getOrientation(), bf.getScale(), shortArrayToFloatArray(bf.getVectorData()));
        }
        
        return root.getClusterID((ObjectFeatureFloatL2) o, 0, MaxLevel);
    }
    
    public int getNodesNumber () {
        return root.getChildNodesNumber();
    }

    public int getDepth() {
        return depth;
    }

    private float[] shortArrayToFloatArray(short[] shortVec) {
        float[] floatVec = new float[shortVec.length];
        for (int i = 0; i < shortVec.length; i++)
            floatVec[i] = shortVec[i];
        return floatVec;
    }
}

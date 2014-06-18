package messif.quantization.kmeans;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import messif.objects.impl.ObjectFeatureFloatL2;
import messif.objects.impl.ObjectFloatVectorL2;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

public class KMeansNode implements BinarySerializable, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    private ArrayList<float[]> keys;
    private ArrayList<KMeansNode> children;

    //*****************************************************
    //************ ACCESS METHODS *************************
    //*****************************************************
        
    public long getClusterID (ObjectFeatureFloatL2 o, int currentLevel, int maxLevel) {
        float minDist = Float.MAX_VALUE;
        int minIdx = -1;
        for (int i = 0; i < keys.size(); i++) {    
            ObjectFeatureFloatL2 o2 = new ObjectFeatureFloatL2(0, 0, 0, 0, keys.get(i));
            float d = o.getDistance(o2);            
            if (d < minDist) {
                minDist = d;
                minIdx = i;
            }
        }
        long clusterID = (long) Math.pow(10, currentLevel) * minIdx;
        if (children.get(minIdx) != null && currentLevel + 1 < maxLevel) {
            clusterID +=   children.get(minIdx).getClusterID(o, currentLevel+1, maxLevel);
        }
        return clusterID;
    }
    
    public int getChildNodesNumber() {
        if (children == null) {
            return 0;
        }
        int val = children.size();
        for (int i = children.size()-1; i >= 0; i--) {
            if (children.get(i) != null) {
                val += children.get(i).getChildNodesNumber();
            }
        }
        return val;
    }
    
    //*****************************************************
    //************ BINARY SERIALIZATION *******************
    //*****************************************************
    
    public KMeansNode (BinaryInput input, BinarySerializator serializator) throws IOException {
        int numOfChildren = serializator.readByte(input);
        keys = new ArrayList<float[]>(numOfChildren);
        for (int i = 0; i < numOfChildren; i++) {
            try {
                float [] o = serializator.readFloatArray(input);
                keys.add(o);
            }
            catch (IOException ex) 
            { 
                throw ex;
            }
        }
        List<Boolean> hasChild = new ArrayList<Boolean>(numOfChildren);
        for (int i = 0; i < numOfChildren; i++) {
            hasChild.add( (serializator.readByte(input) != 0) );
        }
        
        children = new ArrayList<KMeansNode>(numOfChildren);
        for (int i = 0; i < numOfChildren; i++) {
            if (hasChild.get(i)) {
                children.add(new KMeansNode(input, serializator));
            } else {
                children.add(null);
            }
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int written = serializator.write(output, (byte)keys.size());
        for (int i = 0; i < keys.size(); i++) {
            written += serializator.write(output, keys.get(i));
        }
        for (int i = 0; i < keys.size(); i++) {
            written += serializator.write(output, ((children.get(i) != null) ? (byte)1 : (byte)0) );
        }
        for (int i = 0; i < keys.size(); i++) {        
            if (children.get(i) != null) {
                written += children.get(i).binarySerialize(output, serializator);
            }
        }
        return written;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int s = serializator.getBinarySize((byte)keys.size());
        for (int i = 0; i < keys.size(); i++) {
            s += serializator.getBinarySize(keys.get(i));
        }
        for (int i = 0; i < keys.size(); i++) {
            s += serializator.getBinarySize((children.get(i) != null) ? (byte)1 : (byte)0);
        }
        for (int i = 0; i < keys.size(); i++) {        
            if (children.get(i) != null) {
                s += children.get(i).getBinarySize(serializator);
            }
        }
        return s;
    }

    //*****************************************************
    //************ TEXT SERIALIZATION *********************
    //*****************************************************
    
    /** For reading from a text file */
    private transient int maxChildren;
    /** For reading from a text file */
    private transient int maxNestedLevel;
    
    /** Constructor for creating an instance to be initialized from a text file */
    public KMeansNode(int MaxChilds, int MaxLevel) {
        keys = new ArrayList<float[]>();
        children = new ArrayList<KMeansNode>();
        this.maxChildren = MaxChilds; 
        this.maxNestedLevel = MaxLevel;
    }

    public int readFromTextFile (String DirectoryRoot, String FilePrefix, int level, String ID, String Method) {
        try {
            String FileName;
            if (FilePrefix != null && FilePrefix.length() > 0) {
                FileName = String.format("%s/%s_%s_cl_l%d%s", DirectoryRoot, FilePrefix, Method, level, ID);
            } else {
                FileName = DirectoryRoot;
            }
            System.out.println(FileName);
            BufferedReader r = openStream(FileName);
            // String Line;
            int ChildrenRead = 0;
            
            while (ChildrenRead < maxChildren) {
                ObjectFloatVectorL2 ov2 = new ObjectFloatVectorL2(r);
                keys.add(ov2.getVectorData());
                KMeansNode node = new KMeansNode(maxChildren, maxNestedLevel);
                if (level + 1 < maxNestedLevel && node.readFromTextFile(DirectoryRoot, FilePrefix, level+1, ID+"_"+Integer.toString(ChildrenRead), Method) > 0) {
                    children.add(node);
                } else {
                    children.add(null);
                }
                ChildrenRead++;
            }
            r.close();
            return keys.size();
        } catch (IOException ex) {
            System.err.println(ex.toString());
            return 0;
        }
    }
    
    private static BufferedReader openStream(String path) throws java.io.IOException {
        if (path.endsWith("gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
        } else {
            return new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        }
    }
    
}

package messif.quantization;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.MultiClassSerializator;
import messif.objects.text.lucene.LuceneAlgorithm;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AnswerType;
import messif.operations.data.BulkInsertOperation;
import messif.operations.query.KNNQueryOperation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import messif.RansacMessif;

/**
 * Visual vocabulary Lucene algorithm use Lucene encapsulated into MESSIF
 * algorithm in order to index visual vocabulary.
 *
 * @author Marian Labuda
 */
public class VisualVocabularyLuceneAlgorithm extends LuceneAlgorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Visual vocabulary used for quantization of raw visual features */
    private AbstractVisualVocabulary visualVocabulary;
    
    /** Maximum number of terms accepted by Lucene when querying */
    private int luceneMaxTermCount;

    @AlgorithmConstructor(description="Lucene index for bag-of-features model", 
                          arguments={"index directory", "locator field name", "binary object field name", "binary serializator", 
                          "text analyzer instance", "store term vectors flag", "visual vocabulary", 
                          "max Lucene boolean term count for searching", "search field names array"})
    public VisualVocabularyLuceneAlgorithm(File indexDir, String locatorField, String objectField, BinarySerializator serializator, 
            Analyzer analyzer, boolean storeTermVectors, AbstractVisualVocabulary visualVocabulary, int luceneMaxTermCount, String... searchFields) throws CorruptIndexException, IOException {
        super(indexDir, locatorField, objectField, serializator, analyzer, storeTermVectors, searchFields);
        if (!(analyzer instanceof ObjectFeatureAnalyzer)) {
            throw new IllegalArgumentException("The parameter analyzer must be an instance of ObjectFeatureAnalyzer");
        }
        this.visualVocabulary = visualVocabulary;
        setMaximumBooleanTermCount(luceneMaxTermCount);
    }
    
    @AlgorithmConstructor(description="Lucene index for bag-of-features model", 
                          arguments={"index directory", "locator field name", 
                          "text analyzer instance", "store term vectors flag", "visual vocabulary", 
                          "max Lucene boolean term count for searching", "search field names array"})
    public VisualVocabularyLuceneAlgorithm(File indexDir, String locatorField, 
            Analyzer analyzer, boolean storeTermVectors, AbstractVisualVocabulary visualVocabulary, int luceneMaxTermCount, String... searchFields) throws CorruptIndexException, IOException {
        this(indexDir, locatorField, 
             "objectBinData", new MultiClassSerializator<ObjectFeatureSet>(ObjectFeatureSet.class),
             analyzer, storeTermVectors, visualVocabulary, luceneMaxTermCount, searchFields);
    }
    
    /**
     * Create new instance of Visual Vocabulary Lucene Algorithm class which encapsulated kmeans tree.
     * @param luceneIndexDirectoryPath path where to create lucene index
     * @param visualVocabularyPath path to visual vocabulary storage
     * @param visualVocabularyClass class of visual vocabulary to load from the file storage
     * @throws CorruptIndexException
     * @throws IOException 
     */
    @AlgorithmConstructor(description="Lucene index for bag-of-features model", 
                          arguments={"path to a directory where Lucene stores its files", "file with binary serialized visual vocabulary", 
                                     "class of visual vocabulary to instantiate"})
    public VisualVocabularyLuceneAlgorithm(String luceneIndexDirectoryPath, String visualVocabularyPath, 
            Class<? extends AbstractVisualVocabulary> visualVocabularyClass) throws CorruptIndexException, IOException {
        this(new File(luceneIndexDirectoryPath), "fileName", 
             new ObjectFeatureAnalyzer(LuceneAlgorithm.usedLuceneVersion), true, 
             AbstractVisualVocabulary.readVocabulary(visualVocabularyPath, visualVocabularyClass), -1, "features");
    }

    @AlgorithmConstructor(description="Lucene index for bag-of-features model", 
                          arguments={"index directory", "locator field name", "text analyzer instance", "store term vectors flag", 
                                     "visual vocabulary", "search field names array"})
    public VisualVocabularyLuceneAlgorithm(File indexDir, String locatorField, Analyzer analyzer, boolean storeTermVectors,
            AbstractVisualVocabulary visualVocabulary, String... searchFields) throws CorruptIndexException, IOException {
        this(indexDir, locatorField, analyzer, storeTermVectors, visualVocabulary, -1, searchFields);
    }
    
    //*************************************************
    //************* METHOD ****************************
    //*************************************************
    
    public static void clearIndex(String luceneIndex) throws IOException {
        System.out.print("Clearing index in " + luceneIndex);
        // Remove old Lucene index if exist
        File file = new File(luceneIndex);
        if (file.exists()) {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println(": cleared");
        } else {
            System.out.println(": not found");
        }
    }
    
    /**
     * Convert the given feature sets to the quantized feature sets.
     *
     * @param objectFeatureSets
     * @return sets of the quantized features
     */
    public Set<ObjectFeatureSet> convertToQuantizedFeatureSets(Set<ObjectFeatureSet> objectFeatureSets) {
        Set<ObjectFeatureSet> quantizedSets = new HashSet<ObjectFeatureSet>();
        for (ObjectFeatureSet set : objectFeatureSets) {
            quantizedSets.add(visualVocabulary.convertToFeatureSet(set));
        }
        return quantizedSets;
    }

    /**
     * Insert the given quantized feature sets into the lucene index. If
     * analyzer doesn't have stop word set, then this set is created.
     *
     * @param objectFeatureSets queantized feature sets to insert
     * @return true on success, false otherwise
     * @throws IOException on error in Lucene insert
     */
    public boolean insertQuantizedFeatureSets(Set<ObjectFeatureSet> objectFeatureSets) throws IOException {
        ObjectFeatureAnalyzer anlzr = (ObjectFeatureAnalyzer) analyzer;
        if (!anlzr.isStopWordSet()) {
            anlzr.createStopWordSet(objectFeatureSets, 6, 7);
        }
        return super.insert(objectFeatureSets);
    }

    public void storeStopWordSet(String fileName) throws IOException {
        ObjectFeatureAnalyzer anlzr = (ObjectFeatureAnalyzer) analyzer;
        anlzr.storeStopWordSet(fileName);
    }
    
    /** Clears the list of stop words set to a Lucene analyzer.
     * New list is automatically created when bulkInsert operation is called. It is created from the objects passed
     * in the bulkInsert operation.
     */
    public void clearStopWords() {
        ((ObjectFeatureAnalyzer)analyzer).clearStopWords();
    }

    public ObjectFeatureAnalyzer getObjectFeatureAnalyzer() {
        return (ObjectFeatureAnalyzer) analyzer;
    }
    
    /**
     * Insert the given feature sets extracted from images into the lucene
     * index.
     *
     * @param objectFeatureSets feature sets to insert
     * @return true on success, false otherwise
     * @throws IOException on error in Lucene insert
     */
    public boolean insertFeatureSets(Set<ObjectFeatureSet> objectFeatureSets) throws IOException {
        return insertQuantizedFeatureSets(convertToQuantizedFeatureSets(objectFeatureSets));
    }

    /**
     * Search k nearest similar images in lucene index.
     *
     * @param rawDescriptors descriptor extracted from image
     * @param numberOfImages how many images will be retrieved
     * @return list of corresponding file names (nearest according to similarity)
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public KNNQueryOperation searchSimilarImages(ObjectFeatureSet rawDescriptors, int numberOfImages, boolean doGeomConsistencyCheck) throws IllegalArgumentException, IOException, CloneNotSupportedException {
        ObjectFeatureSet quantizedDescriptors = visualVocabulary.convertToFeatureSet(rawDescriptors);
        KNNQueryOperation kNNQueryOperation = new KNNQueryOperation(quantizedDescriptors, numberOfImages, AnswerType.ORIGINAL_OBJECTS);
        knnSearch(kNNQueryOperation);
        
        // Optionally do RANSAC on quantized descriptors here.
        if (doGeomConsistencyCheck) {
            KNNQueryOperation op = (KNNQueryOperation)(kNNQueryOperation.clone(false));
            List<RankedAbstractObject> reranked = RansacMessif.doRansacQuantized((ObjectFeatureSet)kNNQueryOperation.getQueryObject(), kNNQueryOperation.getAnswer());
            for (RankedAbstractObject ro : reranked) {
                op.addToAnswer((LocalAbstractObject)ro.getObject(), ro.getDistance(), null);
            }
            kNNQueryOperation = op;
        }
        
        return kNNQueryOperation;
    }

    @Override
    protected Query parseQuery(Object query) throws IllegalArgumentException {
        KNNQueryOperation oper = (KNNQueryOperation)query;
        ObjectFeatureSet set = (ObjectFeatureSet)oper.getQueryObject();
                
        try {
            String queryString;
            if (oper.getParameter("doTermBoosting", Boolean.class, false)) {
                queryString = "";
                Iterator<ObjectFeature> iterator = set.iterator();
                while (iterator.hasNext()) {
                    ObjectFeatureQuantized feature = (ObjectFeatureQuantized) iterator.next();
                    queryString += feature.getStringData() + "^" + feature.getScale() + " ";
                }
            } else {
                queryString = set.getStringData();
            }
            return queryParser.parse(queryString);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Increase maximum boolean query terms count.
     *
     * Default is 1024 - it could be not enough in case of many descriptors.
     *
     * @param count maximum count of terms in boolean query
     */
    public final void setMaximumBooleanTermCount(int count) {
        luceneMaxTermCount = count;
        updateLuceneTermCount();
    }

    private void updateLuceneTermCount() {
        if (luceneMaxTermCount > 0) {
            System.setProperty("org.apache.lucene.maxClauseCount", Integer.toString(luceneMaxTermCount));
            BooleanQuery.setMaxClauseCount(luceneMaxTermCount);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(", stopwords: ").append(getObjectFeatureAnalyzer().getStopWordCount());
        sb.append(", visual vocabulary size: ").append(visualVocabulary.getVocabularySize());
        return sb.toString();
    }

    //********************************************************************************
    //******************** STANDARD MESSIF ALGORITHM INTERFACE ***********************
    //********************************************************************************

    /**
     * The operation passed can be parametrized:
     * - doTermBoosting (boolean) = term boosting based on feature scale attribute is done if set to true
     * - doGeomConsistencyCheck (boolean) = geometric consistency check is done using RANSAC if set to true
     * 
     * @param operation knn search operation 
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws CloneNotSupportedException 
     */
    public void search(KNNQueryOperation operation) throws IllegalArgumentException, IOException, CloneNotSupportedException {
        System.out.println("QueryOperationParam: doTermBoosting=" + operation.getParameter("doTermBoosting", Boolean.class, false));
        System.out.println("QueryOperationParam: doGeomConsistencyCheck=" + operation.getParameter("doGeomConsistencyCheck", Boolean.class, false));
        
        KNNQueryOperation res = searchSimilarImages((ObjectFeatureSet)operation.getQueryObject(), operation.getK(), 
                                                    operation.getParameter("doGeomConsistencyCheck", Boolean.class, false));
        // Convert answer to the passed operation
        operation.updateFrom(res);
        operation.endOperation();
    }

    /**
     * Insert a bunch of feature sets to Lucene. The feature quantization is done automatically via the vocabulary set
     * in advance. A list of stop words is created automatically too (if it has not been created yet or if it was cleared).
     * It is created from the objects passed in the operation.
     * @param operation operation holding a list of feature sets to insert
     * @throws IOException on error caused by insert to Lucene
     */
    public void bulkInsert(BulkInsertOperation operation) throws IOException {
        @SuppressWarnings("unchecked")
        List<ObjectFeatureSet> featureSetsList = (List<ObjectFeatureSet>) operation.getInsertedObjects();
        insertFeatureSets(new HashSet<ObjectFeatureSet>(featureSetsList));
        operation.endOperation();
    }

    //********************************************************************************
    //*************************** JAVA SERIALIZATION *********************************
    //********************************************************************************

//    private void writeObject(ObjectOutputStream out) throws IOException {
//        out.defaultWriteObject();
//    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        updateLuceneTermCount();
   }
}

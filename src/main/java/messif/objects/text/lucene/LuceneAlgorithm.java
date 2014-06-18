package messif.objects.text.lucene;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import messif.algorithms.Algorithm;
import messif.buckets.BucketErrorCode;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.NoDataObject;
import messif.objects.impl.ObjectString;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.BufferInputStream;
import messif.objects.text.StringDataProvider;
import messif.objects.text.StringFieldDataProvider;
import messif.objects.text.TextConversion;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.data.BulkInsertOperation;
import messif.operations.data.InsertOperation;
import messif.operations.query.KNNQueryOperation;
import messif.utility.FileProcessorInfoWrapper;
import messif.utility.FileSearchProcess;
import messif.utility.reflection.InstantiatorSignature;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Encapsulation of a Lucene index into MESSIF algorithm.
 * Note that this algorithm works only with {@link ObjectString} objects
 * and the distance function is ignored. Note also, that this algorithm
 * is not dynamic, thus no data-manipulation operations are supported.
 * 
 * @author xbatko
 */
public class LuceneAlgorithm extends Algorithm {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /** Constant specifying which Lucene version to use */
    public static final Version usedLuceneVersion = Version.LUCENE_35;

    /** Name of the field that stores the document locator */
    private final String locatorField;
    /** Name of the field that stores the binary object representation */
    private final String objectField;
    /** Serializator for converting objects to binary representation and vice versa */
    private final BinarySerializator serializator;
    /** Name of the fields that are searched by queries */
    private final String[] searchFields;
    /** Flag whether to store term frequency vectors in the search fields */
    private final Field.TermVector termVectorsFlag;
    /** Lucene index directory */
    private transient FSDirectory indexDir;
    /** Lucene index reader */
    private transient IndexReader reader;
    /** Lucene index writer */
    private transient IndexWriter writer;
    /** Lucene index searcher */
    private transient IndexSearcher searcher;
    /** Lucene analyzer used to normalize texts */
    protected final Analyzer analyzer;
    /** Lucene query parser */
    protected transient QueryParser queryParser;
    /** Lock for read/write operations synchronization */
    private final ReadWriteLock lock;

    /**
     * Creates a new Lucene algorithm on a given index directory.
     * The index can be created in advance using {@link #main(java.lang.String[]) builder method}.
     * 
     * @param indexDir the Lucene index directory
     * @param locatorField the name of the field in the indexed documents where the locator is stored
     * @param objectField the name of the field that stores the binary object representation
     * @param serializator the serializator for converting objects to binary representation and vice versa
     * @param analyzer instance of the Lucene {@link Analyzer} used to process search fields
     * @param storeTermVectors the flag whether to store term frequency vectors in the search fields
     * @param searchFields the names of the fields in the indexed documents that are used for searching
     * @throws CorruptIndexException if the index file is corrupted
     * @throws IOException if there was an error reading from the index file
     */
    @AlgorithmConstructor(description="Lucene index", arguments={"index directory", "locator field name", "binary object field name", "binary serializator", "text analyzer instance", "store term vectors flag", "search field names array"})
    public LuceneAlgorithm(File indexDir, String locatorField, String objectField, BinarySerializator serializator, Analyzer analyzer, boolean storeTermVectors, String... searchFields) throws CorruptIndexException, IOException {
        super("Lucene index in " + indexDir);

        // Open the index
        this.indexDir = FSDirectory.open(indexDir);

        // Initialize query parser
        this.analyzer = analyzer;
        initQueryParser(searchFields, analyzer);

        this.locatorField = locatorField;
        this.objectField = objectField;
        this.serializator = serializator;
        this.termVectorsFlag = storeTermVectors ? Field.TermVector.YES : Field.TermVector.NO;
        if (objectField != null && serializator == null)
            throw new IllegalArgumentException("Cannot store binary objects without serializator");
        this.searchFields = searchFields;
        if (searchFields.length == 0)
            throw new IllegalArgumentException("Search fields must contain at least one name");
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Creates a new Lucene algorithm on a given index directory.
     * The index can be created in advance using {@link #main(java.lang.String[]) builder method}.
     * {@link StandardAnalyzer} is used to process search fields.
     * 
     * @param indexDir the Lucene index directory
     * @param locatorField the name of the field in the indexed documents where the locator is stored
     * @param objectField the name of the field that stores the binary object representation
     * @param serializator the serializator for converting objects to binary representation and vice versa
     * @param storeTermVectors the flag whether to store term frequency vectors in the search fields
     * @param searchFields the names of the fields in the indexed documents that are used for searching
     * @throws CorruptIndexException if the index file is corrupted
     * @throws IOException if there was an error reading from the index file
     */
    @AlgorithmConstructor(description="Lucene index", arguments={"index directory", "locator field name", "binary object field name", "binary serializator", "store term vectors flag", "search field names array"})
    public LuceneAlgorithm(File indexDir, String locatorField, String objectField, BinarySerializator serializator, boolean storeTermVectors, String... searchFields) throws CorruptIndexException, IOException {
        this(indexDir, locatorField, objectField, serializator, new StandardAnalyzer(usedLuceneVersion), storeTermVectors, searchFields);
    }

    /**
     * Creates a new Lucene algorithm on a given index directory.
     * The index can be created in advance using {@link #main(java.lang.String[]) builder method}.
     * Note that no binary representation of the indexed objects is stored in the Lucene index.
     * 
     * @param indexDir the Lucene index directory
     * @param locatorField the name of the field in the indexed documents where the locator is stored
     * @param storeTermVectors the flag whether to store term frequency vectors in the search fields
     * @param searchFields the names of the fields in the indexed documents that are used for searching
     * @throws CorruptIndexException if the index file is corrupted
     * @throws IOException if there was an error reading from the index file
     */
    @AlgorithmConstructor(description="Lucene index without binary object field", arguments={"index directory", "locator field name", "store term vectors flag", "search field names array"})
    public LuceneAlgorithm(File indexDir, String locatorField, boolean storeTermVectors, String... searchFields) throws CorruptIndexException, IOException {
        this(indexDir, locatorField, null, null, storeTermVectors, searchFields);
    }

    @Override
    public void finalize() throws Throwable {
        if (writer != null) {
            writer.commit();
            writer.close();
            writer = null;
        }
        if (searcher != null) {
            searcher.close();
            searcher = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
        super.finalize();
    }

    /**
     * Returns the Lucene index reader.
     * If there was a writer opened (i.e. there was an index update), the writing
     * is committed and reader restarted. Note that this method should be called
     * when read-lock is active.
     * 
     * @return the index searcher
     * @throws IOException if there was a problem opening the index
     */
    private synchronized IndexReader getIndexReader() throws IOException {
        if (writer != null) {
            writer.commit();
            writer.close();
            writer = null;
        }
        if (reader == null)
            reader = IndexReader.open(indexDir);
        return reader;
    }

    /**
     * Returns the Lucene index searcher.
     * If there was a writer opened (i.e. there was an index update), the writing
     * is committed and searcher restarted. Note that this method should be called
     * when read-lock is active.
     * 
     * @return the index searcher
     * @throws IOException if there was a problem opening the index
     */
    private synchronized IndexSearcher getIndexSearcher() throws IOException {
        if (searcher == null) {
            searcher = new IndexSearcher(getIndexReader());
        }
        return searcher;
    }

    /**
     * Returns the Lucene index writer.
     * If there were a reader or a searcher opened, the searching is disabled.
     * Note that this method should be called when write-lock is active.
     * 
     * @return the index searcher
     * @throws IOException if there was a problem opening the index
     */
    private synchronized IndexWriter getIndexWriter() throws IOException {
        if (searcher != null) {
            searcher.close();
            searcher = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (writer == null)
            writer = new IndexWriter(indexDir, new IndexWriterConfig(usedLuceneVersion, analyzer));
        return writer;
    }

    /**
     * Adds a search field with the given value to the given document.
     * @param doc the document to add the field to
     * @param fieldName the name of the field to add
     * @param fieldData the search data for the field
     * @return <tt>true</tt> if a field was added to the document or <tt>false</tt>
     *          if the given field data was <tt>null</tt> or empty
     */
    private boolean addDocumentSearchField(Document doc, String fieldName, String fieldData) {
        if (fieldData == null || fieldData.isEmpty())
            return false;
        doc.add(new Field(fieldName, fieldData, Field.Store.NO, Field.Index.ANALYZED, termVectorsFlag));
        return true;
    }

    /**
     * Adds all search fields from the given {@link StringFieldDataProvider data provider}
     * to the given document.
     * @param doc the document to add the field to
     * @param dataProvider the textual fields data provider
     * @return the number of fields added to the document
     */
    private int addDocumentSearchFields(Document doc, StringFieldDataProvider dataProvider) {
        int count = 0;
        for (String fieldName : dataProvider.getStringDataFields())
            if (addDocumentSearchField(doc, fieldName, dataProvider.getStringData(fieldName)))
                count++;
        return count;
    }

    /**
     * Adds a locator field with the {@code locator} value to the given document.
     * @param doc the document to add the field to
     * @param locator the value for the locator field
     * @return <tt>true</tt> if a field was added to the document or <tt>false</tt>
     *          if the locator field name was not specified or the {@code locator} was empty
     */
    private boolean addDocumentLocatorField(Document doc, String locator) {
        if (locatorField == null || locator == null || locator.isEmpty())
            return false;
        doc.add(new Field(locatorField, locator, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        return true;
    }

    /**
     * Adds a field with binary object data to the given document.
     * @param doc the document to add the field to
     * @param object the object the data of which to use as field value
     * @return <tt>true</tt> if a field was added to the document or <tt>false</tt>
     *          if the object field name or the binary serializator was not specified
     * @throws IOException if there was an error converting the object to binary representation
     */
    private boolean addDocumentObjectField(Document doc, Object object) throws IOException {
        if (objectField == null || serializator == null)
            return false;
        doc.add(new Field(objectField, serializator.write(object, false).write()));
        return true;
    }

    /**
     * Returns a Lucene indexable document that contains data from the given object.
     * If the object is {@link MetaObject}, all encapsulated {@link ObjectString} objects
     * are added as separated fields to the document. Otherwise, a single "data"
     * field is added. The string data are extracted via
     * {@link #getObjectData(messif.objects.LocalAbstractObject) getObjectData} method.
     * 
     * @param object the object that supplies the data for the created document
     * @return a new Lucene indexable document or <tt>null</tt> if the object does not contain any string data
     * @throws IOException if there was an error converting the object to binary representation (if object field was used)
     */
    protected Document getTextDocument(LocalAbstractObject object) throws IOException {
        Document doc = new Document();
        if (object instanceof StringDataProvider) {
            if (object instanceof StringFieldDataProvider)
                addDocumentSearchFields(doc, (StringFieldDataProvider)object);
            else {
                addDocumentSearchField(doc, searchFields[0], ((StringDataProvider)object).getStringData());
            }
        } else if (object instanceof MetaObject) {
            addDocumentSearchFields(doc, TextConversion.metaobjectToTextProvider(((MetaObject)object)));
        }

        // No data fields => no document
        if (doc.getFields().isEmpty())
            return null;

        addDocumentLocatorField(doc, object.getLocatorURI());
        addDocumentObjectField(doc, object);
        return doc;
    }

    /**
     * Reads the object stored in the given document.
     * The object is either stored binary in a {@link #objectField} of the document,
     * or a simple {@link NoDataObject} using the {@link #locatorField} is returned.
     * 
     * @param doc the Lucene document to read the object from
     * @return the object from the Lucene document
     * @throws IOException if there was an error reading binary object data
     */
    protected AbstractObject readDocumentObject(Document doc) throws IOException {
        if (objectField != null && serializator != null) {
            byte[] data = doc.getBinaryValue(objectField);
            if (data != null)
                 return serializator.readObject(new BufferInputStream(data), LocalAbstractObject.class);
        }
        return new NoDataObject(doc.get(locatorField));
    }

    /**
     * Parse the query specified by the given query object.
     * Note that the query is expected to be either {@link String} or to implement
     * the {@link StringDataProvider} interface.
     * @param query the query string to parse
     * @return the parsed Lucene query
     * @throws IllegalArgumentException if an unknown object or non-parsable string was specified
     */
    protected Query parseQuery(Object query) throws IllegalArgumentException {
        try {
            String data;
            if (query instanceof String)
                data = (String)query;
            else if (query instanceof StringDataProvider) 
                data = ((StringDataProvider)query).getStringData();
            else if (query instanceof RankingSingleQueryOperation && ((RankingSingleQueryOperation)query).getQueryObject() instanceof StringDataProvider)
                data = ((StringDataProvider)((RankingSingleQueryOperation)query).getQueryObject()).getStringData();
            else
                throw new IllegalArgumentException("Unknown query object: " + query);
            
            return queryParser.parse(data);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Search the index and returns the results.
     * @param operation the kNN operation with {@link ObjectString} query object
     * @throws IllegalArgumentException if there was an error parsing the search text
     * @throws IOException if there was an error reading data from the index
     */
    public void knnSearch(KNNQueryOperation operation) throws IllegalArgumentException, IOException {
        lock.readLock().lock();
        try {
            IndexSearcher indexSearcher = getIndexSearcher();  
            Query q = parseQuery(operation);
            System.out.println("Query to Lucene: " + q);
            
            TopDocs topDocs = indexSearcher.search(q, operation.getK());
            for (ScoreDoc match : topDocs.scoreDocs)
                operation.addToAnswer(readDocumentObject(indexSearcher.doc(match.doc)), 1 - match.score / topDocs.getMaxScore(), null);
            operation.endOperation();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Insert the given collection of objects into the Lucene index.
     * Note that objects are converted to indexable documents using
     * {@link #getTextDocument(messif.objects.LocalAbstractObject) getTextDocument} method.
     * 
     * @param objects the collection of objects to insert
     * @return <tt>true</tt> if the index was modified
     * @throws IOException if there was an error writing to the index
     */
    protected boolean insert(Collection<? extends LocalAbstractObject> objects) throws IOException {
        lock.writeLock().lock();
        try {
            boolean ret = false;
            IndexWriter indexWriter = getIndexWriter();
            for (LocalAbstractObject object : objects) {
                Document doc = getTextDocument(object);
                if (doc != null) {
                    indexWriter.addDocument(doc);
                    ret = true;
                }
            }
            return ret;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Insert operation implementation.
     * The encapsulated object is inserted into the Lucene index.
     * @param operation the insert operation to execute
     * @throws IOException if there was an error writing to the Lucene index
     */
    public void insert(InsertOperation operation) throws IOException {
        if (insert(Collections.singleton(operation.getInsertedObject())))
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_REFUSED);
    }

    /**
     * Bulk-insert operation implementation.
     * All the encapsulated objects are inserted into the Lucene index.
     * @param operation the bulk-insert operation to execute
     * @throws IOException if there was an error writing to the Lucene index
     */
    public void insert(BulkInsertOperation operation) throws IOException {
        if (insert(operation.getInsertedObjects()))
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_REFUSED);
    }

    /**
     * Returns the number of documents stored in this Lucene index.
     * @return the number of indexed documents
     */
    protected int getObjectCount() {
        if (reader != null)
            return reader.numDocs();
        if (writer != null) {
            try {
                return writer.numDocs();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read number of indexed documents: " + e, e);
            }
        }
            
        return -1;
    }

    /**
     * Returns the terms associated with the given document and their tf-idf weights
     * based on the given Lucene {@link IndexReader reader} and {@link Similarity similarity}.
     * 
     * @param documentId the Lucene document identifier for which to get the term weights
     * @param reader the Lucene index reader
     * @param similarity the Lucene tf-idf computation
     * @return the term-weight map for the given document
     * @throws IOException if there was a problem reading the Lucene index
     */
    public static Map<String, Double> getDocumentTermWeights(int documentId, final IndexReader reader, final Similarity similarity) throws IOException {
        try {
            final Map<String, Double> termWeights = new LinkedHashMap<String, Double>();
            reader.getTermFreqVector(documentId, new TermVectorMapper() {
                private String field;
                @Override
                public void setExpectations(String field, int numTerms, boolean storeOffsets, boolean storePositions) {
                    this.field = field;
                }
                @Override
                public void map(String term, int frequency, TermVectorOffsetInfo[] offsets, int[] positions) {
                    try {
                        double weight = similarity.tf(frequency) * similarity.idf(reader.docFreq(new Term(field, term)), reader.numDocs());
                        Double prevWeight = termWeights.put(term, weight);
                        if (prevWeight != null && prevWeight > weight) // If the previous weight was bigger, keep it
                            termWeights.put(term, prevWeight);
                    } catch (IOException e) {
                        // Wrap IO exception into runtime exception
                        throw new IllegalStateException(e);
                    }
                }
            });
            return termWeights;
        } catch (IllegalStateException e) {
            // Unwrap IO exception from runtime exception if necessary
            if (e.getCause() instanceof IOException)
                throw (IOException)e.getCause();
            throw e;
        }
    }

    /**
     * Returns the terms associated with the given document and their tf-idf weights
     * based on the this Lucene algorithm and the {@link Similarity#getDefault() default similarity}.
     * 
     * @param documentId the Lucene document identifier for which to get the term weights
     * @return the term-weight map for the given document
     * @throws IOException if there was a problem reading the Lucene index
     */
    public Map<String, Double> getDocumentTermWeights(int documentId) throws IOException {
        lock.readLock().lock();
        try {
            return getDocumentTermWeights(documentId, reader, Similarity.getDefault());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        int count = getObjectCount();
        if (count == -1)
            return getName();
        return getName() + " with " + getObjectCount() + " objects";
    }

    /**
     * Builds the Lucene index.
     * @param args index directory, handler instance,
     * @throws Throwable if there was an error 
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) throws Throwable {
        if (args.length < 3) {
            System.err.println("Usage: " + LuceneAlgorithm.class.getName() + " <directory> <file name regexp> <handler instance> <dir|TAR|file> ...");
            System.exit(1);
        }

        final LuceneFileProcessor processor = InstantiatorSignature.createInstanceWithStringArgs(args[2], LuceneFileProcessor.class, null);
        LuceneAlgorithm luceneAlgorithm = new LuceneAlgorithm(
                new File(args[0]),
                processor.getLocatorField(),
                false,
                processor.getIndexedFields()
        );
        luceneAlgorithm.lock.writeLock().lock();
        try {
            processor.setIndexWriter(luceneAlgorithm.getIndexWriter());

            long time = System.currentTimeMillis();
            Object fileNamePattern = FileSearchProcess.createFileNamePattern(args[1]);
            if (fileNamePattern instanceof Collection)
                System.out.println("File names are checked against " + ((Collection)fileNamePattern).size() + " names");
            int counter = FileSearchProcess.process(
                    new FileProcessorInfoWrapper(processor, System.out, "Processed {0} in {1}ms", 10000),
                    fileNamePattern, 3, args
            );
            time = System.currentTimeMillis() - time;
            int docs = luceneAlgorithm.getIndexWriter().numDocs();
            System.out.println("Indexing of " + counter + (docs > counter ? (" files (" + docs + " documents)") : " files") + " finished in " + time/1000 + " seconds");
            System.out.println("Average time for indexing one file: " + ((double)time / (double)counter) + "ms");
        } finally {
            luceneAlgorithm.lock.writeLock().unlock();
        }
        luceneAlgorithm.finalize();
    }

    private void initQueryParser(String[] searchFields, Analyzer analyzer) {
        if (searchFields.length == 1)
            this.queryParser = new QueryParser(usedLuceneVersion, searchFields[0], analyzer);
        else
            this.queryParser = new MultiFieldQueryParser(usedLuceneVersion, searchFields, analyzer);
    }

    //********************************************************************************
    //*************************** JAVA SERIALIZATION *********************************
    //********************************************************************************

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (writer != null)
            writer.commit();
        
        out.defaultWriteObject();
        out.writeObject(indexDir.getDirectory());
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.indexDir = FSDirectory.open((File)in.readObject());
        initQueryParser(searchFields, analyzer);
    }
}

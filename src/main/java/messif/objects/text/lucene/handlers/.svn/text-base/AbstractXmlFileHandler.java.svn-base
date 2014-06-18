package messif.objects.text.lucene.handlers;

import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.text.lucene.LuceneFileProcessor;
import messif.utility.FileProcessorException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Abstract ancestor of all handlers that can parse a Lucene {@link Document}.
 * @author xbatko
 */
public abstract class AbstractXmlFileHandler extends DefaultHandler implements LuceneFileProcessor {
    /** XML document parser */
    private SAXParser parser;
    /** Reference to Lucene index writer for this handler */
    private IndexWriter indexWriter;
    /** Current Lucene document */
    private Document document;
    /** Name of the document field currently filled from the XML file */
    private String fieldName;
    /** Flag whether to store the currently filled field data in the index */
    private Field.Store fieldStore;
    /** Flag whether to index the currently filled field */
    private Field.Index fieldIndex;
    /** Data for the currently filled field */
    private StringBuilder fieldValue;

    @Override
    public synchronized int processFile(String source, InputStream dataStream) throws FileProcessorException {
        try {
            if (parser == null)
                parser = SAXParserFactory.newInstance().newSAXParser();
            if (indexWriter == null)
                throw new IllegalStateException("No Lucene index writer set, cannot write documents");
            parser.parse(dataStream, this);
            indexWriter.addDocument(getLuceneDocument());
            releaseLuceneDocument();
            return 1;
        } catch (Exception e) {
            throw new FileProcessorException(source, e);
        }
    }

    @Override
    public void setIndexWriter(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    /**
     * Starts a document field with character data within the tag.
     * @param name the name of the document field
     * @param store flag whether to store the field data in the index
     * @param index flag whether to index the field data
     */
    protected final void startField(String name, Field.Store store, Field.Index index) {
        fieldName = name;
        fieldStore = store;
        fieldIndex = index;
        fieldValue = new StringBuilder();
    }

    /**
     * Starts a document field with the given value.
     * Note that the character data within the tag are ignored and the field
     * is added only if the value is not <tt>null</tt>.
     * @param name the name of the document field
     * @param value the value for the field
     * @param store flag whether to store the field data in the index
     * @param index flag whether to index the field data
     */
    protected final void startField(String name, String value, Field.Store store, Field.Index index) {
        if (name != null && value != null)
            document.add(new Field(name, value, store, index));
    }

    /**
     * Finishes the current field and adds it to the document.
     */
    protected final void endField() {
        if (fieldValue != null) {
            document.add(new Field(fieldName, fieldValue.toString(), fieldStore, fieldIndex));
            fieldName = null;
            fieldStore = null;
            fieldIndex = null;
            fieldValue = null;
        }
    }

    @Override
    public void startDocument() throws SAXException {
        document = new Document();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (fieldValue != null)
            fieldValue.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        endField();
    }

    /**
     * Returns the current Lucene document.
     * A new document is created whenever this handlers is used
     * to parse a new document, i.e. when {@link #startDocument()} is called.
     * @return the current Lucene document
     */
    public Document getLuceneDocument() {
        return document;
    }

    /**
     * Releases the allocated Lucene document.
     */
    public void releaseLuceneDocument() {
        document = null;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.text.lucene.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import messif.objects.text.lucene.LuceneFileProcessor;
import messif.utility.FileProcessorException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

/**
 * Simple Lucene builder handler that indexes files with text fields (typically separated by comas).
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public abstract class AbstractTextFileHandler implements LuceneFileProcessor {
    /** Reference to Lucene index writer for this handler */
    private IndexWriter indexWriter;
    /** Field parse pattern */
    private final Pattern pattern;

    /**
     * Creates a new instance of AbstractTextFileHandler with coma-separated values pattern.
     */
    protected AbstractTextFileHandler() {
        this(',');
    }

    /**
     * Creates a new instance of AbstractTextFileHandler with the given character separated values pattern.
     * @param separator the character used to separate fields
     */
    protected AbstractTextFileHandler(char separator) {
        this(Pattern.compile("\\G\\s*(?:\"([^\"]*)\"|([^" + separator + "]*?))\\s*(?:" + separator + "|$)"));
    }

    /**
     * Creates a new instance of AbstractTextFileHandler.
     * The given pattern is used to parse fields from each line of the text file.
     * The pattern will be applied multiple times and each time a match is found,
     * the first non-null matching group will represent a field.
     * @param pattern the regular expression to parse field from the text file
     */
    protected AbstractTextFileHandler(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void setIndexWriter(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    @Override
    public int processFile(String source, InputStream dataStream) throws FileProcessorException {
        if (indexWriter == null)
            throw new IllegalStateException("No Lucene index writer set, cannot write documents");
        try {
            if (source.toLowerCase().endsWith("gz"))
                dataStream = new GZIPInputStream(dataStream);
            BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataStream));
            int counter = 0;
            String line = dataReader.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                Document document = new Document();
                for (int fieldNo = 0; matcher.find(); fieldNo++) {
                    Field field = createField(fieldNo, matcher);
                    if (field != null)
                        document.add(field);
                }
                if (!document.getFields().isEmpty()) {
                    indexWriter.addDocument(document);
                    counter++;
                }
                line = dataReader.readLine();
            }
            dataReader.close();
            return counter;
        } catch (Exception e) {
            throw new FileProcessorException(source, e);
        }
    }

    /**
     * Creates a Lucene document field from a matcher group value.
     * @param fieldNo the zero-based index of the field in the text file
     * @param matcher the matcher the matching group of which to use as field value
     * @return a Lucene document field to index or <tt>null</tt> if the requested field is not stored
     */
    private Field createField(int fieldNo, Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String value = matcher.group(i);
            if (value != null)
                return createField(fieldNo, value);
        }
        return null;
    }

    /**
     * Creates a Lucene document field from a text file field value.
     * @param fieldNo the zero-based index of the field in the text file
     * @param value the string value of the text file field
     * @return a Lucene document field to index or <tt>null</tt> if the requested field is not stored
     */
    protected abstract Field createField(int fieldNo, String value);
}

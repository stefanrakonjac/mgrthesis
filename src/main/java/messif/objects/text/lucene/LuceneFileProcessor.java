/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.text.lucene;

import messif.utility.FileProcessor;
import org.apache.lucene.index.IndexWriter;

/**
 * Extension of the {@link FileProcessor} that passes a reference to the Lucene {@link IndexWriter}.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public interface LuceneFileProcessor extends FileProcessor {
    /**
     * Set the reference to Lucene index writer using which the documents are indexed.
     * @param indexWriter the new Lucene index writer reference
     */
    public void setIndexWriter(IndexWriter indexWriter);

    /**
     * Returns the name of the field that stores the document locator.
     * @return the name of the field that stores the document locator
     */
    public String getLocatorField();

    /**
     * Returns the list of Lucene document fields created by this processor.
     * @return the list of Lucene document fields created by this processor
     */
    public String[] getIndexedFields();
}

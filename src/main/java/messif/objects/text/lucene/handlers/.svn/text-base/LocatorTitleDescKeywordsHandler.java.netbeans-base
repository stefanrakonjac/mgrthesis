/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.text.lucene.handlers;

import org.apache.lucene.document.Field;

/**
 * Implementation of the {@link AbstractTextFileHandler} that extracts "uri",
 * "title", "description", and "keywords" fields from a coma-separated text file.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class LocatorTitleDescKeywordsHandler extends AbstractTextFileHandler {
    @Override
    protected Field createField(int fieldNo, String value) {
        if (value == null)
            value = "";
        switch (fieldNo) {
            case 0:
                return new Field("uri", value, Field.Store.YES, Field.Index.NO);
            case 1:
                return new Field("title", value, Field.Store.NO, Field.Index.ANALYZED);
            case 2:
                return new Field("description", value, Field.Store.NO, Field.Index.ANALYZED);
            case 3:
                return new Field("keywords", value, Field.Store.NO, Field.Index.ANALYZED);
        }
        return null;
    }

    @Override
    public String getLocatorField() {
        return "uri";
    }

    @Override
    public String[] getIndexedFields() {
        return new String[] { "title", "description", "keywords" };
    }

}

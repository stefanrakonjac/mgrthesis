/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.text.lucene.handlers;

import org.apache.lucene.document.Field;

/**
 * Implementation of the {@link AbstractTextFileHandler} that extracts "uri"
 * and the rest of fields as "allinone" from a coma-separated text file.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 */
public class AllInOneHandler extends AbstractTextFileHandler {
    @Override
    protected Field createField(int fieldNo, String value) {
        if (value == null)
            value = "";
        switch (fieldNo) {
            case 0:
                return new Field("uri", value, Field.Store.YES, Field.Index.NO);
            default:
                return new Field("allinone", value, Field.Store.NO, Field.Index.ANALYZED);
        }
    }

    @Override
    public String getLocatorField() {
        return "uri";
    }

    @Override
    public String[] getIndexedFields() {
        return new String[] { "allinone" };
    }

}

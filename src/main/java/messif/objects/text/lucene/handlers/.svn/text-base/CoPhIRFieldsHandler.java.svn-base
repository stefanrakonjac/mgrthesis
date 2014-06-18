package messif.objects.text.lucene.handlers;

import org.apache.lucene.document.Field;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Implementation of the {@link AbstractXmlFileHandler} that extracts the content
 * of certain CoPhIR text attributes and elements.
 * The content is then stored in a Lucene document in corresponding fields.
 */
public class CoPhIRFieldsHandler extends AbstractXmlFileHandler {
    @Override
    public void startElement(String nsURI, String strippedName, String tagName, Attributes attributes) throws SAXException {
        if (tagName.equalsIgnoreCase("mediauri")) {
            startField("uri", Field.Store.YES, Field.Index.NO);
        } else if (tagName.equals("title")) {
            startField("title", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("description")) {
            startField("description", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("tag")) {
            startField("tag", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("comment")) {
            startField("comment", Field.Store.NO, Field.Index.ANALYZED);
        } else if (tagName.equals("owner")) {
            startField("username", attributes.getValue("username"), Field.Store.YES, Field.Index.ANALYZED);
            startField("realname", attributes.getValue("realname"), Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("dates")) {
            startField("taken", attributes.getValue("taken"), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
        }
    }

    @Override
    public String getLocatorField() {
        return "uri";
    }

    @Override
    public String[] getIndexedFields() {
        return new String[] { "title", "description", "tag", "comment", "username", "realname", "taken" };
    }
}

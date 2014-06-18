package messif.objects.text.lucene.handlers;

import org.apache.lucene.document.Field;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Implementation of the {@link AbstractXmlFileHandler} that extracts the content
 * of certain CoPhIR text attributes and elements.
 * The content is then stored in a Lucene document in a single field "allinone".
 */
public class CoPhIRAllInOneHandler extends AbstractXmlFileHandler {
    @Override
    public void startElement(String nsURI, String strippedName, String tagName, Attributes attributes) throws SAXException {
        if (tagName.equalsIgnoreCase("mediauri")) {
            startField("uri", Field.Store.YES, Field.Index.NO);
        } else if (tagName.equals("title")) {
            startField("allinone", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("description")) {
            startField("allinone", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("tag")) {
            startField("allinone", Field.Store.YES, Field.Index.ANALYZED);
        } else if (tagName.equals("comment")) {
            startField("allinone", Field.Store.NO, Field.Index.ANALYZED);
        } else if (tagName.equals("owner")) {
            startField("allinone", attributes.getValue("username"), Field.Store.YES, Field.Index.ANALYZED);
            startField("allinone", attributes.getValue("realname"), Field.Store.YES, Field.Index.ANALYZED);
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
        return new String[] { "allinone", "taken" };
    }
}

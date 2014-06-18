package messif.quantization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.text.lucene.LuceneAlgorithm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * Object feature analyzer is usefull for the quantized object features
 * analysis. Analysis concisists of the white space tokenization and the stop
 * world filtration. The stop words are top X % and bottom X % of all quantized
 * features.
 *
 * @author Marian "Equo" Labuda
 */
public class ObjectFeatureAnalyzer extends Analyzer implements Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    private Version matchVersion;
    private Set<String> stopWordSet = null;

    public ObjectFeatureAnalyzer(Version matchVersion) {
        this.matchVersion = matchVersion;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        final Tokenizer source = new WhitespaceTokenizer(matchVersion, reader);
        return new StopFilter(matchVersion, source, stopWordSet);
    }

    public boolean isStopWordSet() {
        return (stopWordSet != null && !stopWordSet.isEmpty());
    }

    public int getStopWordCount() {
        return ((stopWordSet != null) ? stopWordSet.size() : 0);
    }

    /**
     * Create stop word set from the sets of the quantized features.
     *
     * @param featureSets feature sets of the quantized features
     * @param topPercent how many top percent of the quantized features are the stop words
     * @param bottomPercent how many bottom percent of the quantized features are the stop words
     */
    public void createStopWordSet(Set<ObjectFeatureSet> featureSets, int topPercent, int bottomPercent) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        double topCoefficient = topPercent / 100.0;
        double bottomCoefficient = bottomPercent / 100.0;

        // Get number of unique descriptors as Map<QuantizedFeature, Number>
        Iterator<ObjectFeatureSet> iterator = featureSets.iterator();
        while (iterator.hasNext()) {
            ObjectFeatureSet featureSet = iterator.next();
            
            for (Iterator<ObjectFeature> it = featureSet.iterator(); it.hasNext();) {
                ObjectFeatureQuantized f = (ObjectFeatureQuantized)it.next();
                String qf = f.getStringData();
                Integer v = map.get(qf);
                map.put(qf, ((v == null) ? 1 : v+1));
            }
        }

        // Sort quantized features according to their number
        Map<String, Integer> sortedMap = sortByValues(map);

        // Filter top x and bottom y percent as stop words
        List<String> sortedList = new ArrayList<String>(sortedMap.keySet());
        int size = sortedList.size();
        List<String> stopWords = new ArrayList<String>();

        // Add top x percent to stop words
        for (int i = 0; i < size * topCoefficient; i++) {
            stopWords.add(sortedList.get(i));
        }

        // Add bottom x percent to stop words
        for (int i = size - 1; i > size - size * bottomCoefficient; i--) {
            stopWords.add(sortedList.get(i));
        }

        stopWordSet = new HashSet<String>(stopWords);
    }

    public void storeStopWordSet(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));        // Overwrite the file
        for (String w : stopWordSet) {
            writer.append(w).append(" ");
        }
        writer.close();
    }

    public void readStopWordSet(String fileName) throws IOException {
        stopWordSet = new HashSet<String>();

        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String stopWordsString = reader.readLine();
        reader.close();
        String[] strippedString = stopWordsString.split(" ");
        stopWordSet.addAll(Arrays.asList(strippedString));
    }

    public static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        Map<K, V> sortedMap = new LinkedHashMap<K, V>();

        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    /**
     * Clears the stop words set in this analyzed
     */
    void clearStopWords() {
        if (stopWordSet != null) {
            stopWordSet.clear();
        }
    }

    ////////////////////////////////////
    ///// METHODS FOR TEST PURPOSE /////
    ////////////////////////////////////
    public static void main(String[] args) throws IOException {
        ObjectFeatureAnalyzer anlzr = new ObjectFeatureAnalyzer(LuceneAlgorithm.usedLuceneVersion);
        anlzr.stopWordSet = new HashSet<String>();
        anlzr.stopWordSet.add("a1");
        anlzr.stopWordSet.add("c3");
        anlzr.stopWordSet.add("h8");
        anlzr.stopWordSet.add("lol");

        String text = "lol wtf a1 b2 c3 d4 e5 f6 g7 h8 lol";
        TokenStream stream = anlzr.tokenStream("someField", new StringReader(text));

        // get the TermAttribute from the TokenStream
        CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        // print all tokens until stream is exhausted
        while (stream.incrementToken()) {
            System.out.println(charTermAttribute.buffer());
        }

        stream.end();
        stream.close();
    }
}

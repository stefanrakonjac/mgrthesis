/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.quantization;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import messif.objects.impl.ObjectFeature;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFeatureSet;
import messif.objects.impl.ObjectFeatureSetSumOfSimilar;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.nio.ChannelInputStream;
import messif.objects.nio.ChannelOutputStream;
import messif.objects.nio.MultiClassSerializator;
import messif.objects.nio.SingleClassSerializator;
import messif.objects.util.AbstractObjectIterator;
import messif.quantization.kmeans.KMeansVisualVocabulary;

/**
 * Serialization of visual vocabulary is done through implementing {@link BinarySerializable}.
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public abstract class AbstractVisualVocabulary implements BinarySerializable, Serializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /**
     * Create visual vocabulary from an iterator over feature sets.
     * @param objs iterator over a set of feature set to create / learn a visual vocabulary.
     * @return success / failure of vocabulary creation
     */
    public boolean createVocabularyFromSets(AbstractObjectIterator<ObjectFeatureSet> objs) {
        while (objs.hasNext()) {
            if (!createVocabulary((AbstractObjectIterator<ObjectFeature>) objs.next().iterator())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Create visual vocabulary for an iterator of features.
     * @param feats iterator features from which a visual vocabulary is to be created / learned.
     * @return success / failure of vocabulary creation
     */
    public abstract boolean createVocabulary(AbstractObjectIterator<ObjectFeature> feats);
    
    /**
     * Number of visual words in the visual vocabulary.
     * @return number of distinct visual words in this vocabulary
     */
    public abstract int getVocabularySize();
       
    /**
     * Transform feature to a quantized feature (which includes the visual word).
     * @param feat a feature to transform
     * @return an instance of {@link ObjectFeatureQuantized} with the visual word set in it
     */
    public abstract ObjectFeatureQuantized convertToFeature(ObjectFeature feat);
    
    /**
     * Transform a feature set to a set of quantized features (which wrap visual words).
     * @param featureSet a feature set to transform
     * @return an instance of {@link ObjectFeatureSet} of {@link ObjectFeatureQuantized} instance with visual words set in them
     */
    public final ObjectFeatureSet convertToFeatureSet(ObjectFeatureSet featureSet) {
        ObjectFeatureSet quantizedFeatureSet = new ObjectFeatureSetSumOfSimilar();
        quantizedFeatureSet.setObjectKey(featureSet.getObjectKey());
        Iterator<ObjectFeature> iterator = featureSet.iterator();
        while (iterator.hasNext()) {
            quantizedFeatureSet.addObject(convertToFeature(iterator.next()));
        }
        return quantizedFeatureSet;
    }
    
    /**
     * Transform quantized feature to a string value of visual word
     * @param feature a feature to transform
     * @return a string containing visual word associated to the feature passed
     */
    public String convertToWord(ObjectFeatureQuantized feature) {
        return feature.getStringData();
    }

    /**
     * Transform a feature set of quantized features to a string value which represent a text document of visual words
     * @param featureSet a feature set to transform
     * @return a textual document containing all visual words obtained by quantizing the feature set
     */
    public String convertToWord(ObjectFeatureSet featureSet) {
        return featureSet.getStringData();
    }
    
    /**
     * Instance of binary serializator that should be used to serialize all visual vocabulary implementations.
     * @param vocabularyClass class of a particular visual vocabulary implementation.
     * @return instance of {@link MultiClassSerializator}
     */
    public static BinarySerializator getBinarySerializator(Class<? extends AbstractVisualVocabulary> vocabularyClass) {
        return new MultiClassSerializator<AbstractVisualVocabulary>(vocabularyClass);
    }
    
    /**
     * Instantiate visual vocabulary using a recommended binary seralizator ({@link AbstractVisualVocabulary#getBinarySerializator(java.lang.Class)}).
     * @param <E> class of visual vocabulary to instantiate
     * @param visualVocabularyPath file name of visual vocabulaery storage
     * @param visualVocabularyClass class of visual vocabulary to instantiate
     * @return instance of visual vocabulary
     * @throws IOException on error
     */
    @SuppressWarnings("unchecked")
    public static <E extends AbstractVisualVocabulary> E readVocabulary(String visualVocabularyPath, Class<E> visualVocabularyClass) throws IOException {
        try {
            ChannelInputStream stream = new ChannelInputStream(new FileInputStream(visualVocabularyPath).getChannel(), 10240, true);
            return getBinarySerializator(visualVocabularyClass).readObject(stream, visualVocabularyClass);
        } catch (IllegalArgumentException ex) {
            if (KMeansVisualVocabulary.class.isAssignableFrom(visualVocabularyClass)) {
                // Convert from old binary serialization to the new one
                ChannelInputStream streamIn = new ChannelInputStream(new FileInputStream(visualVocabularyPath).getChannel(), 10240, true);
                KMeansVisualVocabulary vv = new KMeansVisualVocabulary(streamIn, new SingleClassSerializator<KMeansVisualVocabulary>(KMeansVisualVocabulary.class));
                // Overwrite the file
                AbstractVisualVocabulary.writeVocabulary(visualVocabularyPath, vv);
                return (E)vv;       // This is checked!
            } else {
                throw ex;
            }
        }
    }

    /**
     * Instantiate visual vocabulary using a recommended binary seralizator ({@link AbstractVisualVocabulary#getBinarySerializator(java.lang.Class)}).
     * @param visualVocabularyPath file name of visual vocabulaery storage
     * @param visualVocabulary visual vocabulary to store
     * @throws IOException on error
     */
    public static void writeVocabulary(String visualVocabularyPath, AbstractVisualVocabulary visualVocabulary) throws IOException {
        ChannelOutputStream out = new ChannelOutputStream(10240, true, new FileOutputStream(visualVocabularyPath, false).getChannel());
        getBinarySerializator(visualVocabulary.getClass()).write(out, visualVocabulary);
        out.flush();
    }
}

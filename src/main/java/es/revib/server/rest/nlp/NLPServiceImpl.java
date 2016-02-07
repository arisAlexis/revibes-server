package es.revib.server.rest.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * POS Tagging is done with stanford nlp and synonym and related words with wordnet (library extjwnl).
 *
 The nlp system works with stanford nlp. Whenever an activity is archived for each term (noun or verb) in the title ,description and tags we expand it to include all its hypernyms.
 The title and description are parsed with the stanford nlp to extract the POSs and then give them to wordnet for finding the hypernyms and synonyms.
 We could get also the hyponyms.
 */
public class NLPServiceImpl implements INLPService {

    static StanfordCoreNLP coreNLP;
    static Dictionary dictionary;

    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        coreNLP = new StanfordCoreNLP(props);
        try {
            dictionary = Dictionary.getDefaultResourceInstance();
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> findImportantTerms(String text) {

        Set<String> terms=new HashSet<>();

        Annotation annotation = new Annotation(text);
        coreNLP.annotate(annotation);

        // taken from the official documentation

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (pos.startsWith("NN") || pos.startsWith("VB")) {
                    //todo undecided yet if we are going to use lemmas
                    //terms.add(toLemma(word));
                    terms.add(word);
                }
            }

        }

        return terms;
    }

    /**
     * Returns the lemma of the word. Used in query expanding
     *
     * @param word
     * @return
     */
    @Override
    public String toLemma(String word) {
        Annotation annotation = new Annotation(word);
        coreNLP.annotate(annotation);
        CoreLabel token =annotation.get(CoreAnnotations.TokensAnnotation.class).get(0);
        return token.get(CoreAnnotations.LemmaAnnotation.class);
    }

    /**
     * finds related words such as synonyms,hyponyms,hypernyms using wordnet synsets
     *
     * @param s a lemma obtained from nlp core
     * @return
     */
    @Override
    public Set<String> expandWord(String s) {
        Set<String> expandedSet=new HashSet<>();

        /*
        logic is take a lemma, look it up and for every lemma-pos combo (this is a bit problematic)
        get all the first sense (we could iterate them all) and get all hypernyms (why not hyponyms too?)
         */
        try {
            IndexWordSet indexWordSet=dictionary.lookupAllIndexWords(s);
            for (IndexWord i:indexWordSet.getIndexWordCollection()) {
                for (Synset synset:i.getSenses()) {
                    for (Word w:synset.getWords()) {
                        expandedSet.add(w.getLemma());
                    }
                }
                /*              this is for finding hypernyms
                PointerTargetNodeList hypernyms = PointerUtils.getDirectHypernyms(i.getSenses().get(0));
                for (PointerTargetNode pt:hypernyms) {
                    for (Word w:pt.getPointerTarget().getSynset().getWords())
                    expandedSet.add(w.getLemma());
                }
                */
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }
        return expandedSet;
    }

    /**
     * uses all methods to produce a set of terms that include important terms,related terms and synonyms to
     * be used for storing
     *
     * @param text
     * @return
     */
    @Override
    public Set<String> expandText(String text) {
        return null;
    }
}

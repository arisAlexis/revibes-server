package es.revib.server.rest.nlp;

import java.util.Set;

public interface INLPService {

    /**
     * This method finds terms that exist already in the text
     * @param text
     * @return
     */
    Set<String> findImportantTerms(String text);

    /**
     * Returns the lemma of the word. Used in query expanding
     * @param word
     * @return
     */
    String toLemma(String word);

    /**
     * finds related words such as synonyms,hyponyms,hypernyms using wordnet synsets
     *
     * @param s
     * @return
     */
    Set<String> expandWord(String s);

    /**
     * uses all methods to produce a set of terms that include important terms,related terms and synonyms to
     * be used for storing
     * @param text
     * @return
     */
    Set<String> expandText(String text);
}


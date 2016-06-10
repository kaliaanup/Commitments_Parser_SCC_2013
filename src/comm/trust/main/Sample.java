package comm.trust.main;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import comm.trust.libs.CommTrustParser;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Sample {

	public static Hashtable<Integer, ArrayList<String>> nerDictionary = new Hashtable<Integer, ArrayList<String>>();
	public static Map<Integer, CorefChain> graph;
	public static int nerID=0;
	/**
	 * 
	 * @param text
	 */
	public static void updatePronounDictionary(String text)
	{


		Annotation document = new Annotation(text);
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods

			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				// this is the text of the token

				String word = token.get(TextAnnotation.class);
				if(!word.equals("."))
				{
					ArrayList<String> info=new ArrayList<String>();
					info.add(word);
					info.add(token.get(PartOfSpeechAnnotation.class));
					info.add(token.get(NamedEntityTagAnnotation.class));
					nerID++;
					nerDictionary.put(nerID, info);
				}
			}

		}

		graph = document.get(CorefChainAnnotation.class);
	}
	public static void main(String args[])
	{
		updatePronounDictionary("Kim has scheduled you for an interview on Wednesday.");
	}
	
}

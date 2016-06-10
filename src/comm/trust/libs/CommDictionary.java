package comm.trust.libs;

import net.didion.jwnl.*;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.*;
import net.didion.jwnl.dictionary.*;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import comm.trust.config.ProjectConfig;

public class CommDictionary {
	
	private  static Dictionary dic;
	private  static MorphologicalProcessor morph;
	private  static boolean isInitialized = false;  
	/**
	 * open the dictionary connection
	 * creates a connection to WordNet
	 */
	public  static void openDictionaryConnection() 
	{
		try
		{
			JWNL.initialize(new FileInputStream(ProjectConfig.wordnetpath));
			dic = Dictionary.getInstance();
			morph = dic.getMorphologicalProcessor();
			isInitialized = true;
		}
		catch ( FileNotFoundException e )
		{
			System.out.println ( "Error initializing Dictionary: JWNLproperties.xml not found" );
		}
		catch ( JWNLException e )
		{
			System.out.println ( "Error initializing Dictionary: "+ e.toString() );
		}
	}
	/**
	 * close the dictionary connection
	 */
	public static void closeDictionaryConnection()
	{ 
		dic.close();
		Dictionary.uninstall();
		JWNL.shutdown();
	}
	/**
	 * get the base form of the word
	 * @param word
	 * @return
	 */
	public  static String getBaseForm(String word)
	{
		if ( word == null ) return null;
		if ( morph == null ) morph = dic.getMorphologicalProcessor();
		
		IndexWord w;
		try
		{
			
			w = morph.lookupBaseForm( POS.VERB, word );
			if ( w != null )
				return w.getLemma().toString ();
			w = morph.lookupBaseForm( POS.NOUN, word );
			if ( w != null )
				return w.getLemma().toString();
			w = morph.lookupBaseForm( POS.ADJECTIVE, word );
			if ( w != null )
				return w.getLemma().toString();
			w = morph.lookupBaseForm( POS.ADVERB, word );
			if ( w != null )
				return w.getLemma().toString();
		} 
		catch ( JWNLException e )
		{
			System.out.println("Error in the word"+e);
		}
		return null;
	}
	public  static String getPOSFromDictionary(String word)
	{
		if ( word == null ) return null;
		if ( morph == null ) morph = dic.getMorphologicalProcessor();
		
		IndexWord w;
		try
		{
			
			w = morph.lookupBaseForm( POS.VERB, word );
			if ( w != null )
				return "VERB";
			w = morph.lookupBaseForm( POS.NOUN, word );
			if ( w != null )
				return "NOUN";
			w = morph.lookupBaseForm( POS.ADJECTIVE, word );
			if ( w != null )
				return "ADJECTIVE";
			w = morph.lookupBaseForm( POS.ADVERB, word );
			if ( w != null )
				return "ADVERB";
		} 
		catch ( JWNLException e )
		{
			System.out.println("Error in the word"+e);
		}
		return null;
	}
	/**
	 * 
	 * @param lexicalForm
	 * @return
	 * @throws JWNLException
	 */
	
	public static Set<String> getSynonyms(String word)
	{
		Set<String> synonyms = new HashSet<String>();
		IndexWord w=null;
		Synset[] synSets;
		List<POS> allPos = POS.getAllPOS();//check for all pos such as VERB, ADVERB, NOUN etc.
		try
		{
			for (POS pos : allPos)
			{
				w= dic.getIndexWord(pos, word);
				if(w!=null)
				{
					synSets= w.getSenses();
					for (Synset synset : synSets)
					{
						Word[] words = synset.getWords();
						for (Word word1 : words)
						{
							synonyms.add(word1.getLemma());
						}
					}
				}
			}
			if (w == null) return synonyms;
		}
		catch(JWNLException e)
		{
			System.out.println("Error in finding synonyms"+e);
		}
		return synonyms;
	}
	/**
	 * get the antonym of a word
	 * @param word
	 * @return
	 */
	public static Set<String> getAntonyms(String word)
	{
		Set<String> antonyms = new HashSet<String>();
		IndexWord w=null;
		Synset[] synSets;
		Pointer[] pointerArr;
		List<POS> allPos = POS.getAllPOS();
		try
		{
			for (POS pos : allPos)
			{
				w= dic.getIndexWord(pos, word);//check for all pos such as VERB, ADVERB, NOUN etc.
				if(w!=null)
				{
					synSets= w.getSenses();
					for (Synset synset : synSets)
					{
						pointerArr = synset.getPointers(PointerType.ANTONYM);
						for (Pointer pointer : pointerArr)
						{
		                    Synset curSet = pointer.getTargetSynset();
		                    Word[] allWords = curSet.getWords();
		                    for (Word word1 : allWords)
		                    {
		                    	antonyms.add(word1.getLemma());
		                    }
						}
						
					}
				}
			}
			if (w == null) return antonyms;
		}
		catch(JWNLException e)
		{
			System.out.println("Error in finding antonyms"+e);
		}
		return antonyms;
	}
	/**
	 * get the hyponym of a word
	 * @param word
	 * @return
	 */
	public static Set<String> getHyponyms(String word)
	{
		Set<String> hyponyms = new HashSet<String>();
		IndexWord w=null;
		Synset[] synSets;
		Pointer[] pointerArr;
		List<POS> allPos = POS.getAllPOS();
		try
		{
			for (POS pos : allPos)
			{
				w= dic.getIndexWord(pos, word);//check for all pos such as VERB, ADVERB, NOUN etc.
				if(w!=null)
				{
					synSets= w.getSenses();
					for (Synset synset : synSets)
					{
						pointerArr = synset.getPointers(PointerType.HYPONYM);
						for (Pointer pointer : pointerArr)
						{
		                    Synset curSet = pointer.getTargetSynset();
		                    Word[] allWords = curSet.getWords();
		                    for (Word word1 : allWords)
		                    {
		                    	hyponyms.add(word1.getLemma());
		                    }
						}
						
					}
				}
			}
			if (w == null) return hyponyms;
		}
		catch(JWNLException e)
		{
			System.out.println("Error in finding hyponyms"+e);
		}
		return hyponyms;
	}
	/**
	 * get the hyponym of a word
	 * @param word
	 * @return
	 */
	public static Set<String> getHypernyms(String word)
	{
		Set<String> hypernyms = new HashSet<String>();
		IndexWord w=null;
		Synset[] synSets;
		Pointer[] pointerArr;
		List<POS> allPos = POS.getAllPOS();
		try
		{
			for (POS pos : allPos)
			{
				w= dic.getIndexWord(pos, word);//check for all pos such as VERB, ADVERB, NOUN etc.
				if(w!=null)
				{
					synSets= w.getSenses();
					for (Synset synset : synSets)
					{
						pointerArr = synset.getPointers(PointerType.HYPERNYM);
						for (Pointer pointer : pointerArr)
						{
		                    Synset curSet = pointer.getTargetSynset();
		                    Word[] allWords = curSet.getWords();
		                    for (Word word1 : allWords)
		                    {
		                    	hypernyms.add(word1.getLemma());
		                    }
						}
						
					}
				}
			}
			if (w == null) return hypernyms;
		}
		catch(JWNLException e)
		{
			System.out.println("Error in finding hyponyms"+e);
		}
		return hypernyms;
	}
	/**
	 * check if two words are related as base words or synonyms or antonyms
	 * @param word1
	 * @param word2
	 * @return
	 */
	public  static boolean relatedWords(String word1, String word2)
	{
		//System.out.println(word1+"$$$$"+word2);
		//check by forms
		
		word1 = getBaseForm(word1);
		word2 = getBaseForm(word2);
		if(word1 != null && word2 != null)
		{
			if(getBaseForm(word1).equals(word2))
			{
				return true;
			}
			//check by synonyms
			
			if(getSynonyms(word1.replace(" ", "_")).contains(word2.replace(" ", "_")))
			{
				return true;
			}
			
			//check by antonyms
			if(getAntonyms(word1.replace(" ", "_")).contains(word2.replace(" ", "_")))
			{
				return true;
			}
			//check by hyponyms
			if(getHyponyms(word1.replace(" ", "_")).contains(word2.replace(" ", "_")))
			{
				return true;
			}
			//check by hypernyms
			if(getHypernyms(word1.replace(" ", "_")).contains(word2.replace(" ", "_")))
			{
				return true;
			}
		}
		return false;
	}
	public Set<String> lookupMorphologicallySimilarLexicalForms(String word)
	{
			Set<String> forms = new HashSet<String>();
			morph = dic.getMorphologicalProcessor();
			List baseForms=null;
			List<POS> allPos = POS.getAllPOS();
			try {
				for (POS pos : allPos)
				{
					baseForms = morph.lookupAllBaseForms(pos, word);
					if(baseForms!=null)
					{
						for (Object baseForm : baseForms)
						{
							forms.add(baseForm.toString());
						}
					}
				}
			} catch (JWNLException e) {
				e.printStackTrace();
			}
			return forms;
	}
	public static void main(String[] args){
		CommDictionary obj=new CommDictionary();
		obj.openDictionaryConnection();
		System.out.println(obj.getBaseForm("went"));
		/*System.out.println(obj.relatedWords("send", "broadcast"));
		System.out.println(obj.relatedWords("gives", "taken"));
		System.out.println(obj.relatedWords("seat", "chair"));*/
		System.out.println(obj.lookupMorphologicallySimilarLexicalForms("brzng"));
		obj.closeDictionaryConnection();
		
	}
}

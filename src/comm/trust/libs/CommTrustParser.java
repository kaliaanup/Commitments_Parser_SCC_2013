package comm.trust.libs;



import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import comm.trust.main.CommTrustMain;
import comm.trust.config.ProjectConfig;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class CommTrustParser {
	
		//split the messages into sentences and store them
		public static Hashtable<Integer, String> sentenceArrays= new Hashtable<Integer, String>();
		//store the POS tags for each sentence
		public static Hashtable<Integer, ArrayList<TaggedWord>> taggedSentenceArrays= new Hashtable<Integer, ArrayList<TaggedWord>>();
		//store typed dependencies of each statement
		public static Hashtable<Integer,  List<TypedDependency>> typedSentenceArrays= new Hashtable<Integer, List<TypedDependency>>();
		
		public static Hashtable<Integer, ArrayList<String>> commitmentStore = new Hashtable<Integer, ArrayList<String>>();
		//store all tasks
		public static Hashtable<Integer, ArrayList<String>> taskStore = new Hashtable<Integer, ArrayList<String>>();
		public static Hashtable<Integer, ArrayList<String>> nerDictionary = new Hashtable<Integer, ArrayList<String>>();
		public static Map<Integer, CorefChain> graph;
		//data stores information for weka output
		public static Hashtable<Integer, ArrayList<String>> data = new Hashtable<Integer, ArrayList<String>>();
		public static ArrayList<String> emailBody=new ArrayList<String>();
		public static ArrayList<String> temp=new ArrayList<String>();
		public static ArrayList<String> actionVerbsList=new ArrayList<String>();
		public static int dataID=0;
		public static int  delegationCount = 0;
		public static int sentenceID=0;
		public static int taskID=0;
		public static int commitmentID=0;
		public static int nerID=0;
		/**
		 * 
		 * @param name
		 * @return
		 */
		public static String extractNameCorrectly(String name)
		{
			String firstname=new String();
			firstname="";
			String lastname=new String();
			lastname="";
			String finalname=new String();
			finalname="";
			int posn;
			if(name.contains("@"))
			{
				posn=name.indexOf("@");
				name=name.substring(0,posn);
				if(name.contains("."))
				{
					posn=name.indexOf(".");
					firstname=name.substring(0, posn);
					lastname=name.substring(posn+1, name.length());
				}
				else
				{
					firstname=name;
					lastname="";
				}
				
				finalname=firstname.trim()+" "+lastname.trim();
			}
			else if(name.contains(","))
			{
				posn=name.indexOf(",");
				firstname=name.substring(0, posn);
				firstname=firstname.toLowerCase();
				lastname=name.substring(posn+1, name.length());
				lastname=lastname.toLowerCase();
				finalname=lastname.trim()+" "+firstname.trim();
			}
			else
			{
				finalname=name;
			}
			return finalname;
		}
		public static String extractReceiverNamesCorrectly(String name)
		{
			String finalName= new String();
			name=name.replaceAll("Cc:", ";");
			if(name.contains(";"))
			{
				StringTokenizer tokens=new StringTokenizer(name, ";");
				
				while(tokens.hasMoreTokens())
				{
					finalName=finalName+extractNameCorrectly(tokens.nextToken())+",";
				}
				
			}
			else if(name.contains("@"))
			{
				StringTokenizer tokens=new StringTokenizer(name, ",");
				while(tokens.hasMoreTokens())
				{
					finalName=finalName+extractNameCorrectly(tokens.nextToken())+",";
					System.out.println(finalName);
				}
			}
			else
			{
				finalName=extractNameCorrectly(name);
			}
			
			return finalName;
		}
		/**
		 * 
		 * @param lp
		 */
		public void parseEmailBodies(LexicalizedParser lp) 
		{
			String sender=new String();
			String receiver=new String();
			String email=new String();
			

			sender=emailBody.get(0);
			receiver=emailBody.get(1);
			email=emailBody.get(2);
			
			//normalize all forms of sender names and receiver names to a simpler format
			//e.g., kimberley watson
			System.out.println(sender);
			System.out.println(receiver);
			//sender=extractReceiverNamesCorrectly(sender);
			//receiver=extractReceiverNamesCorrectly(receiver);
			updatePronounDictionary(email);
			int sentenceIDinitial=sentenceID;
			splitSentences(lp, email);
			/*-------------------END UNLOADING THE ENTIRE FILE AT ONCE-----------------*/

			/*-------------------START PARSING ONE SENTENCE AT A TIME-----------------*/
			//Run stanford parser for each line in the message file
			
			for(int i=sentenceIDinitial+1; i<sentenceID+1;i++)
			{
				identifyMeaningOfMessage(i, sender, receiver);
			}
		}
		/**
		 * 
		 * @param lp
		 * @param email
		 */
		public void splitSentences(LexicalizedParser lp, String email)
		{

			BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US); 
			iterator.setText(email); int start = iterator.first();
			String temp = new String();
			for (int end = iterator.next();end != BreakIterator.DONE; start = end, end = iterator.next())
			{
				//ArrayList<String> sentence=new ArrayList<String>();
				//original sentence
				temp = email.substring(start,end);
				//remove quotes from sentences

				temp = temp.replace('"', '\"');
				temp = temp.replace("\"", "");
				temp=temp.trim();
				//add the sentence
				//sentence.add(temp);
				if(temp.length()>0)
				{
				/*-------------------PARSE EACH SENTENCE USING STANFORD PARSER------------------------*/
				TreebankLanguagePack tlp = new PennTreebankLanguagePack();
				GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
				Tree parse = lp.apply(temp);//for pennPrint and taggedYield

				GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
				//Collection tdl = gs.typedDependenciesCCprocessed(true);//for dependencies
				List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

				sentenceID++;
				sentenceArrays.put(sentenceID, temp);

				//sentenceID++;
				taggedSentenceArrays.put(sentenceID, parse.taggedYield());
				//parse.pennPrint();
				//System.out.println(parse.taggedYield());

				//sentenceID++;
				typedSentenceArrays.put(sentenceID,tdl);
				}

			} 
		}
		/**
		 * 
		 * @param pKey
		 * @param sender
		 * @param receiver
		 */
		public static void identifyMeaningOfMessage(Integer pKey, String sender, String receiver)
		{
			String sentence=sentenceArrays.get(pKey);
			ArrayList<TaggedWord> taggedWord= taggedSentenceArrays.get(pKey);
			List<TypedDependency> tdl=typedSentenceArrays.get(pKey);
			//sentenceArrays.get(pKey).get(1);

			/*------------Create commitment structures--------------------*/

			String[] commitmentData=null;
			String[] dischargeData=null;
			String[] delegateData=null;
			String[] cancelData=null;
			String[] signalData=null;
			/*------------Check if a sentence is a report or a chart message----------------*/
			int taskIDinitial=taskID;
			signalData = identifyTasks(pKey, sender, receiver);
			
			int commitmentIDinitial=commitmentID;
			commitmentData = identifyCommitmentCreate(taggedWord,tdl, pKey, sender, receiver, signalData, taskIDinitial);
			delegateData =  identifyCommitmentDelegate(taggedWord, tdl, pKey, commitmentData, commitmentIDinitial);
			dischargeData =  identifyCommitmentDischarge(taggedWord, tdl, pKey, signalData, taskIDinitial, commitmentIDinitial);
			cancelData = identifyCommitmentCancel(taggedWord, tdl, pKey, signalData, taskIDinitial, commitmentIDinitial);
			
			if(commitmentData[15].equals("create") || commitmentData[15].equals("icreate"))
			{
				generateData(taggedWord, tdl, pKey, commitmentData);
				if(delegateData[15].equals("delegate"))
				{
					generateData(taggedWord, tdl, pKey, delegateData);
				}
			}
			else if(dischargeData[15].equals("discharge"))
			{
				generateData(taggedWord, tdl, pKey, dischargeData);
			}
			else if(cancelData[15].equals("cancel"))
			{
				generateData(taggedWord, tdl, pKey, cancelData);
			}
			else if(signalData[15].equals("none"))
			{
				generateData(taggedWord, tdl, pKey, signalData);
			}
			/*------------Generating features data--------------------*/
			//generate data from each message
			//generateData(taggedWord, tdl, pKey, commitmentData, dischargeData);
			/*------------------------------------------------------------------*/
			System.out.println(tdl);
			System.out.println(taggedWord);
			System.out.println();
		}
		/**
		 * 
		 * @param taggedYield
		 * @param tdl
		 * @param pKey
		 * @param sender
		 * @param receiver
		 * @return
		 */
		public static void storeActionVerbs()
		{
			String strLine=new String();
			FileInputStream fstream=null;
			try 
			{
				fstream = new FileInputStream(ProjectConfig.actionVerbList);
			} 
			catch (FileNotFoundException e1) 
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			try 
			{
				while ((strLine = br.readLine()) != null)
				{
					if(strLine.charAt(0) != '%')
					{
						actionVerbsList.add(strLine.toLowerCase());
					}
				}
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		private static String[] identifyCommitmentCreate(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, Integer pKey, 
				String sender, String receiver, String[] taskData, int initalTaskID)
		{
			//String[] signalTaskData=new String[16];
			String[] signalData= new String[16];
			//initialize signals
			//[0]-modal verb signal
			//[1]-action verb signal
			//[2]-type of subject
			//[3]-Negative verb signal
			//[4]-present tense verb signal,
			//[5]-past tense verb signal, 
			//[6]-debtor signal,
			//[7]-creditor signal,
			//[8]-delegation Signal
			//[9]-deadline Signal
			//[10]-createCommitmentPresent
			//[11]-Bigram Modal Second
			//[12]-Bigram First Modal
			//[13]-Bigram Please Action
			//[14]-Question Mark Present
			//[15]-class
			/*for(int i=0; i<signalData.length;i++)
			{
				signalData[i]="no";//rest all no
			}
			signalData[0]="none";//modal verb
			signalData[2]="none";//type of subject
			signalData[15]="none";//class
*/			
			for(int i=0;i<16;i++)
			{
				signalData[i]=taskData[i];
			}
			
			//collect subject, object, and action happening between them: For example, subject asks the object to do a task o
			int taskIDinitial=taskID; //store the latest task ID
			String subject = null;
			String object = null;
			String action = null;

			int validActionSignalFlag=0;
			/*------------------------------------------------BEGIN TASK IDENTIFICATION IN EMAILS------------------------------------*/
			//signalData = identifyTasks(pKey, sender, receiver);


			/*------------------------------------------------BEGIN COMMITMENT CREATE IDENTIFICATION IN EMAILS-------------------------*/
			ArrayList<String> attributes = new ArrayList<String>();

			//check from last taskID+  to latest task ID: It means we will consider
			//tasks extracted from the current conversation
			//From these tasks we will identify commitments
					for(int l=initalTaskID+1; l<taskID+1; l++)
					{
						attributes=taskStore.get(l);
						//check whether a task is a commitment or not. If yes then store the debtor, creditor and its task
						//create a commitment array to store its attributes
		
						ArrayList<String> commitment = new ArrayList<String>();
						int commitSignal=0;
						//check deadline signal
						for(int i=3; i<attributes.size();i++)
						{
							if(checkDeadlineIndicatingWords(attributes.get(i)))
							{
								signalData[9]="yes";
							}
						}
						//for(int i=3; i<attributes.size();i++)
						//{
							//TODO: check for conditionality 
							/*--------------------------------A PRESENT VERB FOLLOWS A MODAL VERB--------------------------------------*/
							for(int j=0; j<taggedYield.size(); j++)
							{
								//check whether the tense of the verb is present
									if(taggedYield.get(j).tag().equals("VB") && taggedYield.get(j).word().equals(attributes.get(3)))
									{	//if(actionVerbsList.contains(attributes.get(3)))
											//{
												signalData[1]="yes";//collect the present verb signal
												signalData[4]="yes";
												validActionSignalFlag=1;
											//}
									}
									//will, may, can, must, could, would, requested, please, should
									if(j !=0 && 
											(taggedYield.get(j-1).word().equals("shall")||
													taggedYield.get(j-1).word().equals("will")||
													taggedYield.get(j-1).word().equals("may") ||
													taggedYield.get(j-1).word().equals("might") ||
													taggedYield.get(j-1).word().equals("must")||
													taggedYield.get(j-1).word().equals("can")||
													taggedYield.get(j-1).word().equals("could")||
													taggedYield.get(j-1).word().equals("would")||
													taggedYield.get(j-1).word().equals("should")||
													taggedYield.get(j-1).word().equals("please")||
													taggedYield.get(j-1).word().equals("Please")))
									{
										signalData[0]=taggedYield.get(j-1).word();//collect the modal verb
										if(validActionSignalFlag == 1)
										{
											commitSignal=1;
											if(signalData[2].equals("first"))
											{
												signalData[13].equals("yes");
											}
										}
									}
									else //to handle situations: will you take?, I will now do..
									{
										for (Iterator iterator = tdl.iterator(); iterator.hasNext();) 
										{
											TypedDependency typedDependency = (TypedDependency) iterator.next();
											if(typedDependency.dep().label().value().equals(taggedYield.get(j).word()))
											{
												if(typedDependency.gov().label().value().equals("shall")||
														typedDependency.gov().label().value().equals("will")||
														typedDependency.gov().label().value().equals("may") ||
														typedDependency.gov().label().value().equals("might") ||
														typedDependency.gov().label().value().equals("must")||
														typedDependency.gov().label().value().equals("can")||
														typedDependency.gov().label().value().equals("could")||
														typedDependency.gov().label().value().equals("would")||
														typedDependency.gov().label().value().equals("should")||
														typedDependency.gov().label().value().equals("please")||
														typedDependency.gov().label().value().equals("Please"))
											
												{
													signalData[0]=typedDependency.gov().label().value();//collect the modal verb
													if(validActionSignalFlag == 1)
													{
														commitSignal=1;
													}
													break;
												}
											}		
											else if(typedDependency.gov().label().value().equals(taggedYield.get(j).word()))
											{
												if(typedDependency.dep().label().value().equals("will")||
														typedDependency.dep().label().value().equals("can") ||
														typedDependency.dep().label().value().equals("must")||
														typedDependency.dep().label().value().equals("may")||
														typedDependency.dep().label().value().equals("could")||
														typedDependency.dep().label().value().equals("would")||
														typedDependency.dep().label().value().equals("should")||
														typedDependency.dep().label().value().equals("please"))
												{
													signalData[0]=typedDependency.dep().label().value();//modal verb
													if(validActionSignalFlag == 1)
													{
														commitSignal=1;
														
													}
													break;
												}
											}
										}
									}
							}
							if(commitSignal == 1)
							{
								System.out.println("CREATE");
								commitment.add(0, pKey.toString());
								commitment.add(1, (new Integer(l)).toString());
								commitment.add(2, attributes.get(1));//insert debtor
								commitment.add(3, attributes.get(2));//insert creditor
								if(!attributes.get(1).equals(""))
								{
									signalData[6]="yes";//debtor signal
								}
								if(!attributes.get(2).equals(""))
								{
									signalData[7]="yes";//creditor signal
								}
								//TODO: creditor signal
								for(int k=3; k<attributes.size();k++)
								{
									if(!commitment.contains(attributes.get(k)))
									{
										commitment.add(attributes.get(k));
									}
								}
								if(signalData[11].equals("yes") || signalData[13].equals("yes"))
								{
									signalData[15]="icreate";
								}
								else
								{
									signalData[15]="create";
								}
								commitmentID++;
								commitmentStore.put(commitmentID, commitment);
								break;
							}
						//}
					//}
				}
			return signalData;
			}
		
		/**
		 * 
		 * @param pKey
		 * @param sender
		 * @param receiver
		 */
		public static String[] identifyTasks(Integer pKey, String sender, String receiver)
		{
			List<TypedDependency> tdl=typedSentenceArrays.get(pKey);
			ArrayList<TaggedWord> taggedYield= taggedSentenceArrays.get(pKey);
			String sentence=sentenceArrays.get(pKey);
			
			String[] signalData= new String[16];
			//initialize signals
			//[0]-modal verb signal
			//[1]-action verb signal
			//[2]-type of subject
			//[3]-Negative verb signal
			//[4]-present tense verb signal,
			//[5]-past tense verb signal, 
			//[6]-debtor signal,
			//[7]-creditor signal,
			//[8]-delegation Signal
			//[9]-deadline Signal
			//[10]-createCommitmentPresent
			//[11]-Bigram Modal Second
			//[12]-Bigram First Modal
			//[13]-Bigram Please Action
			//[14]-Question Mark Present
			//[15]-class
			for(int i=0; i<signalData.length;i++)
			{
				signalData[i]="no";//rest all no
			}
			signalData[0]="none";//modal verb
			signalData[2]="none";//type of subject
			signalData[15]="none";
			
			ArrayList<String> attributes=new ArrayList<String>();
			int taskIDinitial=taskID; //store the latest task ID
			//collect subject, object, and action happening between them: For example, subject asks the object to do a task o
			String subject = null;
			String object = null;
			String action = null;
			
			if(sentence.contains("?"))
			{
					signalData[14]="yes";
			}

			/*----------------------------------DIFFERENT WAYS OF IDENTIFYING SUBJECT IN THE TASK-----------------------------*/
			//Basic points
			//1. Identify subject and check the validity of the subject
			//2. Identify verb and check if its a valid verb or not
			//3. 

			/*-----------------------------------CASE 1 (RELATIONSHIP: NSUBJECT): ACCURACY RESULT: HIGH------------------------------------------------
			 * 1. Subject claims the task or perform the task (when he says I or we)
			 * 2. Subject says somebody else will do the work or has done the work (Third party)
			 */
			if(sentence.contains("will") || sentence.contains("Will"))
			{
				signalData[0]="will";
			}
			else if (sentence.contains("shall") || sentence.contains("Shall"))
			{
				signalData[0]="shall";
			}
			else if (sentence.contains("may") || sentence.contains("May"))
			{
				signalData[0]="may";
			}
			else if (sentence.contains("might") || sentence.contains("Might"))
			{
				signalData[0]="might";
			}
			else if (sentence.contains("can") || sentence.contains("Can"))
			{
				signalData[0]="can";
			}
			else if (sentence.contains("must") || sentence.contains("Must"))
			{
				signalData[0]="must";
			}
			else if (sentence.contains("could") || sentence.contains("Could"))
			{
				signalData[0]="could";
			}
			else if (sentence.contains("would") || sentence.contains("Would"))
			{
				signalData[0]="would";
			}
			else if (sentence.contains("should") || sentence.contains("Should"))
			{
				signalData[0]="should";
			}
			else if (sentence.contains("please") || sentence.contains("Please"))
			{
				signalData[0]="please";
			}
			
			if(subject == null & action == null)
			{
				for (Iterator iterator = tdl.iterator(); iterator.hasNext();)
				{
					//get all typed dependencies for each message
					TypedDependency typedDependency = (TypedDependency) iterator.next();

					if(typedDependency.reln().getShortName().equals("nsubj"))
					{
							attributes=new ArrayList<String>();
							/*------------------------SUBJECT : I/WE -----------------------------------------------*/
							//Chat initiator is the owner of the task when there is a nsubj that has dep as I or We
							if(typedDependency.dep().label().value().equals("I")||
									typedDependency.dep().label().value().equals("i")||
									typedDependency.dep().label().value().equals("We")||
									typedDependency.dep().label().value().equals("we")||
									typedDependency.dep().label().value().equals("myself")||
									typedDependency.dep().label().value().equals("Myself"))
							{
								//check if the verb is valid
								if(checkValidVerb(taggedYield, tdl, typedDependency.gov().label().value()))
								{
									subject=sender;
									object= receiver;
									action=typedDependency.gov().label().value();
									attributes.add(0, pKey.toString());
									attributes.add(1, subject);
									attributes.add(2, object);
									attributes.add(3, action);
									findActionString(taggedYield, tdl,attributes, action);
									//
									signalData[6]="yes";
									signalData[7]="yes";
									
									for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
									{
										TypedDependency typedDependency1 = (TypedDependency) iterator.next();
										if(typedDependency1.gov().label().value().equals(action))
										{
											if(typedDependency1.dep().label().value().equals("not"))
											{
												signalData[3]="yes";
											}
										}
										else if(typedDependency1.dep().label().value().equals(action))
										{
											if(typedDependency1.gov().label().value().equals("not"))
											{
												signalData[3]="yes";
											}
										}
									}
									//action verb signal
									if(actionVerbsList.contains(action))
									{
										signalData[1]="yes";
									}
									for(int j=0; j<taggedYield.size(); j++)
									{
										//check whether the tense of the verb is present
											if(taggedYield.get(j).tag().equals("VB") && taggedYield.get(j).word().equals(action))
											{	//if(actionVerbsList.contains(attributes.get(3)))
												signalData[4]="yes";
											}
											else if((taggedYield.get(j).tag().equals("VBD") || taggedYield.get(j).tag().equals("VBG")) && taggedYield.get(j).word().equals(action))
											{
												signalData[5]="yes";
											}
									}
									signalData[2]="first";
									if(sentence.contains("i can") || sentence.contains("I can")||
											sentence.contains("i will") || sentence.contains("I will")||
											sentence.contains("i shall") || sentence.contains("I shall")||
											sentence.contains("i could") || sentence.contains("I could")||
											sentence.contains("i would") || sentence.contains("I would")||
											sentence.contains("i should") || sentence.contains("I should")||
											sentence.contains("we can") || sentence.contains("We can")||
											sentence.contains("we will") || sentence.contains("We will")||
											sentence.contains("we shall") || sentence.contains("We shall")||
											sentence.contains("we could") || sentence.contains("We could")||
											sentence.contains("we would") || sentence.contains("We would")||
											sentence.contains("we should") || sentence.contains("We should"))
									{
										signalData[12]="yes";
									}
										//once the task performers and its action details are 
									//found it can be stored in a task array
									taskID++;
									taskStore.put(taskID, attributes);
									System.out.println(attributes.toString());
								}
							}
							/*----------------------SUBJECT : 3rd PARTY/YOU-------------------------------------*/
							//the owner of the task is a 3rd party or 'you'
							else if(typedDependency.dep().label().value().equals("you"))
							{
								/*----------------------SUBJECT : YOU-------------------------------------*/
									//check if the subject is 'you'..need to identify the actual subject
									//the gov value can be a noun too..so need to check whether its a
									//verb or not
									if(checkValidVerb(taggedYield, tdl, typedDependency.gov().label().value()))
									{
	
										action=typedDependency.gov().label().value();
	
										/*----------------------SUBJECT : 3rd PARTY + YOU-------------------------------------*/
	
										subject=receiver;
										object=sender;
										action=typedDependency.gov().label().value();
										attributes.add(0, pKey.toString());
										attributes.add(1, subject);
										attributes.add(2,object);
										attributes.add(3,action);
										findActionString(taggedYield, tdl,attributes, action);
										signalData[6]="yes";
										signalData[7]="yes";
										for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
										{
											TypedDependency typedDependency1 = (TypedDependency) iterator.next();
											if(typedDependency1.gov().label().value().equals(action))
											{
												if(typedDependency1.dep().label().value().equals("not"))
												{
													signalData[3]="yes";
												}
											}
											else if(typedDependency1.dep().label().value().equals(action))
											{
												if(typedDependency1.gov().label().value().equals("not"))
												{
													signalData[3]="yes";
												}
											}
										}
										if(actionVerbsList.contains(action))
										{
											signalData[1]="yes";
										}
										//once the task performers and its action details are 
										//found it can be stored in a task array
										signalData[2]="second";
										if(sentence.contains("can you") || sentence.contains("Can you")||
												sentence.contains("will you") || sentence.contains("Will you")||
												sentence.contains("shall you") || sentence.contains("Shall you")||
												sentence.contains("could you") || sentence.contains("Could you")||
												sentence.contains("would you") || sentence.contains("Would you")||
												sentence.contains("should you") || sentence.contains("Should you"))
										{
											signalData[11]="yes";
										}
										
										taskID++;
										taskStore.put(taskID, attributes);
										System.out.println(attributes.toString());
										break;
									}
							}
							/*----------------------SUBJECT : 3rd PARTY-------------------------------------*/
							else if(checkValidVerb(taggedYield, tdl, typedDependency.gov().label().value()))
							{
									subject=typedDependency.dep().label().value();
									if(subject.equals("please") || subject.equals("Please"))
									{
										signalData[0]="please";
										for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
										{
											//get all typed dependencies for each message
											TypedDependency typedDependency1 = (TypedDependency) iterator.next();
											if(typedDependency1.gov().label().value().equals("please") || typedDependency1.gov().label().value().equals("Please"))
											{
												if(checkSubjectValidity(typedDependency1.dep().label().value()))
												{
													subject = typedDependency1.dep().label().value();
												}
											}
											else if(typedDependency1.dep().label().value().equals("please") || typedDependency1.dep().label().value().equals("Please"))
											{
												if(checkSubjectValidity(typedDependency1.gov().label().value()))
												{
													subject = typedDependency1.gov().label().value();
												}
											}
										}
									}
									object = sender;
									action=typedDependency.gov().label().value();
									attributes.add(0, pKey.toString());
									attributes.add(1, subject);
									attributes.add(2,object);
									attributes.add(3,action);
									findActionString(taggedYield, tdl,attributes, action);
									signalData[6]="yes";
									signalData[7]="yes";
									for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
									{
										TypedDependency typedDependency1 = (TypedDependency) iterator.next();
										if(typedDependency1.gov().label().value().equals(action))
										{
											if(typedDependency1.dep().label().value().equals("not"))
											{
												signalData[3]="yes";
											}
										}
										else if(typedDependency1.dep().label().value().equals(action))
										{
											if(typedDependency1.gov().label().value().equals("not"))
											{
												signalData[3]="yes";
											}
										}
									}
									if(actionVerbsList.contains(action))
									{
										signalData[1]="yes";
									}
									//signalData[0]="please";
									signalData[2]="third";
									signalData[12]="yes";
									//once the task performers and its action details are 
									//found it can be stored in a task array
									taskID++;
									taskStore.put(taskID, attributes);
									System.out.println(attributes.toString());
							}
					}
					else if(typedDependency.reln().getShortName().equals("dep"))
					{
						if(typedDependency.gov().label().value().equals("please") || typedDependency.gov().label().value().equals("Please"))
						{
							subject=receiver;
							object=sender;
							action=typedDependency.dep().label().value();
							attributes.add(0, pKey.toString());
							attributes.add(1, subject);
							attributes.add(2,object);
							attributes.add(3,action);
							findActionString(taggedYield, tdl,attributes, action);
							signalData[6]="yes";
							signalData[7]="yes";
							for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
							{
								TypedDependency typedDependency1 = (TypedDependency) iterator.next();
								if(typedDependency1.gov().label().value().equals(action))
								{
									if(typedDependency1.dep().label().value().equals("not"))
									{
										signalData[3]="yes";
									}
								}
								else if(typedDependency1.dep().label().value().equals(action))
								{
									if(typedDependency1.gov().label().value().equals("not"))
									{
										signalData[3]="yes";
									}
								}
							}
							if(actionVerbsList.contains(action))
							{
								signalData[1]="yes";
							}
							//once the task performers and its action details are 
							//found it can be stored in a task array
							signalData[0]="please";
							signalData[2]="second";
							signalData[13]="yes";
							
							taskID++;
							taskStore.put(taskID, attributes);
							System.out.println(attributes.toString());
						}
						else if (typedDependency.dep().label().value().equals("please") || typedDependency.dep().label().value().equals("Please"))
						{
							subject=receiver;
							object=sender;
							action=typedDependency.gov().label().value();
							attributes.add(0, pKey.toString());
							attributes.add(1, subject);
							attributes.add(2,object);
							attributes.add(3,action);
							findActionString(taggedYield, tdl,attributes, action);
							signalData[6]="yes";
							signalData[7]="yes";
							for (Iterator iterator1 = tdl.iterator(); iterator.hasNext();)
							{
								TypedDependency typedDependency1 = (TypedDependency) iterator.next();
								if(typedDependency1.gov().label().value().equals(action))
								{
									if(typedDependency1.dep().label().value().equals("not"))
									{
										signalData[3]="yes";
									}
								}
								else if(typedDependency1.dep().label().value().equals(action))
								{
									if(typedDependency1.gov().label().value().equals("not"))
									{
										signalData[3]="yes";
									}
								}
							}
							if(actionVerbsList.contains(action))
							{
								signalData[1]="yes";
							}
							signalData[0]="please";
							signalData[2]="second";
							signalData[13]="yes";//bigram Please+Action
							//once the task performers and its action details are 
							//found it can be stored in a task array
							taskID++;
							taskStore.put(taskID, attributes);
							System.out.println(attributes.toString());
							
						}
					}
					else if(taggedYield.get(0).tag().equals("VB"))
					{
						subject=receiver;
						object=receiver;
						
						action=taggedYield.get(0).word();
						attributes.add(0, pKey.toString());
						attributes.add(1, subject);
						attributes.add(2,object);
						attributes.add(3,action);
						findActionString(taggedYield, tdl,attributes, action);
						signalData[6]="yes";
						signalData[7]="yes";
						signalData[2]="second";
						signalData[13]="yes";//bigram Please+Action
						
						
					}
				}
			}

			return signalData;
		}
		private static boolean checkVerbValidity(String value) {
			// TODO Auto-generated method stub
			return false;
		}
		/**
		 * 
		 * @param taggedYield
		 * @param tdl
		 * @param pKey
		 * @return
		 */
		private static String[] identifyCommitmentDischarge(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, Integer pKey, String []taskData, int taskInitialID, int  commitmentIDinitial)
		{

			String subject=new String();
			String object = new String();
			String action = new String();
			
			String debtor=new String();
			String creditor= new String();
			String action1=new String();
			String[] signalData= new String[16];
			//initialize signals
			//[0]-modal verb signal
			//[1]-action verb signal
			//[2]-type of subject
			//[3]-Negative verb signal
			//[4]-present tense verb signal,
			//[5]-past tense verb signal, 
			//[6]-debtor signal,
			//[7]-creditor signal,
			//[8]-delegation Signal
			//[9]-deadline Signal
			//[10]-createCommitmentPresent
			//[11]-Bigram Modal Second
			//[12]-Bigram First Modal
			//[13]-Bigram Please Action
			//[14]-Question Mark Present
			//[15]-class
			for(int i=0; i<signalData.length;i++)
			{
				signalData[i]=taskData[i];
			}
			int dischargeCount=0;
			boolean relatedPronounsCheck=false;
			boolean verbValidityCheck=false;

			/*----------------------------------OPEN AN INSTANCE OF WORDNET DICTIONARY-----------------------------------------------*/
			//open connections to wordnet dictionary
			//wordnet dictionary will help to relate words such as verbs
			//CommDictionary.openDictionaryConnection();//moved to main

			/*--------------------------------------------------IDENTIFY COMMITMENTS-------------------------------------------------*/

			//start iterating through all tasks
			for(int w=taskInitialID+1; w<taskID+1;w++)
			{
				ArrayList<String> taskAttrib=new ArrayList<String>();
				taskAttrib=taskStore.get(w);
				Integer sentKey=Integer.parseInt(taskAttrib.get(0));
				//check for actors
				if(sentKey.intValue() == pKey)
				{
					action = taskAttrib.get(3);
					subject = taskAttrib.get(1);
					object = taskAttrib.get(2);


					/*------------------STEP 3: CHECK IF ACTION String and the DEBTOR matches ACTIONS and DEBTOR in COMMITMENT STORE-----------------------------------*/
					for(int i=3; i<taskAttrib.size();i++)
					{
						for(int j=0; j<taggedYield.size();j++)
						{
							if(taggedYield.get(j).word().equals(taskAttrib.get(i)))
							{
								if(taggedYield.get(j).tag().equals("VBD")||
										taggedYield.get(j).tag().equals("VBN")||
										taggedYield.get(j).tag().equals("VBG"))
								{
									//collect signal for verb that occurred in past or still running
									signalData[1]="yes";
									signalData[5]="yes";
									 


										ArrayList<String> commAttrib = new ArrayList<String>();
										int sameActionVerb=0;
										int sameNouns=0;
										//we start from l=2 because 0-sentence ID and 1-task ID
										for(int l=1; l<commitmentIDinitial+1; l++)
										{
											commAttrib=commitmentStore.get(l);
											if(commAttrib != null)
											{
												debtor=commAttrib.get(2);
												creditor=commAttrib.get(3);
												action1=commAttrib.get(4);
												
												if(debtor.equals(subject) || debtor.contains(subject) || subject.contains(debtor))
												{
													if(creditor.equals(object) || creditor.contains(object) || object.contains(creditor))
													{
														if(action.equals(action1) || CommDictionary.relatedWords(action, action1))
														{
															sameActionVerb=1;
															for(int p=5;p<taskAttrib.size();p++)
															{
																for(int q=5;q<commAttrib.size();q++)
																{
																	if(taskAttrib.get(p).equals(commAttrib.get(q)) || relatedPronouns(taskAttrib.get(p), commAttrib.get(q)) )
																	{
																		sameNouns=1;
																		signalData[6]="yes";
																		signalData[7]="yes";
																		signalData[10]="yes";
																		signalData[15]="discharge";
																		break;
																	}
																}
															}
															if(sameActionVerb == 1 && sameNouns== 0)
															{
																signalData[6]="yes";
																signalData[7]="yes";
																signalData[10]="yes";
																signalData[15]="discharge";
																break;
															}
														}
													}
												}
											}
										}
								}
							}
						}
					}
				}
			}
			return signalData;
		}//
		private static String[] identifyCommitmentDelegate(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, Integer pKey, String[] commitmentData, int commitmentIDinitial)
		{
			String [] signalData = new String[16];
			String subject=new String();
			String object=new String();
			String action=new String();
			
			
			for(int i=0;i<16;i++)
			{
				signalData[i]=commitmentData[i];
			}
			
			
			ArrayList<String> attributes = new ArrayList<String>();
			for(int l=commitmentIDinitial+1; l<commitmentID+1; l++)
			{
				attributes=commitmentStore.get(l);
				if(attributes.get(0).equals(pKey.toString()))
				{
					subject=attributes.get(2);
					object=attributes.get(3);
					action=attributes.get(4);
				}
			
				String subject1=new String();
				String object1=new String();
				String action1=new String();
			
				ArrayList<String> attributes1 = new ArrayList<String>();
					for(int x=1; x<commitmentIDinitial+1; x++)
					{
						attributes1=commitmentStore.get(x);
						Integer pKey1=Integer.parseInt(attributes1.get(0));
						if(pKey1 < pKey)
						{
							subject1=attributes1.get(2);
							object1=attributes1.get(3);
							action1=attributes1.get(4);
							
							//compare subject
							if(subject1.equals(object) || subject1.contains(object) || object.contains(subject1))
							{
									if(CommDictionary.relatedWords(action, action1) || action.equals(action1))
									{
										for(int j=5; j <attributes.size();j++)
										{
											for(int k=5; k <attributes1.size();k++)
											{
												if(attributes.get(j).equals(attributes1.get(k)) || relatedPronouns(attributes.get(j), attributes1.get(k)))
												{
														signalData[15]="delegate";
														signalData[10]="yes";
														signalData[8]="yes";
														
												}
											}
										}
									}
							}
						}
					}
			}
					return signalData;
				}
		private static String[] identifyCommitmentCancel(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, Integer pKey, String[] taskData, int initialTaskID, int commitmentIDinitial)
		{
			String subject=new String();
			String object = new String();
			String action = new String();
			
			
			String debtor=new String();
			String creditor= new String();
			String action1=new String();
			String[] signalData= new String[16];
			//initialize signals
			//[0]-modal verb signal
			//[1]-action verb signal
			//[2]-type of subject
			//[3]-Negative verb signal
			//[4]-present tense verb signal,
			//[5]-past tense verb signal, 
			//[6]-debtor signal,
			//[7]-creditor signal,
			//[8]-delegation Signal
			//[9]-deadline Signal
			//[10]-createCommitmentPresent
			//[11]-Bigram Modal Second
			//[12]-Bigram First Modal
			//[13]-Bigram Please Action
			//[14]-Question Mark Present
			//[15]-class
			for(int i=0; i<signalData.length;i++)
			{
				signalData[i]=taskData[i];
			}
			int dischargeCount=0;
			boolean relatedPronounsCheck=false;
			boolean verbValidityCheck=false;

			/*----------------------------------OPEN AN INSTANCE OF WORDNET DICTIONARY-----------------------------------------------*/
			//open connections to wordnet dictionary
			//wordnet dictionary will help to relate words such as verbs
			//CommDictionary.openDictionaryConnection();//moved to main

			/*--------------------------------------------------IDENTIFY COMMITMENTS-------------------------------------------------*/

			//start iterating through all tasks
			for(int w=initialTaskID+1; w<taskID+1;w++)
			{
				ArrayList<String> taskAttrib=new ArrayList<String>();
				taskAttrib=taskStore.get(w);
				Integer sentKey=Integer.parseInt(taskAttrib.get(0));
				//check for actors
				if(sentKey.intValue() == pKey)
				{
					action = taskAttrib.get(3);
					subject = taskAttrib.get(1);
					object = taskAttrib.get(2);
					int negativeVerb=0;
					for (Iterator iterator = tdl.iterator(); iterator.hasNext();)
					{
						TypedDependency typedDependency = (TypedDependency) iterator.next();
						if(typedDependency.gov().label().value().equals(action))
						{
							if(typedDependency.dep().label().value().equals("not"))
							{
								negativeVerb=1;
							}
						}
						else if(typedDependency.dep().label().value().equals(action))
						{
							if(typedDependency.gov().label().value().equals("not"))
							{
								negativeVerb=1;
							}
						}
					}
					/*------------------STEP 3: CHECK IF ACTION String and the DEBTOR matches ACTIONS and DEBTOR in COMMITMENT STORE-----------------------------------*/
					for(int i=3; i<taskAttrib.size();i++)
					{
						for(int j=0; j<taggedYield.size();j++)
						{
							if(taggedYield.get(j).word().equals(taskAttrib.get(i)))
							{
								if(taggedYield.get(j).tag().equals("VBD")|| taggedYield.get(j).tag().equals("VB")||
										taggedYield.get(j).tag().equals("VBN")||
										taggedYield.get(j).tag().equals("VBG") || taggedYield.get(j).tag().equals("VBP"))
								{
									//collect signal for verb that occurred in past or still running
									signalData[1]="yes";
									signalData[5]="yes";
									 


										ArrayList<String> commAttrib = new ArrayList<String>();
										int sameActionVerb=0;
										int sameNouns=0;
										
										//we start from l=2 because 0-sentence ID and 1-task ID
										for(int l=1; l<commitmentIDinitial+1; l++)
										{
											commAttrib=commitmentStore.get(l);
											if(commAttrib != null)
											{
												debtor=commAttrib.get(2);
												creditor=commAttrib.get(3);
												action1=commAttrib.get(4);
												
												if(debtor.equals(subject) || debtor.contains(subject) || subject.contains(debtor))
												{
													if(creditor.equals(object) || creditor.contains(object) || object.contains(creditor))
													{
														if(action.equals(action1) || CommDictionary.relatedWords(action, action1))
														{
															sameActionVerb=1;
															for(int p=5;p<taskAttrib.size();p++)
															{
																for(int q=5;q<commAttrib.size();q++)
																{
																	if(taskAttrib.get(p).equals(commAttrib.get(q)) || relatedPronouns(taskAttrib.get(p), commAttrib.get(q)) )
																	{
																		if(negativeVerb == 1)
																		{
																			sameNouns=1;
																			signalData[6]="yes";
																			signalData[7]="yes";
																			signalData[10]="yes";
																			signalData[3]="yes";
																			signalData[15]="cancel";
																			break;
																		}
																		
																	}
																}
															}
															if(sameActionVerb == 1 && sameNouns== 0 && negativeVerb == 1)
															{
																signalData[6]="yes";
																signalData[7]="yes";
																signalData[10]="yes";
																signalData[3]="yes";
																signalData[15]="cancel";
																break;
															}
														}
													}
												}
											}
										}
								}
							}
						}
					}
				}
			}
			return signalData;

		}
		public static void generateData(ArrayList<TaggedWord> messageLineTagged, Collection tdl, Integer key, String[] signalData)

		{
			int createCount=0;//keep the count of create. Delegate is included in the create.
			int dischargeCount=0;//keep the count of discharge.
			int delegateCount=0;//keep the count of discharge.
			
			
			ArrayList<String> subData = new ArrayList<String>();
			String senSubData = new String();
			senSubData = sentenceArrays.get(key);
			subData.add(0,"\""+senSubData+"\"");
			subData.add(1,signalData[0]);//modal verb
			subData.add(2,signalData[1]);//action verb
			subData.add(3,signalData[2]);//personal pronoun
			subData.add(4,signalData[3]);//negative
			subData.add(5,signalData[4]);//present tense
			subData.add(6,signalData[5]);//past tense
			subData.add(7,signalData[6]);//debtor
			subData.add(8,signalData[7]);//creditor
			subData.add(9,signalData[8]);//delegation
			subData.add(10,signalData[9]);//deadline
			subData.add(11,signalData[10]);//create exist
			subData.add(12,signalData[11]);//bigram modal second
			subData.add(13,signalData[12]);//bigram first modal
			subData.add(14,signalData[13]);//bigram please action
			subData.add(15,signalData[14]);//question mark
			subData.add(16,signalData[15]);//class
			
			dataID++;
			data.put(dataID, subData);
			
			}
		/**
		 * 
		 * @param tdl
		 * @param action
		 * @param debtor
		 * @return
		 */
		private static boolean checkConditionalVerbValidity(List<TypedDependency> tdl, String action, String debtor) {
			// TODO Auto-generated method stub
			for (Iterator iterator = tdl.iterator(); iterator.hasNext();)
			{
				TypedDependency typedDependency = (TypedDependency) iterator.next();
				if(typedDependency.gov().label().value().equals(action))
				{
					if(!typedDependency.dep().label().value().equals("when") || !typedDependency.dep().label().value().equals("if") ||!typedDependency.dep().label().value().equals("but"))
					{
						if(typedDependency.dep().label().value().equals(debtor))
						{
							return true;
						}
					}
				}
			}
			return false;
		}
		/**
		 * 
		 * @param taggedYield
		 * @param word
		 * @return
		 */
		
		public static boolean checkValidVerb(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, String word)
		{
			String posTag=CommDictionary.getPOSFromDictionary(word);
			int verbFlag=0;
			int stopFlag=0;
			for(int i=0;i<taggedYield.size();i++)
			{
				if(taggedYield.get(i).word().equals(word) && 
						(taggedYield.get(i).tag().equals("VB") ||
								taggedYield.get(i).tag().equals("VBG") ||
								taggedYield.get(i).tag().equals("VBD") ||
								taggedYield.get(i).tag().equals("VBN") ||
								taggedYield.get(i).tag().equals("VBP")
								))

				{
					if(posTag != null)
					{
						if(posTag.equals("VERB"))
						{
							verbFlag=1;
							//return true;
						}
					}
				}

			}
			for (Iterator iterator = tdl.iterator(); iterator.hasNext();)
			{
				//get all typed dependencies for each message
				TypedDependency typedDependency = (TypedDependency) iterator.next();

				if(typedDependency.gov().label().value().equals(word))
				{
					if(typedDependency.dep().label().value().equals("if") || 
					    typedDependency.dep().label().value().equals("If")  ||
					    typedDependency.dep().label().value().equals("when") )
					{
						stopFlag=1;
						if(verbFlag == 1 && stopFlag==1)
						{
							return false;
						}
					}
				}
			}
			if(verbFlag == 1 && stopFlag==0)
			{
				return true;
			}
			return false;
		}
		/**
		 * 
		 * @param taggedYield
		 * @param tdl
		 * @param attributes
		 * @param action
		 */
		private static void findActionString(ArrayList<TaggedWord> taggedYield, List<TypedDependency> tdl, ArrayList<String> attributes, String action) 
		{
			for (Iterator iterator = tdl.iterator(); iterator.hasNext();) {
				TypedDependency typedDependency = (TypedDependency) iterator.next();
				if(typedDependency.gov().label().value().equals(action))
				{
					for(int i=0; i<taggedYield.size();i++)
					{
						if(taggedYield.get(i).word().equals(typedDependency.dep().label().value()))
						{
							//collect all verbs which are either present or running
							//collect all nouns may be pronouns
							//consider all forms of verbs along with singular and plural nouns
							if(taggedYield.get(i).tag().equals("VB")  ||
									taggedYield.get(i).tag().equals("VBG") ||
									taggedYield.get(i).tag().equals("VBD") ||
									taggedYield.get(i).tag().equals("VBN") ||
									taggedYield.get(i).tag().equals("VBP") ||
									taggedYield.get(i).tag().equals("NN")  ||
									taggedYield.get(i).tag().equals("NNS") ||
									taggedYield.get(i).tag().equals("NNP") ||
									taggedYield.get(i).tag().equals("NNPS"))
							{
								if(!attributes.contains(typedDependency.dep().label().value()))
								{
									attributes.add(typedDependency.dep().label().value());
									findActionString(taggedYield, tdl, attributes, typedDependency.dep().label().value());
								}
							}
						}
					}
				}
			}
		}
		/**
		 * 
		 * @param subject
		 * @return
		 */
		public static boolean checkSubjectValidity(String subject)
		{
			ArrayList<String> temp=new ArrayList<String>();
			for(int i=1; i<nerID+1; i++)
			{
				temp= nerDictionary.get(i);
				if(temp!=null)
				{
					if(temp.get(0).equals(subject))
					{
						if(temp.get(1).equals("NNP")||
								temp.get(1).equals("NNPS") ||
								temp.get(1).equals("PRP")||
								temp.get(2).equals("ORGANIZATION")||
								temp.get(2).equals("PERSON")||
								temp.get(0).contains("/")
								)
						{
							return  true;
						}
					}
				}
			}
			return false;
		}
		public static boolean checkDeadlineIndicatingWords(String word)
		{
			ArrayList<String> temp=new ArrayList<String>();
			for(int i=1; i<nerID+1; i++)
			{
				temp= nerDictionary.get(i);
				if(temp!=null)
				{
					if(temp.get(0).equals(word))
					{
						if(temp.get(2).equals("DATE") && 
								temp.get(2).equals("TIME")
								)
						{
							return  true;
						}
					}
				}
			}
			return false;
		}
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
						CommTrustParser.nerID++;
						CommTrustParser.nerDictionary.put(CommTrustParser.nerID, info);
					}
				}

			}

			CommTrustParser.graph = document.get(CorefChainAnnotation.class);
		}
		public static  boolean relatedPronouns(String noun1, String noun2)
		{
			String temp=new String();
			String temp1=new String();
			boolean noun1Check=false;
			boolean noun2Check=false;
			boolean validNoun1=false;
			boolean validNoun2=false;

			if(noun1.equals("")||noun2.equals(""))
			{
				return false;
			}

			if(graph!=null)
			{
				Iterator<Integer> itr = graph.keySet().iterator();
				while (itr.hasNext()) {

					Integer key = Integer.parseInt(itr.next().toString());


					//String value = graph.get(key).toString();
					//String value = graph.get(key).getCorefMentions().toString();
					//39 ["Batch issue" in sentence 2, "the issue" in sentence 12]

					List<CorefMention> value=graph.get(key).getCorefMentions();
					noun1Check=false;
					noun2Check=false;
					for(int i=0; i<value.size();i++)
					{
						temp=value.get(i).mentionSpan.toLowerCase();
						if(temp.contains(noun1) && temp.contains(noun2) && 
								(!temp.contains(noun1+" and "+noun2)|| 
										(!temp.contains(noun1+" - "+noun2)) || 
										(!temp.contains(noun1+" , "+noun2)) ||
										(!temp.contains(noun1+" : "+noun2)) ||
										(!temp.contains(noun1+":"+noun2)) ||
										(!temp.contains(noun1+", "+noun2)) ||
										(!temp.contains(noun1+" :"+noun2))))
						{
							noun1Check=true;
							noun2Check=true;
						}
						else if(temp.contains(noun1))
						{
							noun1Check=true;
							for(int j=i+1; j<value.size();j++)
							{

								if(j<value.size())
								{
									temp1=value.get(j).mentionSpan.toLowerCase();
									if(temp1.contains(noun2))
									{
										noun2Check=true;
									}
								}
							}
						}
						else if(temp.contains(noun2))
						{
							noun2Check=true;
							for(int j=i+1; j<value.size();j++)
							{

								if(j<value.size())
								{
									temp1=value.get(j).mentionSpan.toLowerCase();
									if(temp1.contains(noun2))
									{
										noun2Check=true;
									}
								}
							}
						}
					}
					if(noun1Check==true && noun2Check==true)
					{
						System.out.println(noun1+"-------------"+noun2);
						return true;
					}
				}
			}
			return false;
		}
	
}

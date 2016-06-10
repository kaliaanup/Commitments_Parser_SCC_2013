package comm.trust.main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;

import comm.trust.config.ProjectConfig;
import comm.trust.libs.CommDictionary;
import comm.trust.libs.CommTrustParser;
import comm.trust.libs.DatabaseConn;
import comm.trust.libs.CommTrustFeatures;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class CommTrustMain {

	//store all email bodies according to the subject id
	public static Hashtable<Integer, ArrayList<String>> emailBodies=new Hashtable<Integer, ArrayList<String>>();
	public static int emailBodiesID=0;
	public static ArrayList<String> splitSentences(String strLine)
	{
		ArrayList<String> sentences=new ArrayList<String>();

		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US); 
		iterator.setText(strLine); int start = iterator.first();
		String temp = new String();
		for (int end = iterator.next();end != BreakIterator.DONE; start = end, end = iterator.next())
		{
			//ArrayList<String> sentence=new ArrayList<String>();
			//original sentence
			temp = strLine.substring(start,end);
			//remove quotes from sentences

			temp = temp.replace('"', '\"');
			temp = temp.replace("\"", "");
			sentences.add(temp);
		} 
		return sentences;
	}
	public static void initializeParams()
	{
		CommTrustParser.sentenceArrays.clear();
		CommTrustParser.data.clear();
		CommTrustParser.commitmentStore.clear();
		CommTrustParser.taskStore.clear();
		if(CommTrustParser.graph != null)
		{
			CommTrustParser.graph.clear();
		}
		CommTrustParser.nerDictionary.clear();
		//CommTrustParser.nounDictionary.clear();
		CommTrustParser.delegationCount =0;
		
		CommTrustParser.taskID=0;
		CommTrustParser.commitmentID=0;
		CommTrustParser.sentenceID=0;
		//CommTrustParser.typedSentenceID=0;
		//CommTrustParser.taggedSentenceID=0;
		CommTrustParser.dataID=0;
		CommTrustParser.nerID=0;
		//CommTrustParser.nounID=0;
	}
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
				//System.out.println(finalName);
			}
		}
		else
		{
			finalName=extractNameCorrectly(name);
		}
		
		return finalName;
	}
	public static void main(String args[])
	{
		String sql="";
		ResultSet rs=null;
		Statement stmt=null;
		int totalSubjects=0;
		String subject=new String("");
		ArrayList<String> sentences=new ArrayList<String>();
		String email=new String();
		String sender=new String();
		String receiver=new String();
		FileWriter out=null;


		/*-----------------OPEN MYSQL DB CONNECTION-----------------------*/
		//DatabaseConn.openMySqlConnection();

		LexicalizedParser lp = LexicalizedParser.loadModel(ProjectConfig.grammer);
		CommTrustParser commParserObj=new CommTrustParser();
		/*-----------------OPEN DICTIONARY CONNECTION-----------------------*/
		CommDictionary.openDictionaryConnection();

		CommTrustParser.storeActionVerbs();
		try{

			//delete the old file
			File f=new File(ProjectConfig.featureFileName);
			if(f.exists() && f.isFile()){
				f.delete();
			}
			// recreate the file
			out = new FileWriter(ProjectConfig.featureFileName, true);
			out.append("@RELATION CommOps");
			/*out.write("\r\n");
			out.append("@ATTRIBUTE text string");*/
			out.write("\r\n");
			out.append("@ATTRIBUTE ModalVerbSignal {shall, will, may, might, can, must, could, would, should, requested, please, none}");
			out.write("\r\n");
			out.append("@ATTRIBUTE ActionVerbSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE TypeOfSubject {first, second, third, none}");
			out.write("\r\n");
			out.append("@ATTRIBUTE NegativeVerbSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE presentTenseVerbSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE pastTenseVerbSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE debtorPresent {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE creditorPresent {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE delegationSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE deadlineSignal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE createCommitmentPresent {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE bigramModalSecond {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE bigramFirstModal {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE bigramPleaseAction {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE questionMarkPresent {yes, no}");
			out.write("\r\n");
			out.append("@ATTRIBUTE class {create, icreate, discharge, cancel, delegate, none}");
			out.write("\r\n");
			out.write("\r\n");
			out.append("@DATA");
			out.write("\r\n");
			//Add Data
			//genDataFile(out);
			// out.close();
			//System.out.println("String is appended.");
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//totalSubjects=1;
		//Extract mail for each subject
		//System.out.println("------------------Started Collecting the Data-----------------");
		//File file=new File(ProjectConfig.processeddataset);
		//if(file.isDirectory())
		//{
			//File[] listOfFiles = file.listFiles();
			//for(int i=0; i<listOfFiles.length;i++)
			//{

				//Extract subject
				//subject=listOfFiles[i].getName();
				//clear old data
				FileInputStream fstream=null;
				try {
					fstream = new FileInputStream(ProjectConfig.processeddataset);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				//emailBodies.clear();
				initializeParams();
				sentences.clear();
				CommTrustParser.emailBody.clear();
				int senderFlag=0;
				int receiverFlag=0;
				int emailFlag=0;
				int sendPosn=0;
				int rcvPosn=0;
				email="";
				try{
					//Load the file and parse it line by line

					String strLine;
					//Parse each line to collect important information
					while ((strLine = br.readLine()) != null){
						if(!strLine.equals(""))
						{
						if(strLine.charAt(0) == '%' & strLine.contains("SENDER"))
						{
							sendPosn=strLine.indexOf("SENDER");
							rcvPosn=strLine.indexOf("RECEIVER");
							
							sender = strLine.substring(sendPosn+8, rcvPosn-2);
							sender = extractNameCorrectly(sender);
							senderFlag = 1;
							receiver = strLine.substring(rcvPosn+10, strLine.length());
							receiverFlag = 1;
							receiver = extractReceiverNamesCorrectly(receiver);
							
							
						}
						if(strLine.equals("%------------------------------------------------------------------------------------"))
						{
							emailFlag=0;
						}
						if(strLine.charAt(0) != '%')
						{
							if(email.endsWith("."))
							{
								email = email +" " + strLine;
							}
							else{
								email = email +". " + strLine;	
							}
						}
						if(strLine.contains("%------------------------------1"))
						{
							if(senderFlag == 1 & receiverFlag == 1)
							{
								emailFlag=1;
							}
						}
								
						/*if(strLine.length()>8)
						{
							if(strLine.substring(0, 8).equals("subject:"))
							{
								sender=strLine.substring(8, strLine.length());
								senderFlag=1;
							}
						}
						//Extract TO:
						if(strLine.length()>9)
						{
							if(strLine.substring(0, 9).equals("receiver:"))
							{
								receiver=strLine.substring(9, strLine.length());
								receiverFlag=1;
							}
						}
						if(strLine.length()>6)
						{
							if(strLine.substring(0, 6).equals("email:"))
							{
								email=strLine.substring(6, strLine.length());
								emailFlag=1;
							}
						}*/
						//Add to the ArrayList
						if(senderFlag == 1 && receiverFlag == 1 && emailFlag == 1)
						{
							CommTrustParser.emailBody.add(sender);
							CommTrustParser.emailBody.add(receiver);
							CommTrustParser.emailBody.add(email);

							commParserObj.parseEmailBodies(lp);
							CommTrustParser.emailBody.clear();
							senderFlag=0;
							receiverFlag=0;
							emailFlag=0;
							email="";
						}

					}
					}
					CommTrustFeatures.genDataFile(out);
					in.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}




				//parse all mails related to each subject
			//}

			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//}


		/*-----------------CLOSE DICTIONARY CONNECTION-----------------------*/
		CommDictionary.closeDictionaryConnection();
		/*-------------------CLOSE MYSQL DB CONNECTION--------------------*/
		//DatabaseConn.closeMySqlConnection();



	}
}

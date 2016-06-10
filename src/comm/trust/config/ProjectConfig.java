package comm.trust.config;

public class ProjectConfig {

	public static String mySQLUsername="anup";
	public static String mySQLPassword="123321";
	public static String mySQLDBName="commtrustdb";
	public static String mySQLDBServiceName="yangtze.csc.ncsu.edu";
	public static String dataset="C:/CommTrustProject/Dataset/sent_items";
	public static String processedDataset="C:/CommTrustProject/ProcessedData";
	
	
	
	//path to stanford NLP english grammer
	public static String grammer="C:/CommTrustProject/libs/englishPCFG.ser.gz";
	//public static String grammer="";
	//path to generated feature file for Weka
	public static String featureFileName="C:/CommTrustProject/FeatureFile/Sample.arff";
	//path to wordnet path
	public static String wordnetpath="F:/Desktop/EclipseGMF/workspace1/EmailParser/DictionaryPath/JWNLproperties.xml";
	//path to language profiles for detecting languages in the sentences
	
	public static String processeddataset="C:/CommTrustProject/Data/testing.txt";
	public static String actionVerbList="C:/CommTrustProject/ClassFeatures/ActionVerbList.txt";
	
	//public static String subjectFileName="C:/CommTrustProject/FinalData/Subjects.txt";
}

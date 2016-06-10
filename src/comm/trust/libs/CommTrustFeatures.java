package comm.trust.libs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import comm.trust.config.ProjectConfig;

public class CommTrustFeatures {

	
	public static void genFeatureFile()
	{
			try{
				//delete the old file
				File f=new File(ProjectConfig.featureFileName);
				if(f.exists() && f.isFile()){
				f.delete();
				}
				// recreate the file
				FileWriter out = new FileWriter(ProjectConfig.featureFileName, true);
					
			    out.append("@RELATION CommOps");
			    out.write("\r\n");
			    out.append("@ATTRIBUTE text string");
			    out.write("\r\n");
			    out.append("@ATTRIBUTE ModalVerbSignal {will, may, can, must, could, would, should, requested, please, none}");
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
			    out.append("@ATTRIBUTE createCommitmentPresent {yes, no}");
			    out.write("\r\n");
			    out.append("@ATTRIBUTE class {create, discharge, delegate, none}");
			    out.write("\r\n");
			    out.write("\r\n");
			    out.append("@DATA");
			    out.write("\r\n");
			    //Add Data
			    genDataFile(out);
			    out.close();
			    System.out.println("String is appended.");
		    } 
			catch (IOException e)
			{
				
			}
	}
	/*
	 * 
	 */
	public static void genDataFile(FileWriter out)
	{
		ArrayList<String> subData = new ArrayList<String>();
		//Integer i=new Integer(0);
		Iterator dataItr = CommTrustParser.data.keySet().iterator();
		//while(dataItr.hasNext())
		for(int i=1;i<CommTrustParser.dataID+1;i++)
		{
			//i = Integer.parseInt(dataItr.next().toString());
			subData=CommTrustParser.data.get(i);
			if(subData!=null)
			{
			for(int j=0; j<subData.size();j++)
			{
				try {
					out.append(subData.get(j).toString());
					if(j!=subData.size()-1)
					{
						out.append(",");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			}
			try {
				if(i<CommTrustParser.dataID+1)
				{
					out.write("\r\n");
				}
				else
				{
					out.write("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
			

}


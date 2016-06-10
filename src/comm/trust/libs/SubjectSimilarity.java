package comm.trust.libs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class SubjectSimilarity {

	public static ArrayList<String> subjects=new ArrayList<String>();
	public static ArrayList<String> subjectTemp=new ArrayList<String>();
	
	public static Hashtable<Integer, ArrayList<String>> subjectCluster=new Hashtable<Integer, ArrayList<String>>();
	public static int clusterId=0;
	public static void calculateSimilarityScore(String subject1)
	{
		
		ArrayList<String> subject1Array=new ArrayList<String>();
		subject1Array=extractWords(subject1);
		ArrayList<String> subject2Array=new ArrayList<String>();
		ArrayList<String> temp=new ArrayList<String>();
		if(subjectTemp.contains(subject1))
		{
			return;
		}
		else
		{
			temp.add(subject1);
			//subjectTemp.add(subject1);
		}
		clusterId++;
		for(int i=0;i<subjects.size();i++)
		{
			
			/*if(!subjectTemp.contains(subjects.get(i)))
			{*/
				if(!subject1.equals(subjects.get(i)))
				{
					subject2Array=extractWords(subjects.get(i));
					int count=0;
					for(int j=0; j<subject1Array.size();j++)
					{
						for(int k=0; k<subject2Array.size();k++)
						{
							if(subject1Array.get(j).equals(subject2Array.get(k)))
							{
								count++;
							}
						}
					}
					float per= (count/subject1Array.size())*100;
					if(per> 20)
					{
						
						if(subjectCluster.get(clusterId) == null)
						{
							temp.add(subjects.get(i));
							//subjectTemp.add(subjects.get(i));
							subjectCluster.put(clusterId,temp);
						}
						else
						{
							temp.add(subjects.get(i));
							//subjectTemp.add(subjects.get(i));
							subjectCluster.remove(clusterId);
							subjectCluster.put(clusterId,temp);
						}
					}
					
					
				}
			//}
		}
	}
	public static ArrayList<String> extractWords(String subject)
	{
		ArrayList<String> subject1Array=new ArrayList<String>();
		
		String delims = "[ ]+";
		String[] tokens = subject.split(delims);
		
		for(int i=0; i<tokens.length;i++)
		{
			subject1Array.add(tokens[i]);
		}
		
		return subject1Array;
	}
	
	public static void main(String args[])
	{
		String sql="";
		ResultSet rs=null;
		Statement stmt=null;
		int totalSubjects=0;
		String subject=new String("");
		
		DatabaseConn.openMySqlConnection();
		
		sql="SELECT count(subjectid) from subjecttbl";
		//count the number of subjects
		try 
		{
			if(DatabaseConn.conn!=null)
			{
				stmt=DatabaseConn.conn.createStatement();
				rs=stmt.executeQuery(sql);
				while(rs.next())
				{
					totalSubjects=rs.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		
		for(int i=1;i<totalSubjects+1;i++)
		{
			sql="SELECT subject from subjecttbl WHERE subjectid='"+i+"'";
			try 
			{
				if(DatabaseConn.conn!=null)
				{
					stmt=DatabaseConn.conn.createStatement();
					rs=stmt.executeQuery(sql);
					while(rs.next())
					{
						subject=rs.getString(1);
						subject = subject.replaceAll("RE:", "");
						subject = subject.replaceAll("FW:", "");
						subjects.add(subject);
					}
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		DatabaseConn.closeMySqlConnection();
		for(int i=0;i<subjects.size();i++)
		{
			calculateSimilarityScore(subjects.get(i));
			//System.out.println(subjects.get(i));
			
		}
		Iterator itr=subjectCluster.keySet().iterator();
		ArrayList<String> cluster=new ArrayList<String>();
		while(itr.hasNext())
		{
			
			Integer key=Integer.parseInt(itr.next().toString());
			cluster=subjectCluster.get(key);
			
			System.out.println("---------------------------------------------");
			for(int i=0;i<cluster.size();i++ )
			{
				System.out.println(cluster.get(i));
			}
		}
		
	}
	
}

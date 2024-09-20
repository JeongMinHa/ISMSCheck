import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class getOracleDBData {
	
	static HashMap<String, String> mSrcQueryMap = null;
	
	static HashMap<String, String> mRdsLists = null;

	static Set sQuerySet = null;
	
	static Set sRdsSet = null;
	
	static String jdbcClass = null;
	
	static String sBasePath = "";
	
	public static void main(String[] args) {
		
		FileToInfo(args[0]);
        
		/*sQuerySet = mSrcQueryMap.entrySet();
		
		for (Object sqlobj : sQuerySet) {
			Map.Entry sqlEntry = (Map.Entry) sqlobj;
			
			System.out.println(
				"SQL Key : " + sqlEntry.getKey().toString().trim()  
				+ ", Value : " + sqlEntry.getValue().toString().trim());
		}*/
		
		List<String> sqlList = new ArrayList<>(mSrcQueryMap.keySet());
		
		sqlList.sort((s1, s2) -> s1.compareTo(s2));
        
		sRdsSet = mRdsLists.entrySet();
		
		for (Object dbobj : sRdsSet) {
			
			Map.Entry rdsEntry = (Map.Entry) dbobj;
			
			System.out.println( // Log Rds List
				"RDS Key : " + rdsEntry.getKey().toString().trim() 
				+ ", Value : " + rdsEntry.getValue().toString().trim());
	        
			File fRsltFile = new File(sBasePath + File.separator + "result" + File.separator + "oracle" + File.separator + rdsEntry.getKey().toString().trim() + ".txt");
			
			BufferedWriter bw = null;
			
			Connection conn = null;
			
			PreparedStatement pstmt = null;
			PreparedStatement arch_pstmt = null;
			
			String[] conn_info = rdsEntry.getValue().toString().trim().split(",");
			
			try {
				
				if(!fRsltFile.exists()) {
					
					fRsltFile.createNewFile();
					
					bw = new BufferedWriter(new FileWriter(fRsltFile, true));
					
					Properties conn_props = new Properties();

					conn_props.put("user", conn_info[2] );
					conn_props.put("password", conn_info[3] );
					conn_props.put("internal_logon", "sysdba");
					
					System.out.println("Identifier : " + conn_info[0]);
					System.out.println("Endpoint : " + conn_info[1]);
					System.out.println("User : " + conn_info[2]);
					System.out.println("Password : " + conn_info[3]);
					
					Class.forName(jdbcClass);
					
					if(!conn_info[2].trim().equals("sys")) {
						conn = DriverManager.getConnection(conn_info[1].trim(), conn_info[2].trim(), conn_info[3].trim());
					}else {
						conn = DriverManager.getConnection(conn_info[1].trim(), conn_props);
					}
					
					ResultSet rslt = null;
					
					for (String sql : sqlList) { // Log Sql List
			            // System.out.println("key: " + sql + ", value: " + mSrcQueryMap.get(sql));
						
						bw.write(mSrcQueryMap.get(sql).trim());
						bw.newLine();
						bw.flush();
						
						bw.write("-->");
						bw.newLine();
						bw.flush();
						
						ArrayList<String> column_list = null;
						
						try {
							
							pstmt = conn.prepareStatement(mSrcQueryMap.get(sql).trim());
							rslt = pstmt.executeQuery();
							
							if(!mSrcQueryMap.get(sql).trim().contains(" from ")) {
								
								String sColumn = mSrcQueryMap.get(sql).trim().substring(mSrcQueryMap.get(sql).trim().indexOf(" "))
										.replace("()", "").trim();
								
								while(rslt.next()) {

									bw.write(rslt.getString(sColumn));
									bw.newLine();
									bw.flush();
								}
							} else {
								
								column_list = new ArrayList<>();
								
								String sTable = mSrcQueryMap.get(sql).trim().substring(
										mSrcQueryMap.get(sql).trim().indexOf(" from ") + 6, 
										mSrcQueryMap.get(sql).trim().length());
								
								if(sTable.contains(" ")) {
									sTable = sTable.substring(0, sTable.indexOf(" "));									
								}
								
								if(sTable.contains(".")) {
							    	sTable = sTable.substring(sTable.indexOf(".")+1, sTable.length());
							    }
							    
							    /*
								String arch_sql = "select column_name from information_schema.columns where table_name = ? order by ordinal_position";
								
								arch_pstmt = conn.prepareStatement(arch_sql);
								arch_pstmt.setString(1, sTable);
								ResultSet arch_result = arch_pstmt.executeQuery();
								
								column_list = new ArrayList<>();
								
								while(arch_result.next()) {
									column_list.add(arch_result.getString("column_name"));
								}
								*/
								
								boolean sColumnYn = false;
								
								if(mSrcQueryMap.get(sql).trim().contains("*")) { // columns clause is * (asterisk)
									sColumnYn = true;
								}
								
								if(sColumnYn) {
									
									// String arch_sql = "select column_name from information_schema.columns where table_name = ? order by ordinal_position";
									String arch_sql = "select column_name from all_tab_columns where table_name = upper( ? )";
									
									arch_pstmt = conn.prepareStatement(arch_sql);
									arch_pstmt.setString(1, sTable);
									ResultSet arch_result = arch_pstmt.executeQuery();
									
									while(arch_result.next()) {
										column_list.add(arch_result.getString("column_name").trim());
									}
									
								} else {
									
									String[] colist = mSrcQueryMap.get(sql).trim().substring(mSrcQueryMap.get(sql).trim().indexOf("select ") + 7, mSrcQueryMap.get(sql).trim().indexOf("from ")).split(",");
									
									for(int i = 0; i < colist.length; i++) {
										
										column_list.add(colist[i].substring(colist[i].indexOf(" as ") + 4, colist[i].length()).trim());
									}
									
								}
								
								String sColvalue = "";
								
								while(rslt.next()) {
									
									for(int i = 0; i < column_list.size(); i++) {
										
										//System.out.println(column_list.get(i));
										
										if (rslt.getString(column_list.get(i)) == null || rslt.getString(column_list.get(i)).trim().equals("")) {
											sColvalue = " ";
											bw.write(sColvalue + "\t");
											bw.flush();
										} else {
											bw.write(rslt.getString(column_list.get(i)) + "\t");
											bw.flush();
										}
									}
									
									bw.newLine();
									bw.flush();
								}
							}
							
							bw.newLine();
							bw.flush();
							
						}catch(org.postgresql.util.PSQLException pe) {
							//pe.printStackTrace();
							try {
								bw.write("Error, Permission Denied!!!");
								bw.newLine();
								bw.newLine();
								bw.flush();
								
								continue;
							}catch(Exception e) {
								e.printStackTrace();
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
						
						if(column_list != null) {
							column_list.clear();
						}
			        }
				} else {
					System.out.println("Do Delete, Exist File!!!!!");
				}
				
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				try {
					if (bw != null) {
						bw.flush();
						bw.close();
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void FileToInfo(String sFile) {

		String sSQLCheckFile = "";
		String sDBListFile = "";
		
		
		Properties prop = new Properties();
		InputStream is = null;

		BufferedReader brd = null;
		
		try {
			is = new FileInputStream(sFile);
			prop.load(is);

			sBasePath = prop.getProperty("BASE_PATH");
			sSQLCheckFile = prop.getProperty("SQL_FILE"); 
			sDBListFile = prop.getProperty("DB_LIST");
			jdbcClass =  prop.getProperty("JDBC_CLASS");
			
			Properties pRds = new Properties();
			
			brd = new BufferedReader(new FileReader(File.separator + sDBListFile));
			
			String line = brd.readLine();
			
			while (line != null) {
				
				// pRds.setProperty(line.substring(0, line.indexOf(".")), line);
				pRds.setProperty(line.substring(0, line.indexOf(",")), line); // oracle
				
				line = brd.readLine();
			}
			
			mRdsLists = new HashMap<String, String>((Map)pRds); 
			
		} catch (Exception e) {
			e.toString();
		} 

		Document document = null;
		Properties pJobSrcQuery = null;

		try {
			document = new SAXBuilder().build(new FileInputStream(sSQLCheckFile));
			pJobSrcQuery = new Properties();

			Element element = document.getRootElement();
			List childElementList = element.getChildren();

			for (int i = 0; i < childElementList.size(); i++) {
				pJobSrcQuery.setProperty(
					((Element) childElementList.get(i)).getName(),
					((Element) childElementList.get(i)).getValue()
				);
			}

			mSrcQueryMap = new HashMap<String, String>((Map)pJobSrcQuery); //Set SQL
			
		} catch (Exception e) {
			e.toString();
		}
	}

}

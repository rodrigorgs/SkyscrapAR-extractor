
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

public class TestingSVNKit {
	
	public static void main(String args[]) throws IOException{
		
		//Field added by Bruno in order to keep a mapping between components and their number of changes
		//Maybe it can be turned into an specific class if we have to deal with more information besides change counting
		HashMap<String, Integer> components = new HashMap<String, Integer>();
			
		 DAVRepositoryFactory.setup( );
			//We can build an interface for choosing the url
	        String url = "http://svn.svnkit.com/repos/svnkit";
	        //String url = "http://mybatis.googlecode.com/svn";
			
			//We can also open an interface for typeing username, passwd, and the revision interval
	        String name = "anonymous";
	        String password = "anonymous";
					
	        long startRevision = 0;
	        long endRevision = -1; //HEAD (the latest) revision
			

	        SVNRepository repository = null;
	        try {
	            repository = SVNRepositoryFactory.create( SVNURL.parseURIEncoded( url ) );
	            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( name, password );
	            repository.setAuthenticationManager( authManager );

	            Collection logEntries = null;
	            
	            logEntries = repository.log( new String[] { "" } , null , startRevision , endRevision , true , true );

	            for ( Iterator entries = logEntries.iterator( ); entries.hasNext( ); ) {
	                SVNLogEntry logEntry = ( SVNLogEntry ) entries.next( );
	                System.out.println( "---------------------------------------------" );
	                System.out.println ("revision: " + logEntry.getRevision( ) );
	                System.out.println( "author: " + logEntry.getAuthor( ) );
	                System.out.println( "date: " + logEntry.getDate( ) );
	                System.out.println( "log message: " + logEntry.getMessage( ) );

	                if ( logEntry.getChangedPaths( ).size( ) > 0 ) {
	                    System.out.println( );
	                    System.out.println( "changed paths:" );
	                    Set changedPathsSet = logEntry.getChangedPaths( ).keySet( );

	                    for ( Iterator changedPaths = changedPathsSet.iterator( ); changedPaths.hasNext( ); ) {
	                        SVNLogEntryPath entryPath = ( SVNLogEntryPath ) logEntry.getChangedPaths( ).get( changedPaths.next( ) );
	                        System.out.println( " "
	                                + entryPath.getType( )
	                                + " "
	                                + entryPath.getPath( )
	                                + ( ( entryPath.getCopyPath( ) != null ) ? " (from "
	                                        + entryPath.getCopyPath( ) + " revision "
	                                        + entryPath.getCopyRevision( ) + ")" : "" ) );
	                        /* 
	                         * Bruno
	                         * TO DO: At this implementation, the code only considers the full path to identify a class.
	                         * So if a class moves to another package, this code identifies the class as a different one,
	                         * because the call to the method containsKey is used by passing the full path.
	                         * */
	                        Integer changeCount = new Integer(0);
	                        if (entryPath.getPath()!=null && isJavaSourcePath(entryPath.getPath())){
	                        	
	                        	String javaFile = getJavaFile(entryPath.getPath());
	                        	String packageFromPath = getPackageFromPath(entryPath.getPath());
	                        	
	                        	if(components.containsKey(packageFromPath+" "+javaFile))
	                        		changeCount = (Integer) components.get(packageFromPath+" "+javaFile);
	                        	
	                        	changeCount++;
                        		//components.put(entryPath.getPath(), changeCount);
	                        	components.put(packageFromPath+" "+javaFile, changeCount);
	                        }
	                    }
	                }
	            }
	        }catch(Exception e){e.printStackTrace();}
	        
            /* 
             * Bruno
             * */	        
	        System.out.println(" ==============================================================");
	        System.out.println(" ======================== CHANGE COUNT ========================");	
	        System.out.println(" ==============================================================");
	        
			File file = new File("FilesChange.txt");
			if(file.exists())
				file.delete();
			else
				file.createNewFile();
			FileWriter output = new FileWriter(file);
			
	        Set<String> compsSet = components.keySet();
	        for ( Iterator<String> compsSetIterator = compsSet.iterator( ); compsSetIterator.hasNext( ); ) {
	        	Integer changeCount = (Integer) components.get(compsSetIterator.next());
	        	String comp = (String)compsSetIterator.next();
	        	System.out.println(comp+": "+changeCount);
	        	
	        	String packageName = "";
	        	String javaFileName = "";
	        	
	        	String[] compSplit = comp.split(" ");
	        	packageName = compSplit[0];
	        	javaFileName = compSplit[1];
	        	        	
	        	output.write(packageName+"\t"+javaFileName+"\t"+changeCount+"\t"+"ConcreteClass"+"\n");
	        	//output.close();
	        }
	}
	
	//Bruno
	private static boolean isJavaSourcePath(String path)
	{
		if (path.contains("/src/") && path.contains(".java"))
			return true;
		return false;
	}
	
	//Bruno
	private static String getJavaFile(String path)
	{
		int index = path.indexOf("/src");
		if(index!= -1) //just confirming that we are handling a src path 
		{	
			StringTokenizer tokenizer = new StringTokenizer(path,"/");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.contains(".java"))
					return token;
			}
		}
		return "";		
	}
	
	//Bruno
	private static String getPackageFromPath(String path)
	{
		String srcPackagePath = "";
		String convertedPackagePath = "";
		int index = path.indexOf("/src");
		if(index!= -1) //just confirming that we are handling a src path 
		{	
			srcPackagePath = path.substring(index+4);
			StringTokenizer tokenizer = new StringTokenizer(srcPackagePath,"/");
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (!token.contains(".java"))
					convertedPackagePath += token+".";
			}
		}
		return convertedPackagePath;
	}
}

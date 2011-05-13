import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.nyu.cs.javagit.api.DotGit;
import edu.nyu.cs.javagit.api.JavaGitException;
import edu.nyu.cs.javagit.api.WorkingTree;
import edu.nyu.cs.javagit.api.commands.GitLogOptions;
import edu.nyu.cs.javagit.api.commands.GitLogResponse.CommitFile;


public class GitExtractor {
	static String REPO_PATH = "git-repos/";
	
	public static void incrementComponentCount(HashMap<ScmFile, Integer>components, String path) {
    	ScmFile scmFile = new ScmFile(path);
    	
    	Integer changeCount = new Integer(0);
    	if (components.containsKey(scmFile))
    		changeCount = (Integer) components.get(scmFile);
    	
    	changeCount++;
    	components.put(scmFile, changeCount);
	}
	
	// based on http://wiki.eclipse.org/JGit/User_Guide
	public static void main(String[] args) throws IOException, JavaGitException {
		HashMap<ScmFile, Integer> components = new HashMap<ScmFile, Integer>();

		File repositoryDirectory = new File(REPO_PATH);
		
		DotGit dotGit = DotGit.getInstance(repositoryDirectory);
//		WorkingTree tree = dotGit.getWorkingTree();
		GitLogOptions options = new GitLogOptions();
		options.setOptFileDetails(true);
		
		for (edu.nyu.cs.javagit.api.commands.GitLogResponse.Commit commit : dotGit.getLog(options)) {
			System.out.println("Commit with " + commit.getFilesChanged() + " changes.");
			
			List<CommitFile> files = commit.getFiles();
			if (files == null || files.size() == 0)
				continue;
			
//			String sha = commit.getSha();
			
			for (CommitFile file : files) {				
				String path = file.getName();
				
                if (ScmFile.isJavaFile(path)){
                	incrementComponentCount(components, path);
                }
			}
		}
		
		System.out.println(components.keySet().size() + " files analyzed.");
		
		addInfoToFiles(components);
		
		writeCountToFile(components, "FilesChange2.txt");
	}
	
	private static void addInfoToFiles(HashMap<ScmFile, Integer>components) throws IOException {
		System.out.println("computing info...");
		
		Collection<ScmFile> toRemove = new ArrayList<ScmFile>(); 
		
		for (ScmFile scmFile : components.keySet()) {
			File file = new File(REPO_PATH + "/" + scmFile.getLocalPath());
			String contents = null;
			if (file.exists())
				contents = readFile(file);
			else
				toRemove.add(scmFile);
			scmFile.extractInfo(contents);
		}
		
		for (ScmFile scmFile : toRemove) {
			components.remove(scmFile);
		}
		
		System.out.println("ok");
	}
	
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private static String readFile(File file) throws IOException {
		  FileInputStream stream = new FileInputStream(file);
		  try {
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		  }
		  finally {
		    stream.close();
		  }
		}
	
	public static void writeCountToFile(HashMap<ScmFile, Integer> components, String filename)
	throws IOException {
		/* 
		 * Bruno
		 * */	        

		File file = new File(filename);
		if(file.exists())
			file.delete();
		else
			file.createNewFile();
		FileWriter output = new FileWriter(file);
		output.write("Package\tFileName\tNumberOfChanges\tFileType\n");

		Set<ScmFile> compsSet = components.keySet();
		for ( Iterator<ScmFile> compsSetIterator = compsSet.iterator( ); compsSetIterator.hasNext( ); ) {
			ScmFile comp = (ScmFile)compsSetIterator.next();
			Integer changeCount = (Integer) components.get(comp);

			output.write(comp.getPackageName() + "\t" + comp.getClassName() + "\t" + changeCount + "\t" + comp.getType()+"\n");
			//output.close();
		}
	}
}

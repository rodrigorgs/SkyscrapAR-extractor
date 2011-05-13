import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import edu.nyu.cs.javagit.api.DotGit;
import edu.nyu.cs.javagit.api.JavaGitException;
import edu.nyu.cs.javagit.api.WorkingTree;
import edu.nyu.cs.javagit.api.commands.GitLogOptions;
import edu.nyu.cs.javagit.api.commands.GitLogResponse.CommitFile;


public class GitExtractor {
	static String REPO_PATH = "git-repos/";
	
	// based on http://wiki.eclipse.org/JGit/User_Guide
	public static void main(String[] args) throws IOException, JavaGitException {
		HashMap<String, Integer> components = new HashMap<String, Integer>();

		File repositoryDirectory = new File(REPO_PATH);
		
		DotGit dotGit = DotGit.getInstance(repositoryDirectory);
		GitLogOptions options = new GitLogOptions();
		options.setOptFileDetails(true);
		
		for (edu.nyu.cs.javagit.api.commands.GitLogResponse.Commit commit : dotGit.getLog(options)) {
			System.out.println("Commit with " + commit.getFilesChanged() + " changes.");
			
			List<CommitFile> files = commit.getFiles();
			if (files == null || files.size() == 0)
				continue;
			
			for (CommitFile file : files) {
				String path = file.getName();
				
				/******** Adapted from Bruno ***/
                Integer changeCount = new Integer(0);
                if (TestingSVNKit.isJavaSourcePath(path)){
                	String javaFile = TestingSVNKit.getJavaFile(path);
                	String packageFromPath = TestingSVNKit.getPackageFromPath(path);
                	String key = packageFromPath+" "+javaFile;
                	
                	if (components.containsKey(key))
                		changeCount = (Integer) components.get(key);
                	
                	changeCount++;
                	components.put(key, changeCount);
                }
				/********/
			}
		}
		
		System.out.println(components.keySet().size() + " files analyzed.");
		
		TestingSVNKit.writeCountToFile(components, "FilesChange2.txt");
	}
}

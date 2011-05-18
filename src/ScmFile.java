import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ScmFile {
	private static Pattern CLASSNAME_REGEX = Pattern.compile("src/.*/(.*?)\\.java$");
	private static Pattern PACKAGE_REGEX = Pattern.compile("src/(.*)/.*?\\.java$");
	private static Pattern PACKAGE_SRC_REGEX = Pattern.compile("package (.*?);", Pattern.MULTILINE);
		
	private String localPath;
	private String packageName;
	private String className;
	private String type;
	private long linesOfCode;
	
	// TODO: move to a Util class.
	public static String firstGroup(Pattern p, String s) {
		Matcher m = p.matcher(s);
		if (m.find())
			return m.group(1);
		else
			return null;
	}
	
	// TODO: move to a Util class.
	public static boolean matches(Pattern p, String s) {
		Matcher m = p.matcher(s);
		return m.find();
	}
	
	public static boolean isJavaFile(String path) {
		return matches(CLASSNAME_REGEX, path);
	}
	
	public ScmFile(String path) {
		this.localPath = path;
	}
	
	public void extractInfo(String fileContents) {
		this.className = firstGroup(CLASSNAME_REGEX, localPath);
		this.packageName = firstGroup(PACKAGE_REGEX, localPath); //.replace('/', '.');
		this.type = "ConcreteClass";
		
		if (fileContents == null)
			return;
		
		this.linesOfCode = fileContents.split("\n").length;
		
		String pkg = ScmFile.firstGroup(PACKAGE_SRC_REGEX, fileContents);
		packageName = (pkg == null ? "Default-Package" : pkg);
		
		Pattern patternAbstract = Pattern.compile("abstract class " + className + "\\b");
		Pattern patternInterface = Pattern.compile("interface " + className + "\\b");
		if (matches(patternAbstract, fileContents))
			type = "AbstractClass";
		else if (matches(patternInterface, fileContents))
			type = "Interface";
	}
	
	@Override
	public String toString() {
		return localPath;
	}
	
	@Override
	public int hashCode() {
		return localPath.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScmFile) {
			ScmFile other = (ScmFile)obj;
			if (localPath.equals(other.localPath))
				return true;
		}
			
		return false;
	}
	
	public long getLinesOfCode() {
		return linesOfCode;
	}
	
	public String getLocalPath() {
		return localPath;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public String getType() {
		return type;
	}
}

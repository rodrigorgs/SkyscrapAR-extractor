import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ScmFile {
	private static Pattern CLASSNAME_REGEX = Pattern.compile("src/.*/(.*?)\\.java$");
	private static Pattern PACKAGE_REGEX = Pattern.compile("src/(.*)/.*?\\.java$");
	
	private String localPath;
	private String packageName;
	private String className;
	private boolean abstractType;
	
	private static String firstGroup(Pattern p, String s) {
		Matcher m = p.matcher(s);
		if (m.matches())
			return m.group(1);
		else
			return null;
	}
	
	public static boolean isJavaFile(String path) {
		return Pattern.matches(CLASSNAME_REGEX.pattern(), path);
	}
	
	public ScmFile(String path) {
		this.localPath = path;
		this.className = firstGroup(CLASSNAME_REGEX, path);
		this.packageName = firstGroup(PACKAGE_REGEX, path).replace('/', '.');
		this.abstractType = false;
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
	
	public String getLocalPath() {
		return localPath;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public boolean isAbstractType() {
		return abstractType;
	}
}

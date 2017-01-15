package cz.agents.amodsim.configLoader;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Modifier;

/**
 *
 * @author F.I.D.O.
 */
public class ConfigBuilder {
	
	private final File configFile;
	
	private final File configPackageDir;
	
	private final File srcDir;
	
	private final String configPackageName;

	public ConfigBuilder(File configFile) {
		this.configFile = configFile;
		configPackageDir = getConfigPackageDir();
		configPackageName = getConfigPackageName();
		srcDir = getSrcDir();
	}
	
	
	
	public void buildConfig(){
		try {
			Config config = new ConfigParser().parseConfigFile(configFile);
			HashMap<String,Object> configMap = config.getConfig();
		
		generateConfig(configMap, "config");
		} catch (IOException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		
	}

	private File getConfigPackageDir() {
		String path = determinePath();
		
		File directory = new File(path);
		if (!directory.exists()){
			directory.mkdir();
		}
		return directory;
	}

	private String determinePath() {
		String path = null;
		String pathToClassDir = getMainClassDir();
		String pathToSourceDir = pathToClassDir.replace("target/classes", "src/main/java");
		path = pathToSourceDir + "config";
		return path;
	}
	
	private String getMainClassDir(){
		String path = null;
		try {
			Class mainClass = findMainClass();
			String mainClassFilename = mainClass.getSimpleName() + ".class";
			path = mainClass.getResource(mainClassFilename).getPath().replace(mainClassFilename, "");
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
		return path;
	}
	
	public static Class findMainClass() throws ClassNotFoundException{
        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            if (thread.getThreadGroup() != null && thread.getThreadGroup().getName().equals("main")) {
                for (StackTraceElement stackTraceElement : entry.getValue()) {
                    if (stackTraceElement.getMethodName().equals("main")) {
                        try {
                            Class<?> c = Class.forName(stackTraceElement.getClassName());
                            Class[] argTypes = new Class[] { String[].class };
                            //This will throw NoSuchMethodException in case of fake main methods
                            c.getDeclaredMethod("main", argTypes);
//                            return stackTraceElement.getClassName();
							return c;
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

	private void generateConfig(HashMap<String, Object> configMap, String objectName) {
		try {
			Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
			TypeSpec.Builder objectBuilder 
					= TypeSpec.classBuilder(getClassName(objectName)).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
			
			for (Entry<String, Object> entry : configMap.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				
				String propertyName = getPropertyName(key);
				
				if(value instanceof Map){
					ClassName newObjectType = ClassName.get(configPackageName, getClassName(key));
					constructorBuilder.addParameter(newObjectType, propertyName);
					objectBuilder.addField(newObjectType, propertyName, Modifier.PUBLIC, Modifier.FINAL);
					generateConfig((HashMap<String, Object>) value, key);
				}
				else{
					constructorBuilder.addParameter(value.getClass(), propertyName);
					objectBuilder.addField(value.getClass(), propertyName, Modifier.PUBLIC, Modifier.FINAL);
				}
				constructorBuilder.addStatement("this.$N = $N", propertyName, propertyName);
			}

			
			TypeSpec object = objectBuilder.addMethod(constructorBuilder.build()).build();
			
			JavaFile javaFile = JavaFile.builder(configPackageName, object).build();
			javaFile.writeTo(srcDir);
		} catch (IOException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private String getConfigPackageName() {
		String packageName = null;
		try {
			Class mainClass = findMainClass();
			packageName = mainClass.getPackage().getName() + ".config";
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
		return packageName;
	}
	
	private String getClassName(String name){
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
	}
	
	private String getPropertyName(String name){
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
	}

	private File getSrcDir() {
		File srcDir = null;
		try {
			Class mainClass = findMainClass();
			srcDir = new File(getMainClassDir().replaceFirst("target/classes.*", "src/main/java"));
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
		return srcDir;
	}
}

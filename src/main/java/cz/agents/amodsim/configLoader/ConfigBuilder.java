package cz.agents.amodsim.configLoader;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
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
    
    private final Class callerclass;

	public ConfigBuilder(File configFile) {
		this.configFile = configFile;
        callerclass = getCallerClass();
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
//		try {
//			Class mainClass = findMainClass();
            Class mainClass = callerclass;
			String mainClassFilename = mainClass.getSimpleName() + ".class";
			path = mainClass.getResource(mainClassFilename).getPath().replace(mainClassFilename, "");
//		} catch (ClassNotFoundException ex) {
//			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
//		}
		return path;
	}
	
	public static Class findMainClass() throws ClassNotFoundException{
//        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
//            Thread thread = entry.getKey();
//            if (thread.getThreadGroup() != null && thread.getThreadGroup().getName().equals("main")) {
//                for (StackTraceElement stackTraceElement : entry.getValue()) {
//                    if (stackTraceElement.getMethodName().equals("main")) {
//                        try {
//                            Class<?> c = Class.forName(stackTraceElement.getClassName());
//                            Class[] argTypes = new Class[] { String[].class };
//                            //This will throw NoSuchMethodException in case of fake main methods
//                            c.getDeclaredMethod("main", argTypes);
////                            return stackTraceElement.getClassName();
//							return c;
//                        } catch (NoSuchMethodException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }
        StackTraceElement[] stack = Thread.currentThread ().getStackTrace();
        StackTraceElement main = stack[stack.length - 1];
        return Class.forName(main.getClassName());
    }
    
    public static Class getCallerClass(){
        Class callerClass = null;
        
        try {
            callerClass = Class.forName(new Exception().getStackTrace()[2].getClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return callerClass;
    }

	private void generateConfig(HashMap<String, Object> configMap, String mapName) {
		
		Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
		TypeSpec.Builder objectBuilder 
				= TypeSpec.classBuilder(getClassName(mapName)).addModifiers(Modifier.PUBLIC);
		
		String mapParamName = getPropertyName(mapName);

		constructorBuilder.addParameter(HashMap.class, mapParamName);

		for (Entry<String, Object> entry : configMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			String propertyName = getPropertyName(key);

			FieldSpec.Builder fieldBuilder;

			if(value instanceof Map){
				ClassName newObjectType = ClassName.get(configPackageName, getClassName(key));
				generateConfig((HashMap<String, Object>) value, key);
				fieldBuilder = FieldSpec.builder(newObjectType, propertyName);
				constructorBuilder.addStatement("this.$N = new $T(($T) $N.get(\"$N\"))", propertyName, newObjectType, 
						HashMap.class, mapName, key);
			}
			else{
				fieldBuilder = FieldSpec.builder(value.getClass(), propertyName);
				constructorBuilder.addStatement("this.$N = ($T) $N.get(\"$N\")", propertyName, value.getClass(), 
						mapParamName, key);
			}
			objectBuilder.addField(fieldBuilder.addModifiers(Modifier.PUBLIC).build());
		}

			
		TypeSpec object = objectBuilder.addMethod(constructorBuilder.build()).build();

		JavaFile javaFile = JavaFile.builder(configPackageName, object).build();
		try {
			javaFile.writeTo(srcDir);
		} catch (IOException ex) {
			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private String getConfigPackageName() {
		String packageName = null;
//		try {
//			Class mainClass = findMainClass();
            Class mainClass = callerclass;
			packageName = mainClass.getPackage().getName() + ".config";
//		} catch (ClassNotFoundException ex) {
//			Logger.getLogger(ConfigBuilder.class.getName()).log(Level.SEVERE, null, ex);
//		}
		return packageName;
	}
	
	private String getClassName(String name){
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
	}
	
	private String getPropertyName(String name){
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
	}

	private File getSrcDir() {
        return new File(getMainClassDir().replaceFirst("target/classes.*", "src/main/java"));
	}
}

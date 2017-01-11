/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.configLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author fido
 */
public class ConfigParser {
    private static final Pattern WHITESPACE_LINE_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern INDENTION_PATTERN = Pattern.compile("^(    )*");
    private static final Pattern KEY_PATTERN = Pattern.compile("^([a-zA-Z])*(:)");
    private static final Pattern SIMPLE_VALUE_PATTERN = Pattern.compile("^\\s?([^\\s].*)$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^([0-9])");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("$([^\\s\\+]+)+");
    
    
    private final HashMap<String,Object> config;
    
    private final Queue<QueueEntry> referenceQueue;
    
    private int currentLevel;
    
    private int indentionLevel;
    
    private HashMap currentObject;
    
    private String currentKey;
    
    private Object currentValue;
    

    public ConfigParser() {
        this.config = new HashMap<>();
        currentLevel = 0;
        currentObject = config;
    }
    
    
    
    public void parseConfigFile(File configFile) throws FileNotFoundException, IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = WHITESPACE_LINE_PATTERN.matcher(line);
                if(matcher.find()){
                    continue;
                }
            
                if(line.contains("#")){
                    // possibel comment processing
                }
                else{
                    parseLine(line);
                }
            }
        }
    }

    private void parseLine(String line) {
        line = stripIndention(line);
        line = parseKey(line);
        if(parseValue(line)){
            currentObject.put(currentKey, currentValue);
        }
        else{
            HashMap<String, Object> newObject = new HashMap<>();
            currentObject.put(currentKey, newObject);
            currentObject = newObject; 
        }
    }

    private String stripIndention(String line) {
        Matcher matcher = INDENTION_PATTERN.matcher(line);
        if (matcher.find()){
            indentionLevel = matcher.groupCount();
            return matcher.replaceAll("");
        }
        else{
            return line;
        }
    }
    
    

    private String parseKey(String line) {
        Matcher matcher = KEY_PATTERN.matcher(line);
        matcher.find();
        currentKey = matcher.group(1);
        return matcher.replaceAll("");
    }
    
    private boolean parseValue(String line) {
        Matcher matcher = SIMPLE_VALUE_PATTERN.matcher(line);
        if(matcher.find()){
            currentValue = parseExpression(matcher.group(1));
            return true;
        }
            
        return false;
    }

    private Object parseExpression(String value) {
        
        if(value.contains("$")){
            return parseExpressionWithReferences(value);
        }
        else{
            return parseSimpleValue(value);
        }
    }

    private Object parseSimpleValue(String value) {
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if(matcher.find()){
            return Double.parseDouble(value);
        }
        else{
            return value;
        }
    }

    private Object parseExpressionWithReferences(String value) {
        List<String> references = parseReferences(value);
        for (String reference : references) {
            Object variable = getReferencedValue(reference);
            if(variable == null){
                referenceQueue.add(new QueueEntry(currentKey, value, currentObject));
                return null;
            }

            value.replace(reference, variable);
        }
    }

    private List<String> parseReferences(String value) {
        LinkedList<String> references = new LinkedList<>();
        Matcher matcher = REFERENCE_PATTERN.matcher(value);
        matcher.find();
        for(int i = 1; i <= matcher.groupCount(); i++){
            references.add(matcher.group(i));
        }
        return references;  
    }

    private Object getReferencedValue(String reference) {
        HashMap<String,Object> currentObject = config;
        String[] parts = reference.split(".");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if(currentObject.containsKey(part) && currentObject.get(part) != null){
                if(i < parts.length - 1){
                    currentObject = (HashMap<String, Object>) currentObject.get(part);
                }
                else{
                    return currentObject.get(reference);
                }
            }
            else{
                return null;
            }
        }
        return null;
    }
    
    private class QueueEntry{
        private final String key;
        
        private final Object value;
        
        private final HashMap<String,Object> parent;

        public QueueEntry(String key, Object value, HashMap<String, Object> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

    }
}

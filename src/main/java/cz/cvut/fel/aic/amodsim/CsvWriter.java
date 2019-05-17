/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author fido
 */
public class CsvWriter {
	private static final char DEFAULT_SEPARATOR = ',';
	
	private final char separator;
	
	private final Writer writer;

	public CsvWriter(Writer writer, char separator) {
		this.separator = separator;
		this.writer = writer;
	}

	public CsvWriter(Writer writer) {
		this(writer, DEFAULT_SEPARATOR);
	}
	
	public void writeLine(String... values) throws IOException {

		boolean first = true;

		StringBuilder stringBuilder = new StringBuilder();
		for (String value : values) {
			if (!first) {
				stringBuilder.append(separator);
			}
			
			stringBuilder.append(replaceQuotes(value));

			first = false;
		}
		stringBuilder.append("\n");
		writer.append(stringBuilder.toString());


	}
	
	public void close() throws IOException{
		writer.close();
	}

	private String replaceQuotes(String value) {
		String result = value;
		if (result.contains("\"")) {
			result = result.replace("\"", "\"\"");
		}
		return result;
	}
	
	public void flush() throws IOException{
		writer.flush();
	}
}

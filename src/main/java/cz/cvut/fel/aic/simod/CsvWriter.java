/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod;

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

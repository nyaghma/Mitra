/*
    MITRA: Automated Migration of Hierarchical Data to Relational Tables using Programming-by-Example
    Copyright (C) 2017  Navid Yaghmazadeh

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package mitra;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

	/*
	 * Read a given CSV file and construct the corresponding table representing it.
	 * Return the table containing all the values.
	 */
	public List<List<String>> readCSVFile(String filePath) {
		BufferedReader br = null;
		String line = "";
		String delim = ",";
		List<List<String>> inputTableValues =  new ArrayList<List<String>>();
		try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
            	String [] elems = line.split(delim);
            	List<String> row = new ArrayList<String>();
            	for(int i = 0; i < elems.length; i++) {
            		row.add(elems[i]);
            	}
            	inputTableValues.add(row);
            }
		} 
		catch (IOException e) {
            e.printStackTrace();
            return null;
        } 
		finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		return inputTableValues;
	}
	
}

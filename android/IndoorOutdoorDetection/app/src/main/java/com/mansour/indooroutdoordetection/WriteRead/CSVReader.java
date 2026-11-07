package com.mansour.indooroutdoordetection.WriteRead;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

    public static ArrayList<ArrayList> readcsv(String csvFile) {
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        ArrayList<ArrayList> rtkresult = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
//                line = line.replace(line.substring(line.length()-1), "");
                // use comma as separator
                String[] country = line.split(cvsSplitBy);
                ArrayList<Double> rtklist = new ArrayList<>();
                double i = Double.parseDouble(country[1]);
                rtklist.add(i);
                rtklist.add(Double.valueOf(country[2]));
                rtkresult.add(rtklist);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rtkresult;
    }
}

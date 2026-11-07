package com.mansour.indooroutdoordetection.WriteRead;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class Readline {
        // 文件内容的总行数。
    public static int getTotalLines(File file) throws IOException {
            FileReader in = new FileReader(file);
            LineNumberReader reader = new LineNumberReader(in);
            String s = reader.readLine();
            int lines = 0;
            try {
                while (s != null) {
                    lines++;
                    s = reader.readLine();
                    if (lines >= 2) {
                        if (s != null) {
                            System.out.println(s + "$");
                        }
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            reader.close();
            in.close();
            return lines;
        }
    public static void ReadFile(ArrayList<String> arrayList, String path) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null; //用于包装InputStreamReader,提高处理性能。因为BufferedReader有缓冲的，而InputStreamReader没有。
        try {
            String str = "";
            String str1 = "";
            fis = new FileInputStream(path);// FileInputStream
            // 从文件系统中的某个文件中获取字节
            isr = new InputStreamReader(fis);// InputStreamReader 是字节流通向字符流的桥梁,
            br = new BufferedReader(isr);// 从字符输入流中读取文件中的内容,封装了一个new InputStreamReader的对象
            int count = 1;
            File sourceFile = new File(path);
            int Max=0;
            try {
                 Max = getTotalLines(sourceFile);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            while (count <= Max) {
                if ((str = br.readLine()) != null) {
                    arrayList.add(str);
                }
                count++;
                // 当读取的一行不为空时,把读到的str的值赋给str1
                System.out.println(str1);// 打印出str1
            }
        }catch (FileNotFoundException e) {
            System.out.println("找不到指定文件");
        } catch (IOException e) {
            System.out.println("读取文件失败");
        } finally {
            try {
                if(br!=null) {
                    br.close();
                }
                if(isr!=null) {
                    isr.close();
                }
                if(fis!=null) {
                    fis.close();
                }
                // 关闭的时候最好按照先后顺序关闭最后开的先关闭所以先关s,再关n,最后关m
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    static public ArrayList<String> GetData(){
        String path = null;
        try
        {
            File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
            path =parent_path.getCanonicalPath()+"/uploadToServer.txt";

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        ArrayList<String> lines=new ArrayList<>();
        Readline.ReadFile(lines,path);
        return lines;
    }
    static public String[] GetEpoch(ArrayList<String> lines,int index){
        String result=lines.get(index);
        String[] stringList=result.split(",");
        return  stringList;
    }

    static public ArrayList<String> GetWifiData(){
        String path = null;
        try
        {
            path = Environment.getExternalStorageDirectory().getCanonicalPath()+"/Process/acc.txt";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        ArrayList<String> lines=new ArrayList<>();
        Readline.ReadFile(lines,path);
        return lines;
    }

}

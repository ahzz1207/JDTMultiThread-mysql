import java.util.*;
import java.io.*;
import java.lang.*;

public class Merge {
    public static String dir = "src/main/resources/project";
    static final int threadNum = 2; // 线程数

    //合并json存储的ast树
    public static void mergeJson(String feature) throws Exception{
        File[] files = new File(dir).listFiles();
        File feature_txt = new File("result/" + feature + ".json");
        feature_txt.createNewFile();
        BufferedWriter out_feat = new BufferedWriter(new FileWriter(feature_txt));

        ArrayList<String> txt_file = new ArrayList<>(); //之前生成的txt文件
        for(int i = 0; i < threadNum; i++){
            txt_file.add("t" + String.valueOf(i + 1) +"." + feature + ".json");
        }

        for (String f : txt_file){
            if(! new File(f).exists())
                continue;
            FileReader reader = new FileReader(f);
            System.out.println("writing data on " + f);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null){
                out_feat.write(line + "\r\n");
                out_feat.flush();
            }
        }
        out_feat.close();
    }
    public static void merge(String feature) throws Exception{ //按照特征合并 注意合并是时候按照t1～t5的顺序
        File[] files = new File(dir).listFiles();
        File feature_txt = new File("result/" + feature + ".txt");
        feature_txt.createNewFile();
        BufferedWriter out_feat = new BufferedWriter(new FileWriter(feature_txt));

        ArrayList<String> txt_file = new ArrayList<>(); //之前生成的txt文件
        for(int i = 0; i < threadNum; i++){
            txt_file.add("t" + String.valueOf(i + 1) +"." + feature + ".txt");
        }

        for (String f : txt_file){
            if(! new File(f).exists())
                continue;
            FileReader reader = new FileReader(f);
            System.out.println("writing data on " + f);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null){
                out_feat.write(line + "\r\n");
                out_feat.flush();
            }
        }
        out_feat.close();
    }
    public static void main(String[] args){
        String[] feat = {"apiseq", "tokens", "rawcode", "desc", "methname"};
        for(int j = 0; j < feat.length; j++){
            try {
                merge(feat[j]);
            } catch (Exception e){
                System.err.println(e.toString());
            }
        }
        try {
            mergeJson("ast");
        }catch (Exception e){
            System.err.println(e.toString());
        }
    }
}

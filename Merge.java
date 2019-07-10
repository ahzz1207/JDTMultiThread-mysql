//import java.util.*;
//import java.io.*;
//import java.lang.*;
//
//public class Merge {
//    public static String dir = "src/main/resources/project";
//    static final int threadNum = 3;
//
//
//    public static void concat(ArrayList<String> txt_file, BufferedWriter out_feat) throws IOException {
//        for (String f : txt_file){
//            if(! new File(f).exists()) {
//                continue;
//            }
//            FileReader reader = new FileReader(f);
//            System.out.println("writing data on " + f);
//            BufferedReader br = new BufferedReader(reader);
//            String line;
//            while ((line = br.readLine()) != null){
//                out_feat.write(line + "\r\n");
//                out_feat.flush();
//            }
//        }
//    }
//
//    public static void merge(String feature) throws Exception{
//        File feature_txt = new File("result/" + feature + ".txt");
//        feature_txt.createNewFile();
//        BufferedWriter out_feat = new BufferedWriter(new FileWriter(feature_txt));
//        //之前生成的txt文件
//        ArrayList<String> txt_file = new ArrayList<>();
//        for(int i = 0; i < threadNum; i++){
//            txt_file.add("t" + String.valueOf(i + 1) +"." + feature + ".txt");
//        }
//
//        concat(txt_file, out_feat);
//        out_feat.close();
//    }
//
//    public static void main(String[] args){
//        String[] feat = {"apiseq", "tokens", "rawcode", "desc", "methname", "ast"};
//        for(int j = 0; j < feat.length; j++){
//            try {
//                merge(feat[j]);
//            } catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//        try {
//            merge("ast");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//}
//
////class mergeThread implements Runnable{
////    private String[] feat = {"apiseq", "tokens", "rawcode", "desc", "methname", "ast"};
////    private int id;
////    private HashMap<String, ArrayList<String>> map = new HashMap<>();
////
////    @Override
////    public void run() {
////        String file = "Thread-" + id +"." + feat[i] + ".txt";
////        try {
////            FileReader fileReader = new FileReader(file);
////            BufferedReader bufferedReader = new BufferedReader(fileReader);
////            String line;
////            while ((line = bufferedReader.readLine()) != null){
////                line.split(" ");
////            }
////        } catch (FileNotFoundException e) {
////            e.printStackTrace();
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
////}

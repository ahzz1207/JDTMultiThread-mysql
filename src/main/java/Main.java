import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.*;
import java.lang.*;
import java.util.ArrayList;

public class Main {
    public static String dir = "src/main/resources/project";
    public static String log = "log/log.txt";
    public static ArrayList<String> listProj = new ArrayList<>();
    public static void listClasses(File projDir) {
        // 不存在log文件就创建一个
        File logFile = new File(log);
        if(!logFile.exists()){
            try{
                logFile.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        // 一级目录是username
        // 二级目录是projname
        // 从log文件中读取已经处理过的项目
        // 不同username下的projname可能会重复 username-projname
        ArrayList<String> dealtProjects = new ArrayList<>(); //存储已经处理过的项目
        try(FileReader f = new FileReader(log); BufferedReader b = new BufferedReader(f)){
            String proj;
            while((proj = b.readLine()) != null){
                String[] info = proj.split(" "); // threadname username-projname;
                dealtProjects.add(info[info.length - 1]);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        ArrayList<String> wrongProjects = new ArrayList<>();

        File[] files = projDir.listFiles();
        for (File username : files) {
            if (username.isDirectory()){
                for(File proj : username.listFiles()){
                    if(proj.isDirectory() && !(wrongProjects.contains(username.getName() + "-" + proj.getName()))){
                        if(!dealtProjects.contains(username.getName() + "-" + proj.getName()))  //未处理过
                            listProj.add(proj.getPath());
                    }
                }
            }
        }
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        listClasses(new File(dir));

        final int threadNum = 2; // 开2个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理
        ArrayList<ArrayList<String>> splitProj = new ArrayList<>();
        for(int k = 0; k < threadNum; k++)
            splitProj.add(new ArrayList<String>());
        int i = 0;
        for (i = 0; i< listProj.size() / threadNum * threadNum; i += threadNum){
            for(int k = 0; k < threadNum; k++)
                splitProj.get(k).add(listProj.get(i + k));
        }
        for(; i < listProj.size(); i++){
            splitProj.get(0).add(listProj.get(i));
        }
        ArrayList<MultiVisitor> multiVisitors = new ArrayList<>();
        for(int k = 0; k < threadNum; k++){
            multiVisitors.add(new MultiVisitor(splitProj.get(k), "t" + String.valueOf(k + 1)));
        }
        for(int k = 0; k < threadNum; k++)
            multiVisitors.get(k).start();
    }
}

class MultiVisitor implements Runnable{
    private Thread t;
    private ArrayList<String> projPath;
    private String index;
    public static String log = "log/log.txt";

    MultiVisitor(ArrayList<String> projPath, String index){
        this.index = index;
        this.projPath = projPath;
        System.out.println("Dealing " + projPath);
    }

    @Override
    public void run() {
        // 写log文件
        for (int i = 0; i < projPath.size(); i++){
            new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file)  -> {
                System.out.println(file.getPath());
                try {
                    CompilationUnit cu = JdtAstUtil.getCompilationUnit(file.getPath());
                    MyVisitor myVisitor = new MyVisitor(index);
                    cu.accept(myVisitor);
                    System.out.println();
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }).explore(new File(projPath.get(i)));
            // 判断日志文件是否存在 存在就追加写入
            // projPath 是绝对路径
            String[] info = projPath.get(i).split(File.separator);

            try{
                FileOutputStream out = new FileOutputStream(new File(log), true);
                out.write((t.getName() + " " +info[info.length - 2] + "-" + info[info.length - 1] + "\n").getBytes());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void start(){
        System.out.println("Starting ");
        if(t == null){
            t = new Thread(this);
            t.start();
        }
    }
}

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.*;
import java.lang.*;
import java.util.ArrayList;

public class Main {
    public static String dir = "src/main/resources/project";
    public static ArrayList<String> listProj = new ArrayList<>();
    public static void listClasses(File projDir) {
        // 一级目录是username
        // 二级目录是projname
        ArrayList<String> wrongProects = new ArrayList<>();
        File[] files = projDir.listFiles();
        for (File username : files) {
            if (username.isDirectory()){
                for(File proj : username.listFiles()){
                    if(proj.isDirectory() && !(wrongProects.contains(proj.getName()))){
                        listProj.add(proj.getPath());
                    }
                }
            }
        }
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        listClasses(new File(dir));

        final int threadNum = 2; // 开5个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理
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

    MultiVisitor(ArrayList<String> projPath, String index){
        this.index = index;
        this.projPath = projPath;
        System.out.println("Dealing " + projPath);
    }

    @Override
    public void run() {
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

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.*;
import java.lang.*;
import java.util.ArrayList;

public class Main {
    public static String dir = "src/main/resources/project";
    public static ArrayList<String> listProj = new ArrayList<>();
    public static void listClasses(File projDir) {
        ArrayList<String> wrongProects = new ArrayList<>();
        File[] files = projDir.listFiles();
        for (File f : files) {
            if (f.isDirectory() && (!wrongProects.contains(f.getName())))
                listProj.add(f.getPath());
        }
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        listClasses(new File(dir));

        final int threadNum = 5; // 开5个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理

        int i = 0;
        for(i = 0; i < listProj.size(); i+= threadNum) {
            MultiVisitor multiVisitor1 = new MultiVisitor(listProj.get(i),"t1");
            MultiVisitor multiVisitor2 = new MultiVisitor(listProj.get(i + 1), "t2");
            MultiVisitor multiVisitor3 = new MultiVisitor(listProj.get(i + 2), "t3");
            MultiVisitor multiVisitor4 = new MultiVisitor(listProj.get(i + 3), "t4");
            MultiVisitor multiVisitor5 = new MultiVisitor(listProj.get(i + 4), "t5");

            multiVisitor1.start();
            multiVisitor2.start();
            multiVisitor3.start();
            multiVisitor4.start();
            multiVisitor5.start();
        }

        for(; i < listProj.size(); i++){
            MultiVisitor multiVisitor1 = new MultiVisitor(listProj.get(i),"t1");
            multiVisitor1.start();
        }

    }
}

class MultiVisitor implements Runnable{
    private Thread t;
    private String projPath;
    private String index;

    MultiVisitor(String projPath, String index){
        this.index = index;
        this.projPath = projPath;
        System.out.println("Dealing " + projPath);
    }

    @Override
    public void run() {
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
        }).explore(new File(projPath));
    }

    public void start(){
        System.out.println("Starting ");
        if(t == null){
            t = new Thread(this, projPath);
            t.start();
        }
    }
}

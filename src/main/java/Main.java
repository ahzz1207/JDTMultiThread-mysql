import org.eclipse.jdt.core.dom.CompilationUnit;
import java.io.*;
import java.lang.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static String dir = "C:\\githubRepo";
    public static String log = "log/log.txt";
    public static File logFile = new File(log);
    public static ArrayList<String> listProj = new ArrayList<>();
    public static AtomicInteger succesMeth = new AtomicInteger();
    public static AtomicInteger failedMeth = new AtomicInteger();
    public static AtomicInteger repos = new AtomicInteger();

    public static void listClasses(File projDir) {
        // 不存在log文件就创建一个
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 一级目录是username
        // 二级目录是projname
        // 从log文件中读取已经处理过的项目
        // 不同username下的projname可能会重复 username-projname
        //存储已经处理过的项目
        ArrayList<String> dealtProjects = new ArrayList<>();
        try (FileReader f = new FileReader(log); BufferedReader b = new BufferedReader(f)) {
            String proj;
            while ((proj = b.readLine()) != null) {
                dealtProjects.add(proj);
            }
            System.out.println("Already handled repos is: " + dealtProjects.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<String> wrongProjects = new ArrayList<>();

        File[] userNameFiles = projDir.listFiles();
        for (File username : userNameFiles) {
            if (username.isDirectory()) {
                for (File proj : username.listFiles()) {
                    if (proj.isDirectory() && !(wrongProjects.contains(username.getName() + "-" + proj.getName()))) {
                        //未处理过
                        if (!dealtProjects.contains(username.getName() + "-" + proj.getName())) {
                            listProj.add(proj.getPath());
                        }
                    }
                }
            }
        }
        System.out.println("The files number is: " + listProj.size());
    }

    public static void main(String[] args) {
        // 链接数据库
        Connection conn = null;
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/githubreposfile?serverTimezone=UTC";
        String user = "root";
        String password = "17210240114";
        String sql = "insert into reposFile (`methName`, `tokens`, `comments`, `rawcode`, `apiseq`, `ast`) values (?,?,?,?,?,?)" ;
        String logsql = "insert into reposName values (?)";
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        listClasses(new File(dir));
        // 开2个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理
        final int threadNum = 12;
        ArrayList<ArrayList<String>> splitProj = new ArrayList<>();
        for(int k = 0; k < threadNum; k++) {
            splitProj.add(new ArrayList<>());
        }
        for (int i = 0; i < listProj.size() / threadNum * threadNum; i += threadNum){
            for(int k = 0; k < threadNum; k++) {
                splitProj.get(k).add(listProj.get(i + k));
            }
        }
        for(int i = listProj.size() / threadNum * threadNum; i < listProj.size(); i++){
            splitProj.get(0).add(listProj.get(i));
        }
        try {
            FileOutputStream out = new FileOutputStream(logFile, true);
            ReentrantLock lock = new ReentrantLock();
            for(int k = 0; k < threadNum; k++){
                new MultiVisitor(splitProj.get(k), Main.succesMeth, Main.failedMeth, Main.repos, out,
                        lock, conn, sql, logsql).start();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    }

    class MultiVisitor implements Runnable {
        private Thread thread;
        private ArrayList<String> projPath;
        private AtomicInteger succes;
        private AtomicInteger failed;
        private AtomicInteger repos;
        private FileOutputStream out;
        private ReentrantLock lock;
        private String id;
        private Connection conn;
        private String sql;
        private String logsql;

        MultiVisitor(ArrayList<String> projPath, AtomicInteger succes, AtomicInteger failed, AtomicInteger repos,
                     FileOutputStream out, ReentrantLock lock, Connection conn, String sql, String logsql) {
            this.projPath = projPath;
            this.succes = succes;
            this.failed = failed;
            this.repos = repos;
            this.out = out;
            this.lock = lock;
            this.conn = conn;
            this.sql = sql;
            this.logsql = logsql;
        }

        @Override
        public void run() {
            this.id = thread.getName();
            PreparedStatement stmt = null;
            for (int i = 0; i < projPath.size(); i++) {
                long startTime = new Date().getTime();
                new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
                    // HashMap<String, ArrayList<String>> conten = new HashMap<>();
                    // System.out.println(file.getPath());
                    try {
                        CompilationUnit cu = JdtAstUtil.getCompilationUnit(file.getPath());
                        MyVisitor myVisitor = new MyVisitor(this.conn, this.sql);
                        cu.accept(myVisitor);
                        // System.out.println("Already handled files: "  + (this.succes.getAndIncrement()+1));

                    } catch (Exception e) {
                        // System.err.println(path);
                        // System.out.println("Already failed filess: " + (this.failed.getAndIncrement()+1));
                        e.printStackTrace();
                    }
                }).explore(new File(projPath.get(i)));
                // 判断日志文件是否存在 存在就追加写入
                // projPath 是绝对路径
                String[] info = projPath.get(i).split("\\\\");
                long endTIme = new Date().getTime();
                // 记录已完成的repository
                try {
                    lock.lock();
                    out.write((info[info.length - 2] + "-" + info[info.length - 1] + "\n").getBytes());
                    lock.unlock();
                    stmt = conn.prepareStatement(logsql);
                    stmt.setString(1, (info[info.length - 2] + "-" + info[info.length - 1]));
                    stmt.execute();
                    System.out.println(this.thread.getName() + " Already handled repos: " + (this.repos.getAndIncrement() + 1));
                    System.out.println(info[info.length - 2] + "-" + info[info.length - 1] + " consume time:" + (endTIme - startTime));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void start() {
            this.thread = new Thread(this);
            System.out.println("Starting thread: " + thread.getId() + " files:" + this.projPath.size());
            thread.start();
        }
    }

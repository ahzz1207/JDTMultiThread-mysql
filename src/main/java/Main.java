import org.eclipse.jdt.core.dom.CompilationUnit;
import java.io.*;
import java.lang.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import com.mysql.cj.jdbc.Driver;

public class Main {
    public static String dir = "/home/fdse/data/repo/github";
    public static ArrayList<String> listProj = new ArrayList<>();
    public static AtomicInteger succesMeth = new AtomicInteger();
    public static AtomicInteger failedMeth = new AtomicInteger();
    public static AtomicInteger repos = new AtomicInteger();

    public static void listClasses(File projDir, Connection conn, HashSet<String> selected) {
        // 一级目录是username
        // 二级目录是projname
        // 不同username下的projname可能会重复 username-projname
        //存储已经处理过的项目
        HashSet<String> dealtProjects = new HashSet<>();
        String sql = "SELECT repo FROM `githubreposfile`.`reposname`";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                dealtProjects.add(result.getString("repo"));
            }
            System.out.println("Already handled repos is: " + dealtProjects.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        File[] userNameFiles = projDir.listFiles();
        HashSet<String> test = new HashSet<>();
        for (File username : userNameFiles) {
            if (username.isDirectory()) {
                for (File proj : username.listFiles()) {
                    if (proj.isDirectory()) {
                        //未处理过
                        if (!dealtProjects.contains((username.getName() + "@$$" + proj.getName())) &&
                                !dealtProjects.contains((username.getName() + "$$%" + proj.getName())) &&
                                selected.contains(username.getName() + "$$%" + proj.getName())) {
                            // 用set确保没有重复项目
                            if (!test.contains(proj.getPath())){
                                listProj.add(proj.getPath());
                            }
                            test.add(proj.getPath());
                        }
                    }
                }
            }
        }
        System.out.println("The files number is: " + listProj.size());
    }

    public static HashSet<String> selectFile(Connection connection){
        Connection conn = connection;
        PreparedStatement stmt = null;
        HashSet<String> selected = new HashSet<>();
        try {
            String sql = "select repo_name from star20";
            stmt = conn.prepareStatement(sql);
            ResultSet result = stmt.executeQuery();
            while (result.next()){
                selected.add(result.getString(1));
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return selected;
    }
    public static void main(String[] args) {
        // 链接数据库
        Connection conn = null;
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://10.131.252.198:3306/githubreposfile?serverTimezone=UTC";
        String user = "root";
        String password = "17210240114";
        String sql = "insert into reposFile (`methName`, `tokens`, `comments`, `rawcode`, `apiseq`, `ast`) values (?,?,?,?,?,?)" ;
        String logsql = "insert into reposName (`repo`) values (?)";
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        HashSet<String> selected = selectFile(conn);
        System.out.println(selected.size());
        listClasses(new File(dir), conn, selected);
        // 开2个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理
        final int threadNum = 40;
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
        for(int k = 0; k < threadNum; k++){
            new MultiVisitor(splitProj.get(k), Main.succesMeth, Main.failedMeth, Main.repos, conn, sql, logsql).start();
        }
    }
    }

    class MultiVisitor implements Runnable {
        private Thread thread;
        private ArrayList<String> projPath;
        private AtomicInteger succes;
        private AtomicInteger failed;
        private AtomicInteger repos;
        private String id;
        private Connection conn;
        private String sql;
        private String logsql;

        MultiVisitor(ArrayList<String> projPath, AtomicInteger succes, AtomicInteger failed, AtomicInteger repos,
                     Connection conn, String sql, String logsql) {
            this.projPath = projPath;
            this.succes = succes;
            this.failed = failed;
            this.repos = repos;
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
                String[] info = projPath.get(i).split(File.separator);
                // 记录已处理的项目
                try {
                    stmt = conn.prepareStatement(logsql);
                    stmt.setString(1, (info[info.length - 2] + "$$%" + info[info.length - 1]));
                    stmt.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
                    // System.out.println(file.getPath());
                    try {
                        CompilationUnit cu = JdtAstUtil.getCompilationUnit(file.getPath());
                        MyVisitor myVisitor = new MyVisitor(this.conn, this.sql);
                        if (cu != null && myVisitor != null) {
                            cu.accept(myVisitor);
                        } else {
                            return;
                        }
                        // System.out.println("Already handled files: "  + (this.succes.getAndIncrement()+1));

                    } catch (Exception e) {
                        System.err.println(path);
                        // System.out.println("Already failed filess: " + (this.failed.getAndIncrement()+1));
                        e.printStackTrace();
                        return;
                    }
                }).explore(new File(projPath.get(i)));
                // projPath 是绝对路径
                long endTIme = new Date().getTime();
                // 记录已完成的repository
                System.out.println(this.thread.getName() + " Already handled repos: " + (this.repos.getAndIncrement() + 1));
                System.out.println(info[info.length - 2] + "-" + info[info.length - 1] + " consume time:" + (endTIme - startTime));
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

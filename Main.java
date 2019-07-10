import org.eclipse.jdt.core.dom.CompilationUnit;
import java.io.*;
import java.lang.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import com.mysql.cj.jdbc.Driver;

public class Main {
    public static String dir = "src/main/resources/project";
    public static ArrayList<String> listProj = new ArrayList<>();
    public static AtomicInteger succesMeth = new AtomicInteger();
    public static AtomicInteger failedMeth = new AtomicInteger();
    public static AtomicInteger repos = new AtomicInteger();
    public static HashMap<String, Integer> selected = new HashMap<>();

    public static void listClasses(File projDir, Connection conn, HashMap<String, Integer> selected) throws IOException {
        // 一级目录是username
        // 二级目录是projname
        // 不同username下的projname可能会重复 username-projname
        //存储已经处理过的项目
        HashSet<String> dealtProjects = new HashSet<>();
        String sql = "SELECT reponame FROM `repos_deal`";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                dealtProjects.add(result.getString("reponame"));
            }
            System.out.println("Already handled repos is: " + dealtProjects.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //得到需要处理的地址
        HashSet<File> userNameFiles = new HashSet<>();
        FileReader projsPath = new FileReader("addrlist1.txt");
        BufferedReader in = new BufferedReader(projsPath);
        String str;
        while ((str = in.readLine()) != null) {
            userNameFiles.add(new File(str));
        }
        System.out.println("will produce project is " + userNameFiles.size());

        for (File proj : userNameFiles){
            String[] temps = proj.getPath().split(File.separator);
            String name = temps[temps.length-2] + "$$%" + temps[temps.length-1];
            if (!dealtProjects.contains(name) && selected.containsKey(name)) {
                listProj.add(proj.getPath());
            }
        }
//        HashSet<String> test = new HashSet<>();
//        for (File username : userNameFiles) {
//            if (username.isDirectory()) {
//                for (File proj : username.listFiles()) {
//                    if (proj.isDirectory()) {
//                        //未处理过
//                        System.out.println(count++);
//                        if (!dealtProjects.contains((username.getName() + "$$%" + proj.getName())) &&
//                                selected.contains(username.getName() + "$$%" + proj.getName())) {
//                            // 用set确保没有重复项目
//                            if (!test.contains(proj.getPath())){
//                                listProj.add(proj.getPath());
//                            }
//                            test.add(proj.getPath());
//                        }
//                    }
//                }
//            }
//        }

        System.out.println("The files number is: " + listProj.size());
    }

    public static HashMap<String, Integer> selectFile(Connection connection){
        Connection conn = connection;
        PreparedStatement stmt = null;
        HashMap<String, Integer> selected = new HashMap<>();
        try {
            String sql = "select id, reponame from star5";
            stmt = conn.prepareStatement(sql);
            ResultSet result = stmt.executeQuery();
            while (result.next()){
                selected.put(result.getString(2), result.getInt(1));
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return selected;
    }
    public static void main(String[] args) throws IOException {
        // 链接数据库
        Connection conn = null;
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://10.131.252.198:3306/repos?serverTimezone=UTC";
        String user = "root";
        String password = "17210240114";
        String sql = "insert into reposfile (`methName`, `tokens`, `comments`, `rawcode`, `apiseq`, `ast`, `newapiseq`) values (?,?,?,?,?,?,?)" ;
        String logsql = "insert into repos_deal (`id`, `reponame`) values (?,?)";
        String sqlNoComments = "insert into codesearchbase (`methName`, `tokens`, `rawcode`, `apiseq`, `ast`, `newapiseq`) values (?,?,?,?,?,?)" ;

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        selected = selectFile(conn);
        listClasses(new File(dir), conn, selected);
        // 开2个线程, 每个线程处理一个project, 每个线程的处理结果写入一个文件，最后进行merge处理
        final int threadNum = 100;
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
            new MultiVisitor(splitProj.get(k), Main.succesMeth, Main.failedMeth, Main.repos, conn, sql, logsql, sqlNoComments, selected).start();
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
        private String sqlNoComments;
        private HashMap<String, Integer> selected = new HashMap<>();

        MultiVisitor(ArrayList<String> projPath, AtomicInteger succes, AtomicInteger failed, AtomicInteger repos,
                     Connection conn, String sql, String logsql, String sqlNoComments, HashMap<String, Integer> selected) {
            this.projPath = projPath;
            this.succes = succes;
            this.failed = failed;
            this.repos = repos;
            this.conn = conn;
            this.sql = sql;
            this.logsql = logsql;
            this.sqlNoComments = sqlNoComments;
            this.selected = selected;
        }

        @Override
        public void run() {
            this.id = thread.getName();
            PreparedStatement stmt = null;
            for (int i = 0; i < projPath.size(); i++) {
                long startTime = new Date().getTime();
                String[] info = projPath.get(i).split(File.separator);
                String reponame = info[info.length - 2] + "$$%" + info[info.length - 1];
                new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
                    // System.out.println(file.getPath());
                    try {
                        CompilationUnit cu = JdtAstUtil.getCompilationUnit(file.getPath());
                        MyVisitor myVisitor = new MyVisitor(this.conn, this.sql, this.sqlNoComments);
                        if (cu != null && myVisitor != null) {
                            cu.accept(myVisitor);
                        } else {
                            return;
                        }
                        // System.out.println("Already handled files: "  + (this.succes.getAndIncrement()+1));

                    } catch (Exception e) {
                        System.err.println(path);
                        // System.out.println("Already failed filess: " + (this.failed.getAndIncrement()+1));
                        // e.printStackTrace();
                        return;
                    }
                }).explore(new File(projPath.get(i)));
                // projPath 是绝对路径
                long endTIme = new Date().getTime();
                // 记录已完成的repository
                // 记录已处理的项目
                try {
                    stmt = conn.prepareStatement(logsql);
                    stmt.setInt(1, selected.get(reponame));
                    stmt.setString(2, reponame);
                    stmt.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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

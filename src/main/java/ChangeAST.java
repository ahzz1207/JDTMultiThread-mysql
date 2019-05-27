//import net.sf.json.JSONArray;
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ChangeAST {
//    private static Connection conn = null;
//
//    public ChangeAST(){
//
//    }
//
//    public static void main(String[] args) {
//        String driver = "com.mysql.cj.jdbc.Driver";
//        String url = "jdbc:mysql://10.131.252.198:3306/githubreposfile?serverTimezone=UTC";
//        String user = "root";
//        String password = "17210240114";
//        try {
//            Class.forName(driver);
//            conn = DriverManager.getConnection(url, user, password);
//            String sql = "select id, ast from reposfile where id>=200";
//            PreparedStatement stmt = conn.prepareStatement(sql);
//            ResultSet resultSet = stmt.executeQuery();
//            while (resultSet.next()){
//                String ast = resultSet.getString(2);
//                ArrayList<Map<String, Object>> arrayList;
//                try{
//                    arrayList = String2Map(ast);
//                    JSONArray jsonArray = JSONArray.fromObject(arrayList);
//                    String astJson = jsonArray.toString();
//                    sql = "update reposfile set ast = ? where id = ?";
//                    stmt = conn.prepareStatement(sql);
//                    stmt.setString(1, astJson);
//                    stmt.setInt(2, resultSet.getInt(1));
//                    stmt.execute();
//                }
//                catch (Exception e){
//                    sql = "insert into wrongast values (?)";
//                    stmt = conn.prepareStatement(sql);
//                    stmt.setInt(1, resultSet.getInt(1));
//                    stmt.execute();
//                    System.out.println(resultSet.getInt(1));
//                }
//            }
//            stmt.close();
//            conn.close();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//    public static ArrayList<Map<String, Object>> String2Map(String str){
//        ArrayList<Integer> left = new ArrayList<>();
//        ArrayList<Integer> right = new ArrayList<>();
//        ArrayList<String> strs = new ArrayList<>();
//        for (int i = 0; i < str.length(); i++){
//            char s = str.charAt(i);
//            if ((s == '{' && str.charAt(i-1) == '[') || (s == '{' && str.charAt(i-1) == ',')){
//                left.add(i);
//            }
//            if ((s == '}' && str.charAt(i+1) == ']') || (s == '}' && str.charAt(i+1) == ',')){
//                right.add(i);
//            }
//        }
//        assert left.size() == right.size();
//        //得到Array中的所有Map
//        for (int i = 0; i < left.size() ; i++) {
//            strs.add(str.substring(left.get(i)+1, right.get(i)));
//        }
//        ArrayList<Map<String, Object>> mapArrayList = new ArrayList<>();
//        for (String subString:strs
//             ) {
//            Map<String, Object> map = new HashMap<>();
//            String[] entrys;
//            if (subString.startsWith("children")) {
//                String sub = subString.substring(subString.indexOf('['), subString.indexOf(']'));
//                String replace = sub.replaceAll(", ", "~");
//                subString = subString.replace(sub, replace);
//                entrys = subString.split(",");
//                //第一个entry
//                String[] entry1 = entrys[0].split("=");
//                String key = entry1[0];
//                String list = entry1[1].substring(1, entry1[1].length() - 1);
//                String[] ints = list.trim().split("~");
//                ArrayList arrayList = new ArrayList();
//                for (String s : ints
//                        ) {
//                    arrayList.add(Integer.valueOf(s));
//                }
//                map.put(key, arrayList);
//                //第二个entry
//                String[] entry2 = entrys[1].split("=");
//                key = entry2[0].trim();
//                int value = Integer.valueOf(entry2[1]);
//                map.put(key, value);
//                //第三个entry
//                String[] entry3 = entrys[2].split("=");
//                key = entry3[0].trim();
//                String value3 = entry3[1];
//                map.put(key, value3);
//                //放入Array
//                mapArrayList.add(map);
//            } else {
//                entrys = subString.split(",");
//                //第一个entry
//                String[] entry1 = entrys[0].split("=");
//                String key = entry1[0];
//                map.put(key, Integer.valueOf(entry1[1]));
//                //第二个entry
//                String[] entry2 = entrys[1].split("=");
//                key = entry2[0].trim();
//                String value = entry2[1];
//                map.put(key, value);
//                //第三个entry
//                String[] entry3 = entrys[2].split("=");
//                key = entry3[0].trim();
//                String value3 = entry3[1];
//                map.put(key, value3);
//                //放入Array
//                mapArrayList.add(map);
//            }
//        }
//        return mapArrayList;
//    }
//
//}

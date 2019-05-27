import net.sf.json.JSONArray;
import org.eclipse.jdt.core.dom.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

public class MyVisitor extends ASTVisitor{
    static AtomicInteger id = new AtomicInteger();
    private Connection conn;
    private PreparedStatement stmt = null;
    private String sql;

    MyVisitor( Connection conn, String sql) {
        this.conn = conn;
        this.sql = sql;
    }

    static String[] keyWord = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue",
            "default", "do", "double", "else", "enum", "exports", "extends", "final", "finally", "float", "for",
            "if", "implements", "import", "instanceof", "int", "interface", "long", "long", "module", "native", "new",
            "package", "private", "protected", "public", "requires", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "null", "false", "var",
            "const", "goto"
    };
    static String stopwords[] = {
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yourself", "yourselves",
            "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
            "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is",
            "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing",
            "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with",
            "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from",
            "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there",
            "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
            "should", "now", "lbrace", "rbrace", "dot", "comma", "eq", "semi", "lparen", "rparen", "colon", "lbracket", "rbracket",
            "lt", "gt", "{", "}", "(", ")", "[", "]", ",", "."
    };
    static String jdkPrefix[] = {
            "java.applet", "java.awt", "java.awt.color", "java.awt.datatransfer", "java.awt.dnd",
            "java.awt.event", "java.awt.font", "java.awt.geom", "java.awt.im", "java.awt.image", "java.awt.image.renderable",
            "java.awt.im.spi", "java.awt.print", "java.beans", "java.beans.beancontext", "java.io", "java.lang",
            "java.lang.annotation", "java.lang.instrument", "java.lang.invoke", "java.lang.management", "java.lang.ref",
            "java.lang.reflect", "java.math", "java.net", "java.nio", "java.nio.channels", "java.nio.channels.spi",
            "java.nio.charset", "java.nio.charset.spi", "java.nio.file", "java.nio.file.attribute", "java.nio.file.spi",
            "java.rmi", "java.rmi.activation", "java.rmi.dgc", "java.rmi.registry", "java.rmi.server", "java.security",
            "java.security.acl", "java.security.cert", "java.security.interfaces", "java.security.spec", "java.sql",
            "java.text", "java.text.spi", "java.util", "java.util.concurrent", "java.util.concurrent.atomic",
            "java.util.concurrent.locks", "java.util.jar", "java.util.logging", "java.util.prefs", "java.util.regex",
            "java.util.spi", "java.util.zip", "javax.accessibility", "javax.activation", "javax.activity", "javax.annotation",
            "javax.annotation.processing", "javax.crypto", "javax.crypto.interfaces", "javax.crypto.spec", "javax.imageio",
            "javax.imageio.event", "javax.imageio.metadata", "javax.imageio.plugins.bmp", "javax.imageio.plugins.jpeg",
            "javax.imageio.spi", "javax.imageio.stream", "javax.jws", "javax.jws.soap", "javax.lang.model",
            "javax.lang.model.element", "javax.lang.model.type", "javax.lang.model.util", "javax.management",
            "javax.management.loading", "javax.management.modelmbean", "javax.management.monitor",
            "javax.management.openmbean", "javax.management.relation", "javax.management.remote",
            "javax.management.remote.rmi", "javax.management.timer", "javax.naming", "javax.naming.directory",
            "javax.naming.event", "javax.naming.ldap", "javax.naming.spi", "javax.net", "javax.net.ssl", "javax.print",
            "javax.print.attribute", "javax.print.attribute.standard", "javax.print.event", "javax.rmi", "javax.rmi.CORBA",
            "javax.rmi.ssl", "javax.script", "javax.security.auth", "javax.security.auth.callback",
            "javax.security.auth.kerberos", "javax.security.auth.login", "javax.security.auth.spi",
            "javax.security.auth.x500", "javax.security.cert", "javax.security.sasl", "javax.sound.midi",
            "javax.sound.midi.spi", "javax.sound.sampled", "javax.sound.sampled.spi", "javax.sql", "javax.sql.rowset",
            "javax.sql.rowset.serial", "javax.sql.rowset.spi", "javax.swing", "javax.swing.border", "javax.swing.colorchooser",
            "javax.swing.event", "javax.swing.filechooser", "javax.swing.plaf", "javax.swing.plaf.basic",
            "javax.swing.plaf.metal", "javax.swing.plaf.multi", "javax.swing.plaf.nimbus", "javax.swing.plaf.synth",
            "javax.swing.table", "javax.swing.text", "javax.swing.text.html", "javax.swing.text.html.parser",
            "javax.swing.text.rtf", "javax.swing.tree", "javax.swing.undo", "javax.tools", "javax.transaction",
            "javax.transaction.xa", "javax.xml", "javax.xml.bind", "javax.xml.bind.annotation",
            "javax.xml.bind.annotation.adapters", "javax.xml.bind.attachment", "javax.xml.bind.helpers",
            "javax.xml.bind.util", "javax.xml.crypto", "javax.xml.crypto.dom", "javax.xml.crypto.dsig",
            "javax.xml.crypto.dsig.dom", "javax.xml.crypto.dsig.keyinfo", "javax.xml.crypto.dsig.spec",
            "javax.xml.datatype", "javax.xml.namespace", "javax.xml.parsers", "javax.xml.soap", "javax.xml.stream",
            "javax.xml.stream.events", "javax.xml.stream.util", "javax.xml.transform", "javax.xml.transform.dom",
            "javax.xml.transform.sax", "javax.xml.transform.stax", "javax.xml.transform.stream", "javax.xml.validation",
            "javax.xml.ws", "javax.xml.ws.handler", "javax.xml.ws.handler.soap", "javax.xml.ws.http", "javax.xml.ws.soap",
            "javax.xml.ws.spi", "javax.xml.ws.spi.http", "javax.xml.ws.wsaddressing", "javax.xml.xpath",
            "org.ietf.jgss", "org.omg.CORBA", "org.omg.CORBA_2_3", "org.omg.CORBA_2_3.portable", "org.omg.CORBA.DynAnyPackage",
            "org.omg.CORBA.ORBPackage", "org.omg.CORBA.portable", "org.omg.CORBA.TypeCodePackage", "org.omg.CosNaming",
            "org.omg.CosNaming.NamingContextExtPackage", "org.omg.CosNaming.NamingContextPackage", "org.omg.Dynamic",
            "org.omg.DynamicAny", "org.omg.DynamicAny.DynAnyFactoryPackage", "org.omg.DynamicAny.DynAnyPackage",
            "org.omg.IOP", "org.omg.IOP.CodecFactoryPackage", "org.omg.IOP.CodecPackage", "org.omg.Messaging",
            "org.omg.PortableInterceptor", "org.omg.PortableInterceptor.ORBInitInfoPackage", "org.omg.PortableServer",
            "org.omg.PortableServer.CurrentPackage", "org.omg.PortableServer.POAManagerPackage",
            "org.omg.PortableServer.POAPackage", "org.omg.PortableServer.portable", "org.omg.PortableServer.ServantLocatorPackage",
            "org.omg.SendingContext", "org.omg.stub.java.rmi", "org.w3c.dom", "org.w3c.dom.bootstrap", "org.w3c.dom.events",
            "org.w3c.dom.ls", "org.xml.sax", "org.xml.sax.ext",
            "org.xml.sax.helpers"
    };

    public static boolean isJdkApi(String s){
        for(String si: jdkPrefix){
            if(s.startsWith(si)) {
                return true;
            }
        }
        return false;
    }

    private String parseMethname(String name){
        ArrayList<String> methNames = new ArrayList<>();
        int start = 0;
        int l = name.length();
        int end;
        while(start < l){
            StringBuffer token = new StringBuffer();
            token.append(Character.toLowerCase(name.charAt(start)));
            end = start + 1;
            while(end < l && Character.isLowerCase(name.charAt(end))){
                token.append(name.charAt(end));
                end++;
            }
            if(token.length() > 1) {
                methNames.add(token.toString());
            }
            if(end < l){
                start = end;
            }
            else{
                break;
            }
        }
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < methNames.size(); i++){
            sb.append(methNames.get(i));
            sb.append(" ");
        }
        return  sb.toString().trim();
    }

    private String getTokens(String methodBody){
        //排除空方法
        Set<String> tokens = new LinkedHashSet<>();
        methodBody = methodBody.replaceAll("[^a-z^A-Z^0-9]", " ").trim();
        String[] tmp = methodBody.split("\\s+");

        String regex=".*[a-zA-Z]+.*";

        for(int i = 0; i < tmp.length ; i++){
            if(tmp[i].matches(regex)){
                int start = 0;
                int l = tmp[i].length();
                int end;
                while(start < l){
                    StringBuilder sb = new StringBuilder();
                    sb.append(Character.toLowerCase(tmp[i].charAt(start)));
                    end = start + 1;
                    while(end < l && Character.isLowerCase(tmp[i].charAt(end))){
                        sb.append(tmp[i].charAt(end));
                        end++;
                    }
                    if (sb.length() > 1 &&
                            !Arrays.asList(keyWord).contains(sb.toString()) &&
                            !Arrays.asList(stopwords).contains(sb.toString())){
                        tokens.add(sb.toString());
                    }
                    if (end < l){
                        start = end;
                    }
                    else{
                        break;
                    }
                }
            }
        }
        StringBuffer to = new StringBuffer();
        Iterator i = tokens.iterator();
        while(i.hasNext()){
            to.append(i.next());
            to.append(" ");
        }
        return to.toString().trim();
    }

    private String handleAST(List<ASTNode> astNodes) {
        // ast的信息写入json文件
        Map<ASTNode, Integer> astNum = new HashMap<>();
        for (int i = 0; i < astNodes.size(); i++) {
            astNum.put(astNodes.get(i), i);
        }

        Map<ASTNode, ArrayList<Integer>> AST_child = new HashMap<>();
        for (int i = 0; i < astNodes.size(); i++) {
            ASTNode parent = astNodes.get(i).getParent();
            if (parent != null) {
                if (AST_child.containsKey(parent)) {
                    AST_child.get(parent).add(i);
                } else {
                    ArrayList<Integer> child = new ArrayList<>();
                    child.add(i);
                    AST_child.put(parent, child);
                }
            }
        }

        ArrayList<Map<String, Object>> finalAST = new ArrayList<>();
        for (int i = 0; i < astNodes.size(); i++) {
            Map<String, Object> treeNode = new HashMap<>();
            treeNode.put("index", i);
            String type = astNodes.get(i).getClass().toString();

            treeNode.put("type", type.substring(1 + type.lastIndexOf(".")));
            if (AST_child.containsKey(astNodes.get(i))) {
                treeNode.put("children", AST_child.get(astNodes.get(i)));
            } else {
                //叶结点
                // string / char => <STR>
                // number => <NUM>
                ASTNode n = astNodes.get(i);
                if (n instanceof StringLiteral) {
                    treeNode.put("value", "<STR>");
                } else if (n instanceof NumberLiteral) {
                    treeNode.put("value", "<NUM>");
                } else {
                    treeNode.put("value", n.toString());
                }
            }
            finalAST.add(treeNode);
        }

        //写入json文件
        JSONArray jsonArray = JSONArray.fromObject(finalAST);
        return jsonArray.toString();
    }

    @Override
    public boolean visit(MethodDeclaration node) {

        Block block = node.getBody();

        String methName = "";
        String comments = "";
        ArrayList<String> apiseq = new ArrayList<>();
        StringBuffer APIseq = new StringBuffer();
        String bodyToken = "";

        bodyToken = getTokens(node.toString());

        methName = parseMethname(node.getName().toString());

        Javadoc docs = node.getJavadoc();
        if(docs == null) {
            return true;
        }
        List tags = docs.tags();

        boolean hasJavadoc = false;
        if(tags.size() > 0){
            String fullDocs = tags.get(0).toString().replaceAll("\\*|\\t|\\r|\\n", " ").replaceAll(" +", " ").trim();

            String[] sents = fullDocs.split("\\.");

            String regEx = "[\\u4e00-\\u9fa5]+";
            Pattern p = Pattern.compile(regEx);

            for (String sent : sents){
                Matcher m = p.matcher(sent);
                if(sent.trim().length() > 0 && !sent.contains("@") && !m.find() && sent.split(" ").length > 2){
                    comments = sent.trim().toLowerCase();
                    hasJavadoc = true;
                    break;
                }
            }
        }

        if(!hasJavadoc) {
            return true;
        }

        //删掉注释
        docs.delete();
        String methodComplete = node.toString().replaceAll("\\n", " ");

        if (block == null){
            return false;
        }

        block.accept(new ASTVisitor() {
            @Override
            public void endVisit(MethodInvocation node){
                // 处理o.m()
                Expression expr = node.getExpression();
                if(expr != null){
                    ITypeBinding typeBinding = expr.resolveTypeBinding();
                    if(typeBinding != null){
                        String qualifiedName = typeBinding.getQualifiedName();
                        Pattern p = Pattern.compile("<|>|,");
                        Matcher m = p.matcher(qualifiedName);
                        String[] className = p.split(qualifiedName);
                        StringBuffer api = new StringBuffer();

                        api.append(" ");
                        api.append(node.getName());
                        matcher(api, m, className);
                        apiseq.add(api.toString().replaceAll(" +", " ").trim());
                        //System.out.println("api: " + api.toString().replaceAll(" +", " "));
//                        if(isJdkApi(qualifiedName)){
//                            ret.add(qualifiedName + " " + node.getName());
//                        }
                    }
                }
            }

            public void matcher(StringBuffer api, Matcher m, String[] className){
                for(int i = 0; i < className.length; i++){
                    String[] packageName = className[i].split("\\.");
                    api.append(packageName[packageName.length - 1]);
                    if(m.find()){
                        api.append(" ");
                        api.append(m.group());
                        api.append(" ");
                    }
                }
            }

            // 处理new o
            @Override
            public void endVisit(ClassInstanceCreation node){
                if (node == null){
                    return;
                }
                String qualifiedName = node.getType().resolveBinding().getQualifiedName();
                Pattern p = Pattern.compile("<|>|,");
                Matcher m = p.matcher(qualifiedName);
                String[] className = p.split(qualifiedName);
                StringBuffer api = new StringBuffer();

                matcher(api, m, className);

                api.append(" new");

                apiseq.add(api.toString().replaceAll(" +", " ").trim());
                //System.out.println("api: " + api.toString().replaceAll(" +", " "));
//                if(isJdkApi(qualifiedName)){
//                    ret.add(qualifiedName + " " + "new");
//                }
            }
        });

        for(String api: apiseq){
            APIseq.append(api);
            APIseq.append(" ");
        }
        String api = APIseq.toString().trim();

        NodeVisitor nv = new NodeVisitor();
        node.accept(nv);
        List<ASTNode> astNodes = nv.getASTNodes();

        if(!bodyToken.isEmpty() && !methName.isEmpty() && !comments.isEmpty() && !api.isEmpty() && !astNodes.isEmpty()){
            // int key = id.getAndIncrement();
            // 写入数据库
            try {
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, methName);
                stmt.setString(2, bodyToken);
                stmt.setString(3, comments);
                stmt.setString(4, methodComplete);
                stmt.setString(5, api);
                stmt.setString(6, handleAST(astNodes));
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}

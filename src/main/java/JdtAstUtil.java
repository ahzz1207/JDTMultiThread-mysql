import java.io.*;
import org.eclipse.jdt.core.dom.*;


public class JdtAstUtil {
    public static CompilationUnit getCompilationUnit(String javaFilePath){
        byte[] input = null;
        try{
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(javaFilePath));
            input = new byte[bufferedInputStream.available()];
            bufferedInputStream.read(input);
            bufferedInputStream.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        ASTParser astParser = ASTParser.newParser(AST.JLS8);
        astParser.setSource(new String(input).toCharArray());
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setEnvironment(null, null, null, true);
        astParser.setResolveBindings(true);
        astParser.setBindingsRecovery(true);
        astParser.setUnitName("any_name");
        CompilationUnit result = (CompilationUnit) (astParser.createAST(null));

        return result;
    }
}

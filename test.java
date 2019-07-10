import java.io.*;
import org.eclipse.jdt.core.dom.*;

public class test {
    public static ASTNode get(){

        ASTParser astParser = ASTParser.newParser(AST.JLS8);
        astParser.setSource("".toCharArray());
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setEnvironment(null, null, null, true);
        astParser.setResolveBindings(true);
        astParser.setBindingsRecovery(true);
        astParser.setUnitName("any_name");
        try {
            ASTNode result = astParser.createAST(null);
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

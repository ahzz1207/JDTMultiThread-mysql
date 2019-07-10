import java.util.*;

import org.eclipse.jdt.core.dom.*;

public class NodeVisitor extends ASTVisitor{
    public List<ASTNode> nodeList = new ArrayList<>();

    @Override
    public void preVisit(ASTNode node){
        nodeList.add(node);
    }

    public List<ASTNode> getASTNodes(){
        return nodeList;
    }

}

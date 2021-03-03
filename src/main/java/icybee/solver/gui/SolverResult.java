package icybee.solver.gui;

import icybee.solver.GameTree;
import icybee.solver.nodes.ActionNode;
import icybee.solver.nodes.GameActions;
import icybee.solver.nodes.GameTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class SolverResult {
    public JPanel resultPanel;
    private JTable table1;
    private JScrollPane tree_pane;
    private JTree game_tree_field;

    GameTree game_tree;
    GameTreeNode root;
    GameTreeNode.GameRound round;
    SolverResult(GameTree game_tree,GameTreeNode root){
        this.game_tree = game_tree;
        this.root = root;
        this.round = root.getRound();
        /*
        DefaultMutableTreeNode treenode =new DefaultMutableTreeNode("教师学历信息");
        String Teachers[][]=new String[3][];
        Teachers[0]=new String[]{"王鹏","李曼","韩小国","穆保龄","尚凌云","范超峰"};
        Teachers[1]=new String[]{"胡会强","张春辉","宋芳","阳芳","朱山根","张茜","宋媛媛"};
        Teachers[2]=new String[]{"刘丹","张小芳","刘华亮","聂来","吴琼"};
        String gradeNames[]={"硕士学历","博士学历","博士后学历"};
        DefaultMutableTreeNode node=null;
        DefaultMutableTreeNode childNode=null;
        int length=0;

        for(int i=0;i<3;i++)
        {
            length=Teachers[i].length;
            node=new DefaultMutableTreeNode(gradeNames[i]);
            for (int j=0;j<length;j++)
            {
                childNode = new DefaultMutableTreeNode(Teachers[i][j]);
                node.add(childNode);
            }
            treenode.add(node);
        }
         */
        DefaultMutableTreeNode treenode = new DefaultMutableTreeNode(String.format("%s begin",GameTreeNode.gameRound2String(root.getRound())));
        reGenerateTree(this.root,treenode);
        JTree jtree_field = new JTree(treenode);
        game_tree_field.setModel(jtree_field.getModel());
    }

    class NodeDesc{
        NodeDesc(){

        }
    }

    void reGenerateTree(GameTreeNode node,DefaultMutableTreeNode parent){
        if(node.getRound() != this.root.getRound()) return;
        if(node instanceof ActionNode){
            ActionNode actionNode = (ActionNode) node;
            List<GameTreeNode> childs = actionNode.getChildrens();
            List<GameActions> actions = actionNode.getActions();

            assert(childs.size() == actions.size());
            for(int i = 0;i < childs.size();i ++){
                GameTreeNode one_child = childs.get(i);
                GameActions one_action = actions.get(i);
                DefaultMutableTreeNode one_tree_child = new DefaultMutableTreeNode(String.format("p%d %s",((ActionNode) node).getPlayer(),one_action.toString()));
                parent.add(one_tree_child);
                reGenerateTree(one_child,one_tree_child);
            }
        }
    }

    void set_tree(){
    }
}

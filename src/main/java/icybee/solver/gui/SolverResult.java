package icybee.solver.gui;

import icybee.solver.GameTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class SolverResult {
    public JPanel resultPanel;
    private JTable table1;
    private JScrollPane tree_pane;
    private JTree game_tree_field;

    GameTree game_tree;
    SolverResult(GameTree game_tree){
        this.game_tree = game_tree;
        DefaultMutableTreeNode root=new DefaultMutableTreeNode("教师学历信息");
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
            root.add(node);
        }
        JTree jtree_field = new JTree(root);
        game_tree_field.setModel(jtree_field.getModel());
    }

    void set_tree(){
    }
}

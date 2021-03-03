package icybee.solver.gui;

import icybee.solver.GameTree;
import icybee.solver.nodes.ActionNode;
import icybee.solver.nodes.GameActions;
import icybee.solver.nodes.GameTreeNode;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.List;

public class SolverResult {
    public JPanel resultPanel;
    private JTable strategy_table;
    private JScrollPane tree_pane;
    private JTree game_tree_field;

    GameTree game_tree;
    GameTreeNode root;
    GameTreeNode.GameRound round;

    class NodeDesc{
        GameTreeNode node;
        GameActions last_action;
        int action_ind;
        NodeDesc(GameTreeNode node,GameActions last_action,int action_ind){
            this.node = node;
            this.last_action = last_action;
            this.action_ind = action_ind;
        }

        @Override
        public String toString() {
            if(this.last_action == null) {
                return String.format("%s begin", GameTreeNode.gameRound2String(this.node.getRound()));
            }else{
                return String.format("p%d %s",((ActionNode) this.node).getPlayer(),last_action.toString());
            }
        }
    }

    SolverResult(GameTree game_tree,GameTreeNode root){
        this.game_tree = game_tree;
        this.root = root;
        this.round = root.getRound();
        DefaultMutableTreeNode treenode = new DefaultMutableTreeNode();
        treenode.setUserObject(new NodeDesc(root,null,0));
        reGenerateTree(this.root,treenode);
        JTree jtree_field = new JTree(treenode);
        game_tree_field.setModel(jtree_field.getModel());

        game_tree_field.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        game_tree_field.getLastSelectedPathComponent();

                /* if nothing is selected */
                if (node == null) return;
                Object nodeInfoObject = node.getUserObject();
                NodeDesc nodeinfo = (NodeDesc) nodeInfoObject;
                if(nodeinfo.last_action == null) return;
                System.out.println(nodeinfo.last_action.toString());

            }
        });
        construct_inital_table();
    }

    void construct_inital_table(){
        String[] columnName;
        String[][] data;
        if (this.game_tree.getDeck().getCards().size() == 52){
            columnName = new String[]{"A","K","Q","J","T","9","8","7","6","5","4","3","2"};
        }else{
            throw new RuntimeException(String.format("deck size %d unknown",this.game_tree.getDeck().getCards().size()));
        }

        data = new String[columnName.length][columnName.length];
        for(int i = 0;i < columnName.length;i ++) {
            boolean s_start = false;
            for(int j = 0;j < columnName.length;j ++) {
                if(j == i){s_start = true;data[i][j] = String.format("%s%s",columnName[i],columnName[j]);}
                else if(s_start) data[i][j] = String.format("%s%ss",columnName[i],columnName[j]);
                else data[i][j] = String.format("%s%so",columnName[j],columnName[i]);
            }
        }

        DefaultTableModel defaultTableModel = new DefaultTableModel(data, columnName);
        strategy_table.setModel(defaultTableModel);
        strategy_table.setTableHeader(null);
        strategy_table.setShowHorizontalLines(true);
        strategy_table.setShowVerticalLines(true);
        strategy_table.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        strategy_table.setGridColor(Color.BLACK);
        strategy_table.setRowHeight(27);
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
                DefaultMutableTreeNode one_tree_child = new DefaultMutableTreeNode();

                one_tree_child.setUserObject(new NodeDesc(root,one_action,i));
                parent.add(one_tree_child);
                reGenerateTree(one_child,one_tree_child);
            }
        }
    }

    void set_tree(){
    }
}

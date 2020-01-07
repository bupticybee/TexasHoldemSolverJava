package icybee.riversolver;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import icybee.riversolver.exceptions.ActionNotFoundException;
import icybee.riversolver.exceptions.NodeLengthMismatchException;
import icybee.riversolver.exceptions.NodeNotFoundException;
import icybee.riversolver.exceptions.RoundNotFoundException;
import icybee.riversolver.nodes.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contains code for GameTree construction
 */
public class GameTree {
    String tree_json_dir;
    GameTreeNode root = null;
    Deck deck;

    public GameTreeNode getRoot() {
        return root;
    }

    private static String readAllBytes(String filePath) throws IOException
    {
        String content;
        content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        return content;
    }

    GameTreeNode.GameRound strToGameRound(String round){
        GameTreeNode.GameRound game_round;
        switch(round){
            case "preflop":{
                game_round = GameTreeNode.GameRound.PREFLOP;
                break;
            }
            case "flop":{
                game_round = GameTreeNode.GameRound.FLOP;
                break;
            }
            case "turn":{
                game_round = GameTreeNode.GameRound.TURN;
                break;
            }
            case "river":{
                game_round = GameTreeNode.GameRound.RIVER;
                break;
            }
            default:{
                throw new RoundNotFoundException(String.format("round %s not found",round));
            }
        }
        return game_round;
    }

    ActionNode generateActionNode(Map meta,List<String> childrens_actions, List<Map> childrens_nodes, String round,GameTreeNode parent){
        if(childrens_actions.size() != childrens_nodes.size()){
            throw new NodeLengthMismatchException(
                    String.format(
                            "mismatch when generate ActionNode, childrens_action length %d children_nodes length %d",
                            childrens_actions.size(),
                            childrens_nodes.size()
                            ));
        }

        if(! (round.equals("preflop")
                || round.equals("flop")
                || round.equals("turn")
                || round.equals("river"))
                ){
            throw new RoundNotFoundException(String.format("round %s not found",round));
        }

        List<GameActions> actions = new ArrayList<>();
        List<GameTreeNode> childrens = new ArrayList<>();

        // 遍历所有children actions 来生成GameAction 的list，用于初始化ActionNode
        for(int i = 0;i < childrens_actions.size();i++){
            String one_action = childrens_actions.get(i);
            Map one_children_map = childrens_nodes.get(i);
            if(one_action == null) throw new ActionNotFoundException("action is null");
            if(one_children_map == null) throw new RuntimeException("one_children_map is null");

            GameTreeNode.PokerActions action;
            Double amount = null;
            switch(one_action){
                case "check":{
                    action = GameTreeNode.PokerActions.CHECK;
                    break;
                }
                case "fold":{
                    action = GameTreeNode.PokerActions.FOLD;
                    break;
                }
                case "call":{
                    action = GameTreeNode.PokerActions.CALL;
                    break;
                }
                default:{
                    if(one_action.contains("bet")){
                        String[] action_sp = one_action.split("_");
                        if(action_sp.length != 2) throw new RuntimeException(String.format("action sp length %d",action_sp.length));
                        String action_str = action_sp[0];
                        action = GameTreeNode.PokerActions.BET;
                        if(!action_str.equals("bet")) throw new ActionNotFoundException(String.format("Action %s not found",action_str));
                        amount = Double.valueOf(action_sp[1]);

                    }else if (one_action.contains("raise")){
                        String[] action_sp = one_action.split("_");
                        if(action_sp.length != 2) throw new RuntimeException(String.format("action sp length %d",action_sp.length));
                        String action_str = action_sp[0];
                        action = GameTreeNode.PokerActions.RAISE;
                        if(!action_str.equals("raise")) throw new ActionNotFoundException(String.format("Action %s not found",action_str));
                        amount = Double.valueOf(action_sp[1]);
                    }else{
                        throw new ActionNotFoundException(String.format("%s action not found",one_action));
                    }
                }
            }// end of switch
            GameTreeNode one_children_node = recurrentGenerateTreeNode(one_children_map,parent);
            childrens.add(one_children_node);

            GameActions game_action = new GameActions(action,amount);
            actions.add(game_action);
        }
        Integer player = (Integer)meta.get("player");
        Double pot = ((Integer)meta.get("pot")).doubleValue();
        if(player == null) throw new RuntimeException("player is null");
        if(childrens.size() != actions.size()){
            throw new NodeLengthMismatchException(String.format("childrens length %d, actions length %d"
                    ,childrens.size()
                    ,actions.size()));
        }
        GameTreeNode.GameRound game_round = strToGameRound(round);
        ActionNode actionNode = new ActionNode(actions,childrens,player,game_round,pot,parent);
        for(GameTreeNode one_node: childrens){
            one_node.setParent(actionNode);
        }
        return actionNode;
    }

    void convertObject2Double(List<Object> from,Double[] to){
        for(int winner_player = 0;winner_player < from.size();winner_player ++){
            Object tmp_payoff = from.get(winner_player);
            if(tmp_payoff instanceof Integer){
                to[winner_player] = ((Integer)tmp_payoff).doubleValue();
            }else if(tmp_payoff instanceof Double){
                to[winner_player] = ((Double)tmp_payoff);
            }else{
                throw new RuntimeException(String.format("payoff data type %s not underestood",tmp_payoff.toString()));
            }

        }
    }

    ShowdownNode generateShowdownNode(Map<String,Object> meta, String round,GameTreeNode parent){
        Map<String,Object> meta_payoffs = (Map)meta.get("payoffs");
        List<Object> tmp_tie_payoffs = (List<Object>)meta_payoffs.get("tie");
        Double[] tie_payoffs = new Double[tmp_tie_payoffs.size()];
        convertObject2Double(tmp_tie_payoffs,tie_payoffs);

        // meta_payoffs 的key有 n个玩家+1个平局,代表某个玩家赢了的时候如何分配payoff
        Double[][] player_payoffs = new Double[meta_payoffs.keySet().size() - 1][meta_payoffs.keySet().size() - 1];
        Double pot = ((Integer)meta.get("pot")).doubleValue();

        for(String one_player : meta_payoffs.keySet()){
            if(one_player.equals("tie")){
                continue;
            }
            // 获胜玩家id
            Integer player_id = Integer.valueOf(one_player);
            if(player_id == null) throw new RuntimeException("player id json convert fail");

            // 玩家在当前Showdown节点能获得的收益
            List<Object> tmp_payoffs =  (List<Object>)meta_payoffs.get(one_player);
            Double[] player_payoff = new Double[tmp_payoffs.size()];

            convertObject2Double(tmp_payoffs,player_payoff);

            player_payoffs[Integer.valueOf(one_player)] = player_payoff;
        }
        GameTreeNode.GameRound game_round = strToGameRound(round);
        return new ShowdownNode(tie_payoffs,player_payoffs,game_round,pot,parent);
    }

    TerminalNode generateTerminalNode(Map meta, String round,GameTreeNode parent){
        List<Object> player_payoff_list = (List<Object>)meta.get("payoff");
        Double[] player_payoff = new Double[player_payoff_list.size()];
        for(int one_player = 0;one_player < player_payoff_list.size();one_player ++){

            Object tmp_payoff = player_payoff_list.get(one_player);
            if(tmp_payoff instanceof Integer){
                player_payoff[one_player] = ((Integer)tmp_payoff).doubleValue();
            }else if(tmp_payoff instanceof Double){
                player_payoff[one_player] = (Double)tmp_payoff;
            }else{
                throw new RuntimeException(String.format("payoff data type %s not underestood",tmp_payoff.toString()));
            }
        }

        //节点上的下注额度
        Double pot = ((Integer)meta.get("pot")).doubleValue();

        GameTreeNode.GameRound game_round = strToGameRound(round);
        // 多人游戏的时候winner就不等于当前节点的玩家了，这里要注意
        Integer player = (Integer)meta.get("player");
        if(player == null) throw new RuntimeException("player is null");
        return new TerminalNode(player_payoff,player,game_round,pot,parent);
    }

    ChanceNode generateChanceNode(Map meta,Map child,String round,GameTreeNode parent){
        //节点上的下注额度
        Double pot = ((Integer)meta.get("pot")).doubleValue();
        List<GameTreeNode> childrens = new ArrayList<>();
        for(Card one_card:this.deck.getCards()){
            GameTreeNode one_node = recurrentGenerateTreeNode(child,null);
            childrens.add(one_node);
        }
        GameTreeNode.GameRound game_round = strToGameRound(round);
        ChanceNode chanceNode = new ChanceNode(childrens,game_round,pot,parent,this.deck.getCards());
        for(GameTreeNode gameTreeNode: chanceNode.getChildrens()){
            gameTreeNode.setParent(chanceNode);
        }
        return chanceNode;
    }

    GameTreeNode recurrentGenerateTreeNode(Map<String, Map> node_json,GameTreeNode parent) throws NullPointerException{
        Map<String,Object> meta = node_json.get("meta");
        if(meta == null){
            throw new NullPointerException("node json get meta null pointer");
        }
        String round = (String)meta.get("round");
        if(round == null){
            throw new NullPointerException("node json get round null pointer");
        }

        if(! (round.equals("preflop")
                || round.equals("flop")
                || round.equals("turn")
                || round.equals("river"))
                ){
            throw new RoundNotFoundException(String.format("round %s not found",round));
        }

        String node_type = (String)meta.get("node_type");
        if(node_type == null){
            throw new NullPointerException("node json get round null pointer");
        }
        if (! (node_type.equals("Terminal")
                || node_type.equals("Showdown")
                || node_type.equals( "Action")
                || node_type.equals( "Chance")
        )
                ){
            throw new NodeNotFoundException(String.format("node type %s not found",node_type));
        }

        switch(node_type){
            case "Action": {
                // 孩子节点的动作，存在list里
                List<String> childrens_actions = (List<String>) node_json.get("children_actions");
                if (childrens_actions == null) {
                    throw new NullPointerException("action node children null pointer");
                }

                // 孩子节点本身，同样存在list里,和上面的children_actions 一一对应,事实上两者的长度一致
                List<Map> childrens = (List<Map>) node_json.get("children");
                if (childrens == null) {
                    throw new NullPointerException("action node children_actions null pointer");
                }
                if (childrens.size() != childrens_actions.size()) {
                    throw new NodeLengthMismatchException("action node child length mismatch");
                }
                return this.generateActionNode(meta, childrens_actions, childrens, round,parent);
            }
            case "Showdown": {
                return this.generateShowdownNode(meta, round,parent);
            }
            case "Terminal": {
                return this.generateTerminalNode(meta, round,parent);
            }
            case "Chance": {
                List<Map> childrens = (List<Map>) node_json.get("children");
                if(childrens.size() != 1) throw new RuntimeException("Chance node should have only one child");
                return this.generateChanceNode(meta, childrens.get(0), round,parent);
            }
            default:{
                throw new NodeNotFoundException(String.format("node type %s not found",node_type));
            }
        }
    }

    public GameTree(String tree_json_dir,Deck deck) throws IOException{
        this.tree_json_dir = tree_json_dir;
        ObjectMapper mapper = new ObjectMapper();

        String file_content = readAllBytes(tree_json_dir);
        Map<String, Map> json_map = (Map<String, Map>)mapper.readValue(file_content, Map.class);
        Map<String, Map> json_root = (Map<String, Map>)json_map.get("root");
        this.deck = deck;
        this.root = recurrentGenerateTreeNode(json_root,null);
    }

    void recurrentPrintTree(GameTreeNode node,int depth,int depth_limit) throws ClassCastException{
        if(depth_limit != -1 && depth >= depth_limit){
            return;
        }

        if(node instanceof ActionNode){
            List<GameTreeNode> childrens = ((ActionNode)node).getChildrens();
            List<GameActions> actions = ((ActionNode)node).getActions();

            for(int i = 0;i < childrens.size();i++){
                GameTreeNode one_child = childrens.get(i);
                GameActions one_action = actions.get(i);

                String prefix = "";
                for(int j = 0;j < depth;j++) prefix += "\t";
                System.out.println(String.format(
                        "%sp%s: %s",prefix,((ActionNode)node).getPlayer(),one_action.toString()
                ));
                recurrentPrintTree(one_child,depth + 1,depth_limit);
            }
        }else if(node instanceof ShowdownNode){
            ShowdownNode showdown_node = (ShowdownNode)node;
            String prefix = "";
            for(int j = 0;j < depth;j++) prefix += "\t";
            System.out.println(String.format(
                    "%s SHOWDOWN pot %f ",prefix,showdown_node.getPot()
            ));

            prefix += "\t";
            for(int i = 0;i < showdown_node.get_payoffs(ShowdownNode.ShowDownResult.TIE,null).length;i++) {
                System.out.print(String.format("%sif player %d wins, payoff :", prefix,i));
                Double[] payoffs = showdown_node.get_payoffs(ShowdownNode.ShowDownResult.NOTTIE, i);

                for (int player_id = 0; player_id < payoffs.length; player_id++) {
                    System.out.print(
                            String.format(" p%d %f ", player_id, payoffs[player_id])
                    );
                }
                System.out.println();
            }
            System.out.print(String.format("%sif Tie, payoff :", prefix));
            Double[] payoffs = showdown_node.get_payoffs(ShowdownNode.ShowDownResult.TIE, null);

            for (int player_id = 0; player_id < payoffs.length; player_id++) {
                System.out.print(
                        String.format(" p%d %f ", player_id, payoffs[player_id])
                );
            }
            System.out.println();
        }else if(node instanceof  TerminalNode){
            TerminalNode terminal_node = (TerminalNode)node;
            String prefix = "";
            for(int j = 0;j < depth;j++) prefix += "\t";
            System.out.println(String.format(
                    "%s TERMINAL pot %f ",prefix,terminal_node.getPot()
            ));

            prefix += "\t";
            System.out.print(String.format("%sTerminal payoff :", prefix));
            Double[] payoffs = terminal_node.get_payoffs();

            for (int player_id = 0; player_id < payoffs.length; player_id++) {
                System.out.print(
                        String.format(" p%d %f ", player_id, payoffs[player_id])
                );
            }
            System.out.println();

        }
    }

    public void printTree(int depth){
        if(depth < -1 || depth == 0){
            throw new RuntimeException("depth can only be -1 or positive");
        }
        recurrentPrintTree(this.root,0,depth);
    }

    private JSONObject reConvertJson(GameTreeNode node){
        if(node instanceof ActionNode) {
            ActionNode one_node = (ActionNode) node;
            JSONObject retjson = new JSONObject();

            List<String> actions_str = new ArrayList<>();
            for(GameActions one_action:one_node.getActions()) actions_str.add(one_action.toString());

            retjson.put("actions",actions_str);
            retjson.put("player",one_node.getPlayer());

            JSONObject childrens = null;

            for(int i = 0;i < one_node.getActions().size();i ++){
                GameActions one_action = one_node.getActions().get(i);
                GameTreeNode one_child = one_node.getChildrens().get(i);

                JSONObject one_json = this.reConvertJson(one_child);
                if(one_json != null){
                    if(childrens == null) childrens = new JSONObject();
                    childrens.put(one_action.toString(),one_json);
                }
            }
            if(childrens != null) {
                retjson.put("childrens", childrens);
            }
            retjson.put("strategy",one_node.getTrainable().dumps(false));
            retjson.put("node_type","action_node");
            return retjson;

        }else if(node instanceof TerminalNode){
            return null;
        }else if(node instanceof ShowdownNode){
            return null;
        }else if(node instanceof ChanceNode){
            // TODO 写这里的策略导出
            ChanceNode chanceNode = (ChanceNode)node;
            List<Card> cards = chanceNode.getCards();
            List<GameTreeNode> childerns = chanceNode.getChildrens();
            if(cards.size() != childerns.size())
                throw new RuntimeException("length not match");
            JSONObject retjson = new JSONObject();
            List<String> card_strs = new ArrayList<>();
            for(Card card:cards)
                card_strs.add(card.toString());

            JSONObject dealcards = new JSONObject();
            for(int i = 0;i < cards.size();i ++){
                Card one_card = cards.get(i);
                GameTreeNode gameTreeNode = childerns.get(i);
                dealcards.put(one_card.toString(),this.reConvertJson(gameTreeNode));
            }

            retjson.put("deal_cards",dealcards);
            retjson.put("deal_number",dealcards.size());
            retjson.put("node_type","chance_node");
            return retjson;
        }else{
            throw new RuntimeException();
        }
    }

    public JSONObject dumps(boolean with_status){
        if(with_status == true) throw new NotImplementedException();
        return this.reConvertJson(this.root);
    }

}

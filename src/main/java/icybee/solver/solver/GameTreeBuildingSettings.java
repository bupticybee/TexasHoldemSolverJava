package icybee.solver.solver;

import icybee.solver.nodes.GameTreeNode;

public class GameTreeBuildingSettings {
    public static class StreetSetting{
        public float[] bet_sizes;
        public float[] raise_sizes;
        public float[] donk_sizes;
        public boolean allin;

        public StreetSetting(float[] bet_sizes, float[] raise_sizes, float[] donk_sizes, boolean allin) {
            this.bet_sizes = bet_sizes;
            this.raise_sizes = raise_sizes;
            this.donk_sizes = donk_sizes;
            this.allin = allin;
        }
    }
    public StreetSetting flop_ip;
    public StreetSetting turn_ip;
    public StreetSetting river_ip;

    public StreetSetting flop_oop;
    public StreetSetting turn_oop;
    public StreetSetting river_oop;

    public GameTreeBuildingSettings(
            StreetSetting flop_ip,
            StreetSetting turn_ip,
            StreetSetting river_ip,
            StreetSetting flop_oop,
            StreetSetting turn_oop,
            StreetSetting river_oop) {
        this.flop_ip = flop_ip;
        this.turn_ip = turn_ip;
        this.river_ip = river_ip;
        this.flop_oop = flop_oop;
        this.turn_oop = turn_oop;
        this.river_oop = river_oop;

    }

    public StreetSetting getSettings(GameTreeNode.GameRound round,int player){
        if(!(player == 0 || player == 1)) throw new RuntimeException(String.format("player %s not known",player));
        if(round == GameTreeNode.GameRound.RIVER && player == 0) return this.river_ip;
        else if(round == GameTreeNode.GameRound.TURN && player == 0) return this.turn_ip;
        else if(round == GameTreeNode.GameRound.FLOP && player == 0) return this.flop_ip;
        else if(round == GameTreeNode.GameRound.RIVER && player == 1) return this.river_oop;
        else if(round == GameTreeNode.GameRound.TURN && player == 1) return this.turn_oop;
        else if(round == GameTreeNode.GameRound.FLOP && player == 1) return this.flop_oop;
        else throw new RuntimeException(String.format("player %s and round not known",player));
    }

}

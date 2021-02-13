package icybee.solver.ranges;

/**
 * Created by huangxuefeng on 2019/10/11.
 * river ranges code
 */
public class RiverCombs implements Comparable<RiverCombs>{
    int[] board;
    public int rank;
    public PrivateCards private_cards;
    public int reach_prob_index;
    //public float reachprob;
    public RiverCombs(int[] board,PrivateCards private_cards , int rank, int reach_prob_index) {
        this.board = board;
        this.rank = rank;
        this.private_cards = private_cards;
        this.reach_prob_index = reach_prob_index;
    }

    @Override
    public int compareTo(RiverCombs o) {
        if (this.rank < o.rank) // if a's rank is smaller than b's , a win b lose
            return 1;
        if (this.rank > o.rank)
            return -1;
        return 0;
    }

}

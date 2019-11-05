package icybee.riversolver.utils;

import icybee.riversolver.ranges.PrivateCards;

import java.util.Arrays;
import java.util.List;

public class PrivateRangeConverter {
    public static PrivateCards[] rangeStr2Cards(String range_str){
        List<String> range_list = Arrays.asList(range_str.split(","));
        PrivateCards[] private_cards = new PrivateCards[range_list.size()];

        for(String one_range:range_list){
            int range_len = one_range.length();
            // TODO finish here , convert str to a PrivateRanges[]
        }

        return private_cards;
    }
}

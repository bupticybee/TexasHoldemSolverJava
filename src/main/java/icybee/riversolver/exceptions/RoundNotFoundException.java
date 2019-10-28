package icybee.riversolver.exceptions;

/**
 * Created by huangxuefeng on 2019/10/7.
 * This file contains code for round not found exception, where program trys to create a round that doesn't exist
 */
public class RoundNotFoundException extends RuntimeException{
    public RoundNotFoundException(String errmsg){
        super(errmsg);
    }
}

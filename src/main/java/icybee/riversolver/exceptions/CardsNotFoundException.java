package icybee.riversolver.exceptions;

/**
 * Created by huangxuefeng on 2019/10/6.
 * this file cotains code for a custom exception
 */
public class CardsNotFoundException
extends RuntimeException{
    public CardsNotFoundException(String errmsg){
        super(errmsg);
    }
}

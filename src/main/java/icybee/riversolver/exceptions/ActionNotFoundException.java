package icybee.riversolver.exceptions;

/**
 * Created by huangxuefeng on 2019/10/7.
 * When parsing json file, if the program found an abnormal action ,this exception will be called.
 */
public class ActionNotFoundException extends RuntimeException{
    public ActionNotFoundException(String errmsg){
        super(errmsg);
    }
}

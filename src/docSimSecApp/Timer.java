package docSimSecApp;

import java.util.Date;

/**
 * Created by nuplavikar on 6/8/16.
 */
public class Timer {

    /*This would return time in milliseconds passed from epoch*/
    long startTimer()
    {
        //Time start
        Date date= new Date();
        //getTime() returns current time in milliseconds
        return date.getTime();
    }

    /*This should give the difference in milliseconds*/
    long endTimer(long startingTime)
    {
        Date date= new Date();
        //getTime() returns current time in milliseconds
        long endingTime =  date.getTime();
        return (endingTime - startingTime);
    }


    /*return Milli-Seconds from time in milliSeconds*/
    long getMilliSeconds(long timeInMillis)
    {
        return (timeInMillis%1000);
    }

    /*return Seconds from milliSeconds*/
    long getSeconds(long timeInMillis)
    {
        return (timeInMillis/1000);
    }

    /*return Minutes from milliSeconds*/
    long getMinutes(long timeInMillis)
    {
        return getSeconds(timeInMillis)/60;
    }

    /*return Hours from milliSeconds*/
    long getHours(long timeInMillis)
    {
        return getMinutes(timeInMillis)/60;
    }

    String getFormattedTime(String someStr, long timeInMillis)
    {
        return someStr + " = <"+this.getHours(timeInMillis)+":"+this.getMinutes(timeInMillis)+":"+this.getSeconds(timeInMillis)+"."+this.getMilliSeconds(timeInMillis)+">";
    }
}

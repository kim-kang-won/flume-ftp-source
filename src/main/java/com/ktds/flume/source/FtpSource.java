package com.ktds.flume.source;

import com.ktds.flume.source.factory.UFtpTaskFactory;
import com.ktds.flume.source.uftp.UFtp;
import com.ktds.flume.source.util.UFtpUtil;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Timer;

/**
 * Created by kw.kim on 2015-07-13.
 */
public class FtpSource extends AbstractSource implements Configurable, EventDrivenSource {


    private static final Logger LOGGER = LoggerFactory.getLogger(FtpSource.class);

    private Context context;
    private int     period = 0;
    private String  legacy = "";
    private String  pause = "";

    private UFtp ftpTask;

    /**
     *
     * @param context
     */
    @Override
    public void configure(Context context){

        LOGGER.info(legacy + " " + "Set Configure of source ...", this.getName());

        this.context = context;
        legacy = context.getString("legacy");
        period = Integer.parseInt(context.getString("schedule.repeat_period"));
        pause = context.getString("pause", "N").toUpperCase();

    }

    /**
     * @return void
     */
    @Override
    public synchronized void start() {
        LOGGER.info(legacy + " " + "Starting Flume source ...", this.getName());

        try {

            ftpTask = new UFtpTaskFactory(context).getFtpTask();

            if (pause.equals("N")) {
                Date date = UFtpUtil.getJobStartDate(context, 1);
                if (UFtpUtil.isInTime(context))
                    new Timer().scheduleAtFixedRate(ftpTask, date, period * 1000);  // run on Period Seconds from cal.getTime()
                else
                    new Timer().scheduleAtFixedRate(ftpTask, 5 * 1000, period * 1000);
            } else {
                LOGGER.info(legacy + " " + "**** Pause is true ****");
                LOGGER.info(legacy + " " + "**** Flume Ftp-Task is Pause !!!! ****");
                Date date = UFtpUtil.getJobStartDate(context, 9999);
                new Timer(true).scheduleAtFixedRate(ftpTask, date, 9999 * 1000);
            }
        }catch (Exception e) {
            LOGGER.info(legacy + " " + "Exception in Starting:", e.getMessage());
        }

        super.start();
    }


    /**
     * @return void
     */
    @Override
    public synchronized void stop() {
        LOGGER.info(legacy + " " + "Stopping source ...", this.getName());
        super.stop();
        ftpTask.cancel();
    }


}

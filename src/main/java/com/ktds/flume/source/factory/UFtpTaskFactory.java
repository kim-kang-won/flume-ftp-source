package com.ktds.flume.source.factory;

import com.ktds.flume.source.uftp.FtpTask;
import com.ktds.flume.source.uftp.SFtpTask;
import com.ktds.flume.source.uftp.UFtp;
import org.apache.flume.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kw.kim on 2015-07-23.
 */
public class UFtpTaskFactory {


    private String ftpType = "";
    private String legacy="";
    private Context context;
    private UFtp ftpTask;

    private static final Logger LOGGER = LoggerFactory.getLogger(UFtpTaskFactory.class);

    public UFtpTaskFactory(Context ctx) {

        this.context = ctx;
        legacy = context.getString("legacy");
        ftpType = context.getString("ftp_type", "none");

    }

    public UFtp getFtpTask() throws Exception{

        if(ftpType.equals("sftp")) {        // SFTP Case
            ftpTask = new SFtpTask(context);
        }else if(ftpType.equals("ftp")) {  // FTP Case
            ftpTask = new FtpTask(context);
        }else {
            // To do
            LOGGER.info("Not Support FTP Protocol Type!!!");
        }

        return ftpTask;
    }


}

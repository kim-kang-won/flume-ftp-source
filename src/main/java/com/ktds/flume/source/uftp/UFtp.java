package com.ktds.flume.source.uftp;

import com.ktds.flume.source.util.UFtpUtil;
import org.apache.flume.Context;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by kw.kim on 2015-07-23.
 */
public abstract class UFtp<T> extends TimerTask {

    protected String server;
    protected String legacy;
    protected String port;
    protected String password ;
    protected String username;
    protected String remoteDir;
    protected String localDir;
    protected String localMoveDir;
    protected String remoteFileName;
    protected String remoteFileNamePattern;
    protected String remoteFinishedFilePostFix;
    protected String remoteDownloadedFilePostFix;
    protected String ftp_mode;
    protected String remoteFileEncoding;
    protected String localFileEncoding;
    protected String timeOut;
    protected String isCheckFile;
    protected String cleanCheckFileAfterDay;

    boolean isLogin = false;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UFtp.class);

    public abstract boolean login();

    public abstract boolean logout();

    public abstract boolean renameRemoteFileDir(String sourceName, String targetName ) throws Exception;

    public abstract List<T> getList(String serverDir) throws Exception;

    public abstract boolean downloadFile (String dir, String downloadFileName) throws Exception;

    public abstract boolean downloadMultiFiles(String dir, List downloadFileNames, String localDir) throws Exception;



    public void setServer(String server){
        this.server = server;
    }

    public void setPort(String port){
        this.port = port;
    }

    public void setUser(String username , String password){
        this.username = username;
        this.password = password;
    }

    public void setConfig(Context context) throws Exception {

        legacy      = context.getString("legacy", "");
        server      = context.getString("server", "");
        port        = context.getString("port", "");
        username    = context.getString("user_name", "");
        password    = context.getString("password", "");
        remoteDir   = context.getString("remote_dir", "");
        localDir    = context.getString("local_dir", "");
        localMoveDir = context.getString("local_move_dir", "");
        remoteFileName = context.getString("remote_file_name", "");
        remoteFinishedFilePostFix = context.getString("remote_finished_file_postfix", "");
        remoteDownloadedFilePostFix = context.getString("remote_downloaded_file_postfix", "");
        ftp_mode = context.getString("ftp_mode", "local_passive");
        remoteFileEncoding = context.getString("remote_file_encoding", "");
        localFileEncoding = context.getString("local_file_encoding", "");
        remoteFileNamePattern = context.getString("remote_file_name_pattern", "");
        timeOut = context.getString("time_out", "300"); //sec
        isCheckFile = context.getString("is_check_file", "false");
        cleanCheckFileAfterDay = context.getString("clean_check_file_after_day", "3");


        UFtpUtil.checkLocalDir(localDir);

        //Rename to FineName & Path
        if(remoteDir.length() > 0) remoteDir = UFtpUtil.renameFile(remoteDir);
        if(remoteFileName.length() > 0) remoteFileName = UFtpUtil.renameFile(remoteFileName);
    }

    public void init(){

        setServer(server);
        setPort(port);
        setUser(username, password);

        if(login()) {
            LOGGER.info(legacy + " "+ "FTP Login is success!!!");
            isLogin = true;
        }else{
            LOGGER.info(legacy + " "+ "FTP Login is failed!!");
            isLogin = false;
        }

        if(isCheckFile.equals("true")) {
            try {
                UFtpUtil.cleanCheckFile(localDir + "/checkFiles", cleanCheckFileAfterDay);
            }catch(Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }


    public void run() {

        try {
            init();

            if(!isLogin) throw new Exception("FTP Login Fail!");

            String downloadedFileName ;

            if(remoteFileName.length() > 0 ) {

                boolean dupCheckResult = false;

                if (isCheckFile.equals("true")) {
                    if (!UFtpUtil.isDuplicateFileCheck(remoteFileName, this.localDir + "/" + "checkFiles")) {
                        dupCheckResult = true;
                    }
                }

                if (dupCheckResult) {
                    if (this.downloadFile(this.remoteDir, this.remoteFileName)) {
                        downloadedFileName = this.localDir + "/" + this.remoteFileName;

                        if (remoteFileEncoding.length() > 0) downloadedFileName = transFileEncoding(downloadedFileName);

                        if (UFtpUtil.moveLocalFile(downloadedFileName, this.localMoveDir)){
                            LOGGER.info(legacy + " " + "@ Complete - Move Downloaded File to SpoolDir");

                            if (isCheckFile.equals("true"))
                                UFtpUtil.makeChkFile(this.localDir + "/" + "checkFiles", this.remoteFileName);
                        }

                        if (remoteDownloadedFilePostFix.length() > 0) {
                            renameRemoteFileDir(remoteFileName, remoteFileName + "_" + new Date().getTime() + remoteDownloadedFilePostFix);
                            if (remoteFinishedFilePostFix.length() > 0)
                                renameRemoteFileDir(remoteFileName + remoteFinishedFilePostFix, remoteFileName + remoteFinishedFilePostFix + "_" + new Date().getTime() + remoteDownloadedFilePostFix);
                            LOGGER.info(legacy + " " + "@ Complete - Rename remote sourFile");
                        }

                    } else {
                        LOGGER.info(legacy + " " + "@ No Searching File For The Download");
                    }
                }else {
                    LOGGER.info(legacy + " @ " + remoteFileName + " was downloaded already");
                }
            }else {

                List fileList = this.getList(this.remoteDir);

                List remakeFileList = new ArrayList();

                if(isCheckFile.equals("true")) {

                    for (int i = 0; i < fileList.size(); i++) {
                        if (!UFtpUtil.isDuplicateFileCheck(fileList.get(i).toString(), this.localDir + "/" + "checkFiles")) {
                            remakeFileList.add(fileList.get(i).toString());
                        }
                    }
                }

                if (remakeFileList.size() > 0) {

                    if (this.downloadMultiFiles(this.remoteDir, remakeFileList, this.localDir)) {
                        // 파일 Move
                        for (int i = 0; i < remakeFileList.size(); i++) {

                            downloadedFileName = this.localDir + "/" + remakeFileList.get(i).toString();

                            if (remoteFileEncoding.length() > 0)
                                downloadedFileName = transFileEncoding(downloadedFileName);

                            if(UFtpUtil.moveLocalFile(downloadedFileName, this.localMoveDir)) {
                                LOGGER.info(legacy + " " + "# Complete - Move Downloaded File to SpoolDir");


                                if (isCheckFile.equals("true"))
                                    UFtpUtil.makeChkFile(this.localDir + "/" + "checkFiles", remakeFileList.get(i).toString());
                            }

                            if (remoteDownloadedFilePostFix.length() > 0) {
                                renameRemoteFileDir(remakeFileList.get(i).toString(), remakeFileList.get(i).toString() + "_" + new Date().getTime() + remoteDownloadedFilePostFix);
                                if (remoteFinishedFilePostFix.length() > 0)
                                    renameRemoteFileDir(remakeFileList.get(i).toString() + remoteFinishedFilePostFix, remakeFileList.get(i).toString() + remoteFinishedFilePostFix + "_" + new Date().getTime() + remoteDownloadedFilePostFix);
                                LOGGER.info(legacy + " " + "# Complete - Rename remote sourFile");
                            }
                        }
                    } else {
                        LOGGER.info(legacy + " "+ "# No Searching File For The Download");
                    }
                }else {
                    LOGGER.info(legacy + " "+ "# No Searching File For The Download");
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }finally {
            this.logout();
        }
    }

    public String transFileEncoding(String fileName) throws Exception {

        LOGGER.info(legacy + " " + "@ File Encoding from " + remoteFileEncoding + " to " + localFileEncoding);

        File originDownloadedFile = new File(fileName);

        String transFileName = fileName +"_encoding_" + localFileEncoding;
        File transEncodingFile = new File(transFileName);

        UFtpUtil.convertEncoding(originDownloadedFile, transEncodingFile, remoteFileEncoding, localFileEncoding);
        UFtpUtil.removeFile(fileName);

        return transFileName;
    }


}

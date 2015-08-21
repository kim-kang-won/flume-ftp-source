package com.ktds.flume.source.uftp;

import com.ktds.flume.source.util.UFtpUtil;
import org.apache.commons.net.ftp.*;
import org.apache.flume.Context;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by kw.kim on 2015-07-13.
 */
public class FtpTask extends UFtp<ArrayList>{

    FTPClient ftpClient;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FtpTask.class);

    public FtpTask(Context ctx) throws Exception  {
        setConfig(ctx);

        ftpClient = new FTPClient();
    }

    public boolean login() {

        boolean isLogin = false;
        try {

            ftpClient.setDataTimeout(Integer.parseInt(timeOut) * 1000); //5Min
            ftpClient.setDefaultTimeout(Integer.parseInt(timeOut) * 1000); //5Min
            ftpClient.setControlKeepAliveTimeout(30 * 1000);
            ftpClient.setBufferSize(1024 * 1024);

            ftpClient.connect(server, Integer.parseInt(port));
            int reply = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                LOGGER.info("FTP server refused connection." + reply);
                isLogin = false;
            } else {
                ftpClient.login(username, password);
                LOGGER.info("FTP logined~! :" + this.server);


                if(ftp_mode.equals("local_active")) {
                    ftpClient.enterLocalActiveMode();
                } else if (ftp_mode.equals("local_passive")) {
                    ftpClient.enterLocalPassiveMode();
                } else if (ftp_mode.equals("remote_passive")) {
                    ftpClient.enterRemotePassiveMode();
                }

                //ftpClient.setKeepAlive(true);
                LOGGER.info("working dir of userHome :" + ftpClient.printWorkingDirectory());


                LOGGER.info("#####################login()########################");
                LOGGER.info(legacy + " Reply Code:" + ftpClient.getReplyCode());
                LOGGER.info(legacy + " Reply Message:" + ftpClient.getReplyString());
                LOGGER.info("Status:" + ftpClient.getStatus());

                isLogin = true;
            }

        }catch (Exception e) {
            isLogin = false;
            LOGGER.error("Failed Login: " + e.getMessage());
        }

        return isLogin;
    }

    public boolean logout(){
        try{
            ftpClient.disconnect();
            return true;
        }catch(Exception e) {
            LOGGER.error(legacy + " " + "FTP LOGOUT ERROR = " + e.getMessage());
            return false;
        }
    }


    public boolean renameRemoteFileDir(String sourceName, String targetName ) throws Exception {
        boolean result = ftpClient.rename(sourceName, targetName);

        if(result)
            LOGGER.info(legacy + " " + "Remote File Rename is Success!!! From " + sourceName + " To " + targetName);
        else
            LOGGER.info(legacy + " " + "Remote File Rename is Fail!!! From " + sourceName + " To " + targetName);

        return result;

    }

    public List<ArrayList> getList(String serverDir) throws Exception{

        LOGGER.info(legacy + " " + "Get List from " + serverDir);

        List fileList=new ArrayList();

        try{
            ftpClient.changeWorkingDirectory(serverDir);
            FTPFile[] list = ftpClient.listFiles();
            for (FTPFile entry : list) {
                if(!entry.getName().equals(".") && !entry.getName().equals("..") && entry.getName().indexOf(".") != 0  ){
                    if(remoteDownloadedFilePostFix.length() > 0) {
                        if(entry.getName().endsWith(remoteDownloadedFilePostFix)) continue;
                    }
                    if(remoteFinishedFilePostFix.length() > 0) {
                        String fullFinFileName;
                        for (FTPFile finEntry : list) {
                            fullFinFileName = entry.getName() + remoteFinishedFilePostFix;
                            if (finEntry.getName().equals(fullFinFileName)) {
                                if(remoteFileNamePattern.length() > 0) {
                                    if (UFtpUtil.findFileNameMatch(entry.getName(), remoteFileNamePattern)) {
                                        fileList.add(entry.getName());
                                        LOGGER.info(legacy + " " + "Adding List to  " + entry.getName());
                                    }
                                }else {
                                    LOGGER.info(legacy + " " + "Adding List to  " + entry.getName());
                                    fileList.add(entry.getName());
                                }
                            }
                        }
                    }else{
                        if(remoteFileNamePattern.length() > 0) {
                            if (UFtpUtil.findFileNameMatch(entry.getName(), remoteFileNamePattern)) {
                                fileList.add(entry.getName());
                                LOGGER.info(legacy + " " + "Adding List to  " + entry.getName());
                            }
                        } else {
                            fileList.add(entry.getName());
                            LOGGER.info(legacy + " " + "Adding List to  " + entry.getName());
                        }
                    }
                }
            }

            if(fileList.size() <= 0) LOGGER.info(legacy + " " + "No searched file for the download");

        }catch(Exception e){
            LOGGER.error(legacy + " " + "Get file list ERROR = " + e.getMessage());
            throw e;
        }

        return fileList;
    }


    public boolean downloadFile (String dir, String downloadFileName) throws Exception{

        LOGGER.info(legacy + " @ File Download is starting from " + dir + "/" + downloadFileName);

        boolean result = false;

        try {
            ftpClient.changeWorkingDirectory(dir);
            LOGGER.info("@@@ change working directory:" + ftpClient.printWorkingDirectory());


            boolean isDownPossible = true;

            if(remoteFinishedFilePostFix.length() > 0) {
                try {

                    if(ftpClient.getStatus(downloadFileName + remoteFinishedFilePostFix) == null) isDownPossible = false;
                    else isDownPossible = true;

                }catch(Exception e) {
                    LOGGER.error(legacy + " @ in Checking FinFile :" + e.getMessage());
                    isDownPossible = false;
                }
            }

            if(isDownPossible) {


                OutputStream outputStream = null;

                try {

                    File get_file = new File((localDir + "/" + downloadFileName));
                    outputStream = new FileOutputStream(get_file);
                    result = ftpClient.retrieveFile(downloadFileName, outputStream);


                } catch (Exception e) {
                    LOGGER.error(legacy + " @ GetFile Exception :" + e.getMessage());
                    throw e;
                }finally {
                    try {
                        if(outputStream != null) outputStream.close();
                    } catch (IOException e) {
                        LOGGER.error(legacy + " " + "@ FTP File Download Close ERROR = " + e.getMessage());
                    }
                }

                LOGGER.info("################After retrieveFile()#########################");
                LOGGER.info(legacy + " Reply Code:" + ftpClient.getReplyCode());
                LOGGER.info(legacy + " Reply Message:" + ftpClient.getReplyString());
                LOGGER.info("Status:" + ftpClient.getStatus());

                LOGGER.info(legacy + " @ File Download is finished from " + downloadFileName);
            }else {
                LOGGER.info(legacy + " @ No Search Download file of " + downloadFileName);
            }

        } catch (Exception e) {
            LOGGER.error(legacy + " " + "@ FTP File Download ERROR = " + e.getMessage());
            throw e;
        }

        return result;
    }

    public void reconnection(String workingDir) throws Exception {
        LOGGER.info("Starting The FTP Reconnection Function!!!!!!");
        init();
        //ftpClient.setBufferSize(1024 * 1024);
        ftpClient.changeWorkingDirectory(workingDir);
        if(!isLogin) throw new Exception("FTP Login Fail!");
    }

    public boolean downloadMultiFiles(String dir, List downloadFileNames, String localDir) throws Exception{

        LOGGER.info(legacy + " # The Multi-File is downloading in " + dir);

        boolean result = false;
        OutputStream outputStream = null;
        try {

            //ftpClient.setBufferSize(1024 * 1024);

            for(int i=0 ; i < downloadFileNames.size(); i++) {
                LOGGER.info(legacy + " # File Download is start from " + downloadFileNames.get(i).toString());

                String fileName = downloadFileNames.get(i).toString();

                if(!ftpClient.isConnected()) reconnection(dir);

                try {

                    boolean isDownPossible = true;

                    if(remoteFinishedFilePostFix.length() > 0) {
                        try {

                            if(ftpClient.getStatus(fileName + remoteFinishedFilePostFix) == null) isDownPossible = false;
                            else isDownPossible = true;

                        }catch(Exception e) {
                            LOGGER.error(legacy + " # in Checking FinFile :" + e.getMessage());
                            isDownPossible = false;
                        }
                    }

                    if(isDownPossible) {

                        try {

                            File get_file = new File((localDir + "/" + fileName));
                            outputStream = new FileOutputStream(get_file);
                            result = ftpClient.retrieveFile(fileName, outputStream);

                        } catch (Exception e) {
                            LOGGER.error(legacy + " # GetFile Exception!!! :" + e.getMessage());

                            int resCode = ftpClient.getReplyCode();
                            LOGGER.info(legacy + " # Reply Code():" + resCode);

                            if(resCode == 150) {
                                LOGGER.info("Call reconnection()");
                                result = true;
                                reconnection(dir);
                            }else{
                                throw e;
                            }
                        }

                        LOGGER.info("################After retrieveFile()#########################");
                        LOGGER.info(legacy + " # Reply Code:" + ftpClient.getReplyCode());
                        LOGGER.info(legacy + " # Reply Message:" + ftpClient.getReplyString());
                        LOGGER.info(legacy + " # Status:" + ftpClient.getStatus());

                        LOGGER.info(legacy + " # File Download is finished from " + fileName);
                    }else {
                        LOGGER.info(legacy + " # No Search Download file of " + fileName);
                    }

                } catch (Exception e) {
                    LOGGER.error(legacy  + "# FTP File Download ERROR = " + e.getMessage());
                    throw e;
                }

                LOGGER.info(legacy + " # " + fileName + " File is finished in " +downloadFileNames.size());

            }


        }  catch (Exception e) {
            LOGGER.error(legacy + " " + "# FTP All Files Download - Stream = " + e.getMessage());
            throw e;
        }finally {
            try {
                if(outputStream != null) outputStream.close();
            } catch (IOException e) {
                LOGGER.error(legacy + " " + "@ FTP File Download Close ERROR = " + e.getMessage());
            }
        }

        LOGGER.info(legacy + " # The Multi-File is finished in " + dir);
        return result;
    }

}



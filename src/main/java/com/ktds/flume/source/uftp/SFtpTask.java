package com.ktds.flume.source.uftp;

import com.jcraft.jsch.*;
import com.ktds.flume.source.util.UFtpUtil;
import org.apache.flume.Context;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


/**
 * Created by kw.kim on 2015-07-13.
 */
public class SFtpTask extends UFtp<ArrayList>{

    JSch jsch = null;
    Session session = null;
    Channel channel = null;
    ChannelSftp channelSftp = null;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SFtpTask.class);

    public SFtpTask(Context ctx)  throws Exception {
        setConfig(ctx);
        jsch = new JSch();
    }

    public boolean login(){
        try{
            session = jsch.getSession(username, server, Integer.parseInt(port));
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(Integer.parseInt(timeOut) * 1000);
            session.setTimeout(Integer.parseInt(timeOut) * 1000);
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp)channel;
            return true;
        }catch(JSchException e) {
            LOGGER.error(legacy + " " + "SFTP LOGIN ERROR = " + e.getMessage());
            return false;
        }
    }

    public boolean logout(){
        try{
            session.disconnect();
            channel.disconnect();
            return true;
        }catch(Exception e) {
            LOGGER.error(legacy + " " + "SFTP LOGOUT ERROR = " + e.getMessage());
            return false;
        }
    }

    public boolean renameRemoteFileDir(String sourceName, String targetName ) throws Exception {
        boolean result = false;
        try {
            channelSftp.rename(sourceName, targetName);
            result = true;
        }catch(Exception e) {
            throw e;
        }

        if(result)
            LOGGER.info(legacy + " " + "Remote File Rename is Success!!! From " + sourceName + " To " + targetName);
        else
            LOGGER.info(legacy + " " + "Remote File Rename is Fail!!! From " + sourceName + " To " + targetName);

        return result;
    }

    public List<ArrayList> getList(String serverDir) throws Exception{

        List fileList = new ArrayList();

        try{

            Vector<ChannelSftp.LsEntry> list = channelSftp.ls(serverDir);

            for(ChannelSftp.LsEntry entry : list) {
                if(!entry.getFilename().equals(".") && !entry.getFilename().equals("..") && entry.getFilename().indexOf(".") != 0  ){

                    if(remoteDownloadedFilePostFix.length() > 0) {
                        if(entry.getFilename().endsWith(remoteDownloadedFilePostFix)) continue;
                    }

                    if(remoteFinishedFilePostFix.length() > 0) {
                        String fullFinFileName;
                        for (ChannelSftp.LsEntry finEntry : list) {
                            fullFinFileName = entry.getFilename() + remoteFinishedFilePostFix;
                            if (finEntry.getFilename().equals(fullFinFileName)) {

                                if(remoteFileNamePattern.length() > 0) {
                                    if (UFtpUtil.findFileNameMatch(entry.getFilename(), remoteFileNamePattern)) fileList.add(entry.getFilename());
                                }else {
                                    fileList.add(entry.getFilename());
                                }
                            }
                        }
                    }else{
                        if(remoteFileNamePattern.length() > 0) {
                            if (UFtpUtil.findFileNameMatch(entry.getFilename(), remoteFileNamePattern)) fileList.add(entry.getFilename());
                        }else {
                            fileList.add(entry.getFilename());
                        }
                    }
                }
            }

            if(fileList.size() <= 0) LOGGER.info(legacy + " "+ "No searched file for the download");

        }catch(SftpException e){
            LOGGER.error(legacy + " " + "Get file list ERROR = " + e.getMessage());
            throw e;
        }

        return fileList;
    }


    public boolean downloadFile (String dir, String downloadFileName) throws Exception{

        LOGGER.info(legacy + " @ File Download is starting from " + downloadFileName);

        boolean result = false;

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            channelSftp.cd(dir);

            boolean isDownPossible = true;
            if(remoteFinishedFilePostFix.length() > 0) {
                try {
                    if (channelSftp.get(downloadFileName + remoteFinishedFilePostFix).available() == 0) isDownPossible = true;
                    else isDownPossible = false;
                }catch(Exception e) {
                    e.printStackTrace();
                    isDownPossible = false;
                }
            }

            if(isDownPossible) {
                byte[] buffer = new byte[1024];
                bis = new BufferedInputStream(channelSftp.get(downloadFileName));

                File newFile = new File((localDir + "/" + downloadFileName));
                OutputStream os = new FileOutputStream(newFile);
                bos = new BufferedOutputStream(os);

                int readCount;

                while((readCount = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readCount);
                }

                bis.close();
                bos.close();

                result = true;
                LOGGER.info(legacy + " @ File Download is finished from " + downloadFileName);
            }else {
                LOGGER.info(legacy + " @ No Search Download file of " + downloadFileName);
            }

        } catch (IOException e) {
            LOGGER.error(legacy + " " + "SFTP File Download ERROR = " + e.getMessage());
            throw e;
        }  catch (SftpException e) {
            LOGGER.error(legacy + " " + "SFTP File Download = " + e.getMessage());
            throw e;
        }finally {
            try {
                if(bis != null) bis.close();
                if(bos != null) bos.close();
            } catch (IOException e) {
                LOGGER.error(legacy + " " + "@ SFTP File Download Close ERROR = " + e.getMessage());
            }
        }
        return result;
    }

    public void reconnection(String workingDir) throws Exception{

        LOGGER.info("Starting The FTP Reconnection Function!!!!!!");
        init();
        channelSftp.cd(workingDir);
        if(!isLogin) throw new Exception("FTP Login Fail!");

    }

    public boolean downloadMultiFiles(String dir, List downloadFileNames, String localDir) throws Exception{

        LOGGER.info(legacy + " # The Multi-File is download in " + dir);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        boolean result = false;

        try {
            channelSftp.cd(dir);

            for(int i=0 ; i < downloadFileNames.size(); i++) {

                if(!channelSftp.isConnected()) reconnection(dir);

                byte[] buffer = new byte[1024];

                LOGGER.info(legacy + " # File Download is start from " + downloadFileNames.get(i).toString());
                bis = new BufferedInputStream(channelSftp.get(downloadFileNames.get(i).toString()));

                File newFile = new File((localDir + "/" + downloadFileNames.get(i).toString()));
                OutputStream os = new FileOutputStream(newFile);
                bos = new BufferedOutputStream(os);

                int readCount;
                while((readCount = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readCount);
                }

                bis.close();
                bos.close();

                LOGGER.info(legacy + " # " + downloadFileNames.size() + " Files Download is finished from " + downloadFileNames.get(i).toString());
                result = true;
            }


        } catch (IOException e) {
            LOGGER.error(legacy + " "+ "SFTP All Files Download ERROR = " + e.getMessage());
            throw e;
        } catch (SftpException e) {
            e.printStackTrace();
            LOGGER.error(legacy + " "+ "SFTP All Files Download - Stream = " + e.getMessage());
            throw e;
        }finally {
            try {
                if(bis != null) bis.close();
                if(bos != null) bos.close();
            } catch (IOException e) {
                LOGGER.error(legacy + " "+ "SFTP All Files Download Close ERROR = " + e.getMessage());
            }
        }

        LOGGER.info(legacy + " # The Multi-File is finished in " + dir);

        return result;
    }

}



package com.ktds.flume.source.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.flume.Context;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by kw.kim on 2015-07-23.
 */
public class UFtpUtil {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UFtpUtil.class);

    public static String renameFile(String fName) {

        String fileName = fName;

        LOGGER.info("origin File " + fileName);

        Calendar oCalendar = Calendar.getInstance( );

        NumberFormat numFormat = NumberFormat.getInstance();
        numFormat.setMinimumIntegerDigits(2);

        String year =   ""+oCalendar.get(Calendar.YEAR);
        int month =Integer.parseInt("" + (oCalendar.get(Calendar.MONDAY)+1));
        int day =  Integer.parseInt("" + oCalendar.get(Calendar.DAY_OF_MONTH));
        int hour = Integer.parseInt("" + oCalendar.get(Calendar.HOUR_OF_DAY));
        int min = Integer.parseInt("" + oCalendar.get(Calendar.MINUTE));

        fileName = fileName.replaceAll("%year", year);
        fileName = fileName.replaceAll("%month", numFormat.format(month));
        fileName = fileName.replaceAll("%day", numFormat.format(day));
        fileName = fileName.replaceAll("%hour", numFormat.format(hour));
        fileName = fileName.replaceAll("%minute", numFormat.format(min));

        return fileName;
    }


    public static boolean moveLocalFile(String srcFileName , String targetDir) throws IOException {

        boolean removeResult = false;

        File sourceFile = FileUtils.getFile(srcFileName);

        if(FileUtils.sizeOf(sourceFile) > 0) {
            File moveDir = new File(targetDir);
            FileUtils.moveFileToDirectory(sourceFile, moveDir, false);
            removeResult = true;
        }else {
            LOGGER.info("Remove Downloaded Empty File!!!");
            FileUtils.forceDelete(sourceFile);
        }

        return removeResult;

    }

    public static void checkLocalDir(String dir) throws IOException {
        File dirFile = new File(dir);
        if(!dirFile.exists()) FileUtils.forceMkdir(dirFile);
    }

    public static void makeChkFile(String dir , String fileName) throws IOException {
        checkLocalDir(dir);
        File chkFile = new File(dir + "/"+ fileName + ".chk" );
        chkFile.createNewFile();
    }


    public static void removeFile(String fileName) throws IOException {
        FileUtils.forceDelete(new File(fileName));
    }

    public static Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }


    public static Date getJobStartDate(Context ctx, int delayDay) {

        Calendar cal = Calendar.getInstance();
        Date date = null;

        int hour = Integer.parseInt(ctx.getString("schedule.hour", "0"));
        int minute = Integer.parseInt(ctx.getString("schedule.minute", "0"));

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        date=cal.getTime();

        if (date.before(new Date())) date = UFtpUtil.addDay(date, delayDay);

        return date;

    }

    public static boolean isInTime(Context ctx) {

        boolean result = false;
        int hour = Integer.parseInt(ctx.getString("schedule.hour", "0"));
        if(hour != 0) result = true;

        return result;

    }

    public static void delete(File file)  {
        if (!file.exists()) new Exception(file.getAbsolutePath() + " : file is not exist");
        if (!file.delete()) new Exception(file.getAbsolutePath() + " : file deleted fail");
    }

    public static boolean isDuplicateFileCheck(String remoteFileName , String checkFilePath) throws Exception{

        boolean result = false;
        checkLocalDir(checkFilePath);

        File sourceFile = FileUtils.getFile(checkFilePath + "/" + remoteFileName + ".chk");
        if(sourceFile.exists())  result = true;

        return result;

    }

    public static void cleanCheckFile( String checkFilePath , String aDay) throws Exception{

        long dayCnt = Integer.parseInt(aDay);
        checkLocalDir(checkFilePath);

        File checkDir =  FileUtils.getFile(checkFilePath);

        File[] fileList= checkDir.listFiles();

        for(File chkFile : fileList) {
            if(chkFile.isFile()) {
                long lModifyDate = chkFile.lastModified();
                if(calAfterTime(formatTime(lModifyDate, "yyyyMMdd")) >= dayCnt) {
                    FileUtils.forceDelete(chkFile);
                }
            }
        }
    }

    public static long calAfterTime(String yyyymmdd) throws Exception{

        long resultGap;

        if(yyyymmdd.length() == 8) {
            int year = Integer.parseInt(yyyymmdd.substring(0, 4));
            int month = Integer.parseInt(yyyymmdd.substring(4, 6))-1;
            int day = Integer.parseInt(yyyymmdd.substring(6));

            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

            cal2.set(year,month,day);

            resultGap = cal1.getTimeInMillis() - cal2.getTimeInMillis();

            resultGap = (resultGap / 1000) / (60 * 60 * 24);
        }else {
            throw new Exception("Invalid Date Format!!!");
        }

        return resultGap;

    }

    public static String formatTime(long timestamp, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return format.format(calendar.getTime());
    }

    public static String toDate(String pattern) {
        java.util.Date currentDate = new java.util.Date();

        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern);
        String dateString = format.format(currentDate);

        return dateString;

    }



    private static void changeFile(File file, File temp) {
        if (file.isFile()) delete(file);
        if (!temp.renameTo(file))
            throw new RuntimeException(MessageFormat.format("do not change file {0}", file.getAbsolutePath()));
    }


    public static void convertEncoding(File readFile, File writeFile, String readCharset, String writeCharset) {
        // Charset cset = Charset.forName("US-ASCII");
        OutputStreamWriter osw = null;
        InputStreamReader isr = null;

        try {
            osw = new OutputStreamWriter(new FileOutputStream(writeFile), writeCharset);
            isr = new InputStreamReader(new FileInputStream(readFile), readCharset);
            BufferedReader br = new BufferedReader(isr);
            String s = null;

            while ((s = br.readLine()) != null) {
                osw.write(s + IOUtils.LINE_SEPARATOR);
            }
            br.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

            try {
                if (isr != null) isr.close();
                if (osw != null) osw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    public static void transFileEncoding(String sourceFileName, String srcEncoding, String targetFileName, String tgtEncoding) throws IOException {
        BufferedReader br = null;
        BufferedWriter bw = null;
        try{

            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(sourceFileName)), srcEncoding));
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(targetFileName)), tgtEncoding));

            char[] buffer = new char[16384];
            int read;
            while ((read = br.read(buffer)) != -1) {
                bw.write(buffer, 0, read);
            }

        } finally {
            try {
                if (br != null) br.close();
                if (bw != null) bw.close();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static boolean findFileNameMatch(String fileName , String regx) {
        return java.util.regex.Pattern.compile(regx).matcher(fileName).find();

    }

}

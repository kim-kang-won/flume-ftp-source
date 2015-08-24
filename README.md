Overview
=======================================
It's source agent of Flume to do download from Ftp/SFtp Server to Flume-Agent.

    Ftp-Source → Channel → Null Sink
        |→ SpoolDir Source → Channel → HDFS Sink  

How to Install
=======================================
+ It's try to buid from the flume-ftp-source project on this git-repository
+ If you want to use directly a package, You can download from the [flume-ftp-source-1.0-SNAPSHOT.jar](https://github.com/kim-kang-won/flume-ftp-source/blob/master/target/flume-ftp-source-1.0-SNAPSHOT.jar)
+ And downloaded flume-ftp-source-10.-SNAPSHOT.jar move to the library directory of Flume Server.

How to Set for Flume's Configuration
+ FTP Connection: Supporting ftp/sftp protocol
+ Schedule: It's setting a Job-Schedule for the FTP-Task
+ Regular Exeression: Using regular expression for Filename and Path in the Ftp-Server, also It's setting today.
+ Download Directory: It's location that is downloaded directory from ftp-server
+ Move to Directory(Spool-Dir): The downloaded file have to process data at MoveToDir 
+ Duplication Check: Check that you received to download the file last time
+ Except File-List: After downloading, It could except to download next time as renaming the file of ftp-server
+ Check .FIN File: For Excepting working file in ftp-server, It can check rule of the FIN file
+ Pause: It will stop the ftp-task, but already starting task couldn't stop
=======================================

    1. Set FTP Connection
       agent.sources.ftp1.type = com.ktds.flume.source.FtpSource
       agent.sources.ftp1.server = xxx.xxx.xxx.xxx
       agent.sources.ftp1.legacy = legacy alias
       agent.sources.ftp1.ftp_type = 'ftp' or 'sftp'
       agent.sources.ftp1.port = '21' or 'OO'
       agent.sources.ftp1.ftp_mode = 'local_active' or 'local_passive' or 'remote_passive'
       agent.sources.ftp1.user_name = admin
       agent.sources.ftp1.password = admin1234
       agent.sources.ftp1.timeout = 300
       
    2. Set FTP Download Schedule
      # TypeI - Execute download on every PM 8:30
      agent.sources.ftp1.schedule.hour = 20
      agent.sources.ftp1.schedule.minute = 30
      agent.sources.ftp1.schedule.repeat_period = 86400
      # TypeII - Execute download on every 10 minutes
      agent.sources.ftp1.schedule.repeat_period = 600
      
    3. Set File Information of Remote
      # TypeI - regular-expression Filename
      agent.sources.ftp1.remote_dir = /Legacy1/data/%year-%month-%day_AAA/LOG
      agent.sources.ftp1.remote_file_name_pattern = ^aaa\\d{1,2}_employ_auth\\d{8}_\\d{3}(.log.FIN.end)$
      # TypeII - Specific Filename
      agent.sources.ftp1.remote_dir = /Legacy1/data/employee/LOG
      agent.sources.ftp1.remote_file_name_pattern = employee_auth.log
      
    4. Set File Information of local
      agent.sources.ftp1.local_dir = /mnt/download/data1
      agent.sources.ftp1.local_move_dir = /mnt/sool_dir1
      
    5. Set Checking Duplicate file wheather downloaded file at last time
      # It should make '.chk' file in 'local_dir/checkFiles' when ftp-source is finished from the remote file
      agent.sources.ftp1.is_check_file = true
      # If you want to clear '.chk' file, you have to set after-day
      agent.sources.ftp1.clean_check_file_after_day = 5
      
    6. Etc.
      # If you want to download from finished file at remote, setting below config
      agent.sources.ftp1.remote_finished_file_postfix = .FIN or .END or .XX
      # If you want to change the postfix of remote file in ftp server after finishing download, setting below config
      agent.sources.ftp1.remote_downloaded_file_postfix = .end or .end or .xx
      # Set holding the Ftp-Task
      agent.sources.ftp2.pause = Y or N
      
      
How to Make Multi-Thread Toplogy
=======================================
It's consis of the multi-thread topology for using a flume configuration. the channel/sink component is a dummy object.

    ftp1 ---→
            ↓
    ftp2 ---→ channel → Sink
            ↑
    sftp3---→
       
    # Type of source for ftp sources
    # The in-time on every day : repeat_period = 24 * 60 * 60 = 86400
    agent.sources.ftp1.type = com.ktnexr.flume.source.FtpSource
    agent.sources.ftp1.server = xxx.xxx.xxx.xxx
    agent.sources.ftp1.legacy = test_legacy
    agent.sources.ftp1.ftp_type = ftp
    agent.sources.ftp1.port = 21
    agent.sources.ftp1.ftp_mode = local_passive
    agent.sources.ftp1.user_name = admin
    agent.sources.ftp1.password = abc1234
    agent.sources.ftp1.schedule.hour = 5
    agent.sources.ftp1.schedule.minute = 0
    agent.sources.ftp1.schedule.repeat_period = 86400
    agent.sources.ftp1.pause = N
    agent.sources.ftp1.time_out = 300
    agent.sources.ftp1.is_check_file = true
    agent.sources.ftp1.clean_check_file_after_day = 5
    agent.sources.ftp1.remote_dir = /Legacy1/data/%year-%month-%day_AAA/LOG
    agent.sources.ftp1.local_dir = /mnt/download/data1
    agent.sources.ftp1.local_move_dir = /mnt/sool_dir1
    agent.sources.ftp1.remote_file_name_pattern = ^aaa\\d{1,2}_employ_auth\\d{8}_\\d{3}(.log.FIN.end)$


    agent.sources.ftp2.type = com.ktnexr.flume.source.FtpSource
    agent.sources.ftp2.server = xxx.xxx.xxx.xxx
    agent.sources.ftp2.legacy = test_legacy2
    agent.sources.ftp2.ftp_type = ftp
    agent.sources.ftp2.port = 21
    agent.sources.ftp2.ftp_mode = local_passive
    agent.sources.ftp2.user_name = admin
    agent.sources.ftp2.password = admin1234
    agent.sources.ftp2.schedule.hour = 5
    agent.sources.ftp2.schedule.minute = 15
    agent.sources.ftp2.schedule.repeat_period = 86400
    agent.sources.ftp2.pause = N
    agent.sources.ftp2.time_out = 300
    agent.sources.ftp2.is_check_file = true
    agent.sources.ftp2.clean_check_file_after_day = 5
    agent.sources.ftp2.remote_dir = /Legacy2/data/%year-%month-%day_AAA/LOG
    agent.sources.ftp2.local_dir = /mnt/download/data2
    agent.sources.ftp2.local_move_dir = /mnt/sool_dir2
    agent.sources.ftp2.remote_file_name_pattern = ^aaa\\d{1,2}_employee_reject\\d{8}_\\d{3}(.log.FIN.end)$


    agent.sources.sftp3.type = com.ktnexr.flume.source.FtpSource
    agent.sources.sftp3.server = xxx.xxx.xxxx
    agent.sources.sftp3.legacy = Test_Legacy3
    agent.sources.sftp3.ftp_type = sftp
    agent.sources.sftp3.port = 22
    agent.sources.sftp3.user_name = admin
    agent.sources.sftp3.password = admin123
    agent.sources.sftp3.schedule.hour = 5
    agent.sources.sftp3.schedule.minute = 30
    agent.sources.sftp3.schedule.repeat_period = 86400
    agent.sources.sftp3.pause = N
    agent.sources.sftp3.time_out = 300
    agent.sources.sftp3.is_check_file = true
    agent.sources.sftp3.clean_check_file_after_day = 5
    agent.sources.sftp3.remote_dir = /Legacy3/data/%year-%month-%day_AAA/LOG
    agent.sources.sftp3.local_dir =  /mnt/download/data3
    agent.sources.sftp3.local_move_dir = /mnt/sool_dir3
    agent.sources.sftp3.remote_file_name = test_%year-%month-%day.txt
    agent.sources.sftp3.remote_finished_file_postfix = .FIN
    agent.sources.sftp3.remote_downloaded_file_postfix = .end
    
    agent.sinks.k1.type = null
    
    agent.channels.ch1.type = memory
    agent.channels.ch1.capacity = 10
    agent.channels.ch1.transactionCapacity = 10

    agent.sources.ftp1.channels = ch1
    agent.sources.ftp2.channels = ch1
    agent.sources.sftp3.channels = ch1

    agent.sinks.k1.channel = ch1


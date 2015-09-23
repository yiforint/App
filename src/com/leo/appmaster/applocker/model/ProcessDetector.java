package com.leo.appmaster.applocker.model;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.integer;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.Constants;
import com.leo.appmaster.utils.LeoLog;
import com.leo.imageloader.utils.IoUtils;
import com.tendcloud.tenddata.ad;

public class ProcessDetector {
    private static final String TAG = "ProcessDetector";
    private static final String REGEX_SPACE = "\\s+";
    private static final int INDEX_USER = 0;
    private static final int INDEX_PID = 1;
    private static final int INDEX_PPID = 2;
    private static final int INDEX_STATE = 5;
    private static final int INDEX_PROCESS_NAME = 9;
    
    private static final String PS = "ps -P";
    private static final String OOM_SCORE_ADJ = "oom_score_adj";
    
    public static final int PERMISSION_DENY = -9999;
    
    // wait 5秒超时，防止开始监听后，一直收不到文件改动通知，导致thread无法被唤醒
    private static final int TIME_OUT = 5 * 1000;
    
    private static final boolean DBG = false;

    private ProcessFilter[] mFilters;
    private ProcessFilter mHomeFilter;
    
    public ProcessDetector() {
        mFilters = new ProcessFilter[] {
                new SystemProcessFilter(),
                new PatternProcessFilter()
        };
        
        mHomeFilter = new HomeProcessFilter(AppMasterApplication.getInstance());
        
    }
    
    public int getTimeoutMs(ProcessAdj proAdj) {
        return TIME_OUT;
    }
    
    public String getObservePath(ProcessAdj processAdj) {
        return getProcessAdjPath(processAdj.pid);
    }
    
    /**
     * 功能是否已准备好
     * @return
     */
    public boolean ready() {
        return true;
    }
    
    public boolean checkAvailable() {
        Context context = AppMasterApplication.getInstance();
        String pkg = context.getPackageName();
        
        ProcessAdj processAdj = null;
        Process p = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(PS);
//            ProcessBuilder builder = new ProcessBuilder(PS);
//            builder.redirectErrorStream(false);
//            p = builder.start();

            is = p.getInputStream();
            
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            
            int zygoteId = 0;
            while ((line = br.readLine()) != null) {
                ProcessAdj pair = getProcessAdjByFormatedLine(line, zygoteId);
                if (pair == null || pkg.equals(pair.pkg)) continue;

                processAdj = pair;
                break;
            }
            
            int oomAdj = getOomScoreAdj(processAdj.pid);
            if (oomAdj != PERMISSION_DENY) {
                return true;
            }
        } catch (Exception e) {
            LeoLog.e(TAG, "getForegroundProcess ex  " + e.getMessage());
        } finally {
            IoUtils.closeSilently(br);
            IoUtils.closeSilently(is);

            if (p != null) {
                p.destroy();
            }
        } 
        
        return false;
    }
    
    /**
     * 获取前台进程, oom_adj为0
     * @return
     */
    public ProcessAdj getForegroundProcess() {
        List<ProcessAdj> result = new ArrayList<ProcessAdj>();
        
        Process p = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(PS);
//            ProcessBuilder builder = new ProcessBuilder(PS);
//            builder.redirectErrorStream(false);
//            p = builder.start();

            is = p.getInputStream();
            
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            
            int zygoteId = 0;
            while ((line = br.readLine()) != null) {
//                if (zygoteId == 0) {
//                    zygoteId = getZygoteProcessId(line);
//                }
                
                ProcessAdj pair = getProcessAdjByFormatedLine(line, zygoteId);
                if (pair == null || pair.oomAdj == PERMISSION_DENY) continue;

                result.add(pair);
            }
            return filterForegroundProcess(result);
        } catch (Exception e) {
            LeoLog.e(TAG, "getForegroundProcess ex  " + e.getMessage(), e);
        } finally {
            IoUtils.closeSilently(br);
            IoUtils.closeSilently(is);

            if (p != null) {
                p.destroy();
            }
        }
        
        return null;  
    }
    
    public boolean isHomePackage(String pkgName) {
        ProcessAdj adj = new ProcessAdj();
        adj.pkg = pkgName;
        
        return mHomeFilter.filterProcess(adj);
    }
    
    /**
     * Home比较特殊，oom_score基本保持不变
     * @param processAdj
     * @return
     */
    public boolean isHomePackage(ProcessAdj processAdj) {
        return mHomeFilter.filterProcess(processAdj);
    }
    
    protected ProcessAdj filterForegroundProcess(List<ProcessAdj> result) {
        if (result == null || result.isEmpty()) return null;
        
        Iterator<ProcessAdj> iterator = result.iterator();
        ProcessAdj adj = null;
        while (iterator.hasNext()) {
            adj = iterator.next();
            if (adj.oomAdj != 0) {
                iterator.remove();
            }
        }
        
        Context context = AppMasterApplication.getInstance();
        int minScore = Integer.MAX_VALUE;
        
        ProcessAdj target = null;
        if (result.size() > 1) {
            ProcessDetectorCompat22 detector = new ProcessDetectorCompat22();
            
            iterator = result.iterator();
            while (iterator.hasNext()) {
                ProcessAdj processAdj = iterator.next();
                
                Intent intent = new Intent();
                intent.setPackage(processAdj.pkg);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 0);
                if (list == null || list.isEmpty()) continue;
                
                int score = detector.getOomScoreAdj(processAdj.pid);
                if (score < minScore) {
                    minScore = score;
                    target = processAdj;
                }
            }
        } else if (result.size() > 0) {
            target = result.get(0);
        }

        return target;

    }
    
    private ProcessAdj getProcessAdjByFormatedLine(String line, int zygoteId) {
        if (TextUtils.isEmpty(line)) return null;
        
        Pattern pattern = Pattern.compile(REGEX_SPACE);
        Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            line = matcher.replaceAll(",");
        }

        if (DBG) {
            LeoLog.i(TAG, line);
        }
        
        String[] array = line.split(",");
        if (array == null || array.length == 0) return null;
        
        if (array.length <= INDEX_PROCESS_NAME) return null;
        
        if (DBG) {
            LeoLog.i(TAG, "array length: " + array.length);
        }

        ProcessAdj processAdj = new ProcessAdj();

        String user = array[INDEX_USER];
        processAdj.user = user;
        
        String ppidStr = array[INDEX_PPID];
        int ppid = Integer.parseInt(ppidStr);
        if (zygoteId != 0 && ppid != zygoteId) return null;

        
        String processName = array[INDEX_PROCESS_NAME];
        processAdj.pkg = processName;
        for (ProcessFilter filter : mFilters) {
            if (filter.filterProcess(processAdj)) return null;
        }
        
        String processIdStr = array[INDEX_PID];
        if (TextUtils.isEmpty(processName)) return null;

        int processId = 0;  
        try {
            processId = Integer.parseInt(processIdStr); 
        } catch (Exception e) {
        }
        if (processId == 0) return null;
        
        String state = array[INDEX_STATE];
        if (!"fg".equals(state)) return null;

        int oomAdj = getOomScoreAdj(processId);

        // 解决一些前台进程的进程名是子进程的问题,com.mobisystems.office:browser
        if (processName.contains(":")) {
            processName = processName.substring(0, processName.indexOf(":"));
        }
        processAdj.oomAdj = oomAdj;
        processAdj.pid = processId;
        processAdj.pkg = processName;
        processAdj.ppid = ppid;
        
        if (DBG) {
            LeoLog.i(TAG, processAdj.toString());
        }
        
        return processAdj;
    }
    
    private int getZygoteProcessId(String line) {
        if (TextUtils.isEmpty(line) || !isZygoteProcess(line)) {
            return 0;
        }
        
        Pattern pattern = Pattern.compile(REGEX_SPACE);
        Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            line = matcher.replaceAll(",");
        }

        String[] array = line.split(",");
        if (array == null || array.length == 0) return 0;
        
        if (array.length <= INDEX_PID) return 0;
        
        String zygoteId = array[INDEX_PID];
        
        if (TextUtils.isEmpty(zygoteId)) return 0;
        
        try {
            return Integer.parseInt(array[INDEX_PID]);
        } catch (Exception e) {
        }
        
        return 0;
    }
    
    public int getOomScoreAdj(int pid) {
        String path = getProcessAdjPath(pid);
        
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        try {
            fis = new FileInputStream(path);
            baos = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int len;  
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);  
            }  
            String str = baos.toString();  
            
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            LeoLog.e(TAG, "getAdjByProcessPath ex path: " + path  + " | " + e.getMessage());
        } finally {
            IoUtils.closeSilently(baos);
            IoUtils.closeSilently(fis);
        }
        return PERMISSION_DENY;
    }
    
    public String getProcessAdjPath(int pid) {
        return "/proc/" + pid + File.separator + OOM_SCORE_ADJ;
    }
    
    public boolean isOOMScoreMode() {
        return false;
    }
    
    private boolean isZygoteProcess(String cmdline) {
        if (TextUtils.isEmpty(cmdline)) return false;
        
        return cmdline.endsWith("zygote");
    }

    /**
     * Usage是否可用
     * @return
     */
    public static boolean isUsageAvailable() {
        ProcessDetectorUsageStats usageStats = new ProcessDetectorUsageStats();
        return usageStats.checkAvailable();
    }
    
    private static interface ProcessFilter {
        public boolean filterProcess(ProcessAdj processAdj);
    }
    
    /**
     * 桌面filter
     * @author Jasper
     *
     */
    private static class HomeProcessFilter implements ProcessFilter {
        private List<ResolveInfo> mHomeList;
        public HomeProcessFilter(Context context) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            
            mHomeList = context.getPackageManager().queryIntentActivities(
                    intent, 0);
        }

        @Override
        public boolean filterProcess(ProcessAdj processAdj) {
            if (mHomeList == null || processAdj == null) return false;
            
            // google桌面比较特殊，和quicksearch集成在一个包里，但是获取的packagename又不是quicksearch的
            if (Constants.GOOGLE_HOME_PACKAGE.equals(processAdj.pkg)) return true;
            
            for (ResolveInfo resolveInfo : mHomeList) {
                if (processAdj.pkg.equals(resolveInfo.activityInfo.packageName)) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    /**
     * 系统进程过滤器
     * @author Jasper
     *
     */
    private static class SystemProcessFilter implements ProcessFilter {
        private static final String PROCESS_SEC_PREFIX = "android.sec.android";
        
        private static final String[] USERS = {
            "root",
            "system",
            "radio",
            "media",
            "camera",
            "shell",
            "nfc",
            "bluetooth",
            "audit",
            "dhcp",
            "smartcard"
        };
        
        private static final String[] PROCESSES = {
            "android.process.media",
            "android.process.acore",
            "com.google.android.googlequicksearchbox",
            "com.android.systemui"
        };
        
        private static final String PKG_SETTINGS = "com.android.settings";

        @Override
        public boolean filterProcess(ProcessAdj processAdj) {
            String user = processAdj.user;
            String pkg = processAdj.pkg;
            
            for (String string : PROCESSES) {
                if (string.equals(pkg)) return true;
            }
            
            for (String u : USERS) {
                // 排出掉settings，settings属于system用户
                if (u.equals(user) && !pkg.equals(PKG_SETTINGS)) {
                    return true;
                }
            }
            
            if (pkg.startsWith(PROCESS_SEC_PREFIX)) {
                return true;
            }
                
            return false;
        }
        
    }
    
    /**
     * 非主流进程过滤器
     * @author Jasper
     *
     */
    private static class PatternProcessFilter implements ProcessFilter {
        private static final String REGEX = "[a-z0-9A-Z]+(\\.[a-z0-9A-Z]+)+";
        
        private static final String OFFICE = "com.mobisystems.office";

        @Override
        public boolean filterProcess(ProcessAdj processAdj) {
            String pkg = processAdj.pkg;
            // 此app的主页面在子进程里，过滤掉
            if (pkg.startsWith(OFFICE)) return false;
            
            boolean matches = pkg.matches(REGEX);
            
            return !matches;
        }
        
    }
   
}

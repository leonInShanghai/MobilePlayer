package com.bobo.mobileplayer.utils;

import com.bobo.mobileplayer.domain.Lyric;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Leon on 2018/4/5.
 * Functions: 解析歌词的工具类
 */

public class LyricUtils {



    /**
     * 得到解析好的歌词列表
     * @return
     */
    public ArrayList<Lyric> getLyrics() {
        return lyrics;
    }

    private ArrayList<Lyric> lyrics;

    /**是否存在歌词*/
    public boolean isExistsLyric() {
        return isExistsLyric;
    }

    /**是否存在歌词*/
    private boolean isExistsLyric = false;

    /**
     * 读取歌词文件
     * @param file
     */
    public void readLyricFile(File file){
        if (file == null || !file.exists()){
            //歌词文件不存在
            lyrics = null;
            isExistsLyric = false;
        }else {
            //歌词文件存在-1.解析歌词一行一行的读取再解析
            lyrics = new ArrayList<>();
            isExistsLyric = true;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),getCharset(file)));

                String line = "";

                while ((line = reader.readLine()) != null){
                    line = parsedLyric(line);
                }

                //--------------------------leon解决最后一句歌词--------------------------------
//                do{
//                    line = parsedLyric(line);
//                }while((line = reader.readLine()) != null);
                //--------------------------leon解决最后一句歌词--------------------------------

                reader.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            //2.排序
            Collections.sort(lyrics, new Comparator<Lyric>() {
                @Override
                public int compare(Lyric lhs, Lyric rhs) {
                    if (lhs.getTimePoint() < rhs.getTimePoint()){
                        return -1;
                    }else if (lhs.getTimePoint() > rhs.getTimePoint()){
                        return 1;
                    }else {
                        return 0;
                    }
                }
            });

            //3.计算每句高亮显示的时间
            for (int i = 0;i < lyrics.size();i++){
                Lyric oneLyric = lyrics.get(i);
                if (i + 1 < lyrics.size()){
                    Lyric twoLyric = lyrics.get(i + 1);
                    oneLyric.setSleepTime(twoLyric.getTimePoint() - oneLyric.getTimePoint());
                   // LogUtil.e("歌词停留时间=="+String.valueOf(twoLyric.getTimePoint() - oneLyric.getTimePoint())+" "+String.valueOf(i));
                }
                //------------------leon解决最后一句歌词停留的问题----------------
//                if (i + 1 == lyrics.size()){
//                    LogUtil.e("99999999999999999999999999");
//                    oneLyric.setSleepTime(2000);
//                }
                //------------------leon解决最后一句歌词停留的问题-----------------
            }

        }
    }

    /**
     * 判断文件编码
     * @param file 文件
     * @return 编码：GBK,UTF-8,UTF-16LE
     */
    public String getCharset(File file) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1)
                return charset;
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF)
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    /**解析一句歌词*/
    private String parsedLyric(String line) {
        //第一次出现[的位置
        int pos1 = line.indexOf("[");//0,如果没有就返回-1

        int pos2 = line.indexOf("]");//9,如果没有就返回-1

        if (pos1 == 0 && pos2 != -1){//肯定是有一句歌词

            //装时间
            long[] times = new long[getCountTag(line)];

            String strTime = line.substring(pos1 + 1,pos2);
            times[0] = strTime2LongTime(strTime);

            String content = line;
            int i = 1;

            while (pos1 == 0 && pos2 != -1){
                content = content.substring(pos2 + 1);
                pos1 = content.indexOf("[");//0
                pos2 = content.indexOf("]");//9

                if (pos2 != -1){
                    strTime = content.substring(pos1 + 1,pos2);
                    times[i] = strTime2LongTime(strTime);

                    if (times[i] == -1){
                        return "";
                    }

                    i++;
                }
            }

            Lyric lyric = new Lyric();

            //把时间数组和文本关联起来，并且加入到集合中
            for (int j = 0;j < times.length;j++){
                if (times[j] != 0){//有时间戳

                    lyric.setContent(content);
                    lyric.setTimePoint(times[j]);
                    //添加到集合中
                    lyrics.add(lyric);
                    lyric = new Lyric();

                }
            }
            return content;
        }
        return "";
    }

    /**把String类型的时间转换成long类型*/
    private long strTime2LongTime(String strTime) {

        long  result = -1;

        try {
            //1.把02:05:12 → 02和05.12
            String[] s1 = strTime.split(":");
            //05.12 → 05和12
            String[] s2 = s1[1].split("\\.");//split 一定要转译 \\

            //1.分
            long min = Long.parseLong(s1[0]);

            //2.秒
            long second = Long.parseLong(s2[0]);

            //3.毫秒
            long mil = Long.parseLong(s2[1]);

            result = min * 60 * 1000 + second * 1000 + mil * 10;
        }catch (Exception e){
            e.printStackTrace();
            result = -1;
        }

        return result;
    }

    /**判断有多少句歌词*/
    private int getCountTag(String line) {

        int result = -1;

        String[] left = line.split("\\[");//split 一定要转译 \\
        String[] right = line.split("\\]");//split 一定要转译 \\

        if (left.length == 0 && right.length == 0){
            result = 1;
        }else if (left.length > right.length){
            result = left.length;
        }else{
            result = right.length;
        }

        return result;
    }
}

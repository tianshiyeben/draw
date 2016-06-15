package com.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;

import com.entity.ArticleBean;

/**
 * <p>Title:TikaUtils </p>
 * <p>Description: 抽取html内容，并格式化</p>
 * @author yxz
 * @date Jun 1, 2016
 */
public class TikaUtils {
	
	public static final String MARK_ONE = "#";
	
	public static final String MARK_T = "\t";
	
	public static final String MARK_N = "\n";
	
	//正文起始位置和结束位置的间隔长度
	public static final int SPAN_NUM = 99;

	//文章结尾#符号的密度最大值
	public static final double END_DEN = 0.16;
	
	//结束标记符号
	public static final String END_SIGN1 = "扫一扫在手机打开当前页";
	public static final String END_SIGN2 = "相关链接";
	
	/**
	 * 提取网页的标题，正文，时间戳
	 * @param urlStr
	 * @return
	 */
	public static ArticleBean pickHtml(String urlStr){
		ArticleBean bean = new ArticleBean();
		if(StringUtils.isEmpty(urlStr)){
			return null;
		}
		URL url;
		try {
			 url = new URL(urlStr);
			 BodyContentHandler handler = new BodyContentHandler();
		     Metadata metadata = new Metadata();
		     ParseContext pcontext = new ParseContext();
		     HtmlParser htmlparser = new HtmlParser();
		     htmlparser.parse(url.openStream(), handler, metadata,pcontext);
		     bean.setContent(handler.toString());
		     int dateIndex = setDate(bean);
		     bean.setTitle(metadata.get("title"));
		     bean.setContent(getContent(bean.getContent(),dateIndex,""));
		     System.out.println("---------------------------------------标题------------------------------------");
		     System.out.println(bean.getTitle());
		     System.out.println("---------------------------------------时间------------------------------------");
		     System.out.println(bean.getDate());
		     System.out.println("---------------------------------------正文------------------------------------");
		     System.out.println(bean.getContent());
		} catch (Exception e) {
//			System.out.println("读取网页出错："+e.toString());
			e.printStackTrace();
		}
	    return bean;
	}
	
	/**
	 * 提取正文中的日期，并返回日期的索引位置
	 * @param bean
	 * @return
	 */
	public static int setDate(ArticleBean bean){
		int dateIndex1 = 0;
		int dateIndex2 = 0;
		String date1 = "";
		String date2 = "";
		Pattern pattern = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})"); 
	    Matcher match = pattern.matcher(bean.getContent());      
	    if(match.find()){     
	    	date1 = match.group();//返回1984-10-20  
	    	dateIndex1 = bean.getContent().indexOf(date1);
	    }
	    Pattern pattern2 = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})"); 
	    Matcher match2 = pattern2.matcher(bean.getContent());      
	    if (match2.find()){     
	    	date2 = match2.group();
	    	dateIndex2 = bean.getContent().indexOf(date2);
	    }
	    if(dateIndex1!=0&&dateIndex2!=0&&dateIndex1>dateIndex2){
	    	bean.setDate(date2.replace("年", "-").replace("月", "-"));
	    	 return dateIndex2;
	    }else  if(dateIndex1!=0&&dateIndex2!=0&&dateIndex1<=dateIndex2){
	    	bean.setDate(date1);
	    	 return dateIndex1;
	    }else  if(dateIndex1!=0&&dateIndex2==0){
	    	bean.setDate(date1);
	    	 return dateIndex1;
	    }else  if(dateIndex1==0&&dateIndex2!=0){
	    	bean.setDate(date2);
	    	 return dateIndex2;
	    }
	    return dateIndex1;
	}
	
	/**
	 * 此循环记录内容中所有的#符号位置
	 * @param content
	 * @return
	 */
	public static List<Integer> signIndex(String content){
		int index = 0 ; 
		List<Integer> charIndex = new ArrayList<Integer>();
		for(int i = 0 ; i < content.length(); i++){
			if(charIndex.size()==0){
				charIndex.add(content.indexOf(MARK_ONE));
			}else{
				index = content.indexOf(MARK_ONE,charIndex.get(charIndex.size()-1)+1);
				if(index<0){
					break;
				}
				charIndex.add(index);
			}
		}
		return charIndex;
	}
	
	/**
	 * 提取至少是3个#连在一起的，数组，用,,隔开
	 * @param charIndex
	 * @return
	 */
	public static List<String> splitManyChar(List<Integer> charIndex){
		int resTemp = 0 ;
		String resStr = "";
		List<String> resIndex = new ArrayList<String>();
		for(int i = 0 ;i < charIndex.size();i++){
			if(StringUtils.isEmpty(resStr)){
				resStr=resStr+charIndex.get(i)+",";
				resTemp = charIndex.get(i);
			}else{
				if(charIndex.get(i)-resTemp>1){
					if(resStr.split(",").length>2){
						resIndex.add(resStr);
					}
					resStr=charIndex.get(i)+",";
					resTemp = charIndex.get(i);
				}else{
					resStr=resStr+charIndex.get(i)+",";
					resTemp = charIndex.get(i);
				}
			}
		}
		return resIndex;
	}
	
	
	/**
	 * 提取html页面的正文部分
	 * @param htmDoc 网页内容
	 * @param dateIndex 日期所在的索引位置
	 * @param sign不为空，则为第二次过滤
	 * @return
	 */
	public static String getContent(String htmDoc,int dateIndex,String sign){
		String resContent = "";
		List<String> resIndex = new ArrayList<String>();
		//将内容中的换行符号全部替换为#，方便处理，最后会还原为</br>
		String content = htmDoc.replace(MARK_T, MARK_ONE).replace(MARK_N, MARK_ONE);
		List<Integer> charIndex = signIndex(content);
		//将3个#符号或3个以上#符号链接的#符号组，记录下来
		resIndex = splitManyChar(charIndex);
		String[] s = resIndex.toString().substring(1, resIndex.toString().length()-2).replace(" ", "").split(",,");
		int[] st = new int[s.length];
		for(int i = 0 ;i < s.length;i++){
			if(!StringUtils.isEmpty(s[i])){
				st[i]=Integer.valueOf(s[i].substring(s[i].lastIndexOf(",")+1, s[i].length()));
			}
		}
		int beginIndex = 0;
		int beginStVal = 0;
		//如果有日期戳,则从日期戳的位置开始截取
		if(StringUtils.isEmpty(sign)){
			int[] val = getBeginIndex(st,dateIndex);
			beginIndex = val[0];
			beginStVal = val[1];
		}else{
			beginIndex = dateIndex;
		}
		int endIndex = content.length()-1;
		int end_sign1 = content.lastIndexOf(END_SIGN1);
		int end_sign2 = content.lastIndexOf(END_SIGN2);
		if(end_sign1>-1&&end_sign2==-1){
			endIndex = end_sign1;
		}else if(end_sign1==-1&&end_sign2>-1){
			endIndex = end_sign2;
		}else{
			endIndex = getEndIndex(st,content,beginIndex);
		}
		resContent = content.substring(beginIndex+1, endIndex);
		if(calDen(resContent)>0.09&&StringUtils.isEmpty(sign)){
			List<Integer> childIndex = signIndex(resContent);
			resIndex = splitManyChar(childIndex);
			if(resIndex.size()>1){
				resContent = getContent(content,st[beginStVal+1],"1");
			}
		}
		resContent = resContent.replace(MARK_ONE, MARK_N);
		return resContent;
	}
	
	/**
	 * 计算内容中的#密度
	 * @param content
	 * @return
	 */
	public static double calDen(String content){
		java.text.DecimalFormat   df=new   java.text.DecimalFormat("#.###");
		double a = (double)(content.length()-content.replace(MARK_ONE, "").length())/(double)content.length();
		a = Double.valueOf(df.format(a));
		return a;
	}
	
	/**
	 * 从fromValue+1计算#符号密度，若大于等于0.15，则返回index值
	 * @param s
	 * @param content
	 * @param fromValue
	 * @param sign 不为空，则为第二次过滤
	 * @return
	 */
	public static int getEndIndex(int[] s,String content,int fromValue){
		String tmpContent = "";
		int endIndex = 0;
		java.text.DecimalFormat   df=new   java.text.DecimalFormat("#.###");
		int fromIndex = 0;
		for(int i = 0 ;i < s.length ;i++){
			if(s[i]==fromValue){
				fromIndex=i;
				break;
			}
		}
		fromIndex = fromIndex+1;
		for(int i = fromIndex ;i < s.length ; i++){
			tmpContent = content.substring(s[i]+1,s[s.length-2]);
			double a = (double)(tmpContent.length()-tmpContent.replace(MARK_ONE, "").length())/(double)tmpContent.length();
			a = Double.valueOf(df.format(a));
			if(a>=END_DEN&&(s[i]-s[i-1]>SPAN_NUM)){
				endIndex = s[i] ;
				break;
			}
		}
		return endIndex;
	}
	
	/**
	 * 提取开始截取正文的起始位置，并记录数组的下标
	 * @param st
	 * @param dateIndex
	 * @return
	 */
	public static int[] getBeginIndex(int[] st,int dateIndex){
		int beginIndex = 0;
		int beginStVal = 0;
		//如果有日期戳,则从日期戳的位置开始截取
		if(0!=dateIndex){
			for(int i = 0 ;i < st.length ;i++){
				if(st[i]>dateIndex){
					beginIndex = st[i];
					beginStVal = i;
					if(st[i+1]-st[i]<50){
						while(st[i+1]-st[i]<50){
							i++;
						}
						beginIndex=st[i];
						beginStVal = i;
					}
					break;
				}
			}
		}else{
			for(int i = 1 ;i < st.length ;i++){
				if((st[i+1]-st[i]>SPAN_NUM)&&i<st.length-1){
					beginIndex = st[i];
					beginStVal = i;
					break;
				}
			}
		}
		if(beginIndex-dateIndex>300){
			beginIndex = dateIndex+50;
					
		}
		return new int[]{beginIndex,beginStVal};
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		pickHtml("http://www.shdrc.gov.cn/fzgggz/jggl/jgjgdt/23663.htm");
		
		/*HtmlParser htmlparser = new HtmlParser();
		Tika tika = new Tika();  
		
	    // Parse all given files and print out the extracted text content
		URL url;
		try {
			url = new URL("http://www.sdpc.gov.cn/gzdt/201605/t20160531_806176.html");
			String s = tika.parseToString(url);
			System.out.print(s);  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */

	}

}

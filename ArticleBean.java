package com.entity;

/**
 * <p>Title:ArticleBean </p>
 * <p>Description:文章bean </p>
 * @author yxz
 * @date Jun 1, 2016
 */
public class ArticleBean implements java.io.Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 370831686575591288L;

	private String title="";
	
	private String content="";

	private String date="";
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
	
	
	
}

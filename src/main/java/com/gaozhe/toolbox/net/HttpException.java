package com.gaozhe.toolbox.net;

public class HttpException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HttpException(String message, Throwable e) {
		super(message, e);
	}

	public HttpException(Throwable e) {
		super(e);
	}

}

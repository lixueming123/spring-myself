package org.spring.factory;

public interface InitializingBean {

	void afterPropertiesSet() throws Exception;

}
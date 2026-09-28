package io.mosip.demosdk.client.config;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.appender.RollingFileAppender;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Factory for MOSIP rolling-file loggers used by the Demo SDK.
 *
 * <p>
 * All loggers share one {@link RollingFileAppender} writing to {@code ./logs/biosdk-client.log},
 * rolled daily and when a file reaches 50 MB
 * ({@code ./logs/biosdk-client-yyyy-MM-dd-i.log}).
 * </p>
 */
public final class LoggerConfig {

	/** Utility class: no instances. */
	private LoggerConfig() {
	}

	/** Shared rolling-file appender configuration used by every logger from {@link #logConfig(Class)}. */
	private static RollingFileAppender mosipRollingFileAppender;

	static {
		mosipRollingFileAppender = new RollingFileAppender();
		mosipRollingFileAppender.setAppend(true);
		mosipRollingFileAppender.setAppenderName("fileappender");
		mosipRollingFileAppender.setFileName("./logs/biosdk-client.log");
		mosipRollingFileAppender.setFileNamePattern("./logs/biosdk-client-%d{yyyy-MM-dd}-%i.log");
		mosipRollingFileAppender.setImmediateFlush(true);
		mosipRollingFileAppender.setMaxFileSize("50mb");
		mosipRollingFileAppender.setPrudent(false);
	}

	/**
	 * Returns a MOSIP logger for the given class that writes to the shared rolling file.
	 *
	 * @param clazz class whose name is used as the logger name
	 * @return rolling-file logger for {@code clazz}
	 */
	public static Logger logConfig(Class<?> clazz) {
		return Logfactory.getDefaultRollingFileLogger(mosipRollingFileAppender, clazz);
	}

}

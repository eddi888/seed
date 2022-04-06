/**
 * Seed
 * Copyright (C) 2021 EUU⛰ROCKS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.seed.core.task.job;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.quartz.JobExecutionContext;

import org.seed.core.api.JobContext;
import org.seed.core.config.LogLevel;
import org.seed.core.entity.value.event.ValueObjectFunctionContext;
import org.seed.core.task.Task;
import org.seed.core.task.TaskParameter;
import org.seed.core.task.TaskRunLog;

class DefaultJobContext extends ValueObjectFunctionContext
	implements JobContext {
	
	static final String RUN_ID 		= "RUN_ID"; 
	static final String RUN_TASK 	= "RUN_TASK"; 
	static final String RUN_LOGS 	= "RUN_LOGS"; 
	static final String RUN_PARAMS 	= "RUN_PARAMS"; 
	static final String RUN_SESSION	= "RUN_SESSION"; 
	
	private final List<TaskRunLog> logs = new ArrayList<>();
	
	private final List<TaskParameter> parameters;
	
	@SuppressWarnings("unchecked")
	DefaultJobContext(JobExecutionContext context) {
		super((Session) context.get(RUN_SESSION), 
			  ((Task) context.get(RUN_TASK)).getModule());
		parameters = (List<TaskParameter>) context.get(RUN_PARAMS);
		context.put(RUN_LOGS, logs);
	}
	
	@Override
	public String getJobParameter(String name) {
		if (parameters != null) {
			for (TaskParameter paramwter : parameters) {
				if (paramwter.getName().equalsIgnoreCase(name)) {
					return paramwter.getValue();
				}
			}
		}
		return null;
	}
	
	@Override
	public String getJobParameter(String name, String defaultValue) {
		final String value = getJobParameter(name);
		return value != null ? value : defaultValue;
	}
	
	@Override
	public boolean hasJobParameter(String name) {
		return getJobParameter(name) != null;
	}
	
	@Override
	public void logInfo(String content) {
		log(LogLevel.INFO, content);
	}
	
	@Override
	public void logWarning(String content) {
		log(LogLevel.WARNING, content);
	}
	
	@Override
	public void logError(String content) {
		log(LogLevel.ERROR, content);
	}
	
	private void log(LogLevel level, String content) {
		final TaskRunLog log = new TaskRunLog();
		log.setLevel(level);
		if (content != null && content.length() > 1024) {
			content = content.substring(0, 1020) + "...";
		}
		log.setContent(content);
		logs.add(log);
	}
	
}

/*
 * The MIT License
 *
 * Copyright (c) 2012, Cedric Chabanois
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE. 
 */
package jenkins.plugins.ivyreport;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.ivy.IvyMessageImpl;
import hudson.ivy.IvyModuleSetBuild;
import hudson.ivy.Messages;
import hudson.ivy.IvyModuleSet;
import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.Message;

/**
 * Get the ivy resolution cache root on the slave or master that built the project  
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 *
 */
public class GetResolutionCacheRootCallable implements
		Callable<FilePath, Throwable> {
	private static final long serialVersionUID = 8422944415684825418L;
	private final BuildListener listener;
	private final String ivySettingsFile;
	private final String ivySettingsPropertyFiles;
	private final String ivyBranch;
	private final String workspaceProper;
	private final EnvVars envVars;
	
	public GetResolutionCacheRootCallable(BuildListener listener, IvyModuleSetBuild build) throws IOException, InterruptedException {
		// project cannot be shipped to the remote JVM, so all the relevant
		// properties need to be captured now.
		this.listener = listener;
		IvyModuleSet project = build.getProject();
		this.ivyBranch = project.getIvyBranch();
		this.ivySettingsFile = project.getIvySettingsFile();
		this.ivySettingsPropertyFiles = project.getIvySettingsPropertyFiles();
		this.workspaceProper = project.getLastBuild().getWorkspace()
				.getRemote();
		this.envVars = build.getEnvironment();
	}

	@Override
	public FilePath call() throws Throwable {
		File resolutionCacheRoot = getResolutionCacheRoot();
		if (resolutionCacheRoot == null) {
			return null;
		}
		return new FilePath(resolutionCacheRoot);
	}

	private File getResolutionCacheRoot() throws AbortException {
		PrintStream logger = listener.getLogger();
		IvySettings ivySettings = getIvySettings(logger);
		if (ivySettings == null) {
			return null;
		}
		File resolutionCacheRoot = ivySettings.getResolutionCacheManager()
				.getResolutionCacheRoot();
		return resolutionCacheRoot;
	}

	private IvySettings getIvySettings(PrintStream logger) throws AbortException {
		Message.setDefaultLogger(new IvyMessageImpl());
		File settingsLoc = (ivySettingsFile == null) ? null : new File(
				workspaceProper, ivySettingsFile);

		if ((settingsLoc != null) && (!settingsLoc.exists())) {
			throw new AbortException(
					Messages.IvyModuleSetBuild_NoSuchIvySettingsFile(settingsLoc
							.getAbsolutePath()));
		}

		ArrayList<File> propertyFiles = new ArrayList<File>();
		if (StringUtils.isNotBlank(ivySettingsPropertyFiles)) {
			for (String file : ivySettingsPropertyFiles.split(",")) {
				File propertyFile = new File(workspaceProper, file.trim());
				if (!propertyFile.exists()) {
					throw new AbortException(
							Messages.IvyModuleSetBuild_NoSuchPropertyFile(propertyFile
									.getAbsolutePath()));
				}
				propertyFiles.add(propertyFile);
			}
		}
		try {
			IvySettings ivySettings = new IvySettings(new EnvVarsVariableContainer(envVars));
			for (File file : propertyFiles) {
				ivySettings.loadProperties(file);
			}
			if (settingsLoc != null) {
				ivySettings.load(settingsLoc);
			} else {
				ivySettings.loadDefault();
			}
			if (ivyBranch != null) {
				ivySettings.setDefaultBranch(ivyBranch);
			}
			return ivySettings;
		} catch (Exception e) {
			logger.println("Error while reading the default Ivy 2.1 settings: "
					+ e.getMessage());
			logger.println(e.getStackTrace());
		}
		return null;
	}

}
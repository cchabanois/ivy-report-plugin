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

import hudson.ivy.IvyModuleSet;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.AbstractBuild;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action used to display the ivy report for the project
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 *
 */
public class IvyReportProjectAction implements Action {
	private final IvyModuleSet project;
	private static final String ICON_FILENAME = "/plugin/ivy-report-plugin/ivyReport.png";
	
	public IvyReportProjectAction(IvyModuleSet ivyModuleSet) {
		this.project = ivyModuleSet;
	}
	
	@Override
	public String getIconFileName() {
		return ICON_FILENAME;
	}

	@Override
	public String getDisplayName() {
		return "Ivy report";
	}

	@Override
	public String getUrlName() {
		return "ivyreport";
	} 

	public void doDynamic(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		IvyReportBuildAction lastResult = getLastResult();
		if (lastResult != null) {
			lastResult.doDynamic(req, rsp);
		}
	}
	
	private IvyReportBuildAction getLastResult() {
		for (AbstractBuild<?, ?> b = project.getLastSuccessfulBuild(); b != null; b = b.getPreviousNotFailedBuild()) {
            if (b.getResult() == Result.FAILURE)
                continue;
            IvyReportBuildAction r = b.getAction(IvyReportBuildAction.class);
            if (r != null)
                return r;
        }
        return null;
	}
	
}

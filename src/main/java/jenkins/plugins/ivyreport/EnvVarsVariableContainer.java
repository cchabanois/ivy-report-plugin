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

import hudson.EnvVars;

import org.apache.ivy.core.settings.IvyVariableContainerImpl;

/**
 * An ivy variable container that get environment variables from an
 * {@link EnvVars} instance instead of using System.getEnv
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 * 
 */
public class EnvVarsVariableContainer extends IvyVariableContainerImpl {
	private EnvVars envVars;

	public EnvVarsVariableContainer(EnvVars envVars) {
		this.envVars = envVars;
	}

	@Override
	public String getVariable(String name) {
		String val = null;
		String envPrefix = getEnvironmentPrefix();
		if ((envPrefix != null) && name.startsWith(envPrefix)) {
			val = envVars.get(name.substring(envPrefix.length()));
		} else {
			val = super.getVariable(name);
		}
		return val;
	}

	@Override
	public Object clone() {
		EnvVarsVariableContainer clone;
        clone = (EnvVarsVariableContainer) super.clone();
        clone.envVars = new EnvVars(envVars);
        return clone;
	}
	
}

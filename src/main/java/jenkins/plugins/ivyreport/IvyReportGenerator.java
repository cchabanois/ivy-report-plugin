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

import hudson.Launcher;
import hudson.model.Hudson;
import hudson.util.LogTaskListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.JAXPUtils;

/**
 * Generates the ivy report
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 */
public class IvyReportGenerator {
	private final Logger LOGGER = Logger.getLogger(IvyReportGenerator.class.getName());
	private final String[] confs;
	private final File targetDir;
	private final String resolveId;
	private final File resolutionCacheRoot;
	private final Hudson hudson;
	
	public IvyReportGenerator(Hudson hudson, String resolveId, String[] confs,
			File resolutionCacheRoot, File targetDir) {
		this.hudson = hudson;
		this.confs = confs;
		this.targetDir = targetDir;
		this.resolveId = resolveId;
		this.resolutionCacheRoot = resolutionCacheRoot;
	}

	public File generateReports() throws IOException, InterruptedException {
		File htmlReport = genHtmlReport();
		File[] dotFiles = genDotFiles();
		genSvgFiles(dotFiles);
		delete(dotFiles);
		return htmlReport;
	}

	private void delete(File[] files) {
		for (File file : files) {
			file.delete();
		}
	}
	
	private String getConfsAsComaSeparatedString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < confs.length; i++) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(confs[i]);
		}
		return sb.toString();
	}

	private File getReportFile(String conf) {
		return new File(resolutionCacheRoot, resolveId + "-" + conf + ".xml");
	}

	private File genHtmlReport() throws IOException {
        File[] generatedHtmlFiles = genStyled(getHtmlXslFile(), "html");
        File css = new File(targetDir, "ivy-report.css");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report.css"), css,
                null);
        return generatedHtmlFiles[0];
	}
	
	private File[] genDotFiles() throws IOException {
		return genStyled(getDotXslFile(), "dot");
	}
	
	private File[] genSvgFiles(File[] dotFiles) throws IOException, InterruptedException {
		File[] svgFiles = new File[dotFiles.length];
		for (int i = 0; i < dotFiles.length; i++) {
			svgFiles[i] = runDot(dotFiles[i]);
		}
		return svgFiles;
	}
	
    private File getDotXslFile() throws IOException {
        // style should be a file (and not an url)
        File style = new File(resolutionCacheRoot, "ivy-report-dot.xsl");
        FileUtil.copy(XmlReportOutputter.class.getResourceAsStream("ivy-report-dot.xsl"), style, null);
        return style;
    }	
	
    private File getHtmlXslFile() throws IOException {
        // style should be a file (and not an url)    	
    	File style = new File(resolutionCacheRoot, "ivy-report.xsl");
        FileUtil.copy(this.getClass().getResourceAsStream("ivy-report.xsl"), style, null);
        return style;
    }
    
    private File runDot(File inputFile) throws IOException, InterruptedException {
    	IvyReportPublisher.DescriptorImpl descriptor = hudson.getDescriptorByType(IvyReportPublisher.DescriptorImpl.class);
    	String dotPath;
    	if (descriptor != null) {
    		dotPath = descriptor.getDotExeOrDefault();
    	} else {
    		dotPath = IvyReportPublisher.DescriptorImpl.getDefaultDotExe();	
    	}
    	Launcher launcher = hudson.createLauncher(new LogTaskListener(LOGGER, Level.CONFIG));
    	File outputFile = new File(inputFile.getParentFile(), inputFile.getName().replace(".dot", ".svg"));
    	InputStream input = null;
    	OutputStream output = null;
    	try {
    		input = new FileInputStream(inputFile);
    		output = new FileOutputStream(outputFile);
            launcher.launch()
                    .cmds(dotPath,"-T" + "svg")
                    .stdin(input)
                    .stdout(output).start().join();
            return outputFile;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while waiting for dot-file to be created",e);
            throw e;
        }
        finally {
            if (output != null) {
                output.close();
            }
            if (input != null) {
            	input.close();
            }
        }
    }

	private File[] genStyled(File style, String ext)
			throws IOException {
		InputStream xsltStream = null;
		try {
			// create stream to stylesheet
			xsltStream = new BufferedInputStream(new FileInputStream(style));
			Source xsltSource = new StreamSource(xsltStream,
					JAXPUtils.getSystemId(style));

			// create transformer
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(xsltSource);

			// add standard parameters
			transformer.setParameter("confs", getConfsAsComaSeparatedString());
			transformer.setParameter("extension", "html");

			File[] generatedFiles = new File[confs.length];
			// create the report
			for (int i = 0; i < confs.length; i++) {
				File reportFile = getReportFile(confs[i]);
				File outFile = new File(targetDir, resolveId + "-" + confs[i]
						+ "." + ext);
				generatedFiles[i] = outFile;
				
				// make sure the output directory exist
				File outFileDir = outFile.getParentFile();
				if (!outFileDir.exists()) {
					if (!outFileDir.mkdirs()) {
						throw new BuildException("Unable to create directory: "
								+ outFileDir.getAbsolutePath());
					}
				}

				InputStream inStream = null;
				OutputStream outStream = null;
				try {
					inStream = new BufferedInputStream(new FileInputStream(
							reportFile));
					outStream = new BufferedOutputStream(new FileOutputStream(
							outFile));
					StreamResult res = new StreamResult(outStream);
					Source src = new StreamSource(inStream,
							JAXPUtils.getSystemId(style));
					transformer.transform(src, res);
				} catch (TransformerException e) {
					throw new BuildException(e);
				} finally {
					if (inStream != null) {
						try {
							inStream.close();
						} catch (IOException e) {
							// ignore
						}
					}
					if (outStream != null) {
						try {
							outStream.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
			return generatedFiles;
		} catch (TransformerConfigurationException e) {
			throw new BuildException(e);
		} finally {
			if (xsltStream != null) {
				try {
					xsltStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
}

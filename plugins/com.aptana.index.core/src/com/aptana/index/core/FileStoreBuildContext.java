/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.index.core;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

import com.aptana.core.build.IValidationItem;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.IOUtil;
import com.aptana.index.core.build.BuildContext;

/**
 * A BuildContext subclass that wraps IFileStores. This is meant solely for use by the index code when we're indexing
 * files outside the build process. As a result, this context ignores problem markers since none should be generated by
 * index participants. This class is meant as an adapter so we can treat build-triggered indexing the same as non-build
 * indexing (CSS/HTML/JS metadata, ruby std/core lib, etc)
 * 
 * @author cwilliams
 */
public class FileStoreBuildContext extends BuildContext
{

	private IFileStore fileStore;

	public FileStoreBuildContext(IFileStore fileStore)
	{
		super(null);
		this.fileStore = fileStore;
	}

	@Override
	public IProject getProject()
	{
		return null;
	}

	@Override
	public IFile getFile()
	{
		return null;
	}

	protected IContentType[] getContentTypes()
	{
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects == null || projects.length == 0)
		{
			IContentTypeManager manager = Platform.getContentTypeManager();
			return manager.findContentTypesFor(getName());
		}
		for (IProject project : projects)
		{
			try
			{
				IContentType[] type = project.getContentTypeMatcher().findContentTypesFor(getName());
				if (type != null)
				{
					return type;
				}
			}
			catch (CoreException e)
			{
				IdeLog.logError(IndexPlugin.getDefault(), e);
			}
		}
		return NO_CONTENT_TYPES;
	}

	@Override
	public String getName()
	{
		return fileStore.getName();
	}

	@Override
	public URI getURI()
	{
		return fileStore.toURI();
	}

	@Override
	public void putProblems(String markerType, Collection<IValidationItem> problems)
	{
		// ignore. Shouldn't be any markers generated on external resources, or during indexing
	}
	
	@Override
	protected String getCharset() throws CoreException
	{
		return null;
	}
	
	@Override
	public InputStream openInputStream(IProgressMonitor monitor) throws CoreException
	{
		return fileStore.openInputStream(EFS.NONE, monitor);
	}
}

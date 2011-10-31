/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.build;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aptana.index.core.build.BuildContext;

public interface IBuildParticipant
{

	public void clean(IProject project, IProgressMonitor monitor);

	public int getPriority();

	public void setPriority(int priority);

	public void buildFile(BuildContext context, IProgressMonitor monitor);

	public void deleteFile(BuildContext context, IProgressMonitor monitor);
}
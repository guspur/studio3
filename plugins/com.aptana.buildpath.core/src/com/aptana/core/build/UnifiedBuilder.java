/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.build;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.aptana.buildpath.core.BuildPathCorePlugin;
import com.aptana.core.CorePlugin;
import com.aptana.core.IDebugScopes;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.resources.IMarkerConstants;
import com.aptana.index.core.build.BuildContext;

public class UnifiedBuilder extends IncrementalProjectBuilder
{

	public static final String ID = "com.aptana.ide.core.unifiedBuilder"; //$NON-NLS-1$

	public UnifiedBuilder()
	{
	}

	private static void removeProblemsAndTasksFor(IResource resource)
	{
		try
		{
			if (resource != null && resource.exists())
			{
				resource.deleteMarkers(IMarkerConstants.PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IMarkerConstants.TASK_MARKER, true, IResource.DEPTH_INFINITE);
			}
		}
		catch (CoreException e)
		{
			// assume there were no problems
		}
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException
	{
		super.clean(monitor);

		// List<IBuildParticipant> participants = getBuildParticipantManager().getBuildParticipants();
		// SubMonitor sub = SubMonitor.convert(monitor, participants.size() + 1);

		IProject project = getProject();
		removeProblemsAndTasksFor(project);
		// sub.worked(1);

		// FIXME Should we visit all files and call "deleteFile" sort of like what we do with fullBuild?
		// for (IBuildParticipant participant : participants)
		// {
		// participant.clean(project, sub.newChild(1));
		// }
		// sub.done();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException
	{
		String projectName = getProject().getName();
		long startTime = System.nanoTime();

		if (kind == IncrementalProjectBuilder.FULL_BUILD)
		{
			if (IdeLog.isInfoEnabled(CorePlugin.getDefault(), IDebugScopes.BUILDER))
			{
				// @formatter:off
				IdeLog.logInfo(
					CorePlugin.getDefault(),
					MessageFormat.format(Messages.UnifiedBuilder_PerformingFullBuld, projectName),
					IDebugScopes.BUILDER
				);
				// @formatter:on
			}
			fullBuild(monitor);
		}
		else
		{
			IResourceDelta delta = getDelta(getProject());
			if (delta == null)
			{
				// @formatter:off
				IdeLog.logInfo(
					CorePlugin.getDefault(),
					MessageFormat.format(Messages.UnifiedBuilder_PerformingFullBuildNullDelta, projectName),
					IDebugScopes.BUILDER
				);
				// @formatter:on
				fullBuild(monitor);
			}
			else
			{
				// @formatter:off
				IdeLog.logInfo(
					CorePlugin.getDefault(),
					MessageFormat.format(Messages.UnifiedBuilder_PerformingIncrementalBuild, projectName),
					IDebugScopes.BUILDER
				);
				// @formatter:on
				incrementalBuild(delta, monitor);
			}
		}

		double endTime = ((double) System.nanoTime() - startTime) / 1000000;
		// @formatter:off
		IdeLog.logInfo(
			CorePlugin.getDefault(),
			MessageFormat.format(Messages.UnifiedBuilder_FinishedBuild, projectName, endTime),
			IDebugScopes.BUILDER
		);
		// @formatter:on

		return null;
	}

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor)
	{
		try
		{
			SubMonitor sub = SubMonitor.convert(monitor, 100);
			ResourceCollector collector = new ResourceCollector();
			delta.accept(collector);

			// Notify of the removed files
			removeFiles(collector.filesToRemoveFromIndex, sub.newChild(25));

			// Now build the new/updated files
			buildFiles(collector.filesToIndex, sub.newChild(75));
		}
		catch (CoreException e)
		{
			IdeLog.logError(BuildPathCorePlugin.getDefault(), e);
		}
	}

	private void removeFiles(Set<IFile> filesToRemoveFromIndex, IProgressMonitor monitor) throws CoreException
	{
		if (filesToRemoveFromIndex == null || filesToRemoveFromIndex.isEmpty())
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, 16 * filesToRemoveFromIndex.size());
		for (IFile file : filesToRemoveFromIndex)
		{
			BuildContext context = new BuildContext(file);
			sub.worked(1);
			List<IBuildParticipant> participants = getBuildParticipantManager().getBuildParticipants(
					context.getContentType());
			sub.worked(5);
			deleteFile(context, participants, sub.newChild(10));
		}
		sub.done();
	}

	protected void deleteFile(BuildContext context, List<IBuildParticipant> participants, IProgressMonitor monitor)
	{
		if (participants == null || participants.isEmpty())
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, participants.size());
		for (IBuildParticipant participant : participants)
		{
			participant.deleteFile(context, sub.newChild(1));
		}
		sub.done();
	}

	protected IBuildParticipantManager getBuildParticipantManager()
	{
		return BuildPathCorePlugin.getDefault().getBuildParticipantManager();
	}

	private void fullBuild(IProgressMonitor monitor) throws CoreException
	{
		CollectingResourceVisitor visitor = new CollectingResourceVisitor();
		getProject().accept(visitor);
		buildFiles(visitor.files, monitor);
	}

	protected void buildFiles(Collection<IFile> files, IProgressMonitor monitor) throws CoreException
	{
		if (files == null || files.isEmpty())
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, 16 * files.size());
		for (IFile file : files)
		{
			BuildContext context = new BuildContext(file);
			sub.worked(1);
			List<IBuildParticipant> participants = getBuildParticipantManager().getBuildParticipants(
					context.getContentType());
			sub.worked(5);
			buildFile(context, participants, sub.newChild(10));
		}
		sub.done();
	}

	protected void buildFile(BuildContext context, List<IBuildParticipant> participants, IProgressMonitor monitor)
			throws CoreException
	{
		if (participants == null || participants.isEmpty())
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, 2 * participants.size());
		for (IBuildParticipant participant : participants)
		{
			participant.buildFile(context, sub.newChild(1));
		}
		updateMarkers(context, sub.newChild(participants.size()));
		sub.done();
	}

	private void updateMarkers(BuildContext context, IProgressMonitor monitor)
	{
		final IFile file = context.getFile();
		final Map<String, Collection<IValidationItem>> itemsByType = context.getProblems();
		if (itemsByType == null || itemsByType.isEmpty())
		{
			return;
		}
		// Performance fix: schedules the error handling as a single workspace update so that we don't trigger a
		// bunch of resource updated events while problem markers are being added to the file.
		IWorkspaceRunnable runnable = new IWorkspaceRunnable()
		{

			public void run(IProgressMonitor monitor)
			{
				updateMarkers(file, itemsByType);
			}
		};

		try
		{
			ResourcesPlugin.getWorkspace().run(runnable, getMarkerRule(file), IWorkspace.AVOID_UPDATE, monitor);
		}
		catch (CoreException e)
		{
			IdeLog.logError(BuildPathCorePlugin.getDefault(), "Error updating markers", e);
		}
	}

	private static ISchedulingRule getMarkerRule(Object resource)
	{
		if (resource instanceof IResource)
		{
			return ResourcesPlugin.getWorkspace().getRuleFactory().markerRule((IResource) resource);
		}
		return null;
	}

	/**
	 * Collects all files with infinite depth.
	 * 
	 * @author cwilliams
	 */
	private static class CollectingResourceVisitor implements IResourceVisitor
	{
		Collection<IFile> files;

		CollectingResourceVisitor()
		{
			files = new ArrayList<IFile>();
		}

		public boolean visit(IResource resource) throws CoreException
		{
			if (IResource.FILE == resource.getType())
			{
				files.add((IFile) resource);
				return false;
			}
			return true;
		}
	}

	private static class ResourceCollector implements IResourceDeltaVisitor
	{
		Set<IFile> filesToIndex = new HashSet<IFile>();
		Set<IFile> filesToRemoveFromIndex = new HashSet<IFile>();

		public boolean visit(IResourceDelta delta) throws CoreException
		{
			IResource resource = delta.getResource();
			if (resource instanceof IFile)
			{
				if (delta.getKind() == IResourceDelta.ADDED
						|| (delta.getKind() == IResourceDelta.CHANGED && ((delta.getFlags() & (IResourceDelta.CONTENT | IResourceDelta.ENCODING)) != 0)))
				{
					filesToIndex.add((IFile) resource);
				}
				else if (delta.getKind() == IResourceDelta.REMOVED)
				{
					filesToRemoveFromIndex.add((IFile) resource);
				}
			}
			return true;
		}
	}

	private synchronized void updateMarkers(IFile file, Map<String, Collection<IValidationItem>> itemsByType)
	{
		if (!file.exists())
		{
			// no need to update the marker when the resource no longer exists
			return;
		}

		// FIXME Do a diff like we do in ValidationManager!
		for (String markerType : itemsByType.keySet())
		{
			try
			{
				Collection<IValidationItem> newItems = itemsByType.get(markerType);

				// deletes the old markers
				file.deleteMarkers(markerType, true, IResource.DEPTH_INFINITE);

				// adds the new ones
				if (newItems != null)
				{
					addMarkers(newItems, markerType, file);
				}
			}
			catch (CoreException e)
			{
				IdeLog.logError(BuildPathCorePlugin.getDefault(), e);
			}
		}
	}

	private void addMarkers(Collection<IValidationItem> items, String markerType, IFile file) throws CoreException
	{
		for (IValidationItem item : items)
		{
			IMarker marker = file.createMarker(markerType);
			marker.setAttributes(item.createMarkerAttributes());
		}
	}
}
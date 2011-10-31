package com.aptana.editor.css.internal.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.core.build.AbstractBuildParticipant;
import com.aptana.core.build.IValidationItem;
import com.aptana.core.logging.IdeLog;
import com.aptana.editor.css.CSSPlugin;
import com.aptana.editor.css.parsing.ast.CSSCommentNode;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.IParseRootNode;

public class CSSTaskDetector extends AbstractBuildParticipant
{

	public void clean(IProject project, IProgressMonitor monitor)
	{
		// TODO Auto-generated method stub
	}

	public void buildFile(BuildContext context, IProgressMonitor monitor)
	{
		Collection<IValidationItem> tasks = detectTasks(context, monitor);
		context.putProblems(IMarker.TASK, tasks);
	}

	public void deleteFile(BuildContext context, IProgressMonitor monitor)
	{
		context.removeProblems(IMarker.TASK);
	}

	private Collection<IValidationItem> detectTasks(BuildContext context, IProgressMonitor monitor)
	{
		try
		{
			return detectTasks(context.getAST(), context, monitor);
		}
		catch (CoreException e)
		{
			IdeLog.logError(CSSPlugin.getDefault(), e);
		}
		return Collections.emptyList();
	}

	public Collection<IValidationItem> detectTasks(IParseRootNode rootNode, BuildContext context,
			IProgressMonitor monitor)
	{
		Collection<IValidationItem> tasks = new ArrayList<IValidationItem>();
		try
		{
			IParseNode[] comments = rootNode.getCommentNodes();
			if (comments == null || comments.length == 0)
			{
				return Collections.emptyList();
			}

			SubMonitor sub = SubMonitor.convert(monitor, comments.length);
			String source = context.getContents();
			String filePath = context.getURI().toString();
			for (IParseNode commentNode : comments)
			{
				if (commentNode instanceof CSSCommentNode)
				{
					tasks.addAll(processCommentNode(filePath, source, 0, commentNode, "*/")); //$NON-NLS-1$
				}
				sub.worked(1);
			}
			sub.done();
		}
		catch (CoreException e)
		{
			IdeLog.logError(CSSPlugin.getDefault(), e);
		}
		return tasks;
	}
}

/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;

import org.eclipse.core.resources.IMarker;

import com.aptana.core.resources.TaskTag;
import com.aptana.core.util.StringUtil;
import com.aptana.parsing.ast.IParseNode;

public abstract class AbstractBuildParticipant implements IBuildParticipant
{

	private int priority;

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	protected int getLineNumber(int start, String source)
	{
		if (start < 0 || start >= source.length())
		{
			return -1;
		}
		if (start == 0)
		{
			return 1;
		}

		Matcher m = StringUtil.LINE_SPLITTER.matcher(source.substring(0, start));
		int line = 1;
		while (m.find())
		{
			int offset = m.start();
			if (offset > start)
			{
				break;
			}
			line++;
		}
		return line;
	}

	/**
	 * Common code for detecting tasks in comment nodes from ASTs
	 **/
	protected Collection<IValidationItem> processCommentNode(String filePath, String source, int initialOffset,
			IParseNode commentNode, String commentEnding)
	{
		Collection<IValidationItem> tasks = new ArrayList<IValidationItem>();
		String text = commentNode.getText();
		if (text == null || text.length() == 0)
		{
			text = getText(source, commentNode);
		}

		if (!TaskTag.isCaseSensitive())
		{
			text = text.toLowerCase();
		}
		int offset = initialOffset;
		String[] lines = StringUtil.LINE_SPLITTER.split(text);
		for (String line : lines)
		{
			for (TaskTag entry : TaskTag.getTaskTags())
			{
				String tag = entry.getName();
				if (!TaskTag.isCaseSensitive())
				{
					tag = tag.toLowerCase();
				}
				int index = line.indexOf(tag);
				if (index == -1)
				{
					continue;
				}

				String message = new String(line.substring(index).trim());
				// Remove "*/" or whatever language specific comment ending from the end of the line!
				if (message.endsWith(commentEnding))
				{
					message = message.substring(0, message.length() - commentEnding.length()).trim();
				}
				int start = commentNode.getStartingOffset() + offset + index;
				tasks.add(createTask(filePath, message, entry.getPriority(), getLineNumber(start, source), start, start
						+ message.length()));
			}
			// FIXME This doesn't take the newline into account from split!
			offset += line.length();
		}
		return tasks;
	}

	private String getText(String source, IParseNode commentNode)
	{
		return new String(source.substring(commentNode.getStartingOffset(), commentNode.getEndingOffset() + 1));
	}

	protected ValidationItem createTask(String sourcePath, String message, Integer priority, int lineNumber,
			int offset, int endOffset)
	{
		return new ValidationItem(IMarker.SEVERITY_INFO, message, offset, endOffset - offset, lineNumber, sourcePath);
	}
}

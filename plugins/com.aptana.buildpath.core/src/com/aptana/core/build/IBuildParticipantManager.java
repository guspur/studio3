package com.aptana.core.build;

import java.util.List;

public interface IBuildParticipantManager
{

	/**
	 * Grabs the build participants that are associated with the passed-in content type.
	 * 
	 * @param contentTypeId
	 * @return
	 */
	List<IBuildParticipant> getBuildParticipants(String contentTypeId);

}

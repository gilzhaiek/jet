// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.dataset.v0_6;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.DatasetSinkSourceManager;


/**
 * The task manager factory for reading the entire contents of a dataset.
 * 
 * @author Brett Henderson
 */
public class QueryWayDatasetFactory extends TaskManagerFactory {
	
	//private static final String ARG_COMPLETE_WAYS = "completeWays";
	//private static final boolean DEFAULT_COMPLETE_WAYS = false;
	private static final String ARG_NID = "nid";
	private static final long DEFAULT_NID = 1;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		
		long nid = getLongArgument(taskConfig, ARG_NID, DEFAULT_NID);
		
		return new DatasetSinkSourceManager(
			taskConfig.getId(),
			new QueryWayDataset(nid),
			taskConfig.getPipeArgs()
		);
	}
}

// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.pgsnapshot.v0_6.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.OsmosisConstants;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainerIterator;
import org.openstreetmap.osmosis.core.container.v0_6.DatasetContext;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityManager;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainerIterator;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainerIterator;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainerIterator;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.MultipleSourceIterator;
import org.openstreetmap.osmosis.core.store.ReleasableAdaptorForIterator;
import org.openstreetmap.osmosis.core.store.UpcastIterator;
import org.openstreetmap.osmosis.pgsnapshot.common.DatabaseContext;
import org.openstreetmap.osmosis.pgsnapshot.common.PolygonBuilder;
import org.openstreetmap.osmosis.pgsnapshot.common.SchemaVersionValidator;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlVersionConstants;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * Provides read-only access to a PostgreSQL dataset store. Each thread
 * accessing the store must create its own reader. It is important that all
 * iterators obtained from this reader are released before releasing the reader
 * itself.
 * 
 * @author Brett Henderson
 */
public class PostgreSqlDatasetContext implements DatasetContext {
	
	private static final Logger LOG = Logger.getLogger(PostgreSqlDatasetContext.class.getName());
	
	
	private DatabaseLoginCredentials loginCredentials;
	private DatabasePreferences preferences;
	private DatabaseCapabilityChecker capabilityChecker;
	private boolean initialized;
	private DatabaseContext dbCtx;
	private JdbcTemplate jdbcTemplate;
	private UserDao userDao;
	private NodeDao nodeDao;
	private WayDao wayDao;
	private RelationDao relationDao;
	private PostgreSqlEntityManager<Node> nodeManager;
	private PostgreSqlEntityManager<Way> wayManager;
	private PostgreSqlEntityManager<Relation> relationManager;
	private PolygonBuilder polygonBuilder;
	
	
	/**
	 * Creates a new instance.
	 * 
	 * @param loginCredentials
	 *            Contains all information required to connect to the database.
	 * @param preferences
	 *            Contains preferences configuring database behaviour.
	 */
	public PostgreSqlDatasetContext(DatabaseLoginCredentials loginCredentials, DatabasePreferences preferences) {
		this.loginCredentials = loginCredentials;
		this.preferences = preferences;
		
		polygonBuilder = new PolygonBuilder();
		
		initialized = false;

	}
	
	
	/**
	 * Initialises the database connection and associated data access objects.
	 */
	private void initialize() {
		if (dbCtx == null) {
			ActionDao actionDao;
			
			dbCtx = new DatabaseContext(loginCredentials);
			jdbcTemplate = dbCtx.getJdbcTemplate();
			
			dbCtx.beginTransaction();
			
			new SchemaVersionValidator(jdbcTemplate, preferences).validateVersion(
					PostgreSqlVersionConstants.SCHEMA_VERSION);
			
			capabilityChecker = new DatabaseCapabilityChecker(dbCtx);
			
			actionDao = new ActionDao(dbCtx);
			userDao = new UserDao(dbCtx, actionDao);
			nodeDao = new NodeDao(dbCtx, actionDao);
			wayDao = new WayDao(dbCtx, actionDao);
			relationDao = new RelationDao(dbCtx, actionDao);
			
			nodeManager = new PostgreSqlEntityManager<Node>(nodeDao, userDao);
			wayManager = new PostgreSqlEntityManager<Way>(wayDao, userDao);
			relationManager = new PostgreSqlEntityManager<Relation>(relationDao, userDao);
		}
		
		initialized = true;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public Node getNode(long id) {
		LOG.info("node id=" + id);
		return getNodeManager().getEntity(id);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public Way getWay(long id) {
		LOG.info("way id=" + id);
		return getWayManager().getEntity(id);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public Relation getRelation(long id) {
		LOG.info("relation id=" + id);
		return getRelationManager().getEntity(id);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityManager<Node> getNodeManager() {
		if (!initialized) {
			initialize();
		}
		
		return nodeManager;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityManager<Way> getWayManager() {
		if (!initialized) {
			initialize();
		}
		
		return wayManager;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityManager<Relation> getRelationManager() {
		if (!initialized) {
			initialize();
		}
		
		return relationManager;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> iterate() {
		List<Bound> bounds;
		List<ReleasableIterator<EntityContainer>> sources;
		
		if (!initialized) {
			initialize();
		}
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		bounds.add(new Bound("Osmosis " + OsmosisConstants.VERSION));
		
		sources = new ArrayList<ReleasableIterator<EntityContainer>>();
		
		sources.add(new UpcastIterator<EntityContainer, BoundContainer>(
				new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		sources.add(new UpcastIterator<EntityContainer, NodeContainer>(
				new NodeContainerIterator(nodeDao.iterate())));
		sources.add(new UpcastIterator<EntityContainer, WayContainer>(
				new WayContainerIterator(wayDao.iterate())));
		sources.add(new UpcastIterator<EntityContainer, RelationContainer>(
				new RelationContainerIterator(relationDao.iterate())));
		
		return new MultipleSourceIterator<EntityContainer>(sources);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> iterateBoundingBox(
			double left, double right, double top, double bottom, boolean completeWays) {
		List<Bound> bounds;
		Point[] bboxPoints;
		Polygon bboxPolygon;
		int rowCount=0;
		List<ReleasableIterator<EntityContainer>> resultSets;
		boolean completeRelations = false;
		
		
		//LOG.setLevel(Level.FINE);
		
		if (!initialized) {
			initialize();
		}
		
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		bounds.add(new Bound(right, left, top, bottom, "Osmosis " + OsmosisConstants.VERSION));
		
		// PostgreSQL sometimes incorrectly chooses to perform full table scans, these options
		// prevent this. Note that this is not recommended practice according to documentation
		// but fixing this would require modifying the table statistics gathering
		// configuration to produce better plans.
		jdbcTemplate.update("SET enable_seqscan = false");
		jdbcTemplate.update("SET enable_mergejoin = false");
		jdbcTemplate.update("SET enable_hashjoin = false");
		

		bboxPoints = new Point[5];
		bboxPoints[0] = new Point(left, bottom);
		bboxPoints[1] = new Point(left, top);
		bboxPoints[2] = new Point(right, top);
		bboxPoints[3] = new Point(right, bottom);
		bboxPoints[4] = new Point(left, bottom);
		bboxPolygon = polygonBuilder.createPolygon(bboxPoints);
		
		
		
		// Select all nodes inside the box into the node temp table.
		//---------------------------bbox_nodes-------------------------------------------------------
		LOG.finer("Selecting all nodes inside bounding box.");
		jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_nodes ON COMMIT DROP AS"
				+ " SELECT * FROM nodes WHERE (geom && ?)",
				new PGgeometry(bboxPolygon));
		
		LOG.finer("Adding a primary key to the temporary nodes table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_nodes ADD CONSTRAINT pk_bbox_nodes PRIMARY KEY (id)");

		LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
		jdbcTemplate.update("ANALYZE bbox_nodes");
		rowCount = jdbcTemplate.queryForInt("SELECT count(*) from bbox_nodes");
		LOG.info("1. nodes: " + rowCount + " insert into BoundingBox.bbox_nodes.");
		
		//---------------------------bbox_ways-------------------------------------------------------
		// Select all ways inside the bounding box into the way temp table.
		LOG.info("2. isWayLinestringSupported=" + capabilityChecker.isWayLinestringSupported() +", isWayBboxSupported=" + capabilityChecker.isWayBboxSupported());
		//using way linestring geometry
		/*if (capabilityChecker.isWayLinestringSupported()) {
			LOG.finer("Selecting all ways inside bounding box using way linestring geometry.");
			// We have full way geometry available so select ways
			// overlapping the requested bounding box.
			rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT * FROM ways WHERE (linestring && ?)",
					new PGgeometry(bboxPolygon));
			
		} else if (capabilityChecker.isWayBboxSupported()) {
			LOG.info("Selecting all ways inside bounding box using dynamically built"
					+ " way linestring with way bbox indexing.");
			// The inner query selects the way id and node coordinates for
			// all ways constrained by the way bounding box which is
			// indexed.
			// The middle query converts the way node coordinates into
			// linestrings.
			// The outer query constrains the query to the linestrings
			// inside the bounding box. These aren't indexed but the inner
			// query way bbox constraint will minimise the unnecessary data.
			rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT w.* FROM ("
					+ "SELECT c.id AS id, First(c.version) AS version, First(c.user_id) AS user_id,"
					+ " First(c.tstamp) AS tstamp, First(c.changeset_id) AS changeset_id, First(c.tags) AS tags,"
					+ " First(c.nodes) AS nodes, ST_MakeLine(c.geom) AS way_line FROM ("
					+ "SELECT w.*, n.geom AS geom FROM nodes n"
					+ " INNER JOIN way_nodes wn ON n.id = wn.node_id"
					+ " INNER JOIN ways w ON wn.way_id = w.id"
					+ " WHERE (w.bbox && ?) ORDER BY wn.way_id, wn.sequence_id"
					+ ") c "
					+ "GROUP BY c.id"
					+ ") w "
					+ "WHERE (w.way_line && ?)",
					new PGgeometry(bboxPolygon),
					new PGgeometry(bboxPolygon)
			);
			
		} else */{
			LOG.info("2. Selecting all way ids inside bounding box using already selected nodes.");
			// No way bbox support is available so select ways containing
			// the selected nodes.
			rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT w.* FROM ways w"
					+ " INNER JOIN ("
					+ " SELECT wn.way_id FROM way_nodes wn"
					+ " INNER JOIN bbox_nodes n ON wn.node_id = n.id GROUP BY wn.way_id"
					+ ") wids ON w.id = wids.way_id"
			);
		}
		
		LOG.finer("Adding a primary key to the temporary ways table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_ways ADD CONSTRAINT pk_bbox_ways PRIMARY KEY (id)");
		LOG.finer("Updating query analyzer statistics on the temporary ways table.");
		jdbcTemplate.update("ANALYZE bbox_ways");
		rowCount = jdbcTemplate.queryForInt("SELECT count(*) from bbox_ways");
		LOG.info("2. ways: " +rowCount + " inserted into BoundingBox.bbox_ways.");
		
		//---------------------------bbox_relations-------------------------------------------------------
		// Select all relations containing the nodes or ways into the relation table.
		LOG.finer("Selecting all relation ids containing selected nodes or ways.");
		rowCount = jdbcTemplate.update(
			"CREATE TEMPORARY TABLE bbox_relations ON COMMIT DROP AS"
				+ " SELECT r.* FROM relations r"
				+ " INNER JOIN ("
				+ "    SELECT relation_id FROM ("
				+ "        SELECT rm.relation_id AS relation_id FROM relation_members rm"
				+ "        INNER JOIN bbox_nodes n ON rm.member_id = n.id WHERE rm.member_type = 'N' "
				+ "        UNION "
				+ "        SELECT rm.relation_id AS relation_id FROM relation_members rm"
				+ "        INNER JOIN bbox_ways w ON rm.member_id = w.id WHERE rm.member_type = 'W'"
				+ "     ) rids GROUP BY relation_id"
				+ ") rids ON r.id = rids.relation_id"
		);
		
		
		LOG.finer("Adding a primary key to the temporary relations table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_relations ADD CONSTRAINT pk_bbox_relations PRIMARY KEY (id)");
		
		LOG.finer("Updating query analyzer statistics on the temporary relations table.");
		jdbcTemplate.update("ANALYZE bbox_relations");
		
		rowCount = jdbcTemplate.queryForInt("SELECT count(*) from bbox_relations");
		LOG.info("3. relations: " +rowCount + " insert into BoundingBox.bbox_relations.");
		
			
		if (completeRelations){
			// Include all ways are referenced by missed relations into the
			// way table and repeat until no more inclusions occur.
			
			//1. get relation id which has not tag "boundary=administrative"
			int numRelations = jdbcTemplate.queryForInt(
					"SELECT count(*) from bbox_relations");
			LOG.finer("----2.1 relation rows all return_code=" + numRelations);
						
			
			if (numRelations > 0){
				
				//child relation ids
				rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE relation_ids_1 ON COMMIT DROP AS "
						+ "    SELECT rm0.member_id AS mid FROM relation_members rm0"
						+ "    INNER JOIN bbox_relations brm0 ON rm0.relation_id = brm0.id"
						+ "    WHERE rm0.member_type = 'R' "
						+ "    GROUP BY rm0.member_id"
						);
				
				
				rowCount = jdbcTemplate.queryForInt(
						"SELECT count(*) FROM relation_ids_1");				
				LOG.finer("----2.2 child relation rows=" + rowCount);
				
				//child relation ids without tag (boundary, administrative)	
				rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE relation_ids_2 ON COMMIT DROP AS "
						+ "    SELECT rid10.mid AS mid FROM relation_ids_1 AS rid10 WHERE rid10.mid NOT IN ("
						+ "    SELECT r10.id FROM relations r10 WHERE r10.id=rid10.mid AND r10.tags @> hstore('boundary', 'administrative')"
						+ " )"
						);
				
				rowCount = jdbcTemplate.queryForInt(
						"SELECT count(*) from relation_ids_2");				
				LOG.finer("----2.3 child relation rows without boundary=" + rowCount);
				
				//child relation
				do {
					LOG.finer("Selecting relations of selected relations.");
					rowCount = jdbcTemplate.update(
						"CREATE TEMPORARY TABLE bbox_relations_bb ON COMMIT DROP AS"
							+ " SELECT r0.* FROM relations r0 INNER JOIN "
							+ " relation_ids_2 ris0 ON ris0.mid  = r0.id"
					);
					LOG.finer(rowCount + " rows inserted into bbox_ways by completeRelations.");
				} while (rowCount > 0);

				
				LOG.finer("Adding a primary key to the temporary nodes table.");
				jdbcTemplate.update("ALTER TABLE ONLY bbox_relations_bb ADD CONSTRAINT pk_bbox_relations_bb PRIMARY KEY (id)");

				LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
				jdbcTemplate.update("ANALYZE bbox_relations_bb");
				
				
				
				int numChildRelations = jdbcTemplate.queryForInt(
				"SELECT count(*) from bbox_relations_bb");
				LOG.info("----2.1 child relation rows=" + numChildRelations);
				
				
				
				if (numChildRelations > 0){
						
		
					do {
						LOG.finer("Selecting parent relations of selected ways.");
						rowCount = jdbcTemplate.update(
							"INSERT INTO bbox_ways "
								+ "SELECT w2.* FROM ways w2 INNER JOIN ("
								+ "    SELECT rm2.member_id AS memberId FROM relation_members rm2"
								+ "    INNER JOIN bbox_relations_bb brm2 ON rm2.relation_id = brm2.id"
								+ "    WHERE rm2.member_type = 'W' AND NOT EXISTS ("
								+ "        SELECT * FROM bbox_ways bw2 WHERE rm2.member_id = bw2.id"
								+ "    ) GROUP BY rm2.member_id"
								+ ") rids2 ON w2.id = rids2.memberId"
						);
						LOG.finer(rowCount + " rows inserted into bbox_ways by completeRelations.");
					} while (rowCount > 0);
					
					LOG.finer("Updating query analyzer statistics on the temporary ways table.");
					jdbcTemplate.update("ANALYZE bbox_ways");
				
				
					
					// Include all nodes are referenced by missed relations into the
					// nodes table and repeat until no more inclusions occur.
					do {
						LOG.finer("Selecting parent relations of selected nodes.");
						rowCount = jdbcTemplate.update(
							"INSERT INTO bbox_nodes "
								+ "SELECT n3.* FROM nodes n3 INNER JOIN ("
								+ "    SELECT DISTINCT rm3.member_id AS memberId FROM relation_members rm3"
								+ "    INNER JOIN bbox_relations_bb brm3 ON rm3.relation_id = brm3.id"
								+ "    WHERE rm3.member_type = 'N' AND NOT EXISTS ("
								+ "        SELECT * FROM bbox_nodes bn3 WHERE rm3.member_id = bn3.id"
								+ "    ) GROUP BY rm3.member_id"
								+ ") rids3 ON n3.id = rids3.memberId"
						);
						LOG.info(rowCount + " rows inserted into bbox_nodes by completeRelations.");
					} while (rowCount > 0);
					
					LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
					jdbcTemplate.update("ANALYZE bbox_nodes");
					
			}//eof if numChildRelation
		 }//eof if numRelation
		
		}//eof completeRelations
		
		
		if (completeWays){
			
		
			// If complete ways is set, select all nodes contained by the ways into the node temp table.
			//if (completeWays) {
			LOG.finer("3.1 Selecting all nodes for selected ways.");
			jdbcTemplate.update("CREATE TEMPORARY TABLE bbox_way_nodes (id bigint) ON COMMIT DROP");
			jdbcTemplate.queryForList("SELECT unnest_bbox_way_nodes()");
			
			jdbcTemplate.update(
					"CREATE TEMPORARY TABLE bbox_missing_way_nodes ON COMMIT DROP AS "
					+ "SELECT buwn.id FROM (SELECT DISTINCT bwn.id FROM bbox_way_nodes bwn) buwn "
					+ "WHERE NOT EXISTS ("
					+ "    SELECT * FROM bbox_nodes WHERE id = buwn.id"
					+ ");"
			);
			jdbcTemplate.update("ALTER TABLE ONLY bbox_missing_way_nodes"
					+ " ADD CONSTRAINT pk_bbox_missing_way_nodes PRIMARY KEY (id)");
			jdbcTemplate.update("ANALYZE bbox_missing_way_nodes");
			
			rowCount = jdbcTemplate.update("INSERT INTO bbox_nodes "
					+ "SELECT n.* FROM nodes n INNER JOIN bbox_missing_way_nodes bwn ON n.id = bwn.id;");
			
			LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
			jdbcTemplate.update("ANALYZE bbox_nodes");
			LOG.info("3.2 ways:" + rowCount + " inserted into bbox_nodes by BoundingBox.completeWays.");
			
		}//eof completeWays
		
		

		
		// Create iterators for the selected records for each of the entity types.
		LOG.finer("Iterating over results.");
		resultSets = new ArrayList<ReleasableIterator<EntityContainer>>();
		resultSets.add(
				new UpcastIterator<EntityContainer, BoundContainer>(
						new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		resultSets.add(
				new UpcastIterator<EntityContainer, NodeContainer>(
						new NodeContainerIterator(nodeDao.iterate("bbox_"))));
		resultSets.add(
				new UpcastIterator<EntityContainer, WayContainer>(
						new WayContainerIterator(wayDao.iterate("bbox_"))));
		resultSets.add(
				new UpcastIterator<EntityContainer, RelationContainer>(
						new RelationContainerIterator(relationDao.iterate("bbox_"))));

		
		// Merge all readers into a single result iterator and return.			
		return new MultipleSourceIterator<EntityContainer>(resultSets);
	}//eof iterateBoundingBox


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		dbCtx.commitTransaction();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		if (dbCtx != null) {
			dbCtx.release();
			
			dbCtx = null;
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> getNodeById( long id ) {
		   
		List<Bound> bounds;
		int rowCount;
		List<ReleasableIterator<EntityContainer>> resultSets;
		//LOG.setLevel(Level.FINE);
		
		if (!initialized) {
			initialize();
		}
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		double left =1.0f,right=1.2f,bottom=3.0f,top=3.2f;
		bounds.add(new Bound(right, left, top, bottom, "Osmosis " + OsmosisConstants.VERSION));
		
		LOG.info("getNodeById.");
		
		// PostgreSQL sometimes incorrectly chooses to perform full table scans, these options
		// prevent this. Note that this is not recommended practice according to documentation
		// but fixing this would require modifying the table statistics gathering
		// configuration to produce better plans.
		jdbcTemplate.update("SET enable_seqscan = false");
		jdbcTemplate.update("SET enable_mergejoin = false");
		jdbcTemplate.update("SET enable_hashjoin = false");
		
		//---------------------------nodes-------------------------------------------------------
		LOG.info("Selecting nodes by id.");
		rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_nodes ON COMMIT DROP AS"
				+ " SELECT n.* FROM nodes n WHERE n.id=?", id);
		
		LOG.finer("Adding a primary key to the temporary nodes table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_nodes ADD CONSTRAINT pk_bbox_nodes PRIMARY KEY (id)");

		LOG.finer("Updating query analyzer statistics on the temporary bbox_nodes table.");
		jdbcTemplate.update("ANALYZE bbox_nodes");
		

		// Create iterators for the selected records for each of the entity types.
		resultSets = new ArrayList<ReleasableIterator<EntityContainer>>();
		resultSets.add(
				new UpcastIterator<EntityContainer, BoundContainer>(
						new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		resultSets.add(
				new UpcastIterator<EntityContainer, NodeContainer>(
						new NodeContainerIterator(nodeDao.iterate("bbox_"))));
		/*resultSets.add(
				new UpcastIterator<EntityContainer, WayContainer>(
						new WayContainerIterator(wayDao.iterate("bbox_"))));
		resultSets.add(
				new UpcastIterator<EntityContainer, RelationContainer>(
						new RelationContainerIterator(relationDao.iterate("bbox_"))));
		*/
		// Merge all readers into a single result iterator and return.			
		return new MultipleSourceIterator<EntityContainer>(resultSets);
	
	}//eof getNodeById
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> getWayById( long id){
		List<Bound> bounds;
		int rowCount;
		List<ReleasableIterator<EntityContainer>> resultSets;
		boolean completeWays = true;
		//LOG.setLevel(Level.FINE);
		
		if (!initialized) {
			initialize();
		}
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		double left =1.0f,right=1.2f,bottom=3.0f,top=3.2f;
		bounds.add(new Bound(right, left, top, bottom, "Osmosis " + OsmosisConstants.VERSION));
		
		LOG.info("getWayById.");
		
		// PostgreSQL sometimes incorrectly chooses to perform full table scans, these options
		// prevent this. Note that this is not recommended practice according to documentation
		// but fixing this would require modifying the table statistics gathering
		// configuration to produce better plans.
		jdbcTemplate.update("SET enable_seqscan = false");
		jdbcTemplate.update("SET enable_mergejoin = false");
		jdbcTemplate.update("SET enable_hashjoin = false");
		
		//---------------------------nodes-------------------------------------------------------
		LOG.finer("Selecting way by id.");
		rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
				+ " SELECT w.* FROM ways w WHERE w.id=?", id);
		
		LOG.finer("Adding a primary key to the temporary nodes table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_ways ADD CONSTRAINT pk_bbox_ways PRIMARY KEY (id)");

		LOG.finer("2. Updating query analyzer statistics on the temporary bbox_ways table.");
		jdbcTemplate.update("ANALYZE bbox_ways");
		
		
		
		// If complete ways is set, select all nodes contained by the ways into the node temp table.
		if (completeWays) {
			LOG.finer("3.1 Selecting all nodes for selected ways.");
					
			rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE bbox_nodes ON COMMIT DROP AS"
					+ " SELECT n2.* FROM nodes n2 INNER JOIN ("
					+ " SELECT DISTINCT wn2.node_id AS nodeId FROM way_nodes wn2 "
					+ " INNER JOIN bbox_ways bbw2 ON wn2.way_id = bbw2.id"
					+ " GROUP BY wn2.node_id"
					+ " ) wnds2 ON n2.id = wnds2.nodeId"
					);
			
			LOG.finer("3.2. nodes: " + rowCount + " insert into bbox_nodes.");
			
			LOG.finer("Adding a primary key to the temporary nodes table.");
			jdbcTemplate.update("ALTER TABLE ONLY bbox_nodes ADD CONSTRAINT pk_bbox_nodes PRIMARY KEY (id)");

			LOG.finer("Updating query analyzer statistics on the temporary bbox_nodes table.");
			jdbcTemplate.update("ANALYZE bbox_nodes");
			
			
			rowCount = jdbcTemplate.queryForInt(
					"SELECT count(*) from bbox_nodes");				
			LOG.info("-----3.3 bbox_nodes= return_code=" + rowCount);
					
		}
		
		
		// Create iterators for the selected records for each of the entity types.
		resultSets = new ArrayList<ReleasableIterator<EntityContainer>>();
		resultSets.add(
				new UpcastIterator<EntityContainer, BoundContainer>(
						new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		resultSets.add(
				new UpcastIterator<EntityContainer, NodeContainer>(
						new NodeContainerIterator(nodeDao.iterate("bbox_"))));
	
		resultSets.add(
				new UpcastIterator<EntityContainer, WayContainer>(
						new WayContainerIterator(wayDao.iterate("bbox_"))));
		
		/*resultSets.add(
				new UpcastIterator<EntityContainer, RelationContainer>(
						new RelationContainerIterator(relationDao.iterate("bbox_"))));
		*/
		// Merge all readers into a single result iterator and return.			
		return new MultipleSourceIterator<EntityContainer>(resultSets);
		
	}//eof getWayById

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> getRelationById( long id){
		List<Bound> bounds;
		int rowCount;
		List<ReleasableIterator<EntityContainer>> resultSets;
		boolean completeWays = true;
		boolean completeRelations = true;
		//LOG.setLevel(Level.FINE);
		
		if (!initialized) {
			initialize();
		}
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		double left =1.0f,right=1.2f,bottom=3.0f,top=3.2f;
		bounds.add(new Bound(right, left, top, bottom, "Osmosis " + OsmosisConstants.VERSION));
		
		LOG.info("getRelationById.");
		
		// PostgreSQL sometimes incorrectly chooses to perform full table scans, these options
		// prevent this. Note that this is not recommended practice according to documentation
		// but fixing this would require modifying the table statistics gathering
		// configuration to produce better plans.
		jdbcTemplate.update("SET enable_seqscan = false");
		jdbcTemplate.update("SET enable_mergejoin = false");
		jdbcTemplate.update("SET enable_hashjoin = false");
		
		//---------------------------relation-------------------------------------------------------
		LOG.finer("Selecting way by id.");
		rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_relations ON COMMIT DROP AS"
				+ " SELECT r.* FROM relations r WHERE r.id=?", id);
		
		LOG.finer("Adding a primary key to the temporary bbox_relations.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_relations ADD CONSTRAINT pk_bbox_relations PRIMARY KEY (id)");

		LOG.finer("2. Updating query analyzer statistics on the temporary bbox_relations table.");
		jdbcTemplate.update("ANALYZE bbox_relations");
						
		if (completeRelations){
			// Include all ways are referenced by missed relations into the
			// way table and repeat until no more inclusions occur.
						
			//1. get relation id which has not tag "boundary=administrative"
			int numRelations = jdbcTemplate.queryForInt(
					"SELECT count(*) from bbox_relations");
			LOG.info("----2.0 relation rows all return_code=" + numRelations);
			
	
			if (numRelations > 0){
				
				//child relations
				do {
					LOG.finer("Selecting relations of selected relations.");
					rowCount = jdbcTemplate.update(
						"CREATE TEMPORARY TABLE relations_a ON COMMIT DROP AS"
							+ " SELECT r0.* FROM relations r0 INNER JOIN ("
							+ "    SELECT rm0.member_id AS memberId FROM relation_members rm0"
							+ "    INNER JOIN bbox_relations brm0 ON rm0.relation_id = brm0.id"
							+ "    WHERE rm0.member_type = 'R' "
							+ "    GROUP BY rm0.member_id"
							+ ") rids0 ON r0.id = rids0.memberId"
					);
					LOG.finer(rowCount + " rows inserted into bbox_ways by completeRelations.");
				} while (rowCount > 0);
				
				int numChildRelations = jdbcTemplate.queryForInt(
				"SELECT count(*) from relations_a");
				LOG.info("----2.1 child relation rows, return_code=" + numChildRelations);
				
				
				
				if (numChildRelations > 0){
				
					LOG.finer("Adding a primary key to the temporary relations_a.");
					jdbcTemplate.update("ALTER TABLE ONLY relations_a ADD CONSTRAINT pk_relations_a PRIMARY KEY (id)");
					LOG.finer("Updating query analyzer statistics on the temporary ways table.");
					jdbcTemplate.update("ANALYZE relations_a");
					
					
					LOG.finer("Inserting child relation objects into bbox_relations.");
					rowCount = jdbcTemplate.update(
							"INSERT INTO bbox_relations "
							+ " SELECT ra.* FROM relations_a ra WHERE NOT EXISTS ("
							+ " SELECT * FROM bbox_relations br5 WHERE ra.id=br5.id"
					        + " ) "
					);
					
					numRelations = jdbcTemplate.queryForInt(
							"SELECT count(*) from bbox_relations");
					LOG.info("----2.1 relation rows all return_code=" + numRelations);
					LOG.finer("2.1 Updating query analyzer statistics on the temporary bbox_relations table.");
					jdbcTemplate.update("ANALYZE bbox_relations");

				}
				
				//child ways
				do {
					LOG.finer("Selecting parent relations of selected ways.");
					rowCount = jdbcTemplate.update(
						"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
							+ " SELECT w1.* FROM ways w1 INNER JOIN ("
							+ "    SELECT DISTINCT rm1.member_id AS memberId FROM relation_members rm1"
							+ "    INNER JOIN bbox_relations brm1 ON rm1.relation_id = brm1.id"
							+ "    WHERE rm1.member_type = 'W' "
					        + "    GROUP BY rm1.member_id"
							+ ") rids1 ON w1.id = rids1.memberId"
					);
					LOG.finer(rowCount + " rows inserted into bbox_ways by completeRelations.");
				} while (rowCount > 0);
				
				rowCount = jdbcTemplate.queryForInt(
				"SELECT count(*) from bbox_ways");
				LOG.info("----2.2 way rows, return_code=" + rowCount);
				
				LOG.finer("Updating query analyzer statistics on the temporary ways table.");
				jdbcTemplate.update("ANALYZE bbox_ways");
			
			
				// child nodes
				do {
					LOG.finer("Selecting parent relations of selected nodes.");
					rowCount = jdbcTemplate.update(
						"CREATE TEMPORARY TABLE bbox_nodes ON COMMIT DROP AS "
							+ " SELECT n2.* FROM nodes n2 INNER JOIN ("
							+ "    SELECT DISTINCT rm2.member_id AS memberId FROM relation_members rm2"
							+ "    INNER JOIN bbox_relations brm2 ON rm2.relation_id = brm2.id"
							+ "    WHERE rm2.member_type = 'N' "
					        + "    GROUP BY rm2.member_id"
							+ " ) rids2 ON n2.id = rids2.memberId"
					);
					LOG.finer(rowCount + " rows inserted into bbox_nodes by completeRelations.");
				} while (rowCount > 0);
				
				LOG.finer("Adding a primary key to the temporary nodes table.");
				jdbcTemplate.update("ALTER TABLE ONLY bbox_nodes ADD CONSTRAINT pk_bbox_nodes PRIMARY KEY (id)");
				
				LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
				jdbcTemplate.update("ANALYZE bbox_nodes");
				
			}//eof if

			
		}//eof completeRelations
		
		// If complete ways is set, select all nodes contained by the ways into the node temp table.
		if (completeWays) {
			LOG.finer("3.1 Selecting all nodes for selected ways.");
			
			rowCount = jdbcTemplate.update(
					"INSERT INTO bbox_nodes "
					+ " SELECT n3.* FROM nodes n3 INNER JOIN ("
					+ " SELECT DISTINCT wn3.node_id AS nodeId FROM way_nodes wn3 "
					+ " INNER JOIN bbox_ways bbw3 ON wn3.way_id = bbw3.id AND NOT EXISTS ("
				    + " SELECT * FROM bbox_nodes bn3 WHERE wn3.node_id=bn3.id"
					+ " ) GROUP BY wn3.node_id"
					+ " ) wnds3 ON n3.id = wnds3.nodeId"
					);
			
			
			LOG.finer("Updating query analyzer statistics on the temporary bbox_nodes table.");
			jdbcTemplate.update("ANALYZE bbox_nodes");
			
			
			rowCount = jdbcTemplate.queryForInt(
					"SELECT count(*) from bbox_nodes");				
			LOG.info("-----3.3 bbox_nodes= return_code=" + rowCount);
					
		}//eof completeWays
		
		
		// Create iterators for the selected records for each of the entity types.
		resultSets = new ArrayList<ReleasableIterator<EntityContainer>>();
		resultSets.add(
				new UpcastIterator<EntityContainer, BoundContainer>(
						new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		resultSets.add(
				new UpcastIterator<EntityContainer, NodeContainer>(
						new NodeContainerIterator(nodeDao.iterate("bbox_"))));
	
		resultSets.add(
				new UpcastIterator<EntityContainer, WayContainer>(
						new WayContainerIterator(wayDao.iterate("bbox_"))));
		
		resultSets.add(
				new UpcastIterator<EntityContainer, RelationContainer>(
						new RelationContainerIterator(relationDao.iterate("bbox_"))));
		
		// Merge all readers into a single result iterator and return.			
		return new MultipleSourceIterator<EntityContainer>(resultSets);

		
	}//eof getRelationById
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReleasableIterator<EntityContainer> iterateBoundingBox2(
			double left, double right, double top, double bottom, boolean completeWays) {
		List<Bound> bounds;
		Point[] bboxPoints;
		Polygon bboxPolygon;
		int rowCount;
		List<ReleasableIterator<EntityContainer>> resultSets;
		boolean completeRelations = true;
		boolean isIncludeChildRelations = false; //true - it returns child relations and its relatives, normally it gets a big xml file for returning. 
		
		
		if (!initialized) {
			initialize();
		}
		
		
		// Build the bounds list.
		bounds = new ArrayList<Bound>();
		bounds.add(new Bound(right, left, top, bottom, "Osmosis " + OsmosisConstants.VERSION));
		
		// PostgreSQL sometimes incorrectly chooses to perform full table scans, these options
		// prevent this. Note that this is not recommended practice according to documentation
		// but fixing this would require modifying the table statistics gathering
		// configuration to produce better plans.
		jdbcTemplate.update("SET enable_seqscan = false");
		jdbcTemplate.update("SET enable_mergejoin = false");
		jdbcTemplate.update("SET enable_hashjoin = false");
		

		bboxPoints = new Point[5];
		bboxPoints[0] = new Point(left, bottom);
		bboxPoints[1] = new Point(left, top);
		bboxPoints[2] = new Point(right, top);
		bboxPoints[3] = new Point(right, bottom);
		bboxPoints[4] = new Point(left, bottom);
		bboxPolygon = polygonBuilder.createPolygon(bboxPoints);
		
		
		
		// Select all nodes inside the box into the node temp table.
		//---------------------------bbox_nodes-------------------------------------------------------
		LOG.finer("Selecting all nodes inside bounding box.");
		rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_nodes ON COMMIT DROP AS"
				+ " SELECT * FROM nodes WHERE (geom && ?)",
				new PGgeometry(bboxPolygon));
		LOG.finer("1. nodes: " + rowCount + " insert into bbox_nodes.");
		
		LOG.finer("Adding a primary key to the temporary nodes table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_nodes ADD CONSTRAINT pk_bbox_nodes PRIMARY KEY (id)");

		LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
		jdbcTemplate.update("ANALYZE bbox_nodes");
		
		//---------------------------bbox_ways-------------------------------------------------------
		// Select all ways inside the bounding box into the way temp table.
		LOG.info("2. isWayLinestringSupported=" + capabilityChecker.isWayLinestringSupported() +", isWayBboxSupported=" + capabilityChecker.isWayBboxSupported());
		/*if (capabilityChecker.isWayLinestringSupported()) {
			LOG.info("Selecting all ways inside bounding box using way linestring geometry.");
			// We have full way geometry available so select ways
			// overlapping the requested bounding box.
			rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT * FROM ways WHERE (linestring && ?)",
					new PGgeometry(bboxPolygon));
			
		} else if (capabilityChecker.isWayBboxSupported()) {
			LOG.info("Selecting all ways inside bounding box using dynamically built"
					+ " way linestring with way bbox indexing.");
			// The inner query selects the way id and node coordinates for
			// all ways constrained by the way bounding box which is
			// indexed.
			// The middle query converts the way node coordinates into
			// linestrings.
			// The outer query constrains the query to the linestrings
			// inside the bounding box. These aren't indexed but the inner
			// query way bbox constraint will minimise the unnecessary data.
			rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT w.* FROM ("
					+ "SELECT c.id AS id, First(c.version) AS version, First(c.user_id) AS user_id,"
					+ " First(c.tstamp) AS tstamp, First(c.changeset_id) AS changeset_id, First(c.tags) AS tags,"
					+ " First(c.nodes) AS nodes, ST_MakeLine(c.geom) AS way_line FROM ("
					+ "SELECT w.*, n.geom AS geom FROM nodes n"
					+ " INNER JOIN way_nodes wn ON n.id = wn.node_id"
					+ " INNER JOIN ways w ON wn.way_id = w.id"
					+ " WHERE (w.bbox && ?) ORDER BY wn.way_id, wn.sequence_id"
					+ ") c "
					+ "GROUP BY c.id"
					+ ") w "
					+ "WHERE (w.way_line && ?)",
					new PGgeometry(bboxPolygon),
					new PGgeometry(bboxPolygon)
			);
			
		} else */{
			LOG.info("2. Selecting all way ids inside bounding box using already selected nodes.");
			// No way bbox support is available so select ways containing
			// the selected nodes.
			rowCount = jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox_ways ON COMMIT DROP AS"
					+ " SELECT w.* FROM ways w"
					+ " INNER JOIN ("
					+ " SELECT wn.way_id FROM way_nodes wn"
					+ " INNER JOIN bbox_nodes n ON wn.node_id = n.id GROUP BY wn.way_id"
					+ ") wids ON w.id = wids.way_id"
			);
		}
		LOG.finer("2. ways: " +rowCount + " inserted into bbox_ways.");
		
		LOG.finer("Adding a primary key to the temporary ways table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_ways ADD CONSTRAINT pk_bbox_ways PRIMARY KEY (id)");
		
		LOG.finer("Updating query analyzer statistics on the temporary ways table.");
		jdbcTemplate.update("ANALYZE bbox_ways");
		
		//---------------------------bbox_relations-------------------------------------------------------
		// Select all relations containing the nodes or ways into the relation table.
		LOG.finer("Selecting all relation ids containing selected nodes or ways.");
		rowCount = jdbcTemplate.update(
			"CREATE TEMPORARY TABLE bbox_relations ON COMMIT DROP AS"
				+ " SELECT r.* FROM relations r"
				+ " INNER JOIN ("
				+ "    SELECT relation_id FROM ("
				+ "        SELECT rm.relation_id AS relation_id FROM relation_members rm"
				+ "        INNER JOIN bbox_nodes n ON rm.member_id = n.id WHERE rm.member_type = 'N' "
				+ "        UNION "
				+ "        SELECT rm.relation_id AS relation_id FROM relation_members rm"
				+ "        INNER JOIN bbox_ways w ON rm.member_id = w.id WHERE rm.member_type = 'W'"
				+ "     ) rids GROUP BY relation_id"
				+ ") rids ON r.id = rids.relation_id"
		);
		LOG.finer("3. relations: " +rowCount + " insert into bbox_relations.");
		
		LOG.finer("Adding a primary key to the temporary relations table.");
		jdbcTemplate.update("ALTER TABLE ONLY bbox_relations ADD CONSTRAINT pk_bbox_relations PRIMARY KEY (id)");
		
		LOG.finer("Updating query analyzer statistics on the temporary relations table.");
		jdbcTemplate.update("ANALYZE bbox_relations");
		

		//create table bbox2_nodes and bbox2_way data structure
		jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox2_nodes ON COMMIT DROP AS "
					+ "SELECT n5.* FROM nodes n5 where n5.id = 0" );
		jdbcTemplate.update("ALTER TABLE ONLY bbox2_nodes ADD CONSTRAINT pk_bbox2_nodes PRIMARY KEY (id)");
		
		jdbcTemplate.update(
				"CREATE TEMPORARY TABLE bbox2_ways ON COMMIT DROP AS "
					+ "SELECT w5.* FROM ways w5 where w5.id = 0" );
		jdbcTemplate.update("ALTER TABLE ONLY bbox2_ways ADD CONSTRAINT pk_bbox2_ways PRIMARY KEY (id)");
		
		
		
		//---------------------------completeRelations--------------------------------------------------------------------------
		int numRelations = jdbcTemplate.queryForInt(
		"SELECT count(*) from bbox_relations");
		LOG.info("iterateBoundingBox2: 5. relation_rows=" + numRelations);
		
		if (completeRelations && (numRelations > 0)){
		
			if (isIncludeChildRelations) {
				//---------------------------child relations of bbox_relations, added into bbox_relations, bbox2_relations--------------
				//child relation ids
				rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE relation_ids_1 ON COMMIT DROP AS "
						+ "    SELECT rm0.member_id AS mid FROM relation_members rm0"
						+ "    INNER JOIN bbox_relations brm0 ON rm0.relation_id = brm0.id"
						+ "    WHERE rm0.member_type = 'R' "
						+ "    GROUP BY rm0.member_id"
						);
				
				jdbcTemplate.update("ALTER TABLE ONLY relation_ids_1 ADD CONSTRAINT pk_relation_ids_1 PRIMARY KEY (mid)");
				jdbcTemplate.update("ANALYZE relation_ids_1");
				
				//child relation ids without tag (boundary, administrative)	
				rowCount = jdbcTemplate.update(
					"CREATE TEMPORARY TABLE relation_ids_2 ON COMMIT DROP AS "
						+ "    SELECT rid10.mid AS mid FROM relation_ids_1 AS rid10 WHERE rid10.mid NOT IN ("
						+ "    SELECT r10.id FROM relations r10 WHERE r10.id=rid10.mid AND r10.tags @> hstore('boundary', 'administrative')"
						+ " )"
						);
				
				jdbcTemplate.update("ALTER TABLE ONLY relation_ids_2 ADD CONSTRAINT pk_relation_ids_2 PRIMARY KEY (mid)");
				jdbcTemplate.update("ANALYZE relation_ids_2");
				
				rowCount = jdbcTemplate.queryForInt("SELECT count(*) from relation_ids_2");				
				LOG.info("iterateBoundingBox2: 6.2 child_relation_rows_without_boundary=" + rowCount);
				
				//child relation
				do {
					LOG.finer("Selecting relations of selected relations.");
					rowCount = jdbcTemplate.update(
						"CREATE TEMPORARY TABLE bbox2_relations ON COMMIT DROP AS"
							+ " SELECT r0.* FROM relations r0 INNER JOIN "
							+ " relation_ids_2 ris0 ON ris0.mid  = r0.id"
					);
					
					
					jdbcTemplate.update(
							//"CREATE TEMPORARY TABLE bbox2_relations ON COMMIT DROP AS"
							"INSERT INTO bbox_relations "
								+ " SELECT br2.* FROM bbox2_relations br2 WHERE NOT EXISTS ("
								+ " SELECT * FROM bbox_relations bbrs WHERE br2.id = bbrs.id ) "
					);
					LOG.info("iterateBoundingBox2: 6.3 " + rowCount + " rows inserted child realtions into bbox2_relations.");
				} while (rowCount > 0);
	
				
				LOG.finer("Adding a primary key to the temporary nodes table.");
				jdbcTemplate.update("ALTER TABLE ONLY bbox2_relations ADD CONSTRAINT pk_bbox2_relations PRIMARY KEY (id)");
				LOG.finer("Updating query analyzer statistics on the temporary nodes table.");
				jdbcTemplate.update("ANALYZE bbox_relations");
				jdbcTemplate.update("ANALYZE bbox2_relations");
			
			}//eof isIncludeChildRelations
		
		
		//---------------------------nodes and ways is bbox_relations's member, added into bbox2_nodes, bbox2_ways-----------------------

		LOG.finer("add ways2 from relations and child realtions.");
		rowCount = jdbcTemplate.update(
			//"CREATE TEMPORARY TABLE bbox2_ways ON COMMIT DROP AS "
			"INSERT INTO bbox2_ways "
				+ "SELECT w2.* FROM ways w2 INNER JOIN ("
				+ "    SELECT rm2.member_id AS memberId FROM relation_members rm2"
				+ "    INNER JOIN bbox_relations brm2 ON rm2.relation_id = brm2.id"
				+ "    WHERE rm2.member_type = 'W' AND NOT EXISTS ("
				+ "        SELECT * FROM bbox_ways bw2 WHERE rm2.member_id = bw2.id"
				+ "    ) GROUP BY rm2.member_id"
				+ ") rids2 ON w2.id = rids2.memberId"
		);
		LOG.info("iterateBoundingBox2, 7.1 " + rowCount + " rows inserted into bbox2_ways by completeRelations.");
		
		jdbcTemplate.update("ANALYZE bbox2_ways");
	
	
		

		LOG.finer("add nodes2 from relations and child realtions..");
		rowCount = jdbcTemplate.update(
			 "INSERT INTO bbox2_nodes "
				+ "SELECT n3.* FROM nodes n3 INNER JOIN ("
				+ "    SELECT DISTINCT rm3.member_id AS memberId FROM relation_members rm3"
				+ "    INNER JOIN bbox_relations brm3 ON rm3.relation_id = brm3.id"
				+ "    WHERE rm3.member_type = 'N' AND NOT EXISTS ("
				+ "        SELECT * FROM bbox_nodes bn3 WHERE rm3.member_id = bn3.id"
				+ "    ) GROUP BY rm3.member_id"
				+ ") rids3 ON n3.id = rids3.memberId"
		);
		LOG.info("iterateBoundingBox2: 7.2 "+ rowCount + " rows inserted into bbox2_nodes by completeRelations, row=" + rowCount);		
		jdbcTemplate.update("ANALYZE bbox2_nodes");
				


		
		}//eof completeRelations
		
	
		//---------------------------completeWays-------------------------------------------------------
		// If complete ways is set, select all nodes contained by the ways into the node temp table.
		if (completeWays) {
			
			LOG.finer("3.1 Selecting all nodes for selected ways by completeWays.");
			
			//add nodes into bbox2_nodes from bbox2_ways
			rowCount = jdbcTemplate.update(
					"INSERT INTO bbox2_nodes "
					+ " SELECT n3.* FROM nodes n3 INNER JOIN ("
					+ " SELECT DISTINCT wn3.node_id AS nodeId FROM way_nodes wn3 "
					+ " INNER JOIN bbox2_ways bbw3 ON wn3.way_id = bbw3.id AND NOT EXISTS ("
				    + " SELECT * FROM bbox_nodes bn3 WHERE wn3.node_id=bn3.id"
					+ " ) GROUP BY wn3.node_id"
					+ " ) wnds3 ON n3.id = wnds3.nodeId"
					);
			
			LOG.info("iterateBoundingBox2: 8.1 "+ rowCount + " rows inserted into bbox2_nodes by completeWays from bbox2_ways.");
			jdbcTemplate.update("ANALYZE bbox2_nodes");
			
			
			//add nodes into bbox2_nodes from bbox_ways
			jdbcTemplate.update("CREATE TEMPORARY TABLE bbox_way_nodes (id bigint) ON COMMIT DROP");
			jdbcTemplate.queryForList("SELECT unnest_bbox_way_nodes()");

			
			
			//test
			int cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox_ways");
			LOG.warning("bbox_ways=" +cc);
			
			cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox_way_nodes");
			LOG.warning("bbox_way_nodes=" +cc);
			
			cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox_nodes");
			LOG.warning("bbox_nodes=" +cc);
			
			cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox2_nodes");
			LOG.warning("bbox2_nodes=" +cc);
			
			//test
			
			
			jdbcTemplate.update(
					"CREATE TEMPORARY TABLE bbox2_missing_way_nodes ON COMMIT DROP AS "
					+ "SELECT buwn.id FROM (SELECT DISTINCT bwn.id FROM bbox_way_nodes bwn) buwn "
					+ "WHERE NOT EXISTS ("
					+ "    SELECT * FROM bbox_nodes bn WHERE bn.id = buwn.id "
					+ "    UNION "
					+ "    SELECT * FROM bbox2_nodes bn2 WHERE bn2.id = buwn.id "
					+ ");"
			);
			
			//test
			cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox2_missing_way_nodes");
			LOG.warning("bbox2_missing_way_nodes=" +cc);
			//test
			
			
			jdbcTemplate.update("ALTER TABLE ONLY bbox2_missing_way_nodes "
					+ " ADD CONSTRAINT pk_bbox2_missing_way_nodes PRIMARY KEY (id)");
			jdbcTemplate.update("ANALYZE bbox2_missing_way_nodes");
			rowCount = jdbcTemplate.update("INSERT INTO bbox2_nodes "
					+ "SELECT n.* FROM nodes n INNER JOIN bbox2_missing_way_nodes bwn ON n.id = bwn.id;");
			LOG.info("iterateBoundingBox2: 8.2 ways:" + rowCount + " inserted into bbox2_nodes by completeWays from bbox_ways.");
			
			jdbcTemplate.update("ANALYZE bbox2_nodes");
			
			
			//test
			cc = jdbcTemplate.queryForInt("SELECT count(*) from bbox2_nodes");
			LOG.warning("bbox2_nodes=" +cc);
			//test
			
			
		}//eof completeWays
		
		
		// Create iterators for the selected records for each of the entity types.
		LOG.finer("Iterating over results.");
		resultSets = new ArrayList<ReleasableIterator<EntityContainer>>();
		resultSets.add(
				new UpcastIterator<EntityContainer, BoundContainer>(
						new BoundContainerIterator(new ReleasableAdaptorForIterator<Bound>(bounds.iterator()))));
		resultSets.add(
				new UpcastIterator<EntityContainer, NodeContainer>(
						new NodeContainerIterator(nodeDao.iterate("bbox2_"))));
		resultSets.add(
				new UpcastIterator<EntityContainer, WayContainer>(
						new WayContainerIterator(wayDao.iterate("bbox2_"))));
		
		if (isIncludeChildRelations && (numRelations > 0)){
			resultSets.add(
				new UpcastIterator<EntityContainer, RelationContainer>(
						new RelationContainerIterator(relationDao.iterate("bbox2_"))));
		}
		
		// Merge all readers into a single result iterator and return.			
		return new MultipleSourceIterator<EntityContainer>(resultSets);
	}//eof iterateBoundingBox2
}

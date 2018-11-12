package no.sysco.middleware.zipkin.dependencies.streaming.storage;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import no.sysco.middleware.zipkin.dependencies.streaming.DependencyStorage;
import zipkin2.DependencyLink;

public class CassandraDepedencyStorage implements DependencyStorage {

	final String keyspace;

	final Session session;

	public CassandraDepedencyStorage(String keyspace, String[] addresses) {
		this.keyspace = keyspace;
		var cluster = Cluster.builder().addContactPoints(addresses).build();
		this.session = cluster.connect();
	}

	@Override
	public void put(Long start, DependencyLink dependencyLink) {
		final var prepared = session.prepare(QueryBuilder.insertInto(keyspace, "dependency")
				.value("day", QueryBuilder.bindMarker("day"))
				.value("parent", QueryBuilder.bindMarker("parent"))
				.value("child", QueryBuilder.bindMarker("child"))
				.value("calls", QueryBuilder.bindMarker("calls"))
				.value("errors", QueryBuilder.bindMarker("errors")));
		final var bound = prepared.bind()
				.setDate("day", LocalDate.fromMillisSinceEpoch(start))
				.setString("parent", dependencyLink.parent())
				.setString("child", dependencyLink.child())
				.setLong("calls", dependencyLink.callCount());
		if (dependencyLink.errorCount() > 0L) {
			bound.setLong("errors", dependencyLink.errorCount());
		}
		session.execute(bound);
	}

}

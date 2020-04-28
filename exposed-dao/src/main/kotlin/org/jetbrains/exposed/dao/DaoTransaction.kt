package org.jetbrains.exposed.dao

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.AbstractTransaction
import org.jetbrains.exposed.sql.transactions.ITransaction
import java.util.concurrent.CopyOnWriteArrayList

abstract class DaoTransaction(db: Database, transactionIsolation: Int, outerTransaction: ITransaction?, currentStatement: PreparedStatementApi?, debug: Boolean, val entityCache: ICache = EntityCache()) :
		AbstractTransaction(db, transactionIsolation, outerTransaction, currentStatement, debug), ICache by entityCache {

	init {
		// We can't use "this" as an argument to a delegate during construction because it only becomes valid after a call to the super constructor
		// Delegating entity cache hides the cache concept entirely inside the transaction
		entityCache.transaction = this
	}

	private val entityEvents: MutableList<EntityChange> = CopyOnWriteArrayList<EntityChange>()

	fun registerChange(entityClass: EntityClass<*, Entity<*>>, entityId: EntityID<*>, changeType: EntityChangeType) {
		EntityChange(entityClass, entityId, changeType, id).let {
			if (entityEvents.lastOrNull() != it) {
				entityEvents.add(it)
			}
		}
	}

	fun alertSubscribers() {
		entityEvents.forEach { e ->
			entitySubscribers.forEach {
				it(e)
			}
		}
		entityEvents.clear()
	}

	fun registeredChanges() = entityEvents.toList()


}

/*
 * “Commons Clause” License, https://commonsclause.com/
 *
 * The Software is provided to you by the Licensor under the License,
 * as defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software:    NLPCraft
 * License:     Apache 2.0, https://www.apache.org/licenses/LICENSE-2.0
 * Licensor:    Copyright (C) 2018 DataLingvo, Inc. https://www.datalingvo.com
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.ds

import org.apache.ignite.cache.CachePeekMode
import org.apache.ignite.{IgniteAtomicSequence, IgniteCache}
import org.nlpcraft._
import org.nlpcraft.db.NCDbManager
import org.nlpcraft.db.postgres.NCPsql
import org.nlpcraft.ignite.NCIgniteNLPCraft
import org.nlpcraft.mdo._
import org.nlpcraft.notification.NCNotificationManager

import scala.collection.JavaConverters._
import scala.util.control.Exception.catching

/**
  * Data sources manager.
  */
object NCDsManager extends NCLifecycle("Data source manager") with NCIgniteNLPCraft{
    // Caches.
    @volatile private var dsCache: IgniteCache[Long, NCDataSourceMdo] = _
    @volatile private var dsSeq: IgniteAtomicSequence = _

    /**
      * Starts this manager.
      */
    override def start(): NCLifecycle = {
        ensureStopped()

        dsSeq = NCPsql.sqlNoTx {
            ignite.atomicSequence(
                "dsSeq",
                NCDbManager.getMaxColumnValue("ds_instance", "id").getOrElse(0),
                true
            )
        }

        dsCache = ignite.cache[Long, NCDataSourceMdo]("ds-cache")

        require(dsCache != null)

        dsCache.localLoadCache(null)

        super.start()
    }

    /**
      * Stops this manager.
      */
    override def stop(): Unit = {
        dsCache = null

        super.stop()
    }

    /**
      * Gets data source for given user ID.
      *
      * @param dsId User ID.
      */
    @throws[NCE]
    def getDataSource(dsId: Long): Option[NCDataSourceMdo] = {
        ensureStarted()

        catching(wrapIE) {
            dsCache.get(dsId) match {
                case null ⇒ None
                case ds ⇒ Some(ds)
            }
        }
    }

    /**
      * Updates given data source.
      *
      * @param dsId ID of the data source to update.
      * @param name Data source name.
      * @param shortDesc Data source description.
      */
    @throws[NCE]
    def updateDataSource(
        dsId: Long,
        name: String,
        shortDesc: String
    ): Unit = {
        ensureStarted()

        catching(wrapIE) {
            dsCache.get(dsId) match {
                case null ⇒ throw new NCE(s"Unknown data source ID: $dsId")
                case ds ⇒
                    dsCache.put(
                        dsId,
                        NCDataSourceMdo(
                            ds.id,
                            name,
                            shortDesc,
                            ds.modelId,
                            ds.modelName,
                            ds.modelVersion,
                            ds.modelConfig,
                            ds.createdOn
                        )
                    )

                    // Notification.
                    NCNotificationManager.addEvent("NC_DS_UPDATE",
                        "dsId" → dsId,
                        "name" → ds.name,
                        "desc" → ds.shortDesc
                    )
            }
        }
    }

    /**
      * Adds new data source.
      *
      * @param name Data source name.
      * @param desc Data source description.
      * @param mdlId Model ID.
      * @param mdlName Model name.
      * @param mdlVer Model version.
      * @param mdlCfg Model configuration.
      * @return
      */
    @throws[NCE]
    def addDataSource(
        name: String,
        desc: String,
        mdlId: String,
        mdlName: String,
        mdlVer: String,
        mdlCfg: Option[String]
    ): Long = {
        ensureStarted()

        val newDsId = dsSeq.incrementAndGet()

        catching(wrapIE) {
            dsCache.put(
                newDsId,
                NCDataSourceMdo(
                    newDsId,
                    name,
                    desc,
                    mdlId,
                    mdlName,
                    mdlVer,
                    mdlCfg
                )
            )
        }

        // Notification.
        NCNotificationManager.addEvent("NC_DS_ADD",
            "dsId" → newDsId,
            "name" → name,
            "desc" → desc,
            "modelId" → mdlId,
            "modelName" → mdlName,
            "mdlVer" → mdlVer,
            "mdlCfg" → mdlCfg
        )

        newDsId
    }

    /**
      * Deletes data source with given DI.
      *
      * @param dsId ID of the data source to delete.
      */
    @throws[NCE]
    def deleteDataSource(dsId: Long): Unit = {
        ensureStarted()

        catching(wrapIE) {
            dsCache.get(dsId) match {
                case null ⇒ throw new NCE(s"Unknown data source ID: $dsId")
                case ds ⇒
                    dsCache.remove(dsId)

                    // Notification.
                    NCNotificationManager.addEvent("NC_DS_DELETE",
                        "dsId" → dsId,
                        "name" → ds.name,
                        "desc" → ds.shortDesc,
                        "modelId" → ds.modelId,
                        "modelName" → ds.modelName,
                        "mdlVer" → ds.modelVersion,
                        "mdlCfg" → ds.modelConfig
                    )
            }

        }
    }

    /**
      * Gets the list of all data sources.
      */
    @throws[NCE]
    def getAllDataSources: Seq[NCDataSourceMdo] = {
        ensureStarted()

        catching(wrapIE) {
            dsCache.localEntries(CachePeekMode.ALL).asScala.toSeq.map(_.getValue)
        }
    }
}

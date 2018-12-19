/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *     _   ____      ______           ______
 *    / | / / /___  / ____/________ _/ __/ /_
 *   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 *  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
 * /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
 *        /_/
 */

package org.nlpcraft.mdo

import java.sql.Timestamp

import org.nlpcraft.db.postgres.NCPsql.Implicits.RsParser
import org.nlpcraft.mdo.impl._

/**
  * User MDO.
  */
@NCMdoEntity(table = "nc_user")
case class NCUserMdo(
    @NCMdoField(column = "id", pk = true) id: Long,
    @NCMdoField(column = "email") email: String,
    @NCMdoField(column = "first_name") firstName: String,
    @NCMdoField(column = "last_name") lastName: String,
    @NCMdoField(column = "avatar_url") avatarUrl: String,
    @NCMdoField(column = "passwd_salt") passwordSalt: String,
    @NCMdoField(column = "last_ds_id") lastDsId: Long,
    @NCMdoField(column = "is_admin") isAdmin: Boolean,

    // Base MDO.
    @NCMdoField(column = "created_on") createdOn: Timestamp,
    @NCMdoField(column = "last_modified_on") lastModifiedOn: Timestamp
) extends NCEntityMdo with NCAnnotatedMdo[NCUserMdo]

object NCUserMdo {
    implicit val x: RsParser[NCUserMdo] =
        NCAnnotatedMdo.mkRsParser(classOf[NCUserMdo])
}

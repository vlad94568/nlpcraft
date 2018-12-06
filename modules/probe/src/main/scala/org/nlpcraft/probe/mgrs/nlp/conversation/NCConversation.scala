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

package org.nlpcraft.probe.mgrs.nlp.conversation

import java.text.SimpleDateFormat
import java.util.{Date, HashSet ⇒ JHashSet, List ⇒ JList, Set ⇒ JSet}
import java.util.function.Predicate

import org.nlpcraft._
import org.nlpcraft.mdllib.utils.NCTokenUtils._
import org.nlpcraft.mdllib._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import org.nlpcraft.ascii.NCAsciiTable
import com.typesafe.scalalogging.LazyLogging

/**
  * Conversation as an ordered set of utterances.
  */
case class NCConversation(usrId: Long, dsId: Long) extends NCDebug with LazyLogging {
    // After 10 mins pause between questions we clear the STM.
    private final val CONV_CLEAR_DELAY = 10.minutes.toMillis
    
    // Timestamp format.
    private final val TSTAMP_FMT = new SimpleDateFormat("hh:mm:ss a")
    
    // Short-Term-Memory.
    private val stm = mutable.TreeSet.empty[NCConversationItem]
    private var ctx = new JHashSet[NCToken]()
    private var lastUpdateTstamp = System.currentTimeMillis()
    
    /**
      *
      * @param tokens
      * @param text
      * @param srvReqId
      * @param tstamp
      */
    case class NCConversationItem(
        tokens: JList[NCToken],
        text: String,
        srvReqId: String,
        tstamp: Long
    ) extends Ordered[NCConversationItem] {
        override def compare(that: NCConversationItem): Int = this.tstamp.compareTo(that.tstamp)
    }
    
    /**
      *
      */
    def update(): Unit = stm.synchronized {
        val now = System.currentTimeMillis()
    
        if (now - lastUpdateTstamp > CONV_CLEAR_DELAY) {
            if (!IS_PROBE_SILENT)
                logger.trace(s"Conversation reset by timeout [" +
                    s"usrId=$usrId, " +
                    s"dsId=$dsId" +
                s"]")
        
            stm.clear()
        }
    
        lastUpdateTstamp = now
    
        val map = mutable.HashMap.empty[String/*Token group.*/, mutable.Buffer[NCToken]]
    
        // Recalculate the context based on new STM.
        for (item ← stm)
            // NOTE:
            // (1) STM is a red-black tree and traversed in ascending time order (older first).
            // (2) Map update ensure that only the youngest tokens per each group are retained in the context.
            map ++= item.tokens.asScala.filter(t ⇒ !isFreeWord(t) && !isStopWord(t)).groupBy(
                tok ⇒ if (tok.getGroup == null) "" else tok.getGroup
            )
    
        ctx = new JHashSet[NCToken](map.values.flatten.asJavaCollection)
    }
    
    /**
      * Clears all tokens from this conversation satisfying given predicate.
      *
      * @param p Java-side predicate.
      */
    def clear(p: Predicate[NCToken]): Unit = stm.synchronized {
        for (item ← stm)
            item.tokens.removeIf(p)
    
        if (!IS_PROBE_SILENT)
            logger.trace(s"Manually cleared conversation for some tokens.")
    }
    
    /**
      *
      * @param p Scala-side predicate.
      */
    def clear(p: (NCToken) ⇒ Boolean): Unit =
        clear(new Predicate[NCToken] {
            override def test(t: NCToken): Boolean = p(t)
        })
    
    /**
      * Adds new item to the conversation.
      *
      * @param sen Sentence.
      * @param v Sentence's specific variant.
      */
    def addItem(sen: NCSentence, v: NCVariant): Unit = stm.synchronized {
        stm += NCConversationItem(
            v.getTokens,
            sen.getNormalizedText,
            sen.getServerRequestId,
            lastUpdateTstamp
        )
    
        if (!IS_PROBE_SILENT)
            logger.trace(s"Added new sentence to the conversation [" +
                s"usrId=$usrId, " +
                s"dsId=$dsId, " +
                s"text=${sen.getNormalizedText}" +
            s"]")
    }
    
    /**
      * Prints out ASCII table for current STM.
      */
    def ack(): Unit = stm.synchronized {
        val stmTbl = NCAsciiTable("Time", "Sentence", "Server Request ID")
        
        stm.foreach(item ⇒ stmTbl += (
            TSTAMP_FMT.format(new Date(item.tstamp)),
            item.text,
            item.srvReqId
        ))
    
        val ctxTbl = NCAsciiTable("Token ID", "Group", "Text", "Value", "From request")
    
        ctx.asScala.foreach(tok ⇒ ctxTbl += (
            tok.getId,
            tok.getGroup,
            getNormalizedText(tok),
            tok.getValue,
            tok.getServerRequestId
        ))
    
        logger.trace(s"Conversation history [usrId=$usrId, dsId=$dsId]:\n${stmTbl.toString}")
        logger.trace(s"Conversation tokens [usrId=$usrId, dsId=$dsId]:\n${ctxTbl.toString}")
    }
    
    /**
      * 
      * @return
      */
    def tokens: JSet[NCToken] = stm.synchronized {
        ctx
    }
}